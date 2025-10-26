package cn.starlight.fabricproxy.mixin;

import cn.starlight.fabricproxy.FabricProxy;
import net.minecraft.util.UserCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(UserCache.class)
public class UserCacheMixin {
    @Shadow private boolean offlineMode;

    @Redirect(method = "getOfflinePlayerProfile",
            at = @At(value = "FIELD", target = "Lnet/minecraft/util/UserCache;offlineMode:Z"))
    private boolean overrideOfflineMode(UserCache instance) {
        if (FabricProxy.config.getBungeeCord())
        {
            return true;
        }

        return offlineMode;
    }
}
