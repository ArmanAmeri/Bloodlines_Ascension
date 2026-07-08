package com.modishmonkee.bloodlinesascension.client.hud;

import com.modishmonkee.bloodlinesascension.BloodlinesAscension;
import com.modishmonkee.bloodlinesascension.client.ClientSkillBarState;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * The 6-slot skill hotbar, bottom-centre, shown in place of the vanilla hotbar while
 * toggled on (see {@link ClientSkillBarState} / {@code ModClientEvents}, which also
 * hides the vanilla hotbar, XP bar, hearts and hunger). Silver slots by default; a slot
 * flips to its gold sprite while the matching 1-6 key is held. Health and food are shown
 * flanking the bar as "<count>x" + a single black-outlined icon (hearts left, food right).
 */
@OnlyIn(Dist.CLIENT)
public class SkillBarHudLayer implements LayeredDraw.Layer {

    private static final ResourceLocation SILVER =
            ResourceLocation.fromNamespaceAndPath(BloodlinesAscension.MOD_ID, "textures/gui/skillbar_silver.png");
    private static final ResourceLocation GOLD =
            ResourceLocation.fromNamespaceAndPath(BloodlinesAscension.MOD_ID, "textures/gui/skillbar_gold.png");
    // Vanilla HUD sprites, reused for the side health/food columns.
    private static final ResourceLocation HEART = ResourceLocation.withDefaultNamespace("hud/heart/full");
    private static final ResourceLocation FOOD = ResourceLocation.withDefaultNamespace("hud/food_full");

    private static final int SLOT = 32, PITCH = 24; // visible frame is 22px wide → 2px gap between frames
    /** How far the bar hangs past the screen's bottom edge — the 32px slots are taller than the
     *  vanilla hotbar, so we drop them a little to clear the health/hunger row above. */
    private static final int BOTTOM_OVERHANG = 3;
    // Side stats flanking the bar: "<count>x" text + one 9px icon, centred on the bar then nudged
    // down a touch (SIDE_DROP) to sit lower against the bar.
    private static final int ICON = 9, SIDE_GAP = 3, TEXT_GAP = 2, SIDE_DROP = 6;
    /** 4-way offsets for the black outline stamped under each icon. */
    private static final int[][] OUTLINE = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

    @Override
    public void render(GuiGraphics g, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || !ClientSkillBarState.isActive()) return;

        int slots = ClientSkillBarState.SLOTS;
        int totalW = (slots - 1) * PITCH + SLOT;
        int x0 = (g.guiWidth() - totalW) / 2;
        int y = g.guiHeight() - SLOT + BOTTOM_OVERHANG;

        for (int i = 0; i < slots; i++) {
            // Gold while the matching hotbar key is held (holdable), silver otherwise.
            ResourceLocation tex = mc.options.keyHotbarSlots[i].isDown() ? GOLD : SILVER;
            g.blit(tex, x0 + i * PITCH, y, 0f, 0f, SLOT, SLOT, SLOT, SLOT);
        }

        drawSideStats(g, mc.player, x0, x0 + totalW, y);
    }

    /** "<health>x ♥" left of the bar and "<food>x 🍗" right of it, both live and centred on the bar. */
    private void drawSideStats(GuiGraphics g, LocalPlayer player, int barLeft, int barRight, int barTop) {
        Font font = Minecraft.getInstance().font;
        int centerY = barTop + SLOT / 2 + SIDE_DROP;
        int iconY = centerY - ICON / 2;
        int textY = centerY - font.lineHeight / 2;

        // Left: "<health>x" then a heart icon, right-anchored to the bar's left edge.
        String hp = Math.round(player.getHealth()) + "x";
        int hpGroup = font.width(hp) + TEXT_GAP + ICON;
        int hx = barLeft - SIDE_GAP - hpGroup;
        g.drawString(font, hp, hx, textY, 0xFFFFFFFF, true);
        blitOutlined(g, HEART, hx + font.width(hp) + TEXT_GAP, iconY);

        // Right: "<food>x" then a food icon, left-anchored to the bar's right edge.
        String food = player.getFoodData().getFoodLevel() + "x";
        int fx = barRight + SIDE_GAP;
        g.drawString(font, food, fx, textY, 0xFFFFFFFF, true);
        blitOutlined(g, FOOD, fx + font.width(food) + TEXT_GAP, iconY);
    }

    /** Draw a sprite with a 4-way black outline. setColor flushes between tints so they don't bleed. */
    private void blitOutlined(GuiGraphics g, ResourceLocation sprite, int x, int y) {
        g.setColor(0f, 0f, 0f, 1f);
        for (int[] o : OUTLINE) g.blitSprite(sprite, x + o[0], y + o[1], ICON, ICON);
        g.setColor(1f, 1f, 1f, 1f);
        g.blitSprite(sprite, x, y, ICON, ICON);
    }
}
