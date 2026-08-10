package org.fortunevale.fabricBungeecordProxyContinued.mixin.bungee;

import com.google.gson.Gson;
import com.mojang.authlib.properties.Property;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.handshake.ClientIntent;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.network.protocol.login.ClientboundLoginDisconnectPacket;
import net.minecraft.server.network.ServerHandshakePacketListenerImpl;
import org.fortunevale.fabricBungeecordProxyContinued.FabricBungeecordProxyContinued;
import org.fortunevale.fabricBungeecordProxyContinued.interfaces.BungeeClientConnection;
import org.fortunevale.fabricBungeecordProxyContinued.mixin.ClientConnectionAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(ServerHandshakePacketListenerImpl.class)
public class ServerHandshakePacketListenerImplMixin {
    private static final Logger log = LoggerFactory.getLogger(ServerHandshakePacketListenerImpl.class);
    private static final Gson gson = new Gson();

    @Shadow
    @Final
    private Connection connection;

    @Inject(method = "handleIntention", at = @At(value = "HEAD"))
    private void onProcessHandshakeStart(ClientIntentionPacket packet, CallbackInfo ci) {
        try {
            if (FabricBungeecordProxyContinued.config.getBungeeCord() && ClientIntent.LOGIN.equals(packet.intention())) {
                String[] split = packet.hostName().split("\00");
                if (split.length == 3 || split.length == 4) {
                    ((ClientConnectionAccessor) connection).setAddress(new java.net.InetSocketAddress(split[1], ((java.net.InetSocketAddress) connection.getRemoteAddress()).getPort()));

                    String raw = split[2];
                    UUID uuid;

                    if (raw.contains("-")) {
                        uuid = UUID.fromString(raw);
                    } else if (raw.length() == 32) {
                        uuid = new UUID(
                                Long.parseUnsignedLong(raw.substring(0, 16), 16),
                                Long.parseUnsignedLong(raw.substring(16, 32), 16)
                        );
                    } else {
                        throw new IllegalArgumentException("Invalid UUID format: " + raw);
                    }

                    ((org.fortunevale.fabricBungeecordProxyContinued.interfaces.BungeeClientConnection) connection).setSpoofedUUID(uuid);

                    if (split.length == 4) {
                        ((BungeeClientConnection) connection).setSpoofedProfile(gson.fromJson(split[3], Property[].class));
                    }
                } else {
                    if (!FabricBungeecordProxyContinued.config.getAllowBypassProxy()) {
                        Component disconnectMessage = Component.literal("If you wish to use IP forwarding, please enable it in your BungeeCord config as well!");
                        connection.send(new ClientboundLoginDisconnectPacket(disconnectMessage));
                        connection.disconnect(disconnectMessage);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to handle login");
            log.error("Message: {}", e.getMessage());
            log.error("StackTrace: {}", (Object) e.getStackTrace());
            throw new RuntimeException(e);
        }
    }
}
