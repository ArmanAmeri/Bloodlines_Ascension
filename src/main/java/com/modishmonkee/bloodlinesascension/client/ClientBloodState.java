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

    private static float max = 100.0f;
    private static float blood = 60.0f;

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
            float strength = Math.min(Math.abs(changed) * (changed < 0 ? 1.6f : 0.9f), 4.5f);
            WAVES.splash(strength);
        }
    }

    public static void reset() {
        blood = 60.0f;
        max = 100.0f;
        WAVES.reset();
    }
}
