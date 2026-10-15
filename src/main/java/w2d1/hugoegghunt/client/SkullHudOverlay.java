package w2d1.hugoegghunt.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import w2d1.hugoegghunt.Hugoegghunt;
import w2d1.hugoegghunt.config.SkullHuntConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal top-left HUD: mod state, target counts, transient "found!" flash.
 * Rendered after the vanilla in-game HUD via {@code GuiMixin}.
 */
public final class SkullHudOverlay {

    private SkullHudOverlay() {}

    public static void render(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        Font font = mc.font;
        List<Component> lines = new ArrayList<>();

        boolean on = SkullHuntConfig.enabled;
        Component title = Component.literal("[Hugo] ")
                .withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED)
                .append(Component.literal(on ? "Egg hunt active" : "Paused"));

        int targets = SkullScanner.getSnapshot().size();
        long foundTargets = SkullScanner.getSnapshot().stream().filter(t -> t.found()).count();
        Component counts = Component.literal("Skulls: ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal((targets - foundTargets) + " to find").withColor(SkullHuntConfig.targetColor))
                .append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal(foundTargets + " found").withColor(SkullHuntConfig.foundColor));

        Component hints = Component.literal("[" + SkullHuntClient.toggleKey.getTranslatedKeyMessage().getString() + "] toggle  ")
                .withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.literal("[" + SkullHuntClient.settingsKey.getTranslatedKeyMessage().getString() + "] settings")
                        .withStyle(ChatFormatting.DARK_GRAY));

        lines.add(title);
        lines.add(counts);
        if (on) lines.add(hints);
        if (SkullHuntClient.isFlashActive()) {
            lines.add(Component.translatable("text.hugoegghunt.found_flash")
                    .withStyle(ChatFormatting.GOLD));
        }

        int pad = 4;
        int x = 6, y = 6;
        int lineH = 11;
        int width = lines.stream().mapToInt(c -> font.width(c)).max().orElse(0);
        int height = lines.size() * lineH + pad;
        graphics.fill(x - 2, y - 2, x + width + pad, y + height, 0x60000000);

        int dy = y;
        for (Component line : lines) {
            graphics.drawString(font, line, x, dy, 0xFFFFFFFF, true);
            dy += lineH;
        }
    }
}
