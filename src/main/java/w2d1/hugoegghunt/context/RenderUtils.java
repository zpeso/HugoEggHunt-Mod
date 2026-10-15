package w2d1.hugoegghunt.context;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import w2d1.hugoegghunt.client.SkullScanner;
import w2d1.hugoegghunt.client.render.XrayRenderTypes;
import w2d1.hugoegghunt.config.SkullHuntConfig;

import java.util.List;

/**
 * Clean, minimal draw utilities for the skull hunt overlay.
 *
 * Design:
 * - ONE vertex batch per frame instead of a batch per line.
 * - Thin box outlines so many highlights stay readable.
 * - Tracers are drawn only to the closest {@code maxTracers} unfound skulls.
 * - Overlays fade out toward the scan distance so the screen does not turn into noise.
 */
public final class RenderUtils {

    private static final float MIN_ALPHA = 0.25f;
    private static final float FULL_ALPHA_RANGE = 24f; // distance (blocks) inside which alpha stays full

    private RenderUtils() {}

    private static MultiBufferSource.BufferSource buffers() {
        return Minecraft.getInstance().renderBuffers().bufferSource();
    }

    /** Applies {@code scale} (0..1) to the alpha channel of an ARGB color. */
    public static int scaleAlpha(int argb, float scale) {
        int a = (int) (((argb >>> 24) & 0xFF) * scale);
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    /** Draws boxes (all targets) and tracers (closest N unfound) in a single batch. */
    public static void renderSkullTargets(PoseStack ms, float tickProgress) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || !SkullHuntConfig.enabled) return;

        List<SkullScanner.SkullTarget> targets = SkullScanner.getSnapshot();
        if (targets.isEmpty()) return;

        boolean anyVisible = false;
        for (SkullScanner.SkullTarget t : targets) {
            if (SkullHuntConfig.boxes || (SkullHuntConfig.tracers && !t.found())) {
                anyVisible = true;
                break;
            }
        }
        if (!anyVisible) return;

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 origin = tracerOrigin(camera, mc.player.getViewVector(tickProgress));

        RenderType layer = XrayRenderTypes.LINES_NO_DEPTH_TEST;
        VertexConsumer buf = buffers().getBuffer(layer);

        int tracersDrawn = 0;
        for (SkullScanner.SkullTarget t : targets) {
            int color = scaleAlpha(
                    t.found() ? SkullHuntConfig.foundColor : SkullHuntConfig.targetColor,
                    alphaForDistance(t.distance()));

            if (SkullHuntConfig.boxes) {
                AABB box = boxFor(mc, t.pos());
                drawBoxOutline(buf, ms, box, color);
            }

            boolean canTrace = SkullHuntConfig.tracers && !t.found() && tracersDrawn < SkullHuntConfig.maxTracers;
            if (canTrace) {
                drawLine(buf, ms, origin, Vec3.atCenterOf(t.pos()), color);
                tracersDrawn++;
            }
        }

        buffers().endBatch(layer);
    }

    /** Alpha 1 inside FULL_ALPHA_RANGE, linear fade to MIN_ALPHA at scan distance. */
    private static float alphaForDistance(double distance) {
        if (!SkullHuntConfig.distanceFade) return 1f;
        double max = SkullHuntConfig.scanDistance;
        double t = (distance - FULL_ALPHA_RANGE) / Math.max(1, max - FULL_ALPHA_RANGE);
        t = Math.max(0, Math.min(1, t));
        return (float) (1 - t * (1 - MIN_ALPHA));
    }

    private static AABB boxFor(Minecraft mc, BlockPos pos) {
        var state = mc.level.getBlockState(pos);
        return state.getShape(mc.level, pos).bounds().move(pos).inflate(0.002);
    }

    /** Slightly in front of the screen center so lines do not come from inside the player's head. */
    private static Vec3 tracerOrigin(Camera camera, Vec3 lookVec) {
        return camera.position().add(lookVec.scale(1.2));
    }

    private static Vec3 relativize(Vec3 world) {
        Vec3 cam = cameraPos();
        return new Vec3(world.x - cam.x, world.y - cam.y, world.z - cam.z);
    }

    private static Vec3 cameraPos() {
        return Minecraft.getInstance().gameRenderer.getMainCamera().position();
    }

    /** Draws all 12 edges of a box, already relativized to the camera inside. */
    public static void drawBoxOutline(VertexConsumer buf, PoseStack ms, AABB box, int color) {
        PoseStack.Pose pose = ms.last();
        Vec3 cam = cameraPos();
        float x1 = (float) (box.minX - cam.x), y1 = (float) (box.minY - cam.y), z1 = (float) (box.minZ - cam.z);
        float x2 = (float) (box.maxX - cam.x), y2 = (float) (box.maxY - cam.y), z2 = (float) (box.maxZ - cam.z);

        // Bottom
        line(buf, pose, x1, y1, z1, x2, y1, z1, color);
        line(buf, pose, x2, y1, z1, x2, y1, z2, color);
        line(buf, pose, x2, y1, z2, x1, y1, z2, color);
        line(buf, pose, x1, y1, z2, x1, y1, z1, color);
        // Top
        line(buf, pose, x1, y2, z1, x2, y2, z1, color);
        line(buf, pose, x2, y2, z1, x2, y2, z2, color);
        line(buf, pose, x2, y2, z2, x1, y2, z2, color);
        line(buf, pose, x1, y2, z2, x1, y2, z1, color);
        // Verticals
        line(buf, pose, x1, y1, z1, x1, y2, z1, color);
        line(buf, pose, x2, y1, z1, x2, y2, z1, color);
        line(buf, pose, x2, y1, z2, x2, y2, z2, color);
        line(buf, pose, x1, y1, z2, x1, y2, z2, color);
    }

    /** Line between two world-space points, auto relativized to the camera. */
    public static void drawLine(VertexConsumer buf, PoseStack ms, Vec3 start, Vec3 end, int color) {
        PoseStack.Pose pose = ms.last();
        Vec3 s = relativize(start);
        Vec3 e = relativize(end);
        lineVerts(buf, pose, (float) s.x, (float) s.y, (float) s.z,
                (float) e.x, (float) e.y, (float) e.z, color);
    }

    private static void line(VertexConsumer buf, PoseStack.Pose pose, float x1, float y1, float z1, float x2, float y2, float z2, int color) {
        lineVerts(buf, pose, x1, y1, z1, x2, y2, z2, color);
    }

    private static void lineVerts(VertexConsumer buf, PoseStack.Pose pose,
                                  float x1, float y1, float z1,
                                  float x2, float y2, float z2,
                                  int color) {
        float dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        float nx = len > 0 ? dx / len : 0f;
        float ny = len > 0 ? dy / len : 1f;
        float nz = len > 0 ? dz / len : 0f;

        buf.addVertex(pose, x1, y1, z1)
                .setColor(color)
                .setNormal(pose, nx, ny, nz)
                .setLineWidth(1.0f);
        buf.addVertex(pose, x2, y2, z2)
                .setColor(color)
                .setNormal(pose, nx, ny, nz)
                .setLineWidth(1.0f);
    }
}
