package com.modishmonkee.bloodlinesascension.client.hud;

import com.modishmonkee.bloodlinesascension.client.ClientBloodState;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

/**
 * The blood orb HUD element (PoE-style life globe, bottom-left).
 *
 * Draw order (back to front):
 *   1. back plate  — placeholder dark disc   → replaced by orb_back.png
 *   2. liquid      — procedural: wave-sim surface + scrolling noise (this class)
 *   3. meniscus    — bright band along the liquid surface (procedural)
 *   4. glass ring  — placeholder annulus      → replaced by orb_front.png
 *
 * The frame art (back plate / glass front / panel) is hand-drawn and swapped in
 * later; only the placeholder ring and disc get deleted then. Liquid stays code.
 */
@OnlyIn(Dist.CLIENT)
public class BloodOrbHudLayer implements LayeredDraw.Layer {

    /** Liquid area radius in GUI px. Matches the art spec (64x64 canvas, r=22). */
    public static final int LIQUID_RADIUS = 22;
    /** Distance of the orb's outer edge from the screen corner. */
    private static final int MARGIN = 10;

    // Blood palette (vertex tint over the grayscale noise)
    private static final float[] COLOR_DEEP = {0.32f, 0.015f, 0.045f};
    private static final float[] COLOR_SURFACE = {0.62f, 0.06f, 0.09f};
    private static final float[] COLOR_MENISCUS = {0.85f, 0.16f, 0.18f};

    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(true);
        int screenHeight = guiGraphics.guiHeight();

        float cx = MARGIN + LIQUID_RADIUS;
        float cy = screenHeight - MARGIN - LIQUID_RADIUS;
        float time = (mc.level != null ? mc.level.getGameTime() % 240000L : 0) + partialTick;

        Matrix4f matrix = guiGraphics.pose().last().pose();

        drawBackPlate(matrix, cx, cy);
        drawLiquid(matrix, cx, cy, time, partialTick);
        drawPlaceholderRing(matrix, cx, cy);
    }

    // ── 1. placeholder back plate ────────────────────────────────────────────
    private void drawBackPlate(Matrix4f matrix, float cx, float cy) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder buf = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        buf.addVertex(matrix, cx, cy, 0).setColor(0.08f, 0.01f, 0.02f, 0.85f);
        int segments = 40;
        for (int s = 0; s <= segments; s++) {
            float angle = (float) (s * 2 * Math.PI / segments);
            buf.addVertex(matrix, cx + Mth.cos(angle) * LIQUID_RADIUS, cy + Mth.sin(angle) * LIQUID_RADIUS, 0)
                    .setColor(0.10f, 0.01f, 0.03f, 0.85f);
        }
        BufferUploader.drawWithShader(buf.buildOrThrow());
    }

    // ── 2 + 3. the liquid ────────────────────────────────────────────────────
    private void drawLiquid(Matrix4f matrix, float cx, float cy, float time, float partialTick) {
        float fill = ClientBloodState.getFillFraction();
        if (fill <= 0.005f) return;

        BloodWaveSim sim = ClientBloodState.WAVES;
        int r = LIQUID_RADIUS;
        // Fill line: bottom of orb at fill=0, top at fill=1
        float fillY = cy + r - 2f * r * fill;

        int columns = BloodWaveSim.COLUMNS;
        float[] xs = new float[columns + 1];
        float[] surfaceYs = new float[columns + 1];
        float[] bottomYs = new float[columns + 1];

        for (int i = 0; i <= columns; i++) {
            float x = cx - r + (2f * r * i) / columns;
            float dx = x - cx;
            float chord = (float) Math.sqrt(Math.max(0, (float) r * r - dx * dx));
            float top = cy - chord;
            float bottom = cy + chord;

            int simColumn = Math.min(i, columns - 1);
            float wave = sim.sampleHeight(simColumn, partialTick);
            // Gentle ever-present idle motion so the surface never looks dead
            wave += Mth.sin(time * 0.11f + i * 0.55f) * 0.5f + Mth.sin(time * 0.047f + i * 0.21f) * 0.3f;
            // Waves flatten out as the orb approaches empty/full (no room to slosh)
            wave *= Mth.clamp(4f * fill * (1f - fill) + 0.15f, 0f, 1f);

            xs[i] = x;
            surfaceYs[i] = Mth.clamp(fillY + wave, top, bottom);
            bottomYs[i] = bottom;
        }

        // Liquid body — textured strip, two scrolling noise layers baked into UV drift
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, LiquidNoiseTexture.get());
        float texScale = 48f; // GUI px per noise tile
        float scrollX = time * 0.12f, scrollY = time * 0.05f;

        BufferBuilder buf = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (int i = 0; i <= columns; i++) {
            float depth = (bottomYs[i] - surfaceYs[i]) / (2f * r); // 0 shallow .. 1 deep
            float sr = COLOR_SURFACE[0], sg = COLOR_SURFACE[1], sb = COLOR_SURFACE[2];
            float dr = COLOR_DEEP[0], dg = COLOR_DEEP[1], db = COLOR_DEEP[2];
            buf.addVertex(matrix, xs[i], surfaceYs[i], 0)
                    .setUv((xs[i] + scrollX) / texScale, (surfaceYs[i] + scrollY) / texScale)
                    .setColor(sr, sg, sb, 0.93f);
            buf.addVertex(matrix, xs[i], bottomYs[i], 0)
                    .setUv((xs[i] - scrollX * 0.6f) / texScale, (bottomYs[i] + scrollY * 1.4f) / texScale)
                    .setColor(Mth.lerp(depth, sr, dr), Mth.lerp(depth, sg, dg), Mth.lerp(depth, sb, db), 0.96f);
        }
        BufferUploader.drawWithShader(buf.buildOrThrow());

        // Meniscus — thin bright band hugging the surface
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder band = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i <= columns; i++) {
            float bandBottom = Math.min(surfaceYs[i] + 1.6f, bottomYs[i]);
            band.addVertex(matrix, xs[i], surfaceYs[i], 0)
                    .setColor(COLOR_MENISCUS[0], COLOR_MENISCUS[1], COLOR_MENISCUS[2], 0.9f);
            band.addVertex(matrix, xs[i], bandBottom, 0)
                    .setColor(COLOR_MENISCUS[0], COLOR_MENISCUS[1], COLOR_MENISCUS[2], 0.0f);
        }
        BufferUploader.drawWithShader(band.buildOrThrow());
    }

    // ── 4. placeholder glass ring (replaced by orb_front.png) ───────────────
    private void drawPlaceholderRing(Matrix4f matrix, float cx, float cy) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        int segments = 48;
        float inner = LIQUID_RADIUS - 0.5f;
        float outer = LIQUID_RADIUS + 1.8f;
        BufferBuilder buf = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        for (int s = 0; s <= segments; s++) {
            float angle = (float) (s * 2 * Math.PI / segments);
            float cos = Mth.cos(angle), sin = Mth.sin(angle);
            buf.addVertex(matrix, cx + cos * outer, cy + sin * outer, 0).setColor(0.75f, 0.72f, 0.70f, 0.95f);
            buf.addVertex(matrix, cx + cos * inner, cy + sin * inner, 0).setColor(0.45f, 0.42f, 0.42f, 0.95f);
        }
        BufferUploader.drawWithShader(buf.buildOrThrow());
        RenderSystem.disableBlend();
    }
}
