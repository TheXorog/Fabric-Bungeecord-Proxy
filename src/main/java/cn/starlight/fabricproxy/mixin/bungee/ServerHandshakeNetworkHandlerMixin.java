package cn.starlight.fabricproxy.mixin.bungee;

import cn.starlight.fabricproxy.interfaces.BungeeClientConnection;
import cn.starlight.fabricproxy.mixin.ClientConnectionAccessor;
import com.google.gson.Gson;
import com.mojang.authlib.properties.Property;
import com.mojang.util.UUIDTypeAdapter;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.c2s.handshake.ConnectionIntent;
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket;
import net.minecraft.network.packet.s2c.login.LoginDisconnectS2CPacket;
import net.minecraft.server.network.ServerHandshakeNetworkHandler;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

import static cn.starlight.fabricproxy.FabricProxy.config;

@Mixin(ServerHandshakeNetworkHandler.class)
public class ServerHandshakeNetworkHandlerMixin {
    private static final Logger log = LoggerFactory.getLogger(ServerHandshakeNetworkHandlerMixin.class);
    private static final Gson gson = new Gson();

    @Shadow
    @Final
    private ClientConnection connection;

    @Inject(method = "login", at = @At(value = "HEAD"))
    private void onProcessHandshakeStart(HandshakeC2SPacket packet, boolean transfer, CallbackInfo ci) {
        try {
            if (config.getBungeeCord() && ConnectionIntent.LOGIN.equals(packet.intendedState())) {
                String[] split = packet.address().split("\00");
                if (split.length == 3 || split.length == 4) {
                    ((ClientConnectionAccessor) connection).setAddress(new java.net.InetSocketAddress(split[1], ((java.net.InetSocketAddress) connection.getAddress()).getPort()));

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

                    ((BungeeClientConnection) connection).setSpoofedUUID(uuid);

                    if (split.length == 4) {
                        ((BungeeClientConnection) connection).setSpoofedProfile(gson.fromJson(split[3], Property[].class));
                    }
                } else {
                    if (!config.getAllowBypassProxy()) {
                        Text disconnectMessage = Text.literal("If you wish to use IP forwarding, please enable it in your BungeeCord config as well!");
                        connection.send(new LoginDisconnectS2CPacket(disconnectMessage));
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
