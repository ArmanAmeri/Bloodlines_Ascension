package com.modishmonkee.bloodlinesascension.item;

import com.modishmonkee.bloodlinesascension.BloodlinesAscension;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BloodlinesAscension.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> BLOODLINES_TAB =
            CREATIVE_MODE_TABS.register("bloodlines_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("creativetab.bloodlinesascension.bloodlines_tab"))
                    // Placeholder icon until we have a mod icon item (see Arsenal's MOD_ICON pattern)
                    .icon(() -> Items.REDSTONE.getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        // All mod items get accepted here as they're added
                        ModItems.ITEMS.getEntries().forEach(entry -> output.accept(entry.get()));
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
