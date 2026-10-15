package w2d1.hugoegghunt.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;
import w2d1.hugoegghunt.Hugoegghunt;
import w2d1.hugoegghunt.client.config.SkullConfigScreen;
import w2d1.hugoegghunt.config.SkullHuntConfig;

/**
 * Client entrypoint: registers keybinds + tick loop and owns the HUD state.
 */
public class SkullHuntClient implements ClientModInitializer {

    public static KeyMapping toggleKey;
    public static KeyMapping settingsKey;

    private static int lastFoundCount = -1;
    private static long foundFlashUntil = 0L;

    private static String lastLevelKey = "";

    @Override
    public void onInitializeClient() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.hugoegghunt.toggle", GLFW.GLFW_KEY_G, KeyMapping.Category.MISC));
        settingsKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.hugoegghunt.settings", GLFW.GLFW_KEY_H, KeyMapping.Category.MISC));

        SkullHuntConfig.load();

        ClientTickEvents.END_CLIENT_TICK.register(SkullHuntClient::onTick);
    }

    private static void onTick(Minecraft mc) {
        // Reset flashlight/HUD state on world or server change
        String levelKey = mc.level == null ? "" : mc.level.dimension().toString();
        if (!levelKey.equals(lastLevelKey)) {
            lastLevelKey = levelKey;
            SkullScanner.invalidate();
            foundFlashUntil = 0L;
        }

        while (toggleKey.consumeClick()) {
            SkullHuntConfig.enabled = !SkullHuntConfig.enabled;
            SkullHuntConfig.save();
            SkullScanner.invalidate();
            if (mc.player != null) {
                Component status = SkullHuntConfig.enabled
                        ? Component.translatable("text.hugoegghunt.toggled_on").withStyle(ChatFormatting.GREEN)
                        : Component.translatable("text.hugoegghunt.toggled_off").withStyle(ChatFormatting.RED);
                mc.player.displayClientMessage(status, true);
            }
        }

        while (settingsKey.consumeClick()) {
            if (mc.screen == null) {
                mc.setScreen(new SkullConfigScreen(null));
            }
        }

        // Throttled scan runs here (cheap: max 2x per second)
        SkullScanner.tick(mc);

        // "New skull found" feedback
        int count = Hugoegghunt.FOUND_SKULLS.size();
        if (lastFoundCount != -1 && count > lastFoundCount && mc.player != null) {
            mc.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
            foundFlashUntil = System.currentTimeMillis() + 2500;
        }
        lastFoundCount = count;
    }

    /** True while the "found!" flash should be visible on the HUD. */
    public static boolean isFlashActive() {
        return System.currentTimeMillis() < foundFlashUntil;
    }

    /** Remaining flash time in seconds, for alpha animation. */
    public static float flashAlpha() {
        long remaining = foundFlashUntil - System.currentTimeMillis();
        if (remaining <= 0) return 0f;
        float t = remaining / 2500f;
        // fade out over the last second
        return t < 0.4f ? t / 0.4f : 1f;
    }
}
