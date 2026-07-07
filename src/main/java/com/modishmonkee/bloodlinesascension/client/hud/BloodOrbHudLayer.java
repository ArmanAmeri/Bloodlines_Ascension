package com.modishmonkee.bloodlinesascension.client.hud;

import com.modishmonkee.bloodlinesascension.BloodlinesAscension;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

/**
 * The blood orb HUD element (PoE-style life globe, bottom-left), rendered as
 * pixel-art cells from the canonical mod palette.
 *
 * Liquid look (Diablo-style sphere): the blood fills a visible 3D volume —
 * dark limb shading where the liquid meets the glass silhouette, and the
 * liquid's top plane drawn as a tilted ellipse whose front edge (bright
 * meniscus) rides the waves fully while the back edge lags, so the disc
 * tumbles as it sloshes. Solid opaque body, one lagging wave-line, sparse
 * drifting glints. Surface motion = wave sim + irregular noise swell.
 *
 * Draw order (back to front):
 *   1. back plate  — placeholder dark pixel disc  → replaced by orb_back.png
 *   2. liquid      — procedural swirl cells below the wave-sim surface
 *   3. meniscus    — bright surface row (procedural)
 *   4. glass ring  — placeholder pixel annulus    → replaced by orb_front.png
 */
@OnlyIn(Dist.CLIENT)
public class BloodOrbHudLayer implements LayeredDraw.Layer {

    // ModishMonkee's hand-drawn orb, three layers on the same 64x64 canvas
    // (ring 4px from canvas left/bottom). Sandwich: panel → orb_back → liquid → orb_front.
    /** Bottommost: the panel behind the orb. */
    private static final ResourceLocation PANEL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(BloodlinesAscension.MOD_ID, "textures/gui/panel.png");
    /** Dark glass interior, drawn behind the liquid. */
    private static final ResourceLocation BACK_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(BloodlinesAscension.MOD_ID, "textures/gui/orb_back.png");
    /** Ring + decorations, drawn over the liquid so waves tuck under the ring's inner edge. */
    private static final ResourceLocation FRONT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(BloodlinesAscension.MOD_ID, "textures/gui/orb_front.png");
    private static final int FRAME_SIZE = 64;
    /** Canvas offset from the screen's bottom-left corner (art bakes its own 4px inset). */
    private static final int CANVAS_MARGIN = 0;
    // Ring geometry within the 64x64 canvas — VERIFY against the real PNG once
    // it lands (measured estimates from the mockup; liquid must stay inside the ring)
    private static final int LIQUID_CENTER_X = 27;
    private static final int LIQUID_CENTER_Y = 37;
    /** Liquid area radius in GUI px. */
    public static final int LIQUID_RADIUS = 20;

    private static Boolean panelPresent, backPresent, frontPresent;
    /** Liquid cells per GUI pixel. 2 = half-pixel cells (finer); 1 = vanilla density. */
    private static final int SUB = 2;
    /** Thickness of the bright surface row, in GUI px. */
    private static final float MENISCUS_PX = 1.0f;

    // ── Diablo-style sphere look ─────────────────────────────────────────────
    /** Fraction of the radius where the dark silhouette shading (limb) begins. */
    private static final float LIMB = 0.80f;
    /** Max half-height of the surface ellipse (the visible liquid top plane), GUI px. */
    private static final float ELLIPSE_DEPTH = 3.0f;
    /** The back edge of the surface ellipse follows the waves this much (front = 1.0). */
    private static final float BACK_EDGE_CALM = 0.75f;
    /** Bright glints drifting through the upper liquid. */
    private static final float GLINT_THRESHOLD = 0.80f;
    private static final float GLINT_SPEED = 0.11f;

    // ── Idle surface swell (noise-driven — irregular, not sine-rigid) ────────
    // Kept subtle so the actual fill level stays readable at a glance
    /** Long slow swell height, GUI px. */
    private static final float SWELL_MAIN = 1.5f;
    /** Faster small ripple height, GUI px. */
    private static final float SWELL_RIPPLE = 0.8f;

    // Direct canonical palette, dark → bright
    private static final float[] SHADE_BLACK = ModColors.rgb(ModColors.BLOOD_BLACK);
    private static final float[] SHADE_DARK = ModColors.rgb(ModColors.BLOOD_DARK);
    private static final float[] SHADE_BRIGHT = ModColors.rgb(ModColors.BLOOD_BRIGHT);
    /** The visible liquid top plane — slightly lighter than the body. */
    private static final float[] SHADE_PLANE = ModColors.rgb(ModColors.mix(ModColors.BLOOD_DARK, ModColors.BLOOD_BRIGHT, 0.30f));
    private static final float LIQUID_ALPHA = 0.97f;

    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(true);
        int screenHeight = guiGraphics.guiHeight();

        int canvasX = CANVAS_MARGIN;
        int canvasY = screenHeight - FRAME_SIZE - CANVAS_MARGIN;
        int cx = canvasX + LIQUID_CENTER_X;
        int cy = canvasY + LIQUID_CENTER_Y;
        float time = (mc.level != null ? mc.level.getGameTime() % 240000L : 0) + partialTick;

        boolean hasPanel = present(PANEL_TEXTURE, panelPresent, v -> panelPresent = v);
        boolean hasBack = present(BACK_TEXTURE, backPresent, v -> backPresent = v);
        boolean hasFront = present(FRONT_TEXTURE, frontPresent, v -> frontPresent = v);

        // 1. Panel (bottommost), 2. interior behind the liquid
        if (hasPanel) {
            guiGraphics.blit(PANEL_TEXTURE, canvasX, canvasY, 0f, 0f, FRAME_SIZE, FRAME_SIZE, FRAME_SIZE, FRAME_SIZE);
        }
        if (hasBack) {
            guiGraphics.blit(BACK_TEXTURE, canvasX, canvasY, 0f, 0f, FRAME_SIZE, FRAME_SIZE, FRAME_SIZE, FRAME_SIZE);
        }
        if (hasPanel || hasBack) guiGraphics.flush();

        Matrix4f matrix = guiGraphics.pose().last().pose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder buf = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        if (!hasBack) drawBackPlate(buf, matrix, cx, cy);
        // 3. The liquid
        drawLiquid(buf, matrix, cx, cy, time, partialTick);
        if (!hasFront) drawPlaceholderRing(buf, matrix, cx, cy);

        BufferUploader.drawWithShader(buf.buildOrThrow());
        RenderSystem.disableBlend();

        // 4. Ring + decorations over the liquid
        if (hasFront) {
            guiGraphics.blit(FRONT_TEXTURE, canvasX, canvasY, 0f, 0f, FRAME_SIZE, FRAME_SIZE, FRAME_SIZE, FRAME_SIZE);
        }
    }

    /** Cached resource-presence check (falls back to placeholder drawing while absent). */
    private static boolean present(ResourceLocation texture, Boolean cached, java.util.function.Consumer<Boolean> store) {
        if (cached == null) {
            cached = Minecraft.getInstance().getResourceManager().getResource(texture).isPresent();
            store.accept(cached);
        }
        return cached;
    }

    private static void cell(BufferBuilder buf, Matrix4f m, float x, float y, float w, float h,
                             float r, float g, float b, float a) {
        buf.addVertex(m, x, y, 0).setColor(r, g, b, a);
        buf.addVertex(m, x, y + h, 0).setColor(r, g, b, a);
        buf.addVertex(m, x + w, y + h, 0).setColor(r, g, b, a);
        buf.addVertex(m, x + w, y, 0).setColor(r, g, b, a);
    }

    // ── 1. placeholder back plate: blocky dark disc ─────────────────────────
    private void drawBackPlate(BufferBuilder buf, Matrix4f matrix, int cx, int cy) {
        int r = LIQUID_RADIUS;
        for (int y = -r; y < r; y++) {
            for (int x = -r; x < r; x++) {
                if (x * x + y * y <= r * r) {
                    cell(buf, matrix, cx + x, cy + y, 1, 1,
                            SHADE_BLACK[0] * 0.6f, SHADE_BLACK[1] * 0.6f, SHADE_BLACK[2] * 0.6f, 0.9f);
                }
            }
        }
    }

    // ── 2 + 3. the liquid: wave surface + domain-warp swirl body ────────────
    private void drawLiquid(BufferBuilder buf, Matrix4f matrix, int cx, int cy, float time, float partialTick) {
        float fill = ClientBloodState.getFillFraction();
        if (fill <= 0.005f) return;

        BloodWaveSim sim = ClientBloodState.WAVES;
        int r = LIQUID_RADIUS;
        float fillY = cy + r - 2f * r * fill;
        float cellSize = 1f / SUB;
        int subRadius = r * SUB;
        float waveWindow = Mth.clamp(4f * fill * (1f - fill) + 0.15f, 0f, 1f);

        for (int sx = -subRadius; sx < subRadius; sx++) {
            float gx = (sx + 0.5f) * cellSize; // column center, GUI px from orb center
            float chord = (float) Math.sqrt(Math.max(0, (float) r * r - gx * gx));
            if (chord <= cellSize) continue;
            float top = cy - chord;
            float bottom = cy + chord;
            float colX = gx + r; // 0..2r across the orb

            int simColumn = Mth.clamp((int) (colX / (2f * r) * BloodWaveSim.COLUMNS), 0, BloodWaveSim.COLUMNS - 1);
            float wave = sim.sampleHeight(simColumn, partialTick);
            // Noise-driven swell: irregular travelling humps, not metronome sines
            float swell = (LiquidNoiseField.sampleSmooth(colX * 0.16f - time * 0.05f, time * 0.012f) - 0.5f) * SWELL_MAIN;
            float ripple = (LiquidNoiseField.sampleSmooth(colX * 0.45f + time * 0.09f, 7.3f + time * 0.02f) - 0.5f) * SWELL_RIPPLE;
            wave += swell + ripple;
            wave *= waveWindow;

            // Snap the surface to the cell grid so it moves in visible steps
            float surface = Mth.clamp(fillY + wave, top, bottom);
            surface = Math.round(surface * SUB) / (float) SUB;
            if (surface >= bottom) continue;

            float x = cx + sx * cellSize;

            float meniscusEnd = Math.min(surface + MENISCUS_PX, bottom);

            // ── Body with spherical limb shading: blood_dark through the middle,
            //    blood_black where the liquid meets the glass silhouette ─────────
            float limbR = LIMB * r;
            float lowerLimbStart = bottom;
            if (Math.abs(gx) >= limbR) {
                // Side columns sit entirely in the silhouette shadow
                if (meniscusEnd < bottom) {
                    cell(buf, matrix, x, meniscusEnd, cellSize, bottom - meniscusEnd,
                            SHADE_BLACK[0], SHADE_BLACK[1], SHADE_BLACK[2], LIQUID_ALPHA);
                }
            } else {
                float half = (float) Math.sqrt(limbR * limbR - gx * gx);
                float upperLimbEnd = cy - half;
                lowerLimbStart = cy + half;
                if (meniscusEnd < upperLimbEnd) {
                    cell(buf, matrix, x, meniscusEnd, cellSize, Math.min(upperLimbEnd, bottom) - meniscusEnd,
                            SHADE_BLACK[0], SHADE_BLACK[1], SHADE_BLACK[2], LIQUID_ALPHA);
                }
                float bodyStart = Math.max(meniscusEnd, upperLimbEnd);
                float bodyEnd = Math.min(bottom, lowerLimbStart);
                if (bodyEnd > bodyStart) {
                    cell(buf, matrix, x, bodyStart, cellSize, bodyEnd - bodyStart,
                            SHADE_DARK[0], SHADE_DARK[1], SHADE_DARK[2], LIQUID_ALPHA);
                }
                if (bottom > lowerLimbStart) {
                    cell(buf, matrix, x, lowerLimbStart, cellSize, bottom - lowerLimbStart,
                            SHADE_BLACK[0], SHADE_BLACK[1], SHADE_BLACK[2], LIQUID_ALPHA);
                }
            }

            // ── Surface ellipse: the liquid's top plane seen at a tilt. Front
            //    edge rides the waves fully, the back edge lags — the disc
            //    visibly tumbles as the liquid sloshes ─────────────────────────
            float aF = (float) Math.sqrt(Math.max(0.5f, (float) r * r - (fillY - cy) * (fillY - cy)));
            if (Math.abs(gx) < aF) {
                float ef = (float) Math.sqrt(Math.max(0f, 1f - (gx / aF) * (gx / aF)));
                float backY = fillY + wave * BACK_EDGE_CALM - ELLIPSE_DEPTH * 2f * ef;
                backY = Math.max(Math.round(backY * SUB) / (float) SUB, top);
                float planeH = surface - backY;
                if (planeH > 0.01f) {
                    if (planeH > 1f) {
                        cell(buf, matrix, x, backY, cellSize, 1f,
                                SHADE_BLACK[0], SHADE_BLACK[1], SHADE_BLACK[2], LIQUID_ALPHA);
                        cell(buf, matrix, x, backY + 1f, cellSize, planeH - 1f,
                                SHADE_PLANE[0], SHADE_PLANE[1], SHADE_PLANE[2], LIQUID_ALPHA);
                    } else {
                        cell(buf, matrix, x, backY, cellSize, planeH,
                                SHADE_PLANE[0], SHADE_PLANE[1], SHADE_PLANE[2], LIQUID_ALPHA);
                    }
                }
            }

            // Meniscus: bright front edge of the surface
            cell(buf, matrix, x, surface, cellSize, meniscusEnd - surface,
                    SHADE_BRIGHT[0], SHADE_BRIGHT[1], SHADE_BRIGHT[2], LIQUID_ALPHA);
            if (meniscusEnd >= bottom) continue;

            // Rare bright glints drifting through the upper liquid
            for (int j = 0; j < 3; j++) {
                float gy = surface + 3f + j * 4f;
                if (gy >= lowerLimbStart - 1.5f) break;
                float g = LiquidNoiseField.sampleSmooth(colX * 0.35f - time * GLINT_SPEED, gy * 0.3f + j * 5.1f);
                if (g > GLINT_THRESHOLD) {
                    float snapped = Math.round(gy * SUB) / (float) SUB;
                    cell(buf, matrix, x, snapped, cellSize, cellSize,
                            SHADE_BRIGHT[0], SHADE_BRIGHT[1], SHADE_BRIGHT[2], 0.85f);
                }
            }
        }
    }

    // ── 4. placeholder glass ring: blocky annulus (replaced by orb_front.png) ─
    private void drawPlaceholderRing(BufferBuilder buf, Matrix4f matrix, int cx, int cy) {
        int inner = LIQUID_RADIUS;
        int outer = LIQUID_RADIUS + 2;
        float[] silver = ModColors.rgb(ModColors.SILVER);
        float[] shadow = ModColors.rgb(ModColors.SILVER_SHADOW);
        for (int y = -outer; y < outer; y++) {
            for (int x = -outer; x < outer; x++) {
                int d2 = x * x + y * y;
                if (d2 <= outer * outer && d2 > inner * inner) {
                    float[] c = d2 > (outer - 1) * (outer - 1) ? shadow : silver;
                    cell(buf, matrix, cx + x, cy + y, 1, 1, c[0], c[1], c[2], 0.95f);
                }
            }
        }
    }
}
