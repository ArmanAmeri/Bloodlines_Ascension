package com.modishmonkee.bloodlinesascension.client.hud;

import com.modishmonkee.bloodlinesascension.client.ClientBloodState;
import com.modishmonkee.bloodlinesascension.util.ModColors;
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
 * The blood orb HUD element (PoE-style life globe, bottom-left) — rendered as
 * pixel art: everything snaps to a whole-GUI-pixel grid, the liquid is drawn
 * cell by cell from a posterized 3-shade blood palette, and the internal churn
 * animation steps at a fixed low rate instead of sliding smoothly. Matches
 * vanilla's 1 texture pixel = 1 GUI pixel density.
 *
 * Draw order (back to front):
 *   1. back plate  — placeholder dark pixel disc  → replaced by orb_back.png
 *   2. liquid      — procedural pixel cells (wave sim surface, this class)
 *   3. meniscus    — 1px bright surface row (procedural)
 *   4. glass ring  — placeholder pixel annulus    → replaced by orb_front.png
 */
@OnlyIn(Dist.CLIENT)
public class BloodOrbHudLayer implements LayeredDraw.Layer {

    /** Liquid area radius in GUI px. Matches the art spec (64x64 canvas, r=22). */
    public static final int LIQUID_RADIUS = 22;
    /** Distance of the orb's outer edge from the screen corner. */
    private static final int MARGIN = 10;
    /** GUI px per liquid cell. 1 = vanilla texture density; 2 = extra chunky. */
    private static final int PIXEL = 1;
    /** GUI px per noise blob — bigger = chunkier churn pattern. */
    private static final int NOISE_CELL = 3;
    /** Churn animation steps per second (pixel-art style stepped motion). */
    private static final float CHURN_FPS = 6f;

    // Posterized blood palette, light → deep, derived from the canonical mod colors
    private static final float[][] PALETTE = {
            ModColors.rgb(ModColors.mix(ModColors.BLOOD_BRIGHT, ModColors.BLOOD_DARK, 0.45f)), // churn highlights
            ModColors.rgb(ModColors.BLOOD_DARK),                                               // body
            ModColors.rgb(ModColors.mix(ModColors.BLOOD_DARK, ModColors.BLOOD_BLACK, 0.55f)),  // deep
    };
    private static final float[] COLOR_MENISCUS = ModColors.rgb(ModColors.BLOOD_BRIGHT);
    private static final float LIQUID_ALPHA = 0.96f;

    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(true);
        int screenHeight = guiGraphics.guiHeight();

        // Integer center so the whole orb sits on the pixel grid
        int cx = MARGIN + LIQUID_RADIUS;
        int cy = screenHeight - MARGIN - LIQUID_RADIUS;
        float time = (mc.level != null ? mc.level.getGameTime() % 240000L : 0) + partialTick;

        Matrix4f matrix = guiGraphics.pose().last().pose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder buf = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        drawBackPlate(buf, matrix, cx, cy);
        drawLiquid(buf, matrix, cx, cy, time, partialTick);
        drawPlaceholderRing(buf, matrix, cx, cy);

        BufferUploader.drawWithShader(buf.buildOrThrow());
        RenderSystem.disableBlend();
    }

    private static void cell(BufferBuilder buf, Matrix4f m, int x, int y, int size, float r, float g, float b, float a) {
        buf.addVertex(m, x, y, 0).setColor(r, g, b, a);
        buf.addVertex(m, x, y + size, 0).setColor(r, g, b, a);
        buf.addVertex(m, x + size, y + size, 0).setColor(r, g, b, a);
        buf.addVertex(m, x + size, y, 0).setColor(r, g, b, a);
    }

    // ── 1. placeholder back plate: blocky dark disc ─────────────────────────
    private void drawBackPlate(BufferBuilder buf, Matrix4f matrix, int cx, int cy) {
        int r = LIQUID_RADIUS;
        float[] back = ModColors.rgb(ModColors.BLOOD_BLACK);
        for (int y = -r; y < r; y += PIXEL) {
            for (int x = -r; x < r; x += PIXEL) {
                if (x * x + y * y <= r * r) {
                    cell(buf, matrix, cx + x, cy + y, PIXEL, back[0], back[1], back[2], 0.88f);
                }
            }
        }
    }

    // ── 2 + 3. the liquid, cell by cell ──────────────────────────────────────
    private void drawLiquid(BufferBuilder buf, Matrix4f matrix, int cx, int cy, float time, float partialTick) {
        float fill = ClientBloodState.getFillFraction();
        if (fill <= 0.005f) return;

        BloodWaveSim sim = ClientBloodState.WAVES;
        int r = LIQUID_RADIUS;
        float fillY = cy + r - 2f * r * fill;

        // Stepped churn animation: offsets advance CHURN_FPS times per second
        int step = (int) (time * CHURN_FPS / 20f);
        int scrollX = step;          // drifts sideways one noise cell per step
        int scrollY = -(step / 2);   // slow upward crawl

        for (int x = -r; x < r; x += PIXEL) {
            int chord = (int) Math.sqrt(r * r - (x + PIXEL * 0.5f) * (x + PIXEL * 0.5f));
            if (chord <= 0) continue;
            int top = cy - chord;
            int bottom = cy + chord;

            // Wave height for this pixel column, snapped to the pixel grid
            int simColumn = Mth.clamp((int) ((x + r) / (2f * r) * BloodWaveSim.COLUMNS), 0, BloodWaveSim.COLUMNS - 1);
            float wave = sim.sampleHeight(simColumn, partialTick);
            wave += Mth.sin(time * 0.11f + (x + r) * 0.28f) * 0.5f + Mth.sin(time * 0.047f + (x + r) * 0.11f) * 0.3f;
            wave *= Mth.clamp(4f * fill * (1f - fill) + 0.15f, 0f, 1f);
            int surface = Mth.clamp(Math.round(fillY + wave), top, bottom);
            // Snap to the cell grid so the surface moves in whole-pixel steps
            surface = cy - Math.floorDiv(cy - surface, PIXEL) * PIXEL;
            if (surface >= bottom) continue;

            // Meniscus: one bright pixel row at the surface
            cell(buf, matrix, cx + x, surface, PIXEL,
                    COLOR_MENISCUS[0], COLOR_MENISCUS[1], COLOR_MENISCUS[2], LIQUID_ALPHA);

            // Body: posterized noise cells below the surface
            for (int y = surface + PIXEL; y < bottom; y += PIXEL) {
                int nx = Math.floorDiv(cx + x, NOISE_CELL) + scrollX;
                int ny = Math.floorDiv(y, NOISE_CELL) + scrollY;
                float noise = LiquidNoiseField.sample(nx, ny);
                // Deeper liquid biases toward the darker shades
                float depth = (y - surface) / (float) (2 * r);
                int shade = noise + depth * 0.9f < 0.45f ? 0 : (noise + depth * 0.9f < 0.85f ? 1 : 2);
                float[] c = PALETTE[shade];
                cell(buf, matrix, cx + x, y, PIXEL, c[0], c[1], c[2], LIQUID_ALPHA);
            }
        }
    }

    // ── 4. placeholder glass ring: blocky annulus (replaced by orb_front.png) ─
    private void drawPlaceholderRing(BufferBuilder buf, Matrix4f matrix, int cx, int cy) {
        int inner = LIQUID_RADIUS;
        int outer = LIQUID_RADIUS + 2;
        float[] silver = ModColors.rgb(ModColors.SILVER);
        float[] shadow = ModColors.rgb(ModColors.SILVER_SHADOW);
        for (int y = -outer; y < outer; y += PIXEL) {
            for (int x = -outer; x < outer; x += PIXEL) {
                int d2 = x * x + y * y;
                if (d2 <= outer * outer && d2 > inner * inner) {
                    float[] c = d2 > (outer - 1) * (outer - 1) ? shadow : silver;
                    cell(buf, matrix, cx + x, cy + y, PIXEL, c[0], c[1], c[2], 0.95f);
                }
            }
        }
    }
}
