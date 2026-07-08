package com.modishmonkee.bloodlinesascension.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Draws a square texture as a classic 9-slice: four fixed-size corners drawn
 * 1:1, four edges stretched along their long axis, and a centre stretched in
 * both — so one small source image scales cleanly to any destination box.
 */
public final class NineSliceTexture {

    private NineSliceTexture() {}

    /**
     * @param texture square source texture, {@code texSize}×{@code texSize}
     * @param x       destination left
     * @param y       destination top
     * @param width   destination width
     * @param height  destination height
     * @param texSize source texture size (both dimensions)
     * @param border  corner size in source pixels, on all four sides
     */
    public static void draw(GuiGraphics g, ResourceLocation texture, int x, int y, int width, int height,
                             int texSize, int border) {
        int b = Math.min(border, Math.min(width, height) / 2);
        int inner = texSize - 2 * border;
        int innerW = Math.max(0, width - 2 * b);
        int innerH = Math.max(0, height - 2 * b);

        // Corners — 1:1, never stretched.
        blit(g, texture, x, y, b, b, 0, 0, texSize);
        blit(g, texture, x + width - b, y, b, b, texSize - border, 0, texSize);
        blit(g, texture, x, y + height - b, b, b, 0, texSize - border, texSize);
        blit(g, texture, x + width - b, y + height - b, b, b, texSize - border, texSize - border, texSize);

        // Edges — stretched along their long axis.
        blit(g, texture, x + b, y, innerW, b, border, 0, inner, border, texSize);
        blit(g, texture, x + b, y + height - b, innerW, b, border, texSize - border, inner, border, texSize);
        blit(g, texture, x, y + b, b, innerH, 0, border, border, inner, texSize);
        blit(g, texture, x + width - b, y + b, b, innerH, texSize - border, border, border, inner, texSize);

        // Centre — stretched both ways.
        blit(g, texture, x + b, y + b, innerW, innerH, border, border, inner, inner, texSize);
    }

    private static void blit(GuiGraphics g, ResourceLocation texture, int x, int y, int w, int h,
                              int u, int v, int texSize) {
        g.blit(texture, x, y, w, h, (float) u, (float) v, w, h, texSize, texSize);
    }

    private static void blit(GuiGraphics g, ResourceLocation texture, int x, int y, int w, int h,
                              int u, int v, int uSize, int vSize, int texSize) {
        g.blit(texture, x, y, w, h, (float) u, (float) v, uSize, vSize, texSize, texSize);
    }
}
