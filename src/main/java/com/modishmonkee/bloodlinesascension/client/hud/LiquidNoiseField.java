package com.modishmonkee.bloodlinesascension.client.hud;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.Random;

/**
 * Seamlessly wrapping value-noise field sampled per liquid pixel-cell (no
 * interpolation between lattice reads at render time — sampling on integer
 * cells is what keeps the fluid chunky/pixel-art instead of smooth).
 */
@OnlyIn(Dist.CLIENT)
public final class LiquidNoiseField {

    public static final int SIZE = 64;

    private static float[][] field;

    private LiquidNoiseField() {}

    /** Wrapped lookup, safe for any integer coords (including negative). */
    public static float sample(int x, int y) {
        if (field == null) generate();
        return field[Math.floorMod(x, SIZE)][Math.floorMod(y, SIZE)];
    }

    /**
     * Smooth (bilinear) wrapped lookup at fractional coords — used by the
     * domain-warp swirl so the pattern squishes continuously instead of
     * stepping cell to cell. The pixelation comes from the render grid.
     */
    public static float sampleSmooth(float x, float y) {
        int x0 = (int) Math.floor(x), y0 = (int) Math.floor(y);
        float tx = x - x0, ty = y - y0;
        tx = tx * tx * (3 - 2 * tx);
        ty = ty * ty * (3 - 2 * ty);
        float a = sample(x0, y0), b = sample(x0 + 1, y0);
        float c = sample(x0, y0 + 1), d = sample(x0 + 1, y0 + 1);
        return lerp(lerp(a, b, tx), lerp(c, d, tx), ty);
    }

    private static void generate() {
        float[][] octave1 = tileableValueNoise(8, 42L);   // large slow blobs
        float[][] octave2 = tileableValueNoise(16, 1337L); // fine detail
        field = new float[SIZE][SIZE];
        for (int x = 0; x < SIZE; x++)
            for (int y = 0; y < SIZE; y++)
                field[x][y] = 0.65f * octave1[x][y] + 0.35f * octave2[x][y];
    }

    private static float[][] tileableValueNoise(int lattice, long seed) {
        Random random = new Random(seed);
        float[][] grid = new float[lattice][lattice];
        for (int gx = 0; gx < lattice; gx++)
            for (int gy = 0; gy < lattice; gy++)
                grid[gx][gy] = random.nextFloat();

        float[][] out = new float[SIZE][SIZE];
        float cell = (float) SIZE / lattice;
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                float fx = x / cell, fy = y / cell;
                int x0 = (int) fx, y0 = (int) fy;
                int x1 = (x0 + 1) % lattice, y1 = (y0 + 1) % lattice;
                float tx = smooth(fx - x0), ty = smooth(fy - y0);
                float a = grid[x0 % lattice][y0 % lattice];
                float b = grid[x1][y0 % lattice];
                float c = grid[x0 % lattice][y1];
                float d = grid[x1][y1];
                out[x][y] = lerp(lerp(a, b, tx), lerp(c, d, tx), ty);
            }
        }
        return out;
    }

    private static float smooth(float t) {
        return t * t * (3 - 2 * t);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}
