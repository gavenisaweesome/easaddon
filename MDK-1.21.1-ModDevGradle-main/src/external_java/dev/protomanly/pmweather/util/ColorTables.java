/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.util.Mth
 */
package dev.protomanly.pmweather.util;

import java.awt.Color;
import net.minecraft.util.Mth;

public class ColorTables {
    public static Color getReflectivity(float val) {
        Color color = new Color(0, 0, 0, 0);
        color = ColorTables.lerp(Math.clamp(val / 19.0f, 0.0f, 1.0f), color, new Color(6069678));
        color = ColorTables.lerp(Math.clamp((val - 19.0f) / 8.0f, 0.0f, 1.0f), color, new Color(746505));
        color = ColorTables.lerp(Math.clamp((val - 27.0f) / 13.0f, 0.0f, 1.0f), color, new Color(12956416));
        if (val >= 40.0f) {
            color = new Color(16421888);
        }
        color = ColorTables.lerp(Math.clamp((val - 40.0f) / 10.0f, 0.0f, 1.0f), color, new Color(11688204));
        if (val >= 50.0f) {
            color = new Color(16327435);
        }
        color = ColorTables.lerp(Math.clamp((val - 50.0f) / 10.0f, 0.0f, 1.0f), color, new Color(0x822820));
        if (val >= 60.0f) {
            color = new Color(13277620);
        }
        color = ColorTables.lerp(Math.clamp((val - 60.0f) / 10.0f, 0.0f, 1.0f), color, new Color(12721266));
        if (val >= 70.0f) {
            color = new Color(0xFFFFFF);
        }
        return color;
    }

    public static Color getMixedReflectivity(float val) {
        Color color = new Color(255, 255, 255, 0);
        color = ColorTables.lerp(Math.clamp(val / 70.0f, 0.0f, 1.0f), color, new Color(0, 111, 255, 255));
        return color;
    }

    public static Color getSnowReflectivity(float val) {
        Color color = new Color(250, 195, 248, 0);
        color = ColorTables.lerp(Math.clamp(val / 70.0f, 0.0f, 1.0f), color, new Color(210, 0, 210, 255));
        return color;
    }

    public static Color getVelocity(float velocity) {
        Color color = new Color(150, 150, 150);
        if (velocity > 0.0f) {
            color = new Color(9074294);
            color = ColorTables.lerp(Math.clamp(velocity / 12.0f, 0.0f, 1.0f), color, new Color(8665153));
            if (velocity > 12.0f) {
                color = new Color(0x6E0000);
            }
            color = ColorTables.lerp(Math.clamp((velocity - 12.0f) / 27.0f, 0.0f, 1.0f), color, new Color(15925255));
            if (velocity > 39.0f) {
                color = new Color(16398161);
            }
            color = ColorTables.lerp(Math.clamp((velocity - 39.0f) / 30.0f, 0.0f, 1.0f), color, new Color(16771235));
            color = ColorTables.lerp(Math.clamp((velocity - 69.0f) / 71.0f, 0.0f, 1.0f), color, new Color(6751746));
        } else if (velocity < 0.0f) {
            velocity = Mth.abs((float)velocity);
            color = new Color(7505264);
            color = ColorTables.lerp(Math.clamp(velocity / 12.0f, 0.0f, 1.0f), color, new Color(5142860));
            if (velocity > 12.0f) {
                color = new Color(353795);
            }
            color = ColorTables.lerp(Math.clamp((velocity - 12.0f) / 69.0f, 0.0f, 1.0f), color, new Color(0x30E0E3));
            color = ColorTables.lerp(Math.clamp((velocity - 81.0f) / 25.0f, 0.0f, 1.0f), color, new Color(1442457));
            color = ColorTables.lerp(Math.clamp((velocity - 106.0f) / 34.0f, 0.0f, 1.0f), color, new Color(16711812));
        }
        return color;
    }

    public static Color getWindspeed(float val) {
        Color color = new Color(0, 0, 0);
        color = ColorTables.lerp((val - 45.0f) / 20.0f, color, new Color(106, 128, 241));
        color = ColorTables.lerp((val - 65.0f) / 20.0f, color, new Color(117, 243, 224));
        color = ColorTables.lerp((val - 85.0f) / 25.0f, color, new Color(116, 241, 81));
        color = ColorTables.lerp((val - 110.0f) / 25.0f, color, new Color(246, 220, 53));
        color = ColorTables.lerp((val - 135.0f) / 30.0f, color, new Color(246, 127, 53));
        color = ColorTables.lerp((val - 165.0f) / 35.0f, color, new Color(246, 53, 53));
        color = ColorTables.lerp((val - 200.0f) / 50.0f, color, new Color(240, 53, 246));
        color = ColorTables.lerp((val - 250.0f) / 50.0f, color, new Color(255, 255, 255));
        return color;
    }

    public static Color lerp(float delta, Color c1, Color c2) {
        delta = Mth.clamp((float)delta, (float)0.0f, (float)1.0f);
        return new Color((int)Mth.lerp((float)delta, (float)c1.getRed(), (float)c2.getRed()), (int)Mth.lerp((float)delta, (float)c1.getGreen(), (float)c2.getGreen()), (int)Mth.lerp((float)delta, (float)c1.getBlue(), (float)c2.getBlue()));
    }
}

