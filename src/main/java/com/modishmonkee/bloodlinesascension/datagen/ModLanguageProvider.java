package com.modishmonkee.bloodlinesascension.datagen;

import com.modishmonkee.bloodlinesascension.BloodlinesAscension;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class ModLanguageProvider extends LanguageProvider {

    public ModLanguageProvider(PackOutput output) {
        super(output, BloodlinesAscension.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add("creativetab.bloodlinesascension.bloodlines_tab", "Bloodlines: Ascension");
    }
}
