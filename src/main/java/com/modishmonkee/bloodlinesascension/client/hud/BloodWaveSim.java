package com.modishmonkee.bloodlinesascension.client.hud;

import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.Random;

/**
 * 1D spring-wave surface simulation for the blood orb liquid.
 *
 * Each column of the surface has a height (deviation from the fill line) and a
 * velocity. Columns spring back toward rest and pull on their neighbors, which
 * is what produces travelling waves. Gameplay injects energy via {@link #splash};
 * damping bleeds it back out so the surface settles.
 *
 * Ticked at 20 TPS from the client tick; the renderer interpolates between the
 * previous and current tick with the partial tick for smooth 60+ fps motion.
 */
@OnlyIn(Dist.CLIENT)
public final class BloodWaveSim {

    /** Columns across the orb. More = smoother waves, still trivially cheap. */
    public static final int COLUMNS = 48;

    // Tuned for 20 ticks/second. TENSION pulls a column toward the rest line,
    // SPREAD couples neighbors (wave propagation speed), DAMPING settles it.
    private static final float TENSION = 0.06f;
    private static final float SPREAD = 0.18f;
    private static final float DAMPING = 0.92f;
    private static final int SPREAD_PASSES = 2;
    /** Waves can't exceed this many GUI pixels from the fill line. */
    private static final float MAX_AMPLITUDE = 6.0f;

    private final float[] height = new float[COLUMNS];
    private final float[] prevHeight = new float[COLUMNS];
    private final float[] velocity = new float[COLUMNS];
    private final Random random = new Random();

    public void tick() {
        System.arraycopy(height, 0, prevHeight, 0, COLUMNS);

        for (int i = 0; i < COLUMNS; i++) {
            velocity[i] += -TENSION * height[i];
        }

        float[] leftDelta = new float[COLUMNS];
        float[] rightDelta = new float[COLUMNS];
        for (int pass = 0; pass < SPREAD_PASSES; pass++) {
            for (int i = 0; i < COLUMNS; i++) {
                if (i > 0) {
                    leftDelta[i] = SPREAD * (height[i] - height[i - 1]);
                    velocity[i - 1] += leftDelta[i];
                }
                if (i < COLUMNS - 1) {
                    rightDelta[i] = SPREAD * (height[i] - height[i + 1]);
                    velocity[i + 1] += rightDelta[i];
                }
            }
            for (int i = 0; i < COLUMNS; i++) {
                if (i > 0) height[i - 1] += leftDelta[i];
                if (i < COLUMNS - 1) height[i + 1] += rightDelta[i];
            }
        }

        for (int i = 0; i < COLUMNS; i++) {
            height[i] += velocity[i];
            velocity[i] *= DAMPING;
            height[i] = Mth.clamp(height[i], -MAX_AMPLITUDE, MAX_AMPLITUDE);
        }
    }

    /**
     * Slap the surface. Strength in GUI pixels; a few random columns get hit so
     * repeated splashes never look identical.
     */
    public void splash(float strength) {
        int hits = 1 + random.nextInt(3);
        for (int hit = 0; hit < hits; hit++) {
            int center = random.nextInt(COLUMNS);
            // Soft falloff around the hit column
            for (int off = -3; off <= 3; off++) {
                int i = center + off;
                if (i < 0 || i >= COLUMNS) continue;
                float falloff = 1.0f - Math.abs(off) / 4.0f;
                velocity[i] += strength * falloff * (random.nextFloat() * 0.5f + 0.75f);
            }
        }
    }

    /**
     * Directional slosh from camera/body rotation: positive strength pushes the
     * right side of the surface down and the left side up (liquid inertia).
     */
    public void tilt(float strength) {
        for (int i = 0; i < COLUMNS; i++) {
            float lever = (i / (float) (COLUMNS - 1)) - 0.5f; // -0.5 left .. +0.5 right
            velocity[i] += strength * lever * 2.0f;
        }
    }

    /**
     * Uniform vertical push (jumping/landing inertia): positive strength presses
     * the whole surface down; the spring rebound produces the bounce-back.
     * Slight per-column randomness keeps it from looking mechanical.
     */
    public void heave(float strength) {
        for (int i = 0; i < COLUMNS; i++) {
            velocity[i] += strength * (0.85f + random.nextFloat() * 0.3f);
        }
    }

    /** Interpolated height deviation for a column, in GUI pixels. */
    public float sampleHeight(int column, float partialTick) {
        return Mth.lerp(partialTick, prevHeight[column], height[column]);
    }

    public void reset() {
        java.util.Arrays.fill(height, 0);
        java.util.Arrays.fill(prevHeight, 0);
        java.util.Arrays.fill(velocity, 0);
    }
}
