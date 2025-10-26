package cn.starlight.fabricproxy.mixin.bungee;

import cn.starlight.fabricproxy.FabricProxy;
import cn.starlight.fabricproxy.interfaces.BungeeClientConnection;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.encryption.PlayerPublicKey;
import net.minecraft.network.encryption.SignatureVerifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.network.ServerLoginNetworkHandler;
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
import org.spongepowered.include.com.google.common.collect.ListMultimap;

import java.util.Optional;
import java.util.UUID;

@Mixin(ServerLoginNetworkHandler.class)
public abstract class ServerLoginNetworkHandlerMixin {
    private static final Logger log = LoggerFactory.getLogger(ServerLoginNetworkHandlerMixin.class);
    @Unique
    private boolean bypassProxyBungee = false;
    @Shadow
    @Final
    ClientConnection connection;
    @Shadow
    private GameProfile profile;
    @Shadow
    @Final
    MinecraftServer server;

    @Inject(method = "startVerify", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD, target = "Lnet/minecraft/server/network/ServerLoginNetworkHandler;profile:Lcom/mojang/authlib/GameProfile;", shift = At.Shift.AFTER))
    private void initUuid(CallbackInfo ci) {
        try {
            if (FabricProxy.config.getBungeeCord()) {
                if (((BungeeClientConnection) connection).getSpoofedUUID() == null) {
                    bypassProxyBungee = true;
                    return;
                }

                if(FabricProxy.config.getAlwaysOfficialUUID()) {
                    Optional.ofNullable(this.server.getApiServices().nameToIdCache()).ifPresent(
                            userCache -> userCache.findByName(this.profile.name()).ifPresentOrElse(
                                    gameProfile -> this.profile = new GameProfile(gameProfile.id(), this.profile.name()),
                                    () -> this.profile = new GameProfile(((BungeeClientConnection) connection).getSpoofedUUID(), this.profile.name())
                            )
                    );

                    Optional<PlayerConfigEntry> optional = this.server.getApiServices().nameToIdCache().findByName(this.profile.name());
                    optional.ifPresentOrElse(gameProfile -> {
                        this.profile = new GameProfile(gameProfile.id(), this.profile.name());
                    }, () -> {
                        this.profile = new GameProfile(((BungeeClientConnection) connection).getSpoofedUUID(), this.profile.name());
                    });
                }
                else {
                    this.profile = new GameProfile(((BungeeClientConnection) connection).getSpoofedUUID(), this.profile.name());
                }

                ArrayListMultimap<String, Property> mutableProps = ArrayListMultimap.create(); // empty, mutable
                for (Property property : ((BungeeClientConnection) connection).getSpoofedProfile()) {
                    mutableProps.put(property.name(), property);
                }
                this.profile = new GameProfile(this.profile.id(), this.profile.name(), new PropertyMap(mutableProps));
            }
        } catch (Exception e) {
            log.error("Failed to handle verification");
            log.error("Message: {}", e.getMessage());
            log.error("StackTrace: {}", (Object) e.getStackTrace());
            throw new RuntimeException(e);
        }
    }

    // 这部分代码似乎在1.19.4+已经不再需要了，mojang去掉了对公钥验证的相关逻辑，但以防万一，先留着
    /*
    @Redirect(method = "acceptPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerLoginNetworkHandler;getVerifiedPublicKey(Lnet/minecraft/network/encryption/PlayerPublicKey$PublicKeyData;Ljava/util/UUID;Lnet/minecraft/network/encryption/SignatureVerifier;Z)Lnet/minecraft/network/encryption/PlayerPublicKey;"))
    public PlayerPublicKey getVerifiedPublicKey(PlayerPublicKey.PublicKeyData publicKeyData, UUID playerUuid, SignatureVerifier servicesSignatureVerifier, boolean shouldThrowOnMissingKey){
        return null;
    }
     */

    @Redirect(method = "onHello", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;isOnlineMode()Z"))
    private boolean skipKeyPacket(MinecraftServer minecraftServer) {
        return (bypassProxyBungee || !FabricProxy.config.getBungeeCord()) && minecraftServer.isOnlineMode();
    }
}
