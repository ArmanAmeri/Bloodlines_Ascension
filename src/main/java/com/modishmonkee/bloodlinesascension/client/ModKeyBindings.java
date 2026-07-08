package com.modishmonkee.bloodlinesascension.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public class ModKeyBindings {

    /** Opens/closes the Bloodline character sheet ({@code CharacterScreen}). */
    public static final KeyMapping OPEN_CHARACTER_SCREEN = new KeyMapping(
            "key.bloodlinesascension.open_character_screen",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            "key.categories.bloodlinesascension");

    /**
     * The skill-hotbar key ({@code <} / comma). Tap: toggle vanilla ↔ skill hotbar.
     * Hold + scroll: cycle skill sets (see {@code ModClientEvents}).
     */
    public static final KeyMapping TOGGLE_SKILL_BAR = new KeyMapping(
            "key.bloodlinesascension.toggle_skill_bar",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_COMMA,
            "key.categories.bloodlinesascension");

    /** DEV ONLY: fill the blood orb while held (numpad +). Removed once real blood mechanics land (M1). */
    public static final KeyMapping DEV_BLOOD_FILL = new KeyMapping(
            "key.bloodlinesascension.dev_blood_fill",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_KP_ADD,
            "key.categories.bloodlinesascension");

    /** DEV ONLY: drain the blood orb while held (numpad -). */
    public static final KeyMapping DEV_BLOOD_DRAIN = new KeyMapping(
            "key.bloodlinesascension.dev_blood_drain",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_KP_SUBTRACT,
            "key.categories.bloodlinesascension");

    /** DEV ONLY: spawn a Veil point light at the player (numpad *). */
    public static final KeyMapping DEV_SPAWN_LIGHT = new KeyMapping(
            "key.bloodlinesascension.dev_spawn_light",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_KP_MULTIPLY,
            "key.categories.bloodlinesascension");

    /** DEV ONLY: remove all spawned dev lights (numpad /). */
    public static final KeyMapping DEV_CLEAR_LIGHTS = new KeyMapping(
            "key.bloodlinesascension.dev_clear_lights",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_KP_DIVIDE,
            "key.categories.bloodlinesascension");
}
