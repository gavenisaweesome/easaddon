/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.Tag
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.util.Mth
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.Vec2
 *  net.minecraft.world.phys.Vec3
 */
package dev.protomanly.pmweather.weather;

import dev.protomanly.pmweather.PMWeather;
import dev.protomanly.pmweather.config.ServerConfig;
import dev.protomanly.pmweather.data.LevelSavedData;
import dev.protomanly.pmweather.interfaces.IWorldData;
import dev.protomanly.pmweather.util.Util;
import dev.protomanly.pmweather.weather.Storm;
import dev.protomanly.pmweather.weather.WeatherHandlerServer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public abstract class WeatherHandler
implements IWorldData {
    private List<Storm> storms = new ArrayList<Storm>();
    private ResourceKey<Level> dimension;
    public HashMap<Long, Storm> lookupStormByID = new HashMap();
    public long seed;

    public WeatherHandler(ResourceKey<Level> dimension) {
        this.dimension = dimension;
    }

    public void tick() {
        Level level = this.getWorld();
        if (level != null) {
            List<Storm> stormList = this.getStorms();
            for (int i = 0; i < stormList.size(); ++i) {
                Storm storm = stormList.get(i);
                WeatherHandler weatherHandler = this;
                if (weatherHandler instanceof WeatherHandlerServer) {
                    WeatherHandlerServer weatherHandlerServer = (WeatherHandlerServer)weatherHandler;
                    if (storm.dead) {
                        this.removeStorm(storm.ID);
                        weatherHandlerServer.syncStormRemove(storm);
                        continue;
                    }
                }
                if (!storm.dead) {
                    storm.tick();
                    continue;
                }
                this.removeStorm(storm.ID);
            }
        }
    }

    public List<Storm> getStorms() {
        return this.storms;
    }

    public void addStorm(Storm storm) {
        if (!this.lookupStormByID.containsKey(storm.ID)) {
            this.storms.add(storm);
            this.lookupStormByID.put(storm.ID, storm);
        } else {
            PMWeather.LOGGER.warn("Tried to add a storm with existing ID: {}", (Object)storm.ID);
        }
    }

    public void removeStorm(long id) {
        Storm storm = this.lookupStormByID.get(id);
        if (storm != null) {
            storm.remove();
            this.storms.remove(storm);
            this.lookupStormByID.remove(id);
        } else {
            PMWeather.LOGGER.warn("Tried to remove a non-existent storm with ID: {}", (Object)id);
        }
    }

    public float getPrecipitation(Vec3 pos) {
        float precip = 0.0f;
        for (Storm storm : this.getStorms()) {
            if (storm.visualOnly) continue;
            double dist = pos.distanceTo(new Vec3(storm.position.x, pos.y, storm.position.z));
            double perc = 0.0;
            float smoothStage = (float)storm.stage + (float)storm.energy / 100.0f;
            if (storm.stage == 3) {
                smoothStage = 3.0f;
            }
            if (storm.stormType == 1) {
                Vec2 v2fWorldPos = new Vec2((float)pos.x, (float)pos.z);
                Vec2 stormVel = new Vec2((float)storm.velocity.x, (float)storm.velocity.z);
                Vec2 v2fStormPos = new Vec2((float)storm.position.x, (float)storm.position.z);
                Vec2 right = new Vec2(stormVel.y, -stormVel.x).normalized();
                Vec2 fwd = stormVel.normalized();
                Vec2 le = Util.mulVec2(right, -((float)ServerConfig.stormSize) * 5.0f);
                Vec2 ri = Util.mulVec2(right, (float)ServerConfig.stormSize * 5.0f);
                Vec2 off = Util.mulVec2(fwd, -((float)Math.pow(Mth.clamp((double)(dist / (double)((float)ServerConfig.stormSize * 5.0f)), (double)0.0, (double)1.0), 2.0)) * ((float)ServerConfig.stormSize * 1.5f));
                le = le.add(off);
                ri = ri.add(off);
                float d = Util.minimumDistance(le = le.add(v2fStormPos), ri = ri.add(v2fStormPos), v2fWorldPos);
                if ((double)d > ServerConfig.stormSize * 16.0) continue;
                Vec2 nearPoint = Util.nearestPoint(le, ri, v2fWorldPos);
                Vec2 facing = v2fWorldPos.add(nearPoint.negated());
                float behind = -facing.dot(fwd);
                float sze = (float)ServerConfig.stormSize * 1.5f;
                sze *= Mth.lerp((float)Mth.clamp((float)(smoothStage - 1.0f), (float)0.0f, (float)1.0f), (float)4.0f, (float)12.0f);
                if ((behind += (float)ServerConfig.stormSize / 2.0f) > 0.0f) {
                    float start;
                    float p = Mth.clamp((float)(Math.abs(behind) / sze), (float)0.0f, (float)1.0f);
                    p = p <= (start = 0.06f) ? (p /= start) : 1.0f - (p - start) / (1.0f - start);
                    perc = (float)Math.pow(Mth.clamp((float)p, (float)0.0f, (float)1.0f), 3.0);
                }
                if (storm.stage <= 0) {
                    perc = 0.0;
                } else if (storm.stage == 1) {
                    perc *= (double)((float)storm.energy / 100.0f);
                }
                perc *= (double)Mth.sqrt((float)(1.0f - Mth.clamp((float)(d / sze), (float)0.0f, (float)1.0f)));
            }
            if (storm.stormType == 0) {
                double coreDist = pos.distanceTo(new Vec3(storm.position.x + 2000.0, pos.y, storm.position.z - 900.0));
                if (Math.min(dist, coreDist) > ServerConfig.stormSize * 6.0) continue;
                perc = 1.0 - Math.clamp(dist / ServerConfig.stormSize, 0.0, 1.0);
                if (storm.stage == 0) {
                    perc *= (double)((float)storm.energy / 100.0f);
                }
                if (storm.stage >= 2) {
                    perc *= (double)Mth.lerp((float)Math.clamp(smoothStage - 2.0f, 0.0f, 1.0f), (float)1.0f, (float)(storm.occlusion * 0.5f + 0.5f));
                }
                double p = 1.0 - Math.clamp(coreDist / (ServerConfig.stormSize * 6.0), 0.0, 1.0);
                if (storm.stage <= 1) {
                    p *= 0.0;
                }
                if (storm.stage >= 2) {
                    p *= (double)Math.clamp((smoothStage - 2.0f) / 0.5f, 0.0f, 1.0f);
                }
                perc = Math.max(p, perc);
            }
            precip += (float)perc;
        }
        return Math.clamp(precip * (float)ServerConfig.rainStrength, 0.0f, 1.0f);
    }

    public abstract Level getWorld();

    @Override
    public CompoundTag save(CompoundTag data) {
        PMWeather.LOGGER.debug("WeatherHandler save");
        CompoundTag listStormsNBT = new CompoundTag();
        for (int i = 0; i < this.storms.size(); ++i) {
            Storm storm = this.storms.get(i);
            storm.getNBTCache().setUpdateForced(true);
            storm.write();
            storm.getNBTCache().setUpdateForced(false);
            listStormsNBT.put("storm_" + storm.ID, (Tag)storm.getNBTCache().getNewNBT());
        }
        data.put("stormData", (Tag)listStormsNBT);
        data.putLong("lastUsedIDStorm", Storm.LastUsedStormID);
        return null;
    }

    public void read() {
        LevelSavedData savedData = (LevelSavedData)((ServerLevel)this.getWorld()).getDataStorage().computeIfAbsent(LevelSavedData.factory(), "pmweather_weather_data");
        savedData.setDataHandler(this);
        PMWeather.LOGGER.debug("Weather Data: {}", (Object)savedData.getData());
        CompoundTag data = savedData.getData();
        Storm.LastUsedStormID = data.getLong("lastUsedIDStorm");
        CompoundTag storms = data.getCompound("stormData");
        for (String tagName : storms.getAllKeys()) {
            CompoundTag stormData = storms.getCompound(tagName);
            Storm storm = new Storm(this, this.getWorld(), null, stormData.getInt("stormType"));
            try {
                storm.getNBTCache().setNewNBT(stormData);
                storm.read();
                storm.getNBTCache().updateCacheFromNew();
            }
            catch (Exception e) {
                PMWeather.LOGGER.error(e.getMessage(), (Throwable)e);
            }
            this.addStorm(storm);
        }
    }
}

