package com.modishmonkee.bloodlinesascension.client.gui;

import com.modishmonkee.bloodlinesascension.BloodlinesAscension;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * A small hover-tooltip box styled with the mod's hand-drawn {@code tooltipbox.png}
 * (16×16, 3px corners fading bright→dark red, drawn as a 9-slice — see
 * {@link NineSliceTexture}) instead of vanilla's flat-colour tooltip. Positioned
 * mouse-follow like vanilla item tooltips, clamped to stay on screen.
 */
public final class TooltipBoxRenderer {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(BloodlinesAscension.MOD_ID, "textures/gui/tooltipbox.png");
    private static final int TEX_SIZE = 16;
    private static final int BORDER = 3;
    private static final int PAD_X = 5;
    private static final int PAD_Y = 5;
    private static final int MOUSE_OFFSET = 12;
    private static final int LINE_SPACING = 2;

    // Named text scales (scaled pose, not separate font assets) — "medium" is
    // this mod's default UI text size; refer to these names, not raw numbers.
    public static final float FONT_SIZE_SMALL = 0.53125f;  // medium − 15%
    public static final float FONT_SIZE_MEDIUM = 0.625f;   // default
    public static final float FONT_SIZE_LARGE = 0.71875f;  // medium + 15%

    private static final float TEXT_SCALE = FONT_SIZE_MEDIUM;

    private TooltipBoxRenderer() {}

    public static void draw(GuiGraphics g, Font font, Component line, int mouseX, int mouseY,
                             int screenWidth, int screenHeight) {
        draw(g, font, List.of(line), mouseX, mouseY, screenWidth, screenHeight);
    }

    public static void draw(GuiGraphics g, Font font, List<Component> lines, int mouseX, int mouseY,
                             int screenWidth, int screenHeight) {
        if (lines.isEmpty()) return;

        int rawTextWidth = 0;
        for (Component line : lines) rawTextWidth = Math.max(rawTextWidth, font.width(line));
        int textWidth = Math.round(rawTextWidth * TEXT_SCALE);
        int lineHeight = Math.round(font.lineHeight * TEXT_SCALE);
        int boxWidth = textWidth + PAD_X * 2;
        int boxHeight = lines.size() * lineHeight + (lines.size() - 1) * LINE_SPACING + PAD_Y * 2;

        int x = mouseX + MOUSE_OFFSET;
        int y = mouseY - MOUSE_OFFSET;
        if (x + boxWidth > screenWidth) x = mouseX - MOUSE_OFFSET - boxWidth;
        if (y + boxHeight > screenHeight) y = screenHeight - boxHeight - 4;
        if (x < 4) x = 4;
        if (y < 4) y = 4;

        NineSliceTexture.draw(g, TEXTURE, x, y, boxWidth, boxHeight, TEX_SIZE, BORDER);

        int ty = y + PAD_Y;
        for (Component line : lines) {
            g.pose().pushPose();
            g.pose().translate(x + PAD_X, ty, 0);
            g.pose().scale(TEXT_SCALE, TEXT_SCALE, 1f);
            g.drawString(font, line, 0, 0, 0xFFFFFFFF, true);
            g.pose().popPose();
            ty += lineHeight + LINE_SPACING;
        }
    }
}
