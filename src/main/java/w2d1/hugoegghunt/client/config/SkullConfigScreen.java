package w2d1.hugoegghunt.client.config;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import w2d1.hugoegghunt.Hugoegghunt;
import w2d1.hugoegghunt.client.SkullScanner;
import w2d1.hugoegghunt.config.SkullHuntConfig;

/**
 * Simple settings screen, built with vanilla widgets (no extra dependencies).
 */
public class SkullConfigScreen extends Screen {

    private final Screen parent;

    public SkullConfigScreen(Screen parent) {
        super(Component.literal("Hugo Egg Hunt"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int top = Math.max(50, this.height / 2 - 70);
        int colL = cx - 160, colR = cx + 10;
        int w = 150, h = 20, gap = 24;

        addRenderableWidget(toggleButton("Enabled", colL, top, w, h, SkullHuntConfig.enabled, v -> {
            SkullHuntConfig.enabled = v;
            SkullScanner.invalidate();
        }));
        addRenderableWidget(toggleButton("Tracers", colR, top, w, h, SkullHuntConfig.tracers, v -> {
            SkullHuntConfig.tracers = v;
            SkullScanner.invalidate();
        }));
        addRenderableWidget(toggleButton("Highlight boxes", colL, top + gap, w, h, SkullHuntConfig.boxes, v -> {
            SkullHuntConfig.boxes = v;
        }));
        addRenderableWidget(toggleButton("Distance fade", colR, top + gap, w, h, SkullHuntConfig.distanceFade, v -> {
            SkullHuntConfig.distanceFade = v;
        }));

        addRenderableWidget(new IntSlider(colL, top + gap * 2, w, h,
                "Tracer count", SkullHuntConfig.maxTracers, 1, 8,
                v -> SkullHuntConfig.maxTracers = v));
        addRenderableWidget(new IntSlider(colR, top + gap * 2, w, h,
                "Scan distance", SkullHuntConfig.scanDistance, 16, 128,
                v -> SkullHuntConfig.scanDistance = v));

        addRenderableWidget(Button.builder(
                        Component.literal("Target color: " + SkullHuntConfig.colorName(SkullHuntConfig.targetColor)),
                        b -> {
                            SkullHuntConfig.targetColor = SkullHuntConfig.nextColor(SkullHuntConfig.targetColor);
                            b.setMessage(Component.literal("Target color: " + SkullHuntConfig.colorName(SkullHuntConfig.targetColor)));
                        })
                .bounds(colL, top + gap * 3, w, h)
                .build());
        addRenderableWidget(Button.builder(
                        Component.literal("Found color: " + SkullHuntConfig.colorName(SkullHuntConfig.foundColor)),
                        b -> {
                            SkullHuntConfig.foundColor = SkullHuntConfig.nextColor(SkullHuntConfig.foundColor);
                            b.setMessage(Component.literal("Found color: " + SkullHuntConfig.colorName(SkullHuntConfig.foundColor)));
                        })
                .bounds(colR, top + gap * 3, w, h)
                .build());

        addRenderableWidget(Button.builder(
                        Component.literal("Reset found skulls (" + Hugoegghunt.FOUND_SKULLS.size() + ")")
                                .withStyle(ChatFormatting.RED),
                        b -> {
                            Hugoegghunt.FOUND_SKULLS.clear();
                            SkullScanner.invalidate();
                            b.setMessage(Component.literal("Reset found skulls (0)").withStyle(ChatFormatting.RED));
                        })
                .bounds(cx - 100, top + gap * 4 + 6, 200, h)
                .build());

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(cx - 100, this.height - 30, 200, h)
                .build());
    }

    private Button toggleButton(String label, int x, int y, int w, int h, boolean value, java.util.function.Consumer<Boolean> setter) {
        // Mutable holder so repeated clicks flip correctly
        final boolean[] state = {value};
        return Button.builder(message(label, state[0]), btn -> {
                    state[0] = !state[0];
                    setter.accept(state[0]);
                    btn.setMessage(message(label, state[0]));
                })
                .bounds(x, y, w, h)
                .build();
    }

    private static Component message(String label, boolean value) {
        Component state = value
                ? Component.translatable("options.on").withStyle(ChatFormatting.GREEN)
                : Component.translatable("options.off").withStyle(ChatFormatting.RED);
        return Component.literal(label + ": ").append(state);
    }

    @Override
    public void onClose() {
        SkullHuntConfig.save();
        if (parent != null) {
            this.minecraft.setScreen(parent);
        } else {
            super.onClose();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, Math.max(30, this.height / 2 - 90), 0xFFFFFFFF);
    }

    /** Minimal integer slider. */
    private static class IntSlider extends AbstractSliderButton {
        private final String label;
        private final int min, max;
        private final java.util.function.IntConsumer setter;

        IntSlider(int x, int y, int w, int h, String label, int value, int min, int max, java.util.function.IntConsumer setter) {
            super(x, y, w, h, Component.empty(), (value - (double) min) / (max - min));
            this.label = label;
            this.min = min;
            this.max = max;
            this.setter = setter;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            int value = current();
            setMessage(Component.literal(label + ": " + value));
            setter.accept(value);
        }

        @Override
        protected void applyValue() {
            int value = current();
            setter.accept(value);
        }

        private int current() {
            return (int) Math.round(min + (max - min) * this.value);
        }
    }
}
