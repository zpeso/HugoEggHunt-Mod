package w2d1.hugoegghunt.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import w2d1.hugoegghunt.Hugoegghunt;
import w2d1.hugoegghunt.config.SkullHuntConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Scans loaded chunks for skulls matching the halloween texture, but *throttled*:
 * a full world scan runs at most every {@link #SCAN_INTERVAL_MS} ms (or on demand),
 * with one transparent snapshot handed to the renderer each frame.
 */
public final class SkullScanner {

    public static final long SCAN_INTERVAL_MS = 500L;

    /** Immutable result for a single matching skull. */
    public record SkullTarget(BlockPos pos, boolean found, double distance) {}

    private static volatile List<SkullTarget> snapshot = List.of();
    private static volatile boolean dirty = true;
    private static long nextScan = 0L;

    private SkullScanner() {}

    public static List<SkullTarget> getSnapshot() {
        return snapshot;
    }

    /** Force the next tick to rescan (config change, block marked found, world change). */
    public static void invalidate() {
        dirty = true;
    }

    /** Called once per client tick; rescans only when due or dirty. */
    public static void tick(Minecraft mc) {
        if (mc.level == null || mc.player == null) {
            snapshot = List.of();
            nextScan = 0L;
            dirty = true;
            return;
        }
        boolean chunkDirty = Hugoegghunt.consumeDirtyFlag();
        if (chunkDirty) dirty = true;
        long now = System.currentTimeMillis();
        if (!dirty && now < nextScan) return;
        dirty = false;
        nextScan = now + SCAN_INTERVAL_MS;
        rescan(mc);
    }

    private static void rescan(Minecraft mc) {
        Level level = mc.level;
        double max = SkullHuntConfig.scanDistance;
        double maxSq = max * max;
        net.minecraft.world.phys.Vec3 cam = mc.gameRenderer.getMainCamera().position();
        net.minecraft.world.level.ChunkPos center = mc.player.chunkPosition();
        int range = (int) Math.ceil(max / 16.0) + 1;

        List<SkullTarget> found = new ArrayList<>();
        for (int cx = center.x - range; cx <= center.x + range; cx++) {
            for (int cz = center.z - range; cz <= center.z + range; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) continue;
                for (var entry : chunk.getBlockEntities().entrySet()) {
                    BlockPos pos = entry.getKey();
                    if (!(entry.getValue() instanceof net.minecraft.world.level.block.entity.SkullBlockEntity)) continue;

                    // cheap distance test before texture unpacking
                    double dx = pos.getX() + 0.5 - cam.x;
                    double dy = pos.getY() + 0.5 - cam.y;
                    double dz = pos.getZ() + 0.5 - cam.z;
                    double distSq = dx * dx + dy * dy + dz * dz;
                    if (distSq > maxSq) continue;

                    var texture = Hugoegghunt.getTexture(level, pos);
                    if (texture.isEmpty() || !texture.get().equals(Hugoegghunt.HALLOWEEN_SKULL_TEXTURE)) continue;

                    boolean isFound = Hugoegghunt.FOUND_SKULLS.contains(pos.immutable());
                    found.add(new SkullTarget(pos.immutable(), isFound, Math.sqrt(distSq)));
                }
            }
        }
        found.sort((a, b) -> Double.compare(a.distance(), b.distance()));
        snapshot = List.copyOf(found);
    }
}
