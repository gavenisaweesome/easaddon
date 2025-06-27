/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Holder
 *  net.minecraft.core.Holder$Reference
 *  net.minecraft.core.Vec3i
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.util.Mth
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.biome.Biome
 *  net.minecraft.world.level.biome.Biomes
 *  net.minecraft.world.level.levelgen.Heightmap$Types
 *  net.minecraft.world.level.levelgen.synth.SimplexNoise
 *  net.minecraft.world.phys.Vec2
 *  net.minecraft.world.phys.Vec3
 */
package dev.protomanly.pmweather.weather;

import dev.protomanly.pmweather.block.entity.RadarBlockEntity;
import dev.protomanly.pmweather.compat.SereneSeasons;
import dev.protomanly.pmweather.config.ServerConfig;
import dev.protomanly.pmweather.util.Util;
import dev.protomanly.pmweather.weather.Storm;
import dev.protomanly.pmweather.weather.WeatherHandler;
import dev.protomanly.pmweather.weather.WindEngine;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class ThermodynamicEngine {
    public static SimplexNoise noise = null;
    public static float xzScale = 15000.0f;
    public static float yScale = 2000.0f;
    public static float timeScale = 20000.0f;
    public static float cachedBiomeTemp = 0.0f;
    public static float cachedHumidity = 0.0f;
    public static float cachedPBLHeight = 0.0f;
    public static float cachedSfcTNoise = 0.0f;
    public static float cachedPNoise = 0.0f;
    public static float cachedNoise = 0.0f;
    public static float cachedTime = 0.0f;
    public static Vec3 cachedPos = null;

    public static float FBM(Vec3 pos, int octaves, float lacunarity, float gain, float amplitude) {
        double y = 0.0;
        for (int i = 0; i < Math.max(octaves, 1); ++i) {
            y += (double)amplitude * noise.getValue(pos.x, pos.y, pos.z);
            pos = pos.multiply((double)lacunarity, (double)lacunarity, (double)lacunarity);
            amplitude *= gain;
        }
        return (float)y;
    }

    public static Precipitation getPrecipitationType(WeatherHandler weatherHandler, Vec3 pos, Level level, int advance) {
        return ThermodynamicEngine.getPrecipitationType(weatherHandler, pos, level, advance, 250);
    }

    public static Precipitation getPrecipitationType(WeatherHandler weatherHandler, Vec3 pos, Level level, int advance, int delta) {
        int start = 4000;
        Precipitation precip = Precipitation.SNOW;
        float groundTemp = ThermodynamicEngine.samplePoint(weatherHandler, pos, level, null, advance).temperature();
        for (int y = start; y >= 0; y -= delta) {
            float rainTemp = ThermodynamicEngine.samplePoint(weatherHandler, pos.add(0.0, (double)y, 0.0), level, null, advance).temperature();
            if (rainTemp < 3.0f && rainTemp > -1.0f) {
                precip = Precipitation.WINTRY_MIX;
                continue;
            }
            if (rainTemp <= 0.0f) {
                precip = switch (precip.ordinal()) {
                    case 0, 4 -> Precipitation.SLEET;
                    default -> precip;
                };
                continue;
            }
            precip = switch (precip.ordinal()) {
                case 2, 3, 4 -> Precipitation.RAIN;
                default -> precip;
            };
        }
        if ((precip == Precipitation.RAIN || precip == Precipitation.WINTRY_MIX) && groundTemp <= 0.0f) {
            precip = Precipitation.FREEZING_RAIN;
        }
        return precip;
    }

    public static AtmosphericDataPoint samplePoint(WeatherHandler weatherHandler, Vec3 pos, Level level, @Nullable RadarBlockEntity radarBlockEntity, int advance) {
        return ThermodynamicEngine.samplePoint(weatherHandler, pos, level, radarBlockEntity, advance, radarBlockEntity != null ? Integer.valueOf(radarBlockEntity.getBlockPos().getY()) : null);
    }

    public static AtmosphericDataPoint samplePoint(WeatherHandler weatherHandler, Vec3 pos, Level level, @Nullable RadarBlockEntity radarBlockEntity, int advance, @Nullable Integer groundHeight) {
        BlockPos blockPos = new BlockPos((int)pos.x, (int)pos.y, (int)pos.z);
        noise = WindEngine.simplexNoise;
        if (noise == null) {
            return new AtmosphericDataPoint(30.0f, 30.0f, 1013.0f, 30.0f);
        }
        float time = level.getDayTime() + (long)advance;
        float biomeTemp = 0.0f;
        float humidity = 0.0f;
        int c = 0;
        boolean cached = false;
        if (cachedPos != null && cachedPos.equals((Object)pos.multiply(1.0, 0.0, 1.0)) && Math.abs(time - cachedTime) < 20.0f) {
            biomeTemp = cachedBiomeTemp;
            humidity = cachedHumidity;
            cached = true;
        } else {
            for (int x = -1; x <= 1; ++x) {
                for (int z = -1; z <= 1; ++z) {
                    if (Mth.abs((int)x) == 1 && Mth.abs((int)z) == 1) continue;
                    ++c;
                    BlockPos p = blockPos.offset(new Vec3i(x * 64, 0, z * 64));
                    Holder<Biome> biome = radarBlockEntity != null && radarBlockEntity.init ? radarBlockEntity.getNearestBiome(p) : level.getBiome(p);
                    biomeTemp += SereneSeasons.getBiomeTemperature(level, biome, p);
                    humidity += ((Biome)biome.value()).getModifiedClimateSettings().downfall();
                }
            }
            cachedPos = pos.multiply(1.0, 0.0, 1.0);
            cachedBiomeTemp = biomeTemp /= (float)c;
            cachedHumidity = humidity /= (float)c;
            cachedTime = time;
        }
        biomeTemp -= 0.15f;
        int elevation = groundHeight != null ? Math.max(level.getSeaLevel(), groundHeight) : Math.max(level.getSeaLevel(), level.getHeight(Heightmap.Types.MOTION_BLOCKING, blockPos.getX(), blockPos.getZ()));
        Holder.Reference biome = level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.PLAINS);
        float gBiomeTemp = SereneSeasons.getBiomeTemperature(level, (Holder<Biome>)biome, blockPos);
        float gHumidity = ((Biome)biome.value()).getModifiedClimateSettings().downfall();
        humidity = Mth.lerp((float)Math.clamp((float)pos.y() / 6000.0f, 0.5f, 0.75f), (float)humidity, (float)gHumidity);
        biomeTemp = Mth.lerp((float)Math.clamp((float)pos.y() / 16000.0f, 0.0f, 0.15f), (float)biomeTemp, (float)(gBiomeTemp - 0.15f));
        float uhumidity = humidity;
        humidity = (float)Math.pow(humidity, 0.3f);
        int elevationSeaLevel = elevation - level.getSeaLevel();
        float aboveSeaLevel = (float)pos.y() - (float)level.getSeaLevel();
        float altitude = Math.max((float)pos.y() - (float)elevation, 0.0f);
        float daytime = (float)(level.getDayTime() + (long)advance) / 24000.0f;
        double x = ((double)daytime - 0.18) * Math.PI * 2.0;
        double timeFactor = Math.sin(x + Math.sin(x) / -2.0);
        float pblHeight = cached ? cachedPBLHeight : ThermodynamicEngine.FBM(pos.multiply((double)(1.0f / xzScale), 0.0, (double)(1.0f / xzScale)).add(0.0, (double)(time / timeScale), 0.0), 2, 2.0f, 0.5f, 1.0f);
        cachedPBLHeight = pblHeight;
        pblHeight = (Math.clamp(pblHeight + 1.0f, 0.0f, 2.0f) + 1.0f) * 500.0f;
        double timeFactorHeightAffected = Mth.lerp((double)Math.clamp(altitude / pblHeight, 0.0f, 1.0f), (double)timeFactor, (double)1.0);
        float sfcPressure = 1013.25f;
        float sfcTNoise = cached ? cachedSfcTNoise : ThermodynamicEngine.FBM(pos.multiply((double)(1.0f / xzScale), 0.0, (double)(1.0f / xzScale)).add(0.0, (double)(time / timeScale), 0.0), 3, 2.0f, 0.5f, 1.0f);
        cachedSfcTNoise = sfcTNoise;
        float sfcTemp = biomeTemp <= 0.0f ? Mth.lerp((float)(-biomeTemp), (float)0.0f, (float)(-20.0f + sfcTNoise)) : Mth.lerp((float)((float)Math.pow((double)biomeTemp / 1.85, 0.5)), (float)0.0f, (float)(35.0f + (sfcTNoise *= 5.0f)));
        sfcTemp += humidity * 3.0f;
        float sfcTempTimeMod = (float)timeFactorHeightAffected * 5.0f * Math.max(1.0f - humidity, 0.05f);
        sfcTemp += (sfcTempTimeMod += 5.0f);
        float tNoise = sfcTNoise / 5.0f;
        float seaLevelTemp = sfcTemp += tNoise * 2.0f;
        sfcTemp -= (float)elevationSeaLevel / 20.0f;
        float pNoise = cached ? cachedPNoise : ThermodynamicEngine.FBM(pos.multiply((double)(1.0f / -xzScale), 0.0, (double)(1.0f / -xzScale)).add(0.0, (double)(time / timeScale), 0.0), 3, 2.0f, 0.5f, 1.0f);
        cachedPNoise = pNoise;
        float seaLevelPressure = sfcPressure += pNoise * 7.0f;
        float stormCooling = 0.0f;
        for (Storm storm : weatherHandler.getStorms()) {
            float start;
            if (storm.stormType != 1) continue;
            double distance = pos.multiply(1.0, 0.0, 1.0).distanceTo(storm.position.multiply(1.0, 0.0, 1.0));
            Vec2 v2fWorldPos = new Vec2((float)pos.x, (float)pos.z);
            Vec2 stormVel = new Vec2((float)storm.velocity.x, (float)storm.velocity.z);
            Vec2 v2fStormPos = new Vec2((float)storm.position.x, (float)storm.position.z);
            Vec2 right = new Vec2(stormVel.y, -stormVel.x).normalized();
            Vec2 fwd = stormVel.normalized();
            Vec2 le = Util.mulVec2(right, -((float)ServerConfig.stormSize) * 5.0f);
            Vec2 ri = Util.mulVec2(right, (float)ServerConfig.stormSize * 5.0f);
            Vec2 off = Util.mulVec2(fwd, -((float)Math.pow(Mth.clamp((double)(distance / (double)((float)ServerConfig.stormSize * 5.0f)), (double)0.0, (double)1.0), 2.0)) * ((float)ServerConfig.stormSize * 1.5f));
            le = le.add(off);
            ri = ri.add(off);
            le = le.add(v2fStormPos);
            ri = ri.add(v2fStormPos);
            Vec2 nearPoint = Util.nearestPoint(le, ri, v2fWorldPos);
            Vec2 facing = v2fWorldPos.add(nearPoint.negated());
            float behind = -facing.dot(fwd);
            behind += ThermodynamicEngine.FBM(new Vec3(pos.x / (ServerConfig.stormSize * 2.0), pos.z / (ServerConfig.stormSize * 2.0), (double)((float)level.getGameTime() / 20000.0f)), 5, 2.0f, 0.2f, 1.0f) * (float)ServerConfig.stormSize * 0.25f;
            behind += (float)ServerConfig.stormSize;
            float sze = (float)ServerConfig.stormSize * 12.0f;
            if (!(behind > 0.0f)) continue;
            float p = Mth.clamp((float)(Math.abs(behind) / sze), (float)0.0f, (float)1.0f);
            p = p <= (start = 0.02f) ? (p /= start) : 1.0f - (p - start) / (1.0f - start);
            stormCooling = Math.max(stormCooling, Mth.clamp((float)p, (float)0.0f, (float)1.0f) * 15.0f * (float)Math.pow((float)storm.coldEnergy / (float)storm.maxColdEnergy, 0.75));
        }
        float h = humidity;
        h = h > 0.5f ? (float)Math.pow(2.0f * (h - 0.5f), 0.25) + 0.5f : (float)Math.pow(2.0f * h, 4.0) * 0.5f;
        float dewP = Mth.clamp((float)((float)Mth.lerp((double)0.7, (double)((noise.getValue(pos.z / 2200.0, (double)(time / 9000.0f) + pos.y / 100.0, pos.x / 300.0) + 1.0) / 2.0), (double)h)), (float)0.2f, (float)1.0f);
        float sfcDew = (sfcTemp -= (stormCooling *= 1.0f - Math.clamp((float)advance / 12000.0f, 0.0f, 1.0f)) * Math.clamp(1.0f - altitude / 3000.0f, 0.0f, 1.0f)) - sfcTempTimeMod - Math.clamp((1.0f - dewP) * (sfcTemp - sfcTempTimeMod), 0.0f, 10.0f);
        sfcDew *= (float)Math.pow(humidity, 0.3f) * 0.75f + 0.25f;
        sfcDew = Math.min(sfcDew, sfcTemp);
        sfcPressure = ThermodynamicEngine.getPressureAtHeight(elevationSeaLevel, sfcTemp, sfcPressure);
        float t = sfcTemp;
        float dp = sfcDew;
        float lapseRate = 5.5f;
        float lrNoise = tNoise;
        if (lrNoise > 0.0f) {
            lrNoise = (float)Math.pow(lrNoise, 1.25);
            lrNoise *= 2.0f;
        }
        lapseRate += lrNoise;
        float dewRatio = tNoise;
        dewRatio = Mth.lerp((float)((dewRatio + 1.0f) / 2.0f), (float)Mth.lerp((float)humidity, (float)0.4f, (float)0.1f), (float)Mth.lerp((float)humidity, (float)0.65f, (float)0.3f));
        t -= (lapseRate *= 0.4f + (1.0f - uhumidity)) * (altitude / 1000.0f);
        dp -= lapseRate * (altitude / 1000.0f) * dewRatio;
        float noise = cached ? cachedNoise : ThermodynamicEngine.FBM(pos.multiply((double)(1.0f / xzScale), 0.0, (double)(1.0f / -xzScale)).add(0.0, (double)(time / timeScale), 0.0), 2, 2.0f, 0.5f, 1.0f);
        cachedNoise = noise;
        float bumpH = (float)elevation + Math.clamp(noise + 0.5f, 0.5f, 1.5f) * 1250.0f;
        noise = ThermodynamicEngine.FBM(pos.multiply((double)(1.0f / -xzScale), 0.0, (double)(1.0f / xzScale)).add(0.0, (double)(time / timeScale), 0.0), 2, 2.0f, 0.5f, 1.0f);
        float bumpStrength = Math.clamp(noise + 0.5f, 0.0f, 1.5f) * 5.5f * Math.clamp(1.0f - humidity, 0.0f, 1.0f);
        bumpStrength -= 4.0f * humidity;
        if (altitude > bumpH) {
            float i = Math.clamp((altitude - bumpH) / 150.0f, 0.0f, 1.0f);
            t += Mth.lerp((float)i, (float)0.0f, (float)bumpStrength);
            dp -= Mth.lerp((float)i, (float)0.0f, (float)bumpStrength);
        }
        float a = Math.clamp(altitude, 0.0f, 1000.0f);
        t -= lapseRate * (a / 1000.0f) * 0.25f;
        dp -= lapseRate * (a / 1000.0f) * dewRatio * 0.25f;
        noise = ThermodynamicEngine.FBM(pos.multiply((double)(1.0f / xzScale), 0.0, (double)(1.0f / xzScale)).add(0.0, (double)(time / timeScale), 0.0), 2, 2.0f, 0.5f, 1.0f);
        float inversionHeight = (float)elevationSeaLevel + Mth.lerp((float)Math.clamp(noise, 0.0f, 1.0f), (float)12000.0f, (float)16000.0f);
        if (altitude > inversionHeight) {
            float dif = altitude - inversionHeight;
            float i = Math.clamp(dif / 1500.0f, 0.0f, 1.0f);
            t += Mth.lerp((float)i, (float)0.0f, (float)(lapseRate * (dif / 1000.0f)));
            dp += Mth.lerp((float)i, (float)0.0f, (float)(lapseRate * (dif / 1000.0f) * dewRatio));
        }
        float offset = ThermodynamicEngine.FBM(pos.multiply((double)(1.0f / xzScale), (double)(1.0f / yScale), (double)(1.0f / xzScale)).add(0.0, (double)(time / -timeScale), 0.0), 4, 2.0f, 0.5f, 1.0f);
        dp -= offset * 1.5f;
        float p = ThermodynamicEngine.getPressureAtHeight(aboveSeaLevel, t += (offset *= 1.5f), elevationSeaLevel, sfcPressure);
        float dewMin = ThermodynamicEngine.FBM(pos.multiply((double)(1.0f / xzScale), (double)(1.0f / yScale), (double)(1.0f / xzScale)).add(0.0, (double)(time / -timeScale), 0.0), 4, 2.0f, 0.5f, 1.0f);
        dewMin = Math.clamp(dewMin + 1.0f, 0.0f, 2.0f) * 2.0f;
        float td = t - (dewMin += (float)Math.pow(pos.y / 16000.0, 2.0) * 40.0f * (1.0f - humidity));
        if (dp > td) {
            float dif = dp - td;
            dp -= dif * Math.clamp(dif / 4.0f, 0.0f, 1.0f);
        }
        dp = Math.min(t, dp);
        return new AtmosphericDataPoint(t, dp, p, ThermodynamicEngine.calcVTemp(t, dp, sfcPressure));
    }

    public static float getPressureAtHeight(float altitude, float temp, float sfcPressure) {
        return ThermodynamicEngine.getPressureAtHeight(altitude, temp, 0.0f, sfcPressure);
    }

    public static float getPressureAtHeight(float altitude, float temp, float refAltitude, float refPressure) {
        return refPressure * (float)Math.exp(-(0.2841926f * (altitude - refAltitude) / (8.31432f * ThermodynamicEngine.celsiusToKelvin(temp))));
    }

    public static float kelvinToCelsius(float k) {
        return k - 273.15f;
    }

    public static float celsiusToKelvin(float c) {
        return c + 273.15f;
    }

    public static float calcVTemp(float t, float dp, float p) {
        return ThermodynamicEngine.kelvinToCelsius(ThermodynamicEngine.celsiusToKelvin(t) / (1.0f - 0.379f * (6.11f * (float)Math.pow(10.0, 7.5f * dp / (237.3f + dp)) / p)));
    }

    public static AtmosphericDataPoint deserializeDataPoint(CompoundTag data) {
        return new AtmosphericDataPoint(data.getFloat("temperature"), data.getFloat("dewpoint"), data.getFloat("pressure"), data.getFloat("virtualTemperature"));
    }

    public static enum Precipitation {
        RAIN,
        FREEZING_RAIN,
        SLEET,
        SNOW,
        WINTRY_MIX,
        HAIL;

    }

    public record AtmosphericDataPoint(float temperature, float dewpoint, float pressure, float virtualTemperature) {
        @Override
        public String toString() {
            return String.format("Temperature: %s, DewPoint: %s, Pressure: %s, Virtual Temperature: %s", Math.floor(this.temperature * 10.0f) / 10.0, Math.floor(this.dewpoint * 10.0f) / 10.0, Math.floor(this.pressure * 10.0f) / 10.0, Math.floor(this.virtualTemperature * 10.0f) / 10.0);
        }

        public CompoundTag serializeNBT() {
            CompoundTag data = new CompoundTag();
            data.putFloat("temperature", this.temperature);
            data.putFloat("dewpoint", this.dewpoint);
            data.putFloat("pressure", this.pressure);
            data.putFloat("virtualTemperature", this.virtualTemperature);
            return data;
        }
    }
}

