package com.modishmonkee.bloodlinesascension.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Client-only state for the skill hotbar: whether it's shown in place of the vanilla
 * hotbar, and which skill set is active. Placeholder until the real skill/attachment
 * systems land (M1/M6) — for now it just drives the HUD look.
 */
@OnlyIn(Dist.CLIENT)
public final class ClientSkillBarState {

    /** Slots on the bar (matches the 6 character-screen skill-slot buttons). */
    public static final int SLOTS = 6;
    /** Selectable skill sets (matches the 9 character-screen skill-set tabs). */
    public static final int SETS = 9;

    private static boolean active = false;
    private static int currentSet = 0; // 0-based

    private ClientSkillBarState() {}

    public static boolean isActive() {
        return active;
    }

    public static void toggle() {
        active = !active;
    }

    /** 1-based set number, for display. */
    public static int getSetNumber() {
        return currentSet + 1;
    }

    /** Cycle the active set, wrapping around. */
    public static void cycleSet(int dir) {
        currentSet = Math.floorMod(currentSet + dir, SETS);
    }

    public static void reset() {
        active = false;
        currentSet = 0;
    }
}
