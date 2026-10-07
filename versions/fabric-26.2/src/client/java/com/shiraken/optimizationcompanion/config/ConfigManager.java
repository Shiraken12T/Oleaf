package com.shiraken.optimizationcompanion.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("oleaf.json");

    private static CompanionConfig config = new CompanionConfig();

    private ConfigManager() {
    }

    public static CompanionConfig getConfig() {
        return config;
    }

    public static void load() {
        Path legacyPath = FabricLoader.getInstance().getConfigDir().resolve("optimization_companion.json");
        if (!Files.exists(CONFIG_PATH) && Files.exists(legacyPath)) {
            try {
                Files.copy(legacyPath, CONFIG_PATH);
            } catch (IOException ignored) {
                // Fall through to defaults if migration fails.
            }
        }

        if (!Files.exists(CONFIG_PATH)) {
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            CompanionConfig loaded = GSON.fromJson(reader, CompanionConfig.class);
            config = loaded == null ? new CompanionConfig() : loaded;
            config.sanitize();
        } catch (IOException | RuntimeException exception) {
            config = new CompanionConfig();
            save();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to save Oleaf config", exception);
        }
    }
}
