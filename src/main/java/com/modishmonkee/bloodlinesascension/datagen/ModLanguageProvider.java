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

        // Character screen
        add("gui.bloodlinesascension.character.title", "Character");
        add("gui.bloodlinesascension.character.button", "Bloodline Character");
        add("gui.bloodlinesascension.character.crest.tooltip", "Bloodline Crest");
        add("gui.bloodlinesascension.character.button.1.tooltip", "Button 1");
        add("gui.bloodlinesascension.character.button.2.tooltip", "Button 2");
        add("gui.bloodlinesascension.character.button.3.tooltip", "Button 3");
        add("gui.bloodlinesascension.character.button.4.tooltip", "Button 4");
        add("gui.bloodlinesascension.character.button.5.tooltip", "Button 5");
        add("gui.bloodlinesascension.character.button.6.tooltip", "Button 6");

        // Keybinds
        add("key.categories.bloodlinesascension", "Bloodlines: Ascension");
        add("key.bloodlinesascension.dev_blood_fill", "DEV: Fill Blood Orb");
        add("key.bloodlinesascension.dev_blood_drain", "DEV: Drain Blood Orb");
        add("key.bloodlinesascension.dev_spawn_light", "DEV: Spawn Veil Light");
        add("key.bloodlinesascension.dev_clear_lights", "DEV: Clear Veil Lights");
    }
}
