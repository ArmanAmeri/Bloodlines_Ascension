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

        // Keybinds
        add("key.categories.bloodlinesascension", "Bloodlines: Ascension");
        add("key.bloodlinesascension.dev_blood_fill", "DEV: Fill Blood Orb");
        add("key.bloodlinesascension.dev_blood_drain", "DEV: Drain Blood Orb");
        add("key.bloodlinesascension.dev_spawn_light", "DEV: Spawn Veil Light");
        add("key.bloodlinesascension.dev_clear_lights", "DEV: Clear Veil Lights");
    }
}
