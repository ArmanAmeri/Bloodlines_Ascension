package com.modishmonkee.bloodlinesascension.client.hud;

import com.modishmonkee.bloodlinesascension.BloodlinesAscension;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.Random;

/**
 * Procedurally generated, seamlessly tiling grayscale noise the liquid scrolls
 * through so the blood reads as churning fluid instead of flat color. Generated
 * once on first use — no texture asset needed.
 */
@OnlyIn(Dist.CLIENT)
public final class LiquidNoiseTexture {

    public static final int SIZE = 64;

    private static ResourceLocation location;

    private LiquidNoiseTexture() {}

    public static ResourceLocation get() {
        if (location == null) {
            location = ResourceLocation.fromNamespaceAndPath(BloodlinesAscension.MOD_ID, "blood_orb_noise");
            NativeImage image = new NativeImage(SIZE, SIZE, false);
            float[][] octave1 = tileableValueNoise(8, 42L);   // large slow blobs
            float[][] octave2 = tileableValueNoise(16, 1337L); // fine detail
            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    float n = 0.65f * octave1[x][y] + 0.35f * octave2[x][y];
                    // Bias bright so the vertex tint controls the palette
                    int v = (int) (150 + n * 105);
                    image.setPixelRGBA(x, y, 0xFF000000 | (v << 16) | (v << 8) | v);
                }
            }
            Minecraft.getInstance().getTextureManager().register(location, new DynamicTexture(image));
        }
        return location;
    }

    /** Value noise on a wrapped lattice — tiles seamlessly in both axes. */
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
