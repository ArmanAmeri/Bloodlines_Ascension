package com.modishmonkee.bloodlinesascension.client.screen;

import com.modishmonkee.bloodlinesascension.BloodlinesAscension;
import com.modishmonkee.bloodlinesascension.util.ModColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * The little inventory button that opens the {@link CharacterScreen}, sat beside
 * the vanilla recipe-book toggle. Uses a hand-drawn icon when present, otherwise a
 * gold-framed blood placeholder so the button is usable before the art lands.
 *
 * Icon sheet (optional): {@code textures/gui/character/menu_button.png}, two stacked
 * 20×18 states — normal on top, hover below (20×36 total).
 */
@OnlyIn(Dist.CLIENT)
public class CharacterMenuButton extends Button {

    private static final ResourceLocation ICON =
            ResourceLocation.fromNamespaceAndPath(BloodlinesAscension.MOD_ID, "textures/gui/menu_button.png");
    public static final int WIDTH = 20, HEIGHT = 18;

    private Boolean iconPresent;

    public CharacterMenuButton(int x, int y, OnPress onPress) {
        super(x, y, WIDTH, HEIGHT,
                Component.translatable("gui.bloodlinesascension.character.button"),
                onPress, DEFAULT_NARRATION);
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (iconPresent == null) {
            iconPresent = Minecraft.getInstance().getResourceManager().getResource(ICON).isPresent();
        }
        boolean hovered = isHoveredOrFocused();
        if (iconPresent) {
            g.blit(ICON, getX(), getY(), 0f, hovered ? HEIGHT : 0f, WIDTH, HEIGHT, WIDTH, HEIGHT * 2);
        } else {
            drawPlaceholder(g, hovered);
        }
    }

    private void drawPlaceholder(GuiGraphics g, boolean hovered) {
        int x0 = getX(), y0 = getY(), x1 = x0 + WIDTH, y1 = y0 + HEIGHT;
        int frame = hovered ? ModColors.GOLD : ModColors.GOLD_SHADOW;
        int body = hovered ? ModColors.mix(ModColors.BLOOD_DARK, ModColors.BLOOD_BRIGHT, 0.25f) : ModColors.BLOOD_DARK;
        g.fill(x0, y0, x1, y1, frame);
        g.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, body);
        // Sigil: a chunky blood_bright diamond/plus, font-free so it survives before art.
        int cx = x0 + WIDTH / 2, cy = y0 + HEIGHT / 2;
        g.fill(cx - 3, cy - 2, cx + 3, cy + 2, ModColors.BLOOD_BRIGHT);
        g.fill(cx - 2, cy - 3, cx + 2, cy + 3, ModColors.BLOOD_BRIGHT);
        g.fill(cx - 1, cy - 4, cx + 1, cy + 4, ModColors.BLOOD_BRIGHT);
    }
}
