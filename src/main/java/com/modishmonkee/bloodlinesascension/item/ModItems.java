package com.modishmonkee.bloodlinesascension.item;

import com.modishmonkee.bloodlinesascension.BloodlinesAscension;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(BloodlinesAscension.MOD_ID);

    // Items land here as systems come online (bloodline sigils, blood vials, etc.)

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
