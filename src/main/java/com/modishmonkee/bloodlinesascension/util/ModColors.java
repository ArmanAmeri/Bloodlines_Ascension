package com.modishmonkee.bloodlinesascension.util;

/**
 * The canonical Bloodlines: Ascension palette. ALL art and rendering derives
 * from these five colors (plus the two shadow companions for metal linework).
 * Matches art/bloodlines_palette.gpl — keep the two in sync.
 */
public final class ModColors {

    /** Black with a blood-red tint — backgrounds, voids, deepest liquid. */
    public static final int BLOOD_BLACK = 0xFF0D0406;
    /** Dark blood red — body color, dried/venous blood. */
    public static final int BLOOD_DARK = 0xFF6B0F16;
    /** Bright blood red — arterial highlights, meniscus, fresh blood. */
    public static final int BLOOD_BRIGHT = 0xFFC8202C;
    /** Silver — frames, linings, lesser-rank trim. */
    public static final int SILVER = 0xFFC0C4CC;
    /** Gold — frames, linings, noble/elder-rank trim. */
    public static final int GOLD = 0xFFD9A93F;

    /** Shadow companion for silver linework (outlines, inner edges). */
    public static final int SILVER_SHADOW = 0xFF7A7E88;
    /** Shadow companion for gold linework. */
    public static final int GOLD_SHADOW = 0xFF8F6B22;

    private ModColors() {}

    public static float red(int argb) {
        return ((argb >> 16) & 0xFF) / 255f;
    }

    public static float green(int argb) {
        return ((argb >> 8) & 0xFF) / 255f;
    }

    public static float blue(int argb) {
        return (argb & 0xFF) / 255f;
    }

    /** {r, g, b} floats 0..1 for vertex coloring. */
    public static float[] rgb(int argb) {
        return new float[]{red(argb), green(argb), blue(argb)};
    }

    /** Linear blend between two packed colors (alpha forced opaque). */
    public static int mix(int argbA, int argbB, float t) {
        int r = (int) (((argbA >> 16) & 0xFF) * (1 - t) + ((argbB >> 16) & 0xFF) * t);
        int g = (int) (((argbA >> 8) & 0xFF) * (1 - t) + ((argbB >> 8) & 0xFF) * t);
        int b = (int) ((argbA & 0xFF) * (1 - t) + (argbB & 0xFF) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
