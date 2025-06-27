/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.util.Mth
 *  net.minecraft.util.RandomSource
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.levelgen.Heightmap$Types
 *  net.minecraft.world.level.levelgen.LegacyRandomSource
 *  net.minecraft.world.level.levelgen.synth.SimplexNoise
 *  net.minecraft.world.phys.Vec2
 *  net.minecraft.world.phys.Vec3
 */
package dev.protomanly.pmweather.weather;

import dev.protomanly.pmweather.PMWeather;
import dev.protomanly.pmweather.config.ServerConfig;
import dev.protomanly.pmweather.event.GameBusClientEvents;
import dev.protomanly.pmweather.event.GameBusEvents;
import dev.protomanly.pmweather.util.Util;
import dev.protomanly.pmweather.weather.Storm;
import dev.protomanly.pmweather.weather.WeatherHandler;
import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class WindEngine {
    public static SimplexNoise simplexNoise;

    public static void init(WeatherHandler weatherHandler) {
        simplexNoise = new SimplexNoise((RandomSource)new LegacyRandomSource(weatherHandler.seed));
    }

    public static double FBM(Vec3 pos, int octaves, float lacunarity, float gain, float amplitude) {
        double y = 0.0;
        if (simplexNoise != null) {
            for (int i = 0; i < Math.max(octaves, 1); ++i) {
                y += (double)amplitude * simplexNoise.getValue(pos.x, pos.y, pos.z);
                pos = pos.multiply((double)lacunarity, (double)lacunarity, (double)lacunarity);
                amplitude *= gain;
            }
        }
        return y;
    }

    public static float getSwirl(Vec3 position, Level level, float sampleSize) {
        Vec3 sample1Z = WindEngine.getWind(position.add(0.0, 0.0, (double)sampleSize), level).normalize();
        Vec3 sample2Z = WindEngine.getWind(position.add(0.0, 0.0, (double)(-sampleSize)), level).normalize();
        Vec3 sample1X = WindEngine.getWind(position.add((double)(-sampleSize), 0.0, 0.0), level).normalize();
        Vec3 sample2X = WindEngine.getWind(position.add((double)sampleSize, 0.0, 0.0), level).normalize();
        double compZ = (-sample1Z.dot(sample2Z) + 1.0) / 2.0;
        double compX = (-sample1X.dot(sample2X) + 1.0) / 2.0;
        return (float)(compZ * compX);
    }

    public static Vec3 getWind(Vec3 position, Level level) {
        return WindEngine.getWind(position, level, false, false, true);
    }

    public static Vec3 getWind(Vec3 position, Level level, boolean ignoreStorms, boolean ignoreTornadoes, boolean windCheck) {
        float val;
        int heightAbove;
        Vec3 wind = Vec3.ZERO;
        BlockPos blockPos = new BlockPos((int)position.x, (int)position.y, (int)position.z);
        ArrayList<Storm> tornadicStorms = new ArrayList<Storm>();
        if (level == null) {
            PMWeather.LOGGER.warn("Level is null");
            return wind;
        }
        int worldHeight = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, blockPos).getY();
        if (windCheck ? !Util.canWindAffect(position, level) : position.y < (double)worldHeight) {
            return wind;
        }
        if (simplexNoise != null) {
            float timeScale = 20000.0f;
            float scale = 12000.0f;
            double ang = WindEngine.FBM(new Vec3(position.x / (double)(scale * 3.0f), position.z / (double)(scale * 3.0f), (double)((float)level.getGameTime() / (timeScale * 6.0f))), 5, 2.0f, 0.1f, 1.0f);
            Vec3 dir = new Vec3(Math.cos(ang *= Math.PI), 0.0, Math.sin(ang)).normalize();
            double speed = Math.max(simplexNoise.getValue(-position.z / (double)scale, -position.x / (double)scale, (double)(-((float)level.getGameTime()) / timeScale)) + 1.0, 0.0) * 10.0;
            wind = wind.add(dir.multiply(speed, speed, speed));
            WeatherHandler weatherHandler = level.isClientSide() ? GameBusClientEvents.weatherHandler : GameBusEvents.MANAGERS.get(level.dimension());
            if (weatherHandler != null && !ignoreStorms) {
                for (Storm storm : weatherHandler.getStorms()) {
                    if (storm.visualOnly) continue;
                    if (storm.stage >= 3 && storm.stormType == 0) {
                        tornadicStorms.add(storm);
                    }
                    double distance = position.multiply(1.0, 0.0, 1.0).distanceTo(storm.position.multiply(1.0, 0.0, 1.0));
                    if (storm.stormType == 1) {
                        Vec2 v2fWorldPos = new Vec2((float)position.x, (float)position.z);
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
                        float d = Util.minimumDistance(le, ri, v2fWorldPos);
                        Vec2 nearPoint = Util.nearestPoint(le, ri, v2fWorldPos);
                        Vec2 facing = v2fWorldPos.add(nearPoint.negated());
                        float behind = -facing.dot(fwd);
                        behind += (float)WindEngine.FBM(new Vec3(position.x / (ServerConfig.stormSize * 2.0), position.z / (ServerConfig.stormSize * 2.0), (double)((float)level.getGameTime() / timeScale)), 5, 2.0f, 0.2f, 1.0f) * (float)ServerConfig.stormSize * 0.25f;
                        float perc = 0.0f;
                        float sze = (float)ServerConfig.stormSize * 4.0f;
                        if ((behind += (float)ServerConfig.stormSize) > 0.0f) {
                            float p = Mth.clamp((float)(Math.abs(behind) / sze), (float)0.0f, (float)1.0f);
                            float start = 0.06f;
                            if (storm.stage >= 3) {
                                start = Mth.lerp((float)((float)storm.energy / 100.0f), (float)start, (float)(start * 2.5f));
                            }
                            if (p <= start) {
                                p /= start;
                            } else {
                                p = 1.0f - (p - start) / (1.0f - start);
                                if (storm.stage >= 3) {
                                    p = (float)Math.pow(p, Mth.lerp((float)((float)storm.energy / 100.0f), (float)1.0f, (float)0.75f));
                                }
                            }
                            perc = Mth.clamp((float)p, (float)0.0f, (float)1.0f);
                        }
                        perc = storm.stage < 1 ? (perc *= (float)storm.energy / 100.0f) : (storm.stage == 1 ? (perc *= (float)storm.energy / 125.0f + 1.0f) : (storm.stage == 2 ? (perc *= (float)storm.energy / 200.0f + 1.8f) : (perc *= (float)storm.energy / 100.0f + 2.3f)));
                        float gustNoise = (float)WindEngine.FBM(new Vec3(position.z / (ServerConfig.stormSize * 2.0), position.x / (ServerConfig.stormSize * 2.0), (double)((float)level.getGameTime() / timeScale)), 7, 2.0f, 0.4f, 1.0f);
                        if (storm.stage >= 3) {
                            float p = (float)storm.energy / 100.0f;
                            gustNoise *= 1.0f - p;
                            perc *= 1.0f + p * 0.3f;
                        }
                        perc *= Mth.lerp((float)Mth.clamp((float)(behind / ((float)ServerConfig.stormSize * 3.0f)), (float)0.0f, (float)1.0f), (float)((float)Math.pow(0.8f + gustNoise * 0.5f, 1.5)), (float)0.5f);
                        wind = wind.add(storm.velocity.multiply((double)((perc *= Mth.sqrt((float)(1.0f - Mth.clamp((float)(d / sze), (float)0.0f, (float)1.0f)))) * 13.0f) * ServerConfig.squallStrengthMultiplier, 0.0, (double)(perc * 13.0f) * ServerConfig.squallStrengthMultiplier));
                    }
                    if (storm.stormType != 0) continue;
                    Vec3 relativePos = position.subtract(storm.position);
                    Vec3 inward = new Vec3(-relativePos.x, 0.0, -relativePos.z).normalize();
                    Vec3 rotational = new Vec3(relativePos.z, 0.0, -relativePos.x).normalize();
                    double pullStrngth = 1.0 - Math.clamp(distance / (ServerConfig.stormSize * 4.0), 0.0, 1.0);
                    double rotStrngth = 1.0 - Math.clamp(distance / ServerConfig.stormSize, 0.0, 1.0);
                    if (storm.stage < 1) {
                        pullStrngth *= 0.5;
                        pullStrngth *= (double)((float)storm.energy / 100.0f);
                        rotStrngth *= 0.0;
                    } else if (storm.stage == 1) {
                        pullStrngth *= (double)((float)storm.energy / 200.0f + 0.5f);
                        rotStrngth *= (double)((float)storm.energy / 100.0f * 0.1f);
                    } else if (storm.stage == 2) {
                        pullStrngth *= (double)(1.0f + (float)storm.energy / 100.0f);
                        rotStrngth *= (double)(0.1f + (float)storm.energy / 100.0f * 0.4f);
                    } else {
                        pullStrngth *= (double)(2.0f + (float)storm.windspeed / 400.0f);
                        rotStrngth *= (double)(0.5f + (float)storm.windspeed / 400.0f);
                    }
                    Vec3 vec = inward.multiply(pullStrngth *= 0.5, 0.0, pullStrngth).add(rotational.multiply(rotStrngth *= 6.0, 0.0, rotStrngth)).multiply(20.0, 20.0, 20.0);
                    wind = wind.add(vec);
                }
            }
        }
        if ((heightAbove = blockPos.getY() - worldHeight) > 0) {
            val = Math.clamp((float)heightAbove / 15.0f, 0.0f, 1.0f) + 1.0f;
            wind = wind.multiply((double)val, (double)val, (double)val);
        }
        if (blockPos.getY() > 85) {
            val = Math.clamp((float)(blockPos.getY() - 85) / 40.0f, 0.0f, 1.0f) + 1.0f;
            wind = wind.multiply((double)val, (double)val, (double)val);
        }
        if (wind.length() > 30.0) {
            double over = wind.length() - 40.0;
            double val2 = 30.0 + over / 3.0;
            wind = wind.normalize().multiply(val2, val2, val2);
        }
        float tornadicEffect = 0.0f;
        Vec3 tornadicWind = Vec3.ZERO;
        if (!ignoreStorms && !ignoreTornadoes) {
            for (Storm tornadicStorm : tornadicStorms) {
                Vec3 relativePos = position.subtract(tornadicStorm.position);
                Vec3 inward = new Vec3(-relativePos.x, 0.0, -relativePos.z).normalize();
                Vec3 rotational = new Vec3(relativePos.z, 0.0, -relativePos.x).normalize();
                double distance = position.distanceTo(tornadicStorm.position);
                if (distance > (double)(tornadicStorm.width * 2.0f)) continue;
                double windEffect = tornadicStorm.getWind(position);
                tornadicEffect = Math.clamp((float)windEffect / (float)Math.max(tornadicStorm.windspeed, 30), tornadicEffect, 1.0f);
                if (Float.isNaN(tornadicEffect)) {
                    tornadicEffect = 0.0f;
                }
                double inPerc = 0.35;
                tornadicWind = tornadicWind.add(inward.multiply(windEffect * inPerc, windEffect * inPerc, windEffect * inPerc)).add(rotational.add(windEffect * (1.0 - inPerc), windEffect * (1.0 - inPerc), windEffect * (1.0 - inPerc)));
            }
        }
        return wind.lerp(tornadicWind, (double)tornadicEffect);
    }

    public static Vec3 getWind(BlockPos position, Level level, boolean ignoreStorms, boolean ignoreTornadoes, boolean windCheck) {
        return WindEngine.getWind(new Vec3((double)position.getX(), (double)(position.getY() + 1), (double)position.getZ()), level, ignoreStorms, ignoreTornadoes, windCheck);
    }

    public static Vec3 getWind(BlockPos position, Level level) {
        return WindEngine.getWind(new Vec3((double)position.getX(), (double)(position.getY() + 1), (double)position.getZ()), level, false, false, true);
    }
}

