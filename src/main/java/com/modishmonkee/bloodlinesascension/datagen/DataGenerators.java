package com.modishmonkee.bloodlinesascension.datagen;

import com.modishmonkee.bloodlinesascension.BloodlinesAscension;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = BloodlinesAscension.MOD_ID)
public class DataGenerators {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        // Client-side providers
        generator.addProvider(event.includeClient(), new ModItemModelProvider(output, event.getExistingFileHelper()));
        generator.addProvider(event.includeClient(), new ModLanguageProvider(output));

        // Server-side providers (tags, recipes, loot, datapack entries) get added
        // here as content comes online — same layout as Arsenal of Extinction.
    }
}
