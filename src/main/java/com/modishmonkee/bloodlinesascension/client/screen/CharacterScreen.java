package com.modishmonkee.bloodlinesascension.client.screen;

import com.modishmonkee.bloodlinesascension.BloodlinesAscension;
import com.modishmonkee.bloodlinesascension.util.ModColors;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * The Bloodline character sheet. Opened from a button in the survival inventory
 * (see {@code ModScreenEvents}); Esc returns straight to the game.
 *
 * The art is ModishMonkee's hand-drawn {@code playerMenu.aseprite}, exported as
 * per-layer PNGs on a shared 250×250 canvas and composited here in the same
 * back-to-front order as the Aseprite layer stack:
 * {@code background → frame → crest → essence bar}. Because every layer is a
 * full-canvas export, they all blit at the same origin.
 *
 * Still driven by placeholders where data isn't wired yet: the essence bar shows
 * the silver (Lesser-rank) variant by default — swaps to gold from Baron up — and
 * real fill / crest / stats data lands with the synced attachment in M6.
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
    private static final ResourceLocation SEPARATOR = tex("separator");

    /** Shared Aseprite canvas; integer scale keeps the pixel art crisp. */
    private static final int ART_W = 250, ART_H = 250, SCALE = 1;
    private static final int PANEL_W = ART_W * SCALE;
    private static final int PANEL_H = ART_H * SCALE;

    private int panelX, panelY;
    private Boolean artPresent;

    public CharacterScreen() {
        super(Component.translatable("gui.bloodlinesascension.character.title"));
    }

    @Override
    protected void init() {
        this.panelX = (this.width - PANEL_W) / 2;
        this.panelY = (this.height - PANEL_H) / 2;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g, mouseX, mouseY, partialTick);

        if (artPresent == null && this.minecraft != null) {
            artPresent = this.minecraft.getResourceManager().getResource(BACKGROUND).isPresent();
        }

        if (Boolean.TRUE.equals(artPresent)) {
            // TODO(M6): pick gold vs silver from the player's rank category; overlay bar fill,
            //           crest hover, and stat text once the synced attachment exists.
            layer(g, BACKGROUND);
            layer(g, FRAME);
            layer(g, CREST);
            layer(g, ESSENCE_SILVER);
            layer(g, SEPARATOR);
        } else {
            drawPlaceholder(g);
        }

        super.render(g, mouseX, mouseY, partialTick);
    }

    /** Blit a full-canvas layer at the panel origin, scaled by SCALE. */
    private void layer(GuiGraphics g, ResourceLocation texture) {
        g.blit(texture, panelX, panelY, PANEL_W, PANEL_H, 0f, 0f, ART_W, ART_H, ART_W, ART_H);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Wireframe until the art resolves (PROJECT_PLAN.md §4 regions). */
    private void drawPlaceholder(GuiGraphics g) {
        int x0 = panelX, y0 = panelY, x1 = panelX + PANEL_W, y1 = panelY + PANEL_H;
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
