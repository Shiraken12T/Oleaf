package com.shiraken.optimizationcompanion;

import com.shiraken.optimizationcompanion.config.ConfigManager;
import com.shiraken.optimizationcompanion.hud.PerformanceOverlay;
import com.shiraken.optimizationcompanion.keybind.CompanionKeybinds;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OleafClient implements ClientModInitializer {
    public static final String MOD_ID = "oleaf";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        ConfigManager.load();
        CompanionKeybinds.register();
        PerformanceOverlay.register();
        LOGGER.info("Oleaf initialized");
    }
}
