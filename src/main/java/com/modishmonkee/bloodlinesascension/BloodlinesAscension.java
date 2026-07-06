package com.modishmonkee.bloodlinesascension;

import com.modishmonkee.bloodlinesascension.item.ModCreativeModeTabs;
import com.modishmonkee.bloodlinesascension.item.ModItems;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

@Mod(BloodlinesAscension.MOD_ID)
public class BloodlinesAscension {

    public static final String MOD_ID = "bloodlinesascension";
    private static final Logger LOGGER = LogUtils.getLogger();

    public BloodlinesAscension(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);

        ModCreativeModeTabs.register(modEventBus);
        ModItems.register(modEventBus);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Bloodlines: Ascension — common setup complete.");
    }
}
