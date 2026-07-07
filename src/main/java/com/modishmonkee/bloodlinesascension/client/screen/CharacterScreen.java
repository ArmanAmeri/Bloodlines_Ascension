package com.modishmonkee.bloodlinesascension.client.screen;

import com.modishmonkee.bloodlinesascension.BloodlinesAscension;
import com.modishmonkee.bloodlinesascension.util.ModColors;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * The Bloodline character sheet. Opened from a button in the survival inventory
 * (see {@code ModScreenEvents}); Esc returns straight to the game.
 *
 * The art is ModishMonkee's hand-drawn {@code playerMenu.aseprite}, exported as
 * per-layer PNGs on a shared 250×250 canvas and composited back-to-front:
 * {@code background → frame → crest → essence bar (frame + code fill + markers)
 * → player model → separator}. Full-canvas layers all blit at the panel origin.
 *
 * Scale is fit-aware: the largest integer up to {@link #MAX_SCALE} that fits the
 * screen (so 2× on roomy GUI scales, 1× when 500px wouldn't fit). The panel is
 * nudged up ~15% of the screen height from centre.
 *
 * Placeholders remain where data isn't wired: the essence bar shows the silver
 * (Lesser-rank) variant filled to {@link #DEMO_FILL}; rank-driven variant, real
 * fill level, in-between milestone marks, and stats land with the M6 attachment.
 */
@OnlyIn(Dist.CLIENT)
public class CharacterScreen extends Screen {

    private static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(BloodlinesAscension.MOD_ID, "textures/gui/" + name + ".png");
    }

    private static final ResourceLocation BACKGROUND = tex("background");
    private static final ResourceLocation FRAME = tex("frame");
    private static final ResourceLocation CREST = tex("crest");
    private static final ResourceLocation CREST_HOVER = tex("crest_hover");
    private static final ResourceLocation ESSENCE_SILVER = tex("essencebar_silver");
    private static final ResourceLocation ESSENCE_GOLD = tex("essencebar_gold");
    private static final ResourceLocation MILESTONE_SILVER = tex("milestonemarker_silver");
    private static final ResourceLocation MILESTONE_GOLD = tex("milestonemarker_gold");
    private static final ResourceLocation SEPARATOR = tex("separator");

    private static final int ART_W = 250, ART_H = 250;
    private static final int MAX_SCALE = 2;
    /** Fraction of screen height to lift the panel above centre. */
    private static final float RAISE = 0.15f;

    // Regions measured from the art (canvas px).
    private static final int CREST_X0 = 112, CREST_Y0 = 44, CREST_X1 = 137, CREST_Y1 = 69;
    private static final int MODEL_X0 = 100, MODEL_Y0 = 103, MODEL_X1 = 149, MODEL_Y1 = 171;
    private static final int MODEL_SCALE = 30;
    // Essence bar: the frame art has a dark-blood channel at y76–77 spanning x30–219;
    // code paints bright blood over it, markers (3×12) bracket the red part.
    private static final int BAR_X0 = 30, BAR_X1 = 219, BAR_Y0 = 76, BAR_H = 2;
    private static final int MARKER_W = 3, MARKER_H = 12, MARKER_Y0 = 71;
    /** Placeholder fill until the blood/pureblood resource is wired (M1/M5). */
    private static final float DEMO_FILL = 1.0f;

    private int scale, panelW, panelH, panelX, panelY;
    private Boolean artPresent;

    public CharacterScreen() {
        super(Component.translatable("gui.bloodlinesascension.character.title"));
    }

    @Override
    protected void init() {
        this.scale = fitScale();
        this.panelW = ART_W * scale;
        this.panelH = ART_H * scale;
        this.panelX = (this.width - panelW) / 2;
        int centered = (this.height - panelH) / 2;
        this.panelY = Math.max(2, centered - Math.round(this.height * RAISE));
    }

    /** Largest integer scale (≤ MAX_SCALE) whose panel fits the screen. */
    private int fitScale() {
        for (int k = MAX_SCALE; k > 1; k--) {
            if (ART_W * k <= this.width && ART_H * k <= this.height) return k;
        }
        return 1;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g, mouseX, mouseY, partialTick);

        if (artPresent == null && this.minecraft != null) {
            artPresent = this.minecraft.getResourceManager().getResource(BACKGROUND).isPresent();
        }

        if (Boolean.TRUE.equals(artPresent)) {
            layer(g, BACKGROUND);
            layer(g, FRAME);
            layer(g, hovering(mouseX, mouseY, CREST_X0, CREST_Y0, CREST_X1, CREST_Y1) ? CREST_HOVER : CREST);
            layer(g, ESSENCE_SILVER);
            drawEssenceFill(g, DEMO_FILL, MILESTONE_SILVER);
            renderPlayerModel(g, mouseX, mouseY);
            layer(g, SEPARATOR);
        } else {
            drawPlaceholder(g);
        }

        super.render(g, mouseX, mouseY, partialTick);
    }

    /** Bright-blood fill over the bar's channel, with a milestone marker at each end of the red part. */
    private void drawEssenceFill(GuiGraphics g, float fraction, ResourceLocation marker) {
        int redEnd = BAR_X0 + Math.round((BAR_X1 - BAR_X0) * fraction);
        g.fill(sx(BAR_X0), sy(BAR_Y0), sx(redEnd), sy(BAR_Y0 + BAR_H), ModColors.BLOOD_BRIGHT);
        drawMarker(g, marker, BAR_X0); // start of red
        drawMarker(g, marker, redEnd); // end of red  (in-between marks added with rank thresholds)
    }

    /** Draw a 3×12 milestone tick centred horizontally on a canvas x, straddling the bar. */
    private void drawMarker(GuiGraphics g, ResourceLocation marker, int tickX) {
        g.blit(marker, sx(tickX) - (MARKER_W * scale) / 2, sy(MARKER_Y0),
                MARKER_W * scale, MARKER_H * scale, 0f, 0f, MARKER_W, MARKER_H, MARKER_W, MARKER_H);
    }

    /** Live paper-doll of the player, framed in the central slot (like the inventory preview). */
    private void renderPlayerModel(GuiGraphics g, int mouseX, int mouseY) {
        if (this.minecraft == null || this.minecraft.player == null) return;
        InventoryScreen.renderEntityInInventoryFollowsMouse(g,
                sx(MODEL_X0), sy(MODEL_Y0), sx(MODEL_X1), sy(MODEL_Y1),
                MODEL_SCALE * scale, 0.0625F, mouseX, mouseY, this.minecraft.player);
    }

    private int sx(int canvasX) { return panelX + canvasX * scale; }
    private int sy(int canvasY) { return panelY + canvasY * scale; }

    /** Mouse-inside test for a canvas-space box (crest hover, etc.). */
    private boolean hovering(int mouseX, int mouseY, int x0, int y0, int x1, int y1) {
        return mouseX >= sx(x0) && mouseX <= sx(x1) && mouseY >= sy(y0) && mouseY <= sy(y1);
    }

    /** Blit a full-canvas layer at the panel origin, scaled. */
    private void layer(GuiGraphics g, ResourceLocation texture) {
        g.blit(texture, panelX, panelY, panelW, panelH, 0f, 0f, ART_W, ART_H, ART_W, ART_H);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Wireframe until the art resolves (PROJECT_PLAN.md §4 regions). */
    private void drawPlaceholder(GuiGraphics g) {
        int x0 = panelX, y0 = panelY, x1 = panelX + panelW, y1 = panelY + panelH;
        g.fill(x0, y0, x1, y1, ModColors.BLOOD_BLACK);
        g.fill(x0, y0, x1, y0 + 1, ModColors.GOLD);
        g.fill(x0, y1 - 1, x1, y1, ModColors.GOLD);
        g.fill(x0, y0, x0 + 1, y1, ModColors.GOLD);
        g.fill(x1 - 1, y0, x1, y1, ModColors.GOLD);
        g.drawCenteredString(this.font, this.title, (x0 + x1) / 2, y0 - 12, ModColors.GOLD);
        g.drawCenteredString(this.font, "art missing: textures/gui/{background,frame,crest,essencebar_*}.png",
                (x0 + x1) / 2, (y0 + y1) / 2, ModColors.SILVER);
    }
}
