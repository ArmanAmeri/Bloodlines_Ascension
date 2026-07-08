package com.modishmonkee.bloodlinesascension.client.screen;

import com.modishmonkee.bloodlinesascension.BloodlinesAscension;
import com.modishmonkee.bloodlinesascension.client.gui.TooltipBoxRenderer;
import com.modishmonkee.bloodlinesascension.util.ModColors;
import com.mojang.blaze3d.systems.RenderSystem;
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
 * → transparent window (glass) → player model → separator → buttons}.
 *
 * Rendered at 1× (integer, crisp), lifted ~10% above screen centre. Region
 * constants are measured straight from the exported PNG alpha, so everything
 * lands on the pixel grid. Placeholders remain where data isn't wired: the
 * essence bar shows the silver (Lesser) variant filled to {@link #DEMO_FILL};
 * button clicks only toggle their pressed art for now — real behaviour and
 * stats come with M6. Hovering the crest or a button shows a hand-drawn
 * tooltip (see {@link TooltipBoxRenderer}) with placeholder text until real
 * labels exist.
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
    private static final ResourceLocation MILESTONE_SILVER = tex("milestonemarker_silver");
    private static final ResourceLocation SEPARATOR = tex("separator");
    private static final ResourceLocation WINDOW = tex("transparent_window");

    private static final ResourceLocation[] BUTTON = new ResourceLocation[6];
    private static final ResourceLocation[] BUTTON_PRESSED = new ResourceLocation[6];
    static {
        for (int i = 0; i < 6; i++) {
            BUTTON[i] = tex("button" + (i + 1));
            BUTTON_PRESSED[i] = tex("button" + (i + 1) + "_hover"); // "_hover" is the clicked/pressed state
        }
    }

    private static final int ART_W = 250, ART_H = 250, SCALE = 1;
    /** Fraction of screen height to lift the panel above centre (positive = up). */
    private static final float RAISE = 0.10f;

    // Regions measured from the art (canvas px).
    private static final int CREST_X0 = 112, CREST_Y0 = 44, CREST_X1 = 137, CREST_Y1 = 69;
    private static final int MODEL_X0 = 100, MODEL_Y0 = 102, MODEL_X1 = 149, MODEL_Y1 = 171;
    private static final int MODEL_SCALE = 26; // vanilla inventory default is 30; nudged down a bit
    // Essence bar: dark-blood channel at y76–77 spanning x30–219; code paints bright
    // blood over it, 3×12 markers (centred on the bar) bracket the red part.
    private static final int BAR_X0 = 30, BAR_X1 = 219, BAR_Y0 = 76, BAR_H = 2;
    private static final int MARKER_W = 3, MARKER_H = 12, MARKER_Y0 = 71;
    /** Placeholder fill until the blood/pureblood resource is wired (M1/M5). */
    private static final float DEMO_FILL = 1.0f;
    // Button row (bottom-right): 6× 6×7 sprites centred in the 8px-wide slots
    // (dividers at x165,174,…,219; slot interiors y142–149) measured from the art.
    private static final int BUTTON_W = 6, BUTTON_H = 7, BUTTON0_X = 167, BUTTON_PITCH = 9, BUTTON_Y = 143;

    private final int scale = SCALE;
    private int panelX, panelY;
    private int selectedButton = -1;
    private Boolean artPresent;

    public CharacterScreen() {
        super(Component.translatable("gui.bloodlinesascension.character.title"));
    }

    @Override
    protected void init() {
        this.panelX = (this.width - ART_W * scale) / 2;
        int centered = (this.height - ART_H * scale) / 2;
        this.panelY = centered - Math.round(this.height * RAISE);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // No renderBackground() dim: the live world shows through the panel's
        // transparent areas so the glass model-window reads as actually transparent.
        if (artPresent == null && this.minecraft != null) {
            artPresent = this.minecraft.getResourceManager().getResource(BACKGROUND).isPresent();
        }

        if (Boolean.TRUE.equals(artPresent)) {
            layerBlended(g, WINDOW);     // translucent glass — bottommost layer; background/frame
                                         // have a transparent hole here so it still shows through
            layer(g, BACKGROUND);
            layer(g, FRAME);
            layer(g, hovering(mouseX, mouseY, CREST_X0, CREST_Y0, CREST_X1, CREST_Y1) ? CREST_HOVER : CREST);
            layer(g, ESSENCE_SILVER);
            drawEssenceFill(g, DEMO_FILL, MILESTONE_SILVER);
            renderPlayerModel(g, mouseX, mouseY);
            layer(g, SEPARATOR);
            drawButtons(g);
        } else {
            drawPlaceholder(g);
        }

        super.render(g, mouseX, mouseY, partialTick);
        renderHoverTooltip(g, mouseX, mouseY);
    }

    /** Hand-drawn tooltip (see TooltipBoxRenderer) for whatever's under the cursor. */
    private void renderHoverTooltip(GuiGraphics g, int mouseX, int mouseY) {
        if (!Boolean.TRUE.equals(artPresent)) return;

        Component text = null;
        if (hovering(mouseX, mouseY, CREST_X0, CREST_Y0, CREST_X1, CREST_Y1)) {
            text = Component.translatable("gui.bloodlinesascension.character.crest.tooltip");
        } else {
            int hovered = hoveredButton(mouseX, mouseY);
            if (hovered >= 0) {
                text = Component.translatable("gui.bloodlinesascension.character.button." + (hovered + 1) + ".tooltip");
            }
        }
        if (text != null) {
            TooltipBoxRenderer.draw(g, this.font, text, mouseX, mouseY, this.width, this.height);
        }
    }

    /** Bright-blood fill over the bar's channel, with a milestone marker at each end of the red part. */
    private void drawEssenceFill(GuiGraphics g, float fraction, ResourceLocation marker) {
        int redEnd = BAR_X0 + Math.round((BAR_X1 - BAR_X0) * fraction);
        g.fill(sx(BAR_X0), sy(BAR_Y0), sx(redEnd), sy(BAR_Y0 + BAR_H), ModColors.BLOOD_BRIGHT);
        drawMarker(g, marker, BAR_X0); // start of red
        drawMarker(g, marker, redEnd); // end of red  (in-between marks added with rank thresholds)
    }

    /** Draw a 3×12 milestone tick centred on a canvas x, straddling the bar. */
    private void drawMarker(GuiGraphics g, ResourceLocation marker, int tickX) {
        g.blit(marker, sx(tickX) - (MARKER_W * scale) / 2, sy(MARKER_Y0),
                MARKER_W * scale, MARKER_H * scale, 0f, 0f, MARKER_W, MARKER_H, MARKER_W, MARKER_H);
    }

    /** The 6 bottom-right buttons; the selected one shows its pressed sprite. */
    private void drawButtons(GuiGraphics g) {
        for (int i = 0; i < BUTTON.length; i++) {
            ResourceLocation t = (i == selectedButton) ? BUTTON_PRESSED[i] : BUTTON[i];
            g.blit(t, sx(BUTTON0_X + i * BUTTON_PITCH), sy(BUTTON_Y),
                    BUTTON_W * scale, BUTTON_H * scale, 0f, 0f, BUTTON_W, BUTTON_H, BUTTON_W, BUTTON_H);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int hovered = hoveredButton((int) mouseX, (int) mouseY);
            if (hovered >= 0) {
                selectedButton = (selectedButton == hovered) ? -1 : hovered; // toggle for now; real actions later
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** Index of the button under the cursor, or -1. */
    private int hoveredButton(int mouseX, int mouseY) {
        for (int i = 0; i < BUTTON.length; i++) {
            int bx = sx(BUTTON0_X + i * BUTTON_PITCH), by = sy(BUTTON_Y);
            if (mouseX >= bx && mouseX < bx + BUTTON_W * scale
                    && mouseY >= by && mouseY < by + BUTTON_H * scale) {
                return i;
            }
        }
        return -1;
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
        g.blit(texture, panelX, panelY, ART_W * scale, ART_H * scale, 0f, 0f, ART_W, ART_H, ART_W, ART_H);
    }

    /**
     * Same as {@link #layer}, but with blending explicitly enabled — needed for
     * textures using *partial* alpha (e.g. the tinted glass window). Plain
     * {@code g.blit} only alpha-tests (fully transparent pixels are discarded);
     * without blending on, partial-alpha pixels draw fully opaque instead of
     * translucent. Everything else in this screen is binary alpha (0 or 255)
     * so doesn't need this — only wrap where partial alpha is actually used.
     */
    private void layerBlended(GuiGraphics g, ResourceLocation texture) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        layer(g, texture);
        RenderSystem.disableBlend();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Wireframe until the art resolves (PROJECT_PLAN.md §4 regions). */
    private void drawPlaceholder(GuiGraphics g) {
        int x0 = panelX, y0 = panelY, x1 = panelX + ART_W * scale, y1 = panelY + ART_H * scale;
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
