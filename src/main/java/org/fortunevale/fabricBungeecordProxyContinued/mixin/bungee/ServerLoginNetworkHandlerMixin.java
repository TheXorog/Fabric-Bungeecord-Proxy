package org.fortunevale.fabricBungeecordProxyContinued.mixin.bungee;

import com.google.common.collect.ArrayListMultimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import net.minecraft.server.players.NameAndId;
import org.fortunevale.fabricBungeecordProxyContinued.FabricBungeecordProxyContinued;
import org.fortunevale.fabricBungeecordProxyContinued.interfaces.BungeeClientConnection;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(ServerLoginPacketListenerImpl.class)
public abstract class ServerLoginNetworkHandlerMixin {
    @Unique
    private static final Logger log = LoggerFactory.getLogger(ServerLoginNetworkHandlerMixin.class);
    @Unique
    private boolean bypassProxyBungee = false;
    @Shadow
    @Final
    private Connection connection;
    @Shadow
    private GameProfile authenticatedProfile;
    @Shadow
    @Final
    private MinecraftServer server;

    @Inject(method = "startClientVerification", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD, target = "net/minecraft/server/network/ServerLoginPacketListenerImpl.authenticatedProfile:Lcom/mojang/authlib/GameProfile;", shift = At.Shift.AFTER))
    private void initUuid(CallbackInfo ci) {
        try {
            if (FabricBungeecordProxyContinued.config.getBungeeCord()) {
                if (((BungeeClientConnection) connection).getSpoofedUUID() == null) {
                    bypassProxyBungee = true;
                    return;
                }

                if(FabricBungeecordProxyContinued.config.getAlwaysOfficialUUID()) {
                    Optional.of(this.server.services().nameToIdCache()).ifPresent(
                            userCache -> userCache.get(this.authenticatedProfile.name()).ifPresentOrElse(
                                    gameProfile -> this.authenticatedProfile = new GameProfile(gameProfile.id(), this.authenticatedProfile.name()),
                                    () -> this.authenticatedProfile = new GameProfile(((BungeeClientConnection) connection).getSpoofedUUID(), this.authenticatedProfile.name())
                            )
                    );

                    Optional<NameAndId> optional = this.server.services().nameToIdCache().get(this.authenticatedProfile.name());
                    optional.ifPresentOrElse(gameProfile -> {
                        this.authenticatedProfile = new GameProfile(gameProfile.id(), this.authenticatedProfile.name());
                    }, () -> {
                        this.authenticatedProfile = new GameProfile(((BungeeClientConnection) connection).getSpoofedUUID(), this.authenticatedProfile.name());
                    });
                }
                else {
                    this.authenticatedProfile = new GameProfile(((BungeeClientConnection) connection).getSpoofedUUID(), this.authenticatedProfile.name());
                }

                ArrayListMultimap<String, Property> mutableProps = ArrayListMultimap.create(); // empty, mutable
                for (Property property : ((BungeeClientConnection) connection).getSpoofedProfile()) {
                    mutableProps.put(property.name(), property);
                }
                this.authenticatedProfile = new GameProfile(this.authenticatedProfile.id(), this.authenticatedProfile.name(), new PropertyMap(mutableProps));
            }
        } catch (Exception e) {
            log.error("Failed to handle verification");
            log.error("Message: {}", e.getMessage());
            log.error("StackTrace: {}", (Object) e.getStackTrace());
            throw new RuntimeException(e);
        }
    }

    @Redirect(method = "handleHello", at = @At(value = "INVOKE", target = "net/minecraft/server/MinecraftServer.usesAuthentication()Z"))
    private boolean skipKeyPacket(MinecraftServer minecraftServer) {
        return (bypassProxyBungee || !FabricBungeecordProxyContinued.config.getBungeeCord()) && minecraftServer.usesAuthentication();
    }
}
