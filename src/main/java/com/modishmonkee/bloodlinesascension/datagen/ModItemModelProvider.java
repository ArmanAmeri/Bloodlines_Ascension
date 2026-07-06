package com.modishmonkee.bloodlinesascension.datagen;

import com.modishmonkee.bloodlinesascension.BloodlinesAscension;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModItemModelProvider extends ItemModelProvider {

    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, BloodlinesAscension.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        // basicItem(...) calls land here as items are added
    }
}
