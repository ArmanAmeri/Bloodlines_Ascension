package com.modishmonkee.bloodlinesascension.client;

import com.modishmonkee.bloodlinesascension.client.hud.BloodWaveSim;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * PLACEHOLDER blood store for HUD development (M1 will replace this with the
 * synced player attachment — blood amounts, gain/spend rules pending design Q2).
 *
 * Holds the client-visible blood value and the orb's wave simulation. Any
 * change in blood level automatically splashes the liquid surface.
 */
@OnlyIn(Dist.CLIENT)
public final class ClientBloodState {

    public static final BloodWaveSim WAVES = new BloodWaveSim();

    private static final float DEFAULT_MAX = 100.0f;
    /** Hard cap on max blood (upgrades can't push it past this). */
    public static final float MAX_CAP = 1000.0f;

    private static float max = DEFAULT_MAX;
    private static float blood = DEFAULT_MAX; // starts full

    private ClientBloodState() {}

    public static float getBlood() {
        return blood;
    }

    public static float getMax() {
        return max;
    }

    /** Fill fraction 0..1 for rendering. */
    public static float getFillFraction() {
        return max <= 0 ? 0 : Mth.clamp(blood / max, 0.0f, 1.0f);
    }

    public static void addBlood(float delta) {
        float before = blood;
        blood = Mth.clamp(blood + delta, 0.0f, max);
        float changed = blood - before;
        if (changed != 0) {
            // Losing blood sloshes harder than gaining it (drain = violence)
            float strength = Math.min(Math.abs(changed) * (changed < 0 ? 1.2f : 0.7f), 3.0f);
            WAVES.splash(strength);
        }
    }

    /** Set the current blood amount (used by {@code /blood <amount>}), clamped to [0, max]. */
    public static void setBlood(float value) {
        float before = blood;
        blood = Mth.clamp(value, 0.0f, max);
        if (blood != before) WAVES.splash(Math.min(Math.abs(blood - before) * 0.7f, 3.0f));
    }

    /**
     * Set max blood (used by {@code /blood max <amount>}). Max blood is what drives the orb's
     * colour (dark red → bright red → white); higher max = "richer" blood. Current blood is kept
     * within the new max so the orb still shows a sane fill percentage.
     */
    public static void setMax(float value) {
        max = Mth.clamp(value, 1.0f, MAX_CAP);
        if (blood > max) blood = max;
    }

    public static void reset() {
        max = DEFAULT_MAX;
        blood = DEFAULT_MAX;
        WAVES.reset();
    }
}
