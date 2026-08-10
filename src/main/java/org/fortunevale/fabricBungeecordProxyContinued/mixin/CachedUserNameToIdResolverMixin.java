package org.fortunevale.fabricBungeecordProxyContinued.mixin;

import net.minecraft.server.players.CachedUserNameToIdResolver;
import org.fortunevale.fabricBungeecordProxyContinued.FabricBungeecordProxyContinued;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CachedUserNameToIdResolver.class)
public class CachedUserNameToIdResolverMixin {
    @Shadow private boolean resolveOfflineUsers;

    @Redirect(method = "createUnknownProfile",
            at = @At(value = "FIELD", target = "net/minecraft/server/players/CachedUserNameToIdResolver.resolveOfflineUsers:Z"))
    private boolean overrideOfflineMode(CachedUserNameToIdResolver instance) {
        if (FabricBungeecordProxyContinued.config.getBungeeCord())
        {
            return true;
        }

        return resolveOfflineUsers;
    }
}
