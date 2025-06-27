/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.core.HolderGetter
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.NbtUtils
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.Vec3
 */
package dev.protomanly.pmweather.weather;

import dev.protomanly.pmweather.PMWeather;
import dev.protomanly.pmweather.config.ClientConfig;
import dev.protomanly.pmweather.config.ServerConfig;
import dev.protomanly.pmweather.event.GameBusClientEvents;
import dev.protomanly.pmweather.particle.ParticleCube;
import dev.protomanly.pmweather.sound.ModSounds;
import dev.protomanly.pmweather.weather.Lightning;
import dev.protomanly.pmweather.weather.Storm;
import dev.protomanly.pmweather.weather.WeatherHandler;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class WeatherHandlerClient
extends WeatherHandler {
    public List<Lightning> lightnings = new ArrayList<Lightning>();

    public WeatherHandlerClient(ResourceKey<Level> dimension) {
        super(dimension);
    }

    @Override
    public Level getWorld() {
        return Minecraft.getInstance().level;
    }

    public float getHail() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return 0.0f;
        }
        float precip = 0.0f;
        for (Storm storm : this.getStorms()) {
            double dist;
            if (storm.visualOnly || (dist = player.position().distanceTo(new Vec3(storm.position.x + 2000.0, player.position().y, storm.position.z - 900.0))) > ServerConfig.stormSize * 4.0) continue;
            double perc = 0.0;
            if (storm.stormType == 0) {
                perc = 1.0 - Math.clamp(dist / (ServerConfig.stormSize * 6.0), 0.0, 1.0);
                if (storm.stage == 2) {
                    perc *= (double)((float)storm.energy / 100.0f);
                }
                if (storm.stage > 2) {
                    perc *= 1.0;
                }
                if (storm.stage < 2) {
                    perc *= 0.0;
                }
            }
            precip += (float)perc;
        }
        return Math.clamp(precip, 0.0f, 1.0f);
    }

    public float getPrecipitation() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return 0.0f;
        }
        return this.getPrecipitation(player.position());
    }

    public void strike(Vec3 pos) {
        Lightning lightning = new Lightning(pos, this.getWorld());
        this.lightnings.add(lightning);
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            double dist = player.position().multiply(1.0, 0.0, 1.0).distanceTo(pos.multiply(1.0, 0.0, 1.0));
            if (dist > 256.0) {
                this.getWorld().playLocalSound(pos.x, pos.y, pos.z, (SoundEvent)ModSounds.THUNDER_FAR.value(), SoundSource.WEATHER, 5000.0f, PMWeather.RANDOM.nextFloat(0.8f, 1.0f), true);
            } else {
                this.getWorld().playLocalSound(pos.x, pos.y, pos.z, (SoundEvent)ModSounds.THUNDER_NEAR.value(), SoundSource.WEATHER, 5000.0f, PMWeather.RANDOM.nextFloat(0.8f, 1.0f), true);
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        Iterator<Lightning> iterator = this.lightnings.iterator();
        while (iterator.hasNext()) {
            Lightning lightning = iterator.next();
            if (lightning.dead || lightning.level != this.getWorld()) {
                iterator.remove();
                continue;
            }
            lightning.tick();
        }
    }

    public void nbtSyncFromServer(CompoundTag compoundTag) {
        String command = compoundTag.getString("command");
        if (command.equals("syncStormNew")) {
            CompoundTag stormCompoundTag = compoundTag.getCompound("data");
            long ID = stormCompoundTag.getLong("ID");
            PMWeather.LOGGER.debug("syncStormNew, ID: {}", (Object)ID);
            Storm storm = new Storm(this, this.getWorld(), null, stormCompoundTag.getInt("stormType"));
            storm.getNBTCache().setNewNBT(stormCompoundTag);
            storm.nbtSyncFromServer();
            storm.getNBTCache().updateCacheFromNew();
            this.addStorm(storm);
        } else if (command.equals("syncStormRemove")) {
            CompoundTag stormCompoundTag = compoundTag.getCompound("data");
            long ID = stormCompoundTag.getLong("ID");
            Storm storm = (Storm)this.lookupStormByID.get(ID);
            if (storm != null) {
                this.removeStorm(ID);
            }
        } else if (command.equals("syncStormUpdate")) {
            CompoundTag stormCompoundTag = compoundTag.getCompound("data");
            long ID = stormCompoundTag.getLong("ID");
            Storm storm = (Storm)this.lookupStormByID.get(ID);
            if (storm != null) {
                storm.getNBTCache().setNewNBT(stormCompoundTag);
                storm.nbtSyncFromServer();
                storm.getNBTCache().updateCacheFromNew();
            }
        } else if (command.equals("syncBlockParticleNew")) {
            if ((double)PMWeather.RANDOM.nextFloat() > ClientConfig.debrisParticleDensity) {
                return;
            }
            CompoundTag nbt = compoundTag.getCompound("data");
            Vec3 pos = new Vec3((double)nbt.getInt("positionX"), (double)(nbt.getInt("positionY") + 1), (double)nbt.getInt("positionZ"));
            BlockState state = NbtUtils.readBlockState((HolderGetter)this.getWorld().holderLookup(Registries.BLOCK), (CompoundTag)nbt.getCompound("blockstate"));
            long stormID = nbt.getLong("stormID");
            Storm storm = (Storm)this.lookupStormByID.get(stormID);
            if (storm != null) {
                ParticleCube debris = new ParticleCube((ClientLevel)this.getWorld(), pos.x + (double)((PMWeather.RANDOM.nextFloat() - PMWeather.RANDOM.nextFloat()) * 3.0f), pos.y + (double)((PMWeather.RANDOM.nextFloat() - PMWeather.RANDOM.nextFloat()) * 3.0f), pos.z + (double)((PMWeather.RANDOM.nextFloat() - PMWeather.RANDOM.nextFloat()) * 3.0f), 0.0, 0.0, 0.0, state);
                GameBusClientEvents.particleBehavior.initParticleCube(debris);
                storm.listParticleDebris.add(debris);
                debris.ignoreWind = true;
                debris.renderRange = 256.0f;
                debris.spawnAsDebrisEffect();
            }
        } else if (command.equals("syncLightningNew")) {
            CompoundTag nbt = compoundTag.getCompound("data");
            this.strike(new Vec3(nbt.getDouble("positionX"), nbt.getDouble("positionY"), nbt.getDouble("positionZ")));
        }
    }
}

