/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.util.Mth
 *  org.joml.Vector2f
 *  org.joml.Vector3f
 *  org.joml.Vector3fc
 */
package dev.protomanly.pmweather.util;

import dev.protomanly.pmweather.util.Sampler2D;
import net.minecraft.util.Mth;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class ShaderCompatibleNoise {
    public static final Sampler2D noiseSampler = new Sampler2D("assets/minecraft/textures/effect/pmweather/noise.png");
    public static final Sampler2D noiseXSampler = new Sampler2D("assets/minecraft/textures/effect/pmweather/noisex.png");

    public static float noise2D(Vector2f x) {
        x = x.div(512.0f);
        return (noiseXSampler.sample(x.x, x.y) - 0.5f) * 2.0f;
    }

    public static float noise(Vector3f x) {
        x = x.div(300.0f, 540.0f, 300.0f);
        x.y = ShaderCompatibleNoise.fract(x.y) * 512.0f;
        float iz = Mth.floor((float)x.y);
        float fz = ShaderCompatibleNoise.fract(x.y);
        Vector2f a_off = new Vector2f(23.0f, 29.0f).mul(iz).div(512.0f);
        Vector2f b_off = new Vector2f(23.0f, 29.0f).mul(iz + 1.0f).div(512.0f);
        float a = noiseSampler.sample(x.x + a_off.x, x.z + a_off.y);
        float b = noiseSampler.sample(x.x + b_off.x, x.z + b_off.y);
        return (ShaderCompatibleNoise.mix(a, b, fz) - 0.5f) * 2.0f;
    }

    public static float fract(float n) {
        return n - (float)Mth.floor((float)n);
    }

    public static Vector3f fract(Vector3f n) {
        return new Vector3f(ShaderCompatibleNoise.fract(n.x), ShaderCompatibleNoise.fract(n.y), ShaderCompatibleNoise.fract(n.z));
    }

    public static Vector3f floor(Vector3f n) {
        return new Vector3f((float)Mth.floor((float)n.x), (float)Mth.floor((float)n.y), (float)Mth.floor((float)n.z));
    }

    public static float mix(float s, float e, float a) {
        return Mth.lerp((float)a, (float)s, (float)e);
    }

    public static Vector3f mix(Vector3f s, Vector3f e, float a) {
        return s.lerp((Vector3fc)e, a);
    }

    public static float hash(float p) {
        p = ShaderCompatibleNoise.fract(p * 0.1031f);
        p *= p + 33.33f;
        p *= p + p;
        return ShaderCompatibleNoise.fract(p);
    }

    public static float onoise(Vector3f pos) {
        Vector3f x = pos.mul(2.0f);
        Vector3f p = ShaderCompatibleNoise.floor(x);
        Vector3f f = ShaderCompatibleNoise.fract(x);
        f = f.mul((Vector3fc)f).mul((Vector3fc)new Vector3f(3.0f, 3.0f, 3.0f).sub((Vector3fc)f.mul(2.0f)));
        float n = p.x + p.y * 57.0f + 113.0f * p.z;
        return ShaderCompatibleNoise.mix(ShaderCompatibleNoise.mix(ShaderCompatibleNoise.mix(ShaderCompatibleNoise.hash(n + 0.0f), ShaderCompatibleNoise.hash(n + 1.0f), f.x), ShaderCompatibleNoise.mix(ShaderCompatibleNoise.hash(n + 57.0f), ShaderCompatibleNoise.hash(n + 58.0f), f.x), f.y), ShaderCompatibleNoise.mix(ShaderCompatibleNoise.mix(ShaderCompatibleNoise.hash(n + 113.0f), ShaderCompatibleNoise.hash(n + 114.0f), f.x), ShaderCompatibleNoise.mix(ShaderCompatibleNoise.hash(n + 170.0f), ShaderCompatibleNoise.hash(n + 171.0f), f.x), f.y), f.z);
    }

    public static float fbm(Vector3f x, int octaves, float lacunarity, float gain, float amplitude) {
        float y = 0.0f;
        for (int i = 0; i < Math.max(octaves, 1); ++i) {
            y += amplitude * ShaderCompatibleNoise.noise(x);
            x = x.mul(lacunarity);
            amplitude *= gain;
        }
        return y;
    }
}

