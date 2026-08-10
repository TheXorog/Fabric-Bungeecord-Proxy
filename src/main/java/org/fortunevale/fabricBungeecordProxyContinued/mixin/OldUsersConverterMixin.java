package org.fortunevale.fabricBungeecordProxyContinued.mixin;

import net.minecraft.server.players.OldUsersConverter;
import org.fortunevale.fabricBungeecordProxyContinued.FabricBungeecordProxyContinued;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(OldUsersConverter.class)
public class OldUsersConverterMixin {
    @Redirect(method = "lookupPlayers", at = @At(value = "INVOKE", target = "net/minecraft/server/MinecraftServer.usesAuthentication()Z"))
    private static boolean lookupProfile(MinecraftServer minecraftServer) {
        if (FabricBungeecordProxyContinued.config.getBungeeCord()) {
            return true;
        }

        return minecraftServer.usesAuthentication();
    }
}
