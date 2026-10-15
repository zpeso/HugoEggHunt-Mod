package w2d1.hugoegghunt.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import w2d1.hugoegghunt.Hugoegghunt;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Central, mutable client settings for the skull hunt.
 * Persisted as JSON in {@code config/hugoegghunt.json} together with the list of found skulls.
 */
public final class SkullHuntConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("hugoegghunt.json");

    // ── Behavior ─────────────────────────────────────────────────────
    /** Master switch for every visual overlay. */
    public static boolean enabled = true;
    /** Draw tracer lines from the camera to skulls. */
    public static boolean tracers = true;
    /** Draw thin highlight boxes around matching skulls. */
    public static boolean boxes = true;
    /** Hard cap of tracer lines drawn at once (targets are taken nearest-first). */
    public static int maxTracers = 3;
    /** Search radius in blocks around the camera. */
    public static int scanDistance = 64;
    /** Fade distant overlays instead of full brightness. */
    public static boolean distanceFade = true;

    // ── Colors (ARGB) ────────────────────────────────────────────────
    public static int targetColor = 0xFF30FF70; // soft green
    public static int foundColor = 0xFF30C0FF;  // calm blue, marks already found skulls

    private SkullHuntConfig() {}

    // ── Persistence ──────────────────────────────────────────────────

    private static class Data {
        boolean enabled = true;
        boolean tracers = true;
        boolean boxes = true;
        int maxTracers = 3;
        int scanDistance = 64;
        boolean distanceFade = true;
        int targetColor = 0xFF30FF70;
        int foundColor = 0xFF30C0FF;
        List<String> found = new ArrayList<>();
    }

    public static void save() {
        Data d = new Data();
        d.enabled = enabled;
        d.tracers = tracers;
        d.boxes = boxes;
        d.maxTracers = maxTracers;
        d.scanDistance = scanDistance;
        d.distanceFade = distanceFade;
        d.targetColor = targetColor;
        d.foundColor = foundColor;
        d.found = Hugoegghunt.FOUND_SKULLS.stream()
                .map(p -> p.getX() + "," + p.getY() + "," + p.getZ())
                .collect(Collectors.toList());
        try {
            FILES_CREATE_DIR();
            Files.writeString(FILE, GSON.toJson(d));
        } catch (IOException e) {
            Hugoegghunt.LOGGER.warn("Could not save config", e);
        }
    }

    private static void FILES_CREATE_DIR() throws IOException {
        Path parent = FILE.getParent();
        if (parent != null) Files.createDirectories(parent);
    }

    public static void load() {
        if (!Files.exists(FILE)) return;
        try {
            Data d = GSON.fromJson(Files.readString(FILE), Data.class);
            if (d == null) return;
            enabled = d.enabled;
            tracers = d.tracers;
            boxes = d.boxes;
            maxTracers = d.maxTracers;
            scanDistance = d.scanDistance;
            distanceFade = d.distanceFade;
            targetColor = d.targetColor;
            foundColor = d.foundColor;
            Hugoegghunt.FOUND_SKULLS.clear();
            for (String s : d.found) {
                String[] p = s.split(",");
                if (p.length == 3) {
                    try {
                        Hugoegghunt.FOUND_SKULLS.add(new net.minecraft.core.BlockPos(
                                Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2])));
                    } catch (NumberFormatException ignored) {}
                }
            }
        } catch (IOException e) {
            Hugoegghunt.LOGGER.warn("Could not read config", e);
        }
    }

    /** Cycle through a small palette of hex colors. Returns true when wrapped. */
    public static int nextColor(int current) {
        int[] palette = {0xFF30FF70, 0xFFFF30FF, 0xFF30C8FF, 0xFFFF7A30, 0xFFFF3050};
        int idx = -1;
        for (int i = 0; i < palette.length; i++) {
            if (palette[i] == current) { idx = i; break; }
        }
        return palette[(idx + 1) % palette.length];
    }

    public static String colorName(int argb) {
        int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
        if (g > r && g > b) return "Green";
        if (r > 200 && b > 200 && g < 120) return "Pink";
        if (b > r && b > g) return "Blue";
        if (r > 200 && g > 100 && g < 160 && b < 90) return "Orange";
        if (r > 200 && g < 90) return "Red";
        return "Custom";
    }
}
