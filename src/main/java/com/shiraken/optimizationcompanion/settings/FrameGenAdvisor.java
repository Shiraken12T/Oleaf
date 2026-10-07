package com.shiraken.optimizationcompanion.settings;

import com.shiraken.optimizationcompanion.OleafClient;
import org.lwjgl.opengl.GL11;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects the GPU vendor and whether Lossless Scaling is installed so the
 * Frame Generation panel can point users at the tool that actually delivers
 * 2x/3x/4x frame generation (driver-level or OS-level, not in-game).
 */
public final class FrameGenAdvisor {
    /** Steam app id for Lossless Scaling. */
    public static final String LOSSLESS_SCALING_APP_ID = "993090";
    public static final String LOSSLESS_SCALING_STORE_URL =
            "https://store.steampowered.com/app/993090/Lossless_Scaling/";
    public static final String LOSSLESS_SCALING_STEAM_URL = "steam://run/993090";

    public enum GpuVendor {
        AMD,
        NVIDIA,
        INTEL,
        UNKNOWN
    }

    private static GpuVendor cachedVendor;
    private static String cachedRawVendor;
    private static Optional<Path> cachedLossless;

    private FrameGenAdvisor() {
    }

    /** Reads the GL vendor string. Must be called on the render thread. */
    public static GpuVendor gpuVendor() {
        if (cachedVendor != null) {
            return cachedVendor;
        }
        String raw = "";
        try {
            String vendor = GL11.glGetString(GL11.GL_VENDOR);
            String renderer = GL11.glGetString(GL11.GL_RENDERER);
            raw = ((vendor == null ? "" : vendor) + " " + (renderer == null ? "" : renderer)).trim();
        } catch (Exception exception) {
            OleafClient.LOGGER.debug("Could not read GL vendor", exception);
        }
        cachedRawVendor = raw.isEmpty() ? "Unknown GPU" : raw;
        String lower = raw.toLowerCase(Locale.ROOT);
        if (lower.contains("nvidia") || lower.contains("geforce") || lower.contains("rtx") || lower.contains("gtx")) {
            cachedVendor = GpuVendor.NVIDIA;
        } else if (lower.contains("amd") || lower.contains("radeon") || lower.contains("ati") || lower.contains("advanced micro")) {
            cachedVendor = GpuVendor.AMD;
        } else if (lower.contains("intel")) {
            cachedVendor = GpuVendor.INTEL;
        } else {
            cachedVendor = GpuVendor.UNKNOWN;
        }
        return cachedVendor;
    }

    public static String rawVendor() {
        if (cachedRawVendor == null) {
            gpuVendor();
        }
        return cachedRawVendor;
    }

    /** Short label for the driver-level FG feature that matches this GPU. */
    public static String driverFgName() {
        return switch (gpuVendor()) {
            case AMD -> "AMD Fluid Motion Frames (AFMF) in Adrenalin";
            case NVIDIA -> "NVIDIA Smooth Motion (app/driver)";
            case INTEL -> "Intel driver frame generation (limited)";
            case UNKNOWN -> "your GPU driver's frame generation (if available)";
        };
    }

    public static boolean isLosslessScalingInstalled() {
        return findLosslessScaling().isPresent();
    }

    public static Optional<Path> findLosslessScaling() {
        if (cachedLossless != null) {
            return cachedLossless;
        }
        try {
            for (Path steam : steamRoots()) {
                Path exe = steam.resolve(Paths.get("steamapps", "common", "Lossless Scaling", "LosslessScaling.exe"));
                if (Files.isRegularFile(exe)) {
                    cachedLossless = Optional.of(exe);
                    return cachedLossless;
                }
            }
        } catch (Exception exception) {
            OleafClient.LOGGER.debug("Lossless Scaling lookup failed", exception);
        }
        cachedLossless = Optional.empty();
        return cachedLossless;
    }

    private static List<Path> steamRoots() {
        List<Path> roots = new ArrayList<>();
        List<Path> defaults = List.of(
                Paths.get("C:", "Program Files (x86)", "Steam"),
                Paths.get("C:", "Program Files", "Steam")
        );
        for (Path candidate : defaults) {
            if (Files.isDirectory(candidate)) {
                roots.add(candidate);
            }
        }

        // Extra Steam libraries listed in libraryfolders.vdf.
        for (Path base : defaults) {
            Path vdf = base.resolve(Paths.get("steamapps", "libraryfolders.vdf"));
            if (Files.isRegularFile(vdf)) {
                try {
                    String content = Files.readString(vdf);
                    Matcher matcher = Pattern.compile("\"path\"\\s*\"([^\"]+)\"").matcher(content);
                    while (matcher.find()) {
                        String raw = matcher.group(1).replace("\\\\", "\\");
                        Path lib = Paths.get(raw);
                        if (Files.isDirectory(lib) && !roots.contains(lib)) {
                            roots.add(lib);
                        }
                    }
                } catch (Exception ignored) {
                    // best-effort parse only
                }
            }
        }
        return roots;
    }

    public static void invalidateCache() {
        cachedVendor = null;
        cachedRawVendor = null;
        cachedLossless = null;
    }
}
