package org.fortunevale.fabricBungeecordProxyContinued;

import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;

public class FabricBungeecordProxyContinued implements ModInitializer {
    public static ModConfig config;
    private final Logger logger = LogManager.getLogger("FabricProxy");

    @Override
    public void onInitialize() {
            if (config == null) {
                var configFile = FabricLoader.getInstance().getConfigDir().resolve("FabricProxy.toml");
                if (!Files.exists(configFile)) {
                    config = new ModConfig();
                    try {
                        new TomlWriter().write(config, configFile.toFile());
                    } catch (IOException e) {
                        LogManager.getLogger().error("Init config failed.", e);
                    }
                } else {
                    config = new Toml().read(FabricLoader.getInstance().getConfigDir().resolve("FabricProxy.toml").toFile()).to(ModConfig.class);
                }
            }
    }
}
