/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Holder
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.biome.Biome
 */
package dev.protomanly.pmweather.compat;

import dev.protomanly.pmweather.PMWeather;
import java.lang.reflect.Method;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

public class SereneSeasons {
    private static boolean hasCheckInstalled = false;
    private static boolean installed = false;
    private static Method method_sereneseasons_getBiomeTemperature;

    public static float getBiomeTemperature(Level level, Holder<Biome> biome, BlockPos pos) {
        pos = new BlockPos(pos.getX(), level.getSeaLevel(), pos.getZ());
        if (SereneSeasons.isInstalled() && method_sereneseasons_getBiomeTemperature != null) {
            try {
                return ((Float)method_sereneseasons_getBiomeTemperature.invoke(null, level, biome, pos)).floatValue();
            }
            catch (Exception e) {
                PMWeather.LOGGER.error(e.getMessage(), (Throwable)e);
            }
        }
        return ((Biome)biome.value()).getModifiedClimateSettings().temperature();
    }

    public static boolean isInstalled() {
        if (!hasCheckInstalled) {
            try {
                hasCheckInstalled = true;
                Class<?> class_sereneseasons_SeasonHooks = Class.forName("sereneseasons.season.SeasonHooks");
                if (class_sereneseasons_SeasonHooks != null) {
                    method_sereneseasons_getBiomeTemperature = class_sereneseasons_SeasonHooks.getDeclaredMethod("getBiomeTemperature", Level.class, Holder.class, BlockPos.class);
                    installed = true;
                }
            }
            catch (Exception e) {
                installed = false;
            }
            if (installed) {
                PMWeather.LOGGER.info("PMWeather Compatibility found Serene Seasons");
            } else {
                PMWeather.LOGGER.info("PMWeather Compatibility did not find Serene Seasons");
            }
        }
        return installed;
    }
}

