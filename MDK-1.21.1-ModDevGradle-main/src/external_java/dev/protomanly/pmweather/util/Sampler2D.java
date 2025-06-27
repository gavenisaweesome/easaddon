/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.util.Mth
 */
package dev.protomanly.pmweather.util;

import dev.protomanly.pmweather.PMWeather;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;
import net.minecraft.util.Mth;

public class Sampler2D {
    private final float[] data;
    private final int width;
    private final int height;
    private boolean bilinear = false;

    public Sampler2D(String path) {
        BufferedImage bufferedImage;
        URL url = Thread.currentThread().getContextClassLoader().getResource(path);
        try {
            bufferedImage = ImageIO.read(url);
        }
        catch (IOException e) {
            PMWeather.LOGGER.error(e.getMessage(), (Throwable)e);
            bufferedImage = new BufferedImage(1, 1, 2);
        }
        this.width = bufferedImage.getWidth();
        this.height = bufferedImage.getHeight();
        this.data = new float[this.width * this.height];
        int[] pixels = new int[this.data.length];
        bufferedImage.getRGB(0, 0, this.width, this.height, pixels, 0, this.width);
        for (int i = 0; i < this.data.length; ++i) {
            this.data[i] = (float)(pixels[i] & 0xFF) / 255.0f;
        }
    }

    private static float interpolate2D(float x, float y, float v1, float v2, float v3, float v4) {
        return Mth.lerp((float)y, (float)Mth.lerp((float)x, (float)v1, (float)v2), (float)Mth.lerp((float)x, (float)v3, (float)v4));
    }

    private static long wrap(long value, long side) {
        if (side != 0L && (side & side - 1L) == 0L) {
            return value & side - 1L;
        }
        long r = value - value / side * side;
        return r < 0L ? r + side : r;
    }

    public float sample(float x, float y) {
        long x1 = Mth.floor((float)x);
        long y1 = Mth.floor((float)y);
        long x2 = Sampler2D.wrap(x1 + 1L, this.width);
        long y2 = Sampler2D.wrap(y1 + 1L, this.height);
        float dx = x - (float)x1;
        float dy = y - (float)y1;
        x1 = Sampler2D.wrap(x1, this.width);
        y1 = Sampler2D.wrap(y1, this.height);
        float a = this.data[this.getIndex((int)x1, (int)y1)];
        float b = this.data[this.getIndex((int)x2, (int)y1)];
        float c = this.data[this.getIndex((int)x1, (int)y2)];
        float d = this.data[this.getIndex((int)x2, (int)y2)];
        if (this.bilinear) {
            dx = this.smoothStep(dx);
            dy = this.smoothStep(dy);
        }
        return Sampler2D.interpolate2D(dx, dy, a, b, c, d);
    }

    public void setBilinear(boolean bilinear) {
        this.bilinear = bilinear;
    }

    private int getIndex(int x, int y) {
        return y * this.width + x;
    }

    private float smoothStep(float x) {
        return x * x * x * (x * (x * 6.0f - 15.0f) + 10.0f);
    }
}

