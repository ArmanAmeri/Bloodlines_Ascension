package com.modishmonkee.bloodlinesascension.event;

import com.modishmonkee.bloodlinesascension.BloodlinesAscension;
import com.modishmonkee.bloodlinesascension.client.screen.CharacterMenuButton;
import com.modishmonkee.bloodlinesascension.client.screen.CharacterScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * Adds the character-sheet button to the survival inventory, mirroring the vanilla
 * recipe-book toggle. Clicking it replaces the inventory with the {@link CharacterScreen}
 * (so closing the character sheet drops straight back to the game, as requested).
 */
@EventBusSubscriber(modid = BloodlinesAscension.MOD_ID, value = Dist.CLIENT)
public class ModScreenEvents {

    // Vanilla's recipe-book toggle sits at (leftPos + 104, topPos + 61); we sit just to its right.
    private static final int BUTTON_DX = 126;
    private static final int BUTTON_DY = 61;

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        // Survival inventory only — the creative screen has no recipe book either.
        if (event.getScreen() instanceof InventoryScreen inv) {
            CharacterMenuButton button = new CharacterMenuButton(
                    inv.getGuiLeft() + BUTTON_DX,
                    inv.getGuiTop() + BUTTON_DY,
                    b -> Minecraft.getInstance().setScreen(new CharacterScreen()));
            button.setTooltip(Tooltip.create(Component.translatable("gui.bloodlinesascension.character.button")));
            event.addListener(button);
        }
    }
}
