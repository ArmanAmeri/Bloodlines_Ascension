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
        add("gui.bloodlinesascension.character.crest.tooltip", "Bloodline Crest");
        for (int i = 1; i <= 6; i++) {
            add("gui.bloodlinesascension.character.button." + i + ".tooltip", "Skill Slot " + i);
        }
        for (int i = 1; i <= 9; i++) {
            add("gui.bloodlinesascension.character.tab." + i + ".tooltip", "Skill Set " + i);
        }

        // Skill bar
        add("message.bloodlinesascension.skill_set", "Skill Set %s");

        // Keybinds
        add("key.categories.bloodlinesascension", "Bloodlines: Ascension");
        add("key.bloodlinesascension.open_character_screen", "Open Character Screen");
        add("key.bloodlinesascension.toggle_skill_bar", "Toggle Skill Bar");
        add("key.bloodlinesascension.dev_blood_fill", "DEV: Fill Blood Orb");
        add("key.bloodlinesascension.dev_blood_drain", "DEV: Drain Blood Orb");
        add("key.bloodlinesascension.dev_spawn_light", "DEV: Spawn Veil Light");
        add("key.bloodlinesascension.dev_clear_lights", "DEV: Clear Veil Lights");
    }
}
