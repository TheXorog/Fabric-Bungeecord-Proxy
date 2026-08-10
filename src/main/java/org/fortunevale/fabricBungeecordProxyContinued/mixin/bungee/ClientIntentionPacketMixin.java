package org.fortunevale.fabricBungeecordProxyContinued.mixin.bungee;

import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import org.fortunevale.fabricBungeecordProxyContinued.FabricBungeecordProxyContinued;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(ClientIntentionPacket.class)
public abstract class ClientIntentionPacketMixin {
    @ModifyConstant(method = "<init>(Lnet/minecraft/network/FriendlyByteBuf;)V", constant = @Constant(intValue = 255))
    private static int readStringSize(int i) {
        if (FabricBungeecordProxyContinued.config.getBungeeCord()) {
            return Short.MAX_VALUE;
        }

        return i;
    }
}
