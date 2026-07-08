package com.modishmonkee.bloodlinesascension.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.List;

/**
 * A small mouse-wheel-scrollable list of text lines, clipped to a box with a
 * scissor. Draws text only — no background of its own, since it's meant to sit
 * inside a box that's already part of the screen's hand-drawn art (e.g. the
 * left-column stat box on {@code CharacterScreen}).
 */
public final class ScrollableStatBox {

    private static final int PAD_X = 1;
    private static final int PAD_Y = 4;
    private static final float TEXT_SCALE = TooltipBoxRenderer.FONT_SIZE_SMALL;
    private static final int LINE_SPACING = 2;
    private static final float SCROLL_SPEED = 10f;

    private float scroll = 0f;

    /** A label (left-aligned) and value (right-aligned) pair, e.g. "Health" / "20 / 20". */
    public record StatLine(Component label, Component value) {}

    /** Draws the clipped, scrolled list at the given screen-space box — label left, value right. */
    public void render(GuiGraphics g, Font font, List<StatLine> lines, int x, int y, int w, int h) {
        int lineHeight = Math.round(font.lineHeight * TEXT_SCALE) + LINE_SPACING;
        int contentHeight = lines.size() * lineHeight;
        int viewHeight = h - PAD_Y * 2;
        float maxScroll = Math.max(0, contentHeight - viewHeight);
        scroll = Mth.clamp(scroll, 0, maxScroll);

        g.enableScissor(x + PAD_X, y + PAD_Y, x + w - PAD_X, y + h - PAD_Y);
        int ty = y + PAD_Y - Math.round(scroll);
        for (StatLine line : lines) {
            g.pose().pushPose();
            g.pose().translate(x + PAD_X, ty, 0);
            g.pose().scale(TEXT_SCALE, TEXT_SCALE, 1f);
            g.drawString(font, line.label(), 0, 0, 0xFFFFFFFF, true);
            g.pose().popPose();

            // Not rounded: each line's width differs, so rounding independently would make
            // the right edge wobble by ±1px between lines. Keeping this a float cancels out
            // exactly against the width used below, landing every line on the same edge.
            float valueWidth = font.width(line.value()) * TEXT_SCALE;
            g.pose().pushPose();
            g.pose().translate(x + w - PAD_X - valueWidth, ty, 0);
            g.pose().scale(TEXT_SCALE, TEXT_SCALE, 1f);
            g.drawString(font, line.value(), 0, 0, 0xFFFFFFFF, true);
            g.pose().popPose();

            ty += lineHeight;
        }
        g.disableScissor();
    }

    /** @return true if the scroll happened inside this box's bounds (event consumed). */
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta, int x, int y, int w, int h) {
        if (mouseX < x || mouseX >= x + w || mouseY < y || mouseY >= y + h) return false;
        scroll -= (float) scrollDelta * SCROLL_SPEED;
        return true;
    }
}
