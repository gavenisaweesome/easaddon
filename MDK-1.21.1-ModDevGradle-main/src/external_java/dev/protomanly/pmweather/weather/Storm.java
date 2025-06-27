/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.particle.Particle
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.client.resources.sounds.SoundInstance
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction$Axis
 *  net.minecraft.core.SectionPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.Tag
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.tags.BlockTags
 *  net.minecraft.tags.TagKey
 *  net.minecraft.util.Mth
 *  net.minecraft.util.RandomSource
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.ChunkPos
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.BlockStateProperties
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.level.chunk.LevelChunk
 *  net.minecraft.world.level.levelgen.Heightmap$Types
 *  net.minecraft.world.level.levelgen.LegacyRandomSource
 *  net.minecraft.world.level.levelgen.synth.SimplexNoise
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec2
 *  net.minecraft.world.phys.Vec3
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.api.distmarker.OnlyIn
 *  net.neoforged.fml.LogicalSide
 *  net.neoforged.fml.util.thread.EffectiveSide
 *  net.neoforged.neoforge.common.Tags$Blocks
 */
package dev.protomanly.pmweather.weather;

import dev.protomanly.pmweather.PMWeather;
import dev.protomanly.pmweather.block.ModBlocks;
import dev.protomanly.pmweather.config.Config;
import dev.protomanly.pmweather.config.ServerConfig;
import dev.protomanly.pmweather.entity.ModEntities;
import dev.protomanly.pmweather.entity.MovingBlock;
import dev.protomanly.pmweather.interfaces.ParticleData;
import dev.protomanly.pmweather.particle.EntityRotFX;
import dev.protomanly.pmweather.sound.ModSounds;
import dev.protomanly.pmweather.sound.MovingSoundStreamingSource;
import dev.protomanly.pmweather.util.CachedNBTTagCompound;
import dev.protomanly.pmweather.util.Util;
import dev.protomanly.pmweather.weather.Vorticy;
import dev.protomanly.pmweather.weather.WeatherHandler;
import dev.protomanly.pmweather.weather.WeatherHandlerServer;
import dev.protomanly.pmweather.weather.WindEngine;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.LogicalSide;
import net.neoforged.fml.util.thread.EffectiveSide;
import net.neoforged.neoforge.common.Tags;

public class Storm {
    public static long LastUsedStormID = 0L;
    private static final float resistance = 0.985f;
    public static final float tickConversion = 0.05f;
    @OnlyIn(value=Dist.CLIENT)
    public MovingSoundStreamingSource tornadicWind;
    @OnlyIn(value=Dist.CLIENT)
    public MovingSoundStreamingSource supercellWind;
    public long ID;
    public WeatherHandler weatherHandler;
    public Vec3 position;
    public Vec3 lastPosition;
    public Vec3 velocity;
    public int windspeed;
    public float smoothWindspeed = 0.0f;
    public float width = 15.0f;
    public float smoothWidth = 15.0f;
    public float tornadoShape = PMWeather.RANDOM.nextFloat() * 10.0f + 6.0f;
    public float spin = 0.0f;
    public float lastSpin = 0.0f;
    public int energy;
    public int stormType;
    public int stage;
    public int tickCount = 0;
    public int tornadoOnGroundTicks = 0;
    public boolean dead = false;
    public Level level;
    private final CachedNBTTagCompound nbtCache;
    public SimplexNoise simplexNoise;
    public float rankineFactor = 4.5f;
    public List<EntityRotFX> listParticleDebris;
    private final List<ChunkPos> forceLoadedChunks = new ArrayList<ChunkPos>();
    public int maxStage = 0;
    public int maxProgress = 0;
    public boolean isDying = false;
    public int growthSpeed = 20;
    public int maxWindspeed = 0;
    public int maxWidth = 15;
    public int ticksSinceDying = 0;
    public int touchdownSpeed = PMWeather.RANDOM.nextInt(65, 120);
    public boolean onWater = false;
    public float occlusion = 0.0f;
    public boolean visualOnly = false;
    public boolean cirus = false;
    public boolean aimedAtPlayer = false;
    public int maxColdEnergy = 300;
    public int coldEnergy = 0;
    public List<Vorticy> vorticies = new ArrayList<Vorticy>();

    public double FBM(Vec3 pos, int octaves, float lacunarity, float gain, float amplitude) {
        double y = 0.0;
        for (int i = 0; i < Math.max(octaves, 1); ++i) {
            y += (double)amplitude * this.simplexNoise.getValue(pos.x, pos.y, pos.z);
            pos = pos.multiply((double)lacunarity, (double)lacunarity, (double)lacunarity);
            amplitude *= gain;
        }
        return y;
    }

    public Vec3 rotateV3(Vec3 x, double angle) {
        double rx = x.x * Math.cos(angle) - x.z * Math.sin(angle);
        double rz = x.x * Math.sin(angle) + x.z * Math.cos(angle);
        return new Vec3(rx, x.y, rz);
    }

    public Storm(WeatherHandler weatherHandler, Level level, @Nullable Float risk, int stormType) {
        this.weatherHandler = weatherHandler;
        this.level = level;
        this.stormType = stormType;
        this.simplexNoise = new SimplexNoise((RandomSource)new LegacyRandomSource(weatherHandler.seed));
        this.nbtCache = new CachedNBTTagCompound();
        if (level.isClientSide()) {
            this.listParticleDebris = new ArrayList<EntityRotFX>();
        } else {
            this.maxStage = 0;
            this.maxProgress = PMWeather.RANDOM.nextInt(25, 99);
            float stage1Chance = 1.0f / (float)ServerConfig.chanceInOneStage1;
            float stage2Chance = 1.0f / (float)ServerConfig.chanceInOneStage2;
            float stage3Chance = 1.0f / (float)ServerConfig.chanceInOneStage3;
            if (risk != null && ServerConfig.environmentSystem && stormType == 0) {
                PMWeather.LOGGER.debug("Readjusted stage chances: 1: {} 2: {} 3: {}", new Object[]{Float.valueOf(stage1Chance *= risk.floatValue() * 1.75f + 0.05f), Float.valueOf(stage2Chance *= risk.floatValue()), Float.valueOf(stage3Chance *= risk.floatValue() * 0.75f)});
            }
            if (PMWeather.RANDOM.nextFloat() <= stage1Chance) {
                this.maxStage = 1;
            }
            if (PMWeather.RANDOM.nextFloat() <= stage2Chance) {
                this.maxStage = 2;
            }
            if (PMWeather.RANDOM.nextFloat() <= stage3Chance) {
                this.maxStage = 3;
            }
            if (this.maxStage == 3 && stormType == 0) {
                this.maxProgress = 100;
                double c = 2.35;
                if (risk != null && ServerConfig.environmentSystem) {
                    c -= (double)risk.floatValue() * 1.5;
                    c = Math.max(c, 0.65);
                    PMWeather.LOGGER.debug("Calculating tornado with strength curve of {}", (Object)c);
                    this.maxWindspeed = risk.floatValue() > 1.0f ? (int)Math.ceil(Math.pow(PMWeather.RANDOM.nextDouble(), c) * 180.0) + 40 : (int)Math.ceil(Math.pow(PMWeather.RANDOM.nextDouble(), c) * 180.0 * Math.pow(risk.floatValue(), 0.5)) + 40;
                } else {
                    c = 1.75;
                    this.maxWindspeed = (int)Math.ceil(Math.pow(PMWeather.RANDOM.nextDouble(), c) * 180.0) + 40;
                }
                this.touchdownSpeed = PMWeather.RANDOM.nextInt(75, Math.max(25 + (int)((float)this.maxWindspeed * 1.1f), 100));
            }
            this.growthSpeed = PMWeather.RANDOM.nextInt(30, 80);
            if (stormType == 1) {
                this.growthSpeed = PMWeather.RANDOM.nextInt(40, 70);
            }
            this.maxWidth = PMWeather.RANDOM.nextInt(15, 25 + (int)(Math.pow((float)this.maxWindspeed / 220.0f, 1.75) * (ServerConfig.maxTornadoWidth - 25.0)));
            PMWeather.LOGGER.debug("Max Stage: {}, Max Energy: {}, Max Windspeed: {}, Max Width: {}, Touchdown Speed: {}", new Object[]{this.maxStage, this.maxProgress, this.maxWindspeed, this.maxWidth, this.touchdownSpeed});
        }
    }

    public void recalc(@Nullable Float risk) {
        if (this.maxStage == 3 && this.stormType == 0) {
            this.maxProgress = 100;
            double c = 2.35;
            if (risk != null && ServerConfig.environmentSystem) {
                c -= (double)risk.floatValue() * 1.5;
                c = Math.max(c, 0.75);
                PMWeather.LOGGER.debug("Recalculating tornado with strength curve of {}", (Object)c);
            } else {
                c = 1.75;
            }
            this.maxWindspeed = (int)Math.ceil(Math.pow(PMWeather.RANDOM.nextDouble(), c) * 180.0) + 40;
            this.touchdownSpeed = PMWeather.RANDOM.nextInt(75, Math.max(25 + (int)((float)this.maxWindspeed * 1.1f), 100));
        }
        this.growthSpeed = PMWeather.RANDOM.nextInt(30, 80);
        if (this.stormType == 1) {
            this.growthSpeed = PMWeather.RANDOM.nextInt(40, 70);
        }
        this.maxWidth = PMWeather.RANDOM.nextInt(15, 25 + (int)(Math.pow((float)this.maxWindspeed / 220.0f, 1.75) * (ServerConfig.maxTornadoWidth - 25.0)));
        PMWeather.LOGGER.debug("Max Stage: {}, Max Energy: {}, Max Windspeed: {}, Max Width: {}, Touchdown Speed: {}", new Object[]{this.maxStage, this.maxProgress, this.maxWindspeed, this.maxWidth, this.touchdownSpeed});
    }

    public void aimAtPlayer() {
        if (this.stormType == 1) {
            return;
        }
        Player nearest = this.level.getNearestPlayer(this.position.x, this.position.y, this.position.z, 4096.0, false);
        if (nearest != null) {
            Vec3 aimPos = nearest.position().add(new Vec3((double)(PMWeather.RANDOM.nextFloat() - 0.5f) * ServerConfig.aimAtPlayerOffset, 0.0, (double)(PMWeather.RANDOM.nextFloat() - 0.5f) * ServerConfig.aimAtPlayerOffset));
            if (this.position.distanceTo(aimPos) >= ServerConfig.aimAtPlayerOffset) {
                Vec3 toward = this.position.subtract(new Vec3(aimPos.x, this.position.y, aimPos.z)).multiply(1.0, 0.0, 1.0).normalize();
                double speed = PMWeather.RANDOM.nextDouble() * 5.0 + 1.0;
                this.velocity = toward.multiply(-speed, 0.0, -speed);
            }
            this.aimedAtPlayer = true;
        }
    }

    public void tick() {
        Level iterator3;
        Level count2;
        ++this.tickCount;
        Iterator<Vorticy> vorts = this.vorticies.iterator();
        while (vorts.hasNext()) {
            Vorticy vorticy = vorts.next();
            vorticy.tick();
            if (!vorticy.dead) continue;
            vorts.remove();
        }
        float vorticySpawnChance = 0.05f;
        if (this.isDying) {
            vorticySpawnChance = 0.25f;
        }
        vorticySpawnChance += Mth.clamp((float)((float)Math.pow(((float)this.windspeed - 100.0f) / 200.0f, 2.0)), (float)0.0f, (float)0.5f);
        if (this.stage == 3 && (float)this.windspeed >= 40.0f) {
            ++this.tornadoOnGroundTicks;
            if (!this.level.isClientSide && PMWeather.RANDOM.nextFloat() < vorticySpawnChance * 0.05f && this.vorticies.size() < 10) {
                Vorticy vorticy = new Vorticy(this, (float)Math.pow(PMWeather.RANDOM.nextFloat(), 0.75) * 0.4f, PMWeather.RANDOM.nextFloat() * 0.3f + 0.05f, 1.0f / this.rankineFactor * 0.5f, PMWeather.RANDOM.nextInt(35, 120));
                this.vorticies.add(vorticy);
            }
        }
        if (this.isDying) {
            ++this.ticksSinceDying;
        }
        BlockPos blockPos = new BlockPos((int)this.position.x, (int)this.position.y, (int)this.position.z);
        if (!this.level.isClientSide() && this.stage >= 2 && this.stormType == 0) {
            float y = 0.0f;
            int count2 = 0;
            for (int x = -1; x <= 1; ++x) {
                for (int z = -1; z <= 1; ++z) {
                    float r = Math.max(this.width, 45.0f);
                    Vec3 samplePos = this.position.add((double)((float)x * r * 0.5f), 0.0, (double)((float)z * r * 0.5f));
                    BlockPos sample = this.level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, new BlockPos((int)samplePos.x, this.level.getMaxBuildHeight(), (int)samplePos.z));
                    y += (float)sample.getY();
                    ++count2;
                }
            }
            blockPos = new BlockPos((int)this.position.x, (int)(y /= (float)count2), (int)this.position.z);
            this.position = new Vec3(this.position.x, Mth.lerp((double)0.01f, (double)this.position.y, (double)y), this.position.z);
        }
        if (this.tickCount % 20 == 0 && !this.level.isClientSide() && (count2 = this.level) instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)count2;
            if (this.windspeed > 60 && this.stormType == 0) {
                ChunkPos chunkPos = new ChunkPos(blockPos);
                if (!serverLevel.hasChunk(chunkPos.x, chunkPos.z) && !this.forceLoadedChunks.contains(chunkPos) && serverLevel.isInWorldBounds(blockPos)) {
                    this.forceLoadedChunks.add(chunkPos);
                    serverLevel.setChunkForced(chunkPos.x, chunkPos.z, true);
                }
                Iterator<ChunkPos> iterator2 = this.forceLoadedChunks.iterator();
                while (iterator2.hasNext()) {
                    ChunkPos cpos = iterator2.next();
                    double dist = Math.sqrt(cpos.distanceSquared(chunkPos));
                    if (!(dist > 4.0)) continue;
                    iterator2.remove();
                    serverLevel.setChunkForced(cpos.x, cpos.z, false);
                }
            } else {
                Iterator<ChunkPos> iterator3 = this.forceLoadedChunks.iterator();
                while (iterator3.hasNext()) {
                    ChunkPos cpos = iterator3.next();
                    iterator3.remove();
                    serverLevel.setChunkForced(cpos.x, cpos.z, false);
                }
            }
        }
        if (this.tickCount % 10 == 0 && !this.level.isClientSide() && (iterator3 = this.level) instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)iterator3;
            float lightningChance = 0.0f;
            if (this.stage == 1) {
                lightningChance = (float)this.energy / 100.0f;
            } else if (this.stage == 2) {
                lightningChance = 1.0f + (float)this.energy / 100.0f;
            } else if (this.stage > 2) {
                lightningChance = 2.0f;
            }
            if (this.visualOnly) {
                lightningChance = 0.0f;
            }
            lightningChance = Math.min(lightningChance * 0.035f, 0.1f);
            if (this.stormType == 1) {
                lightningChance *= 3.0f;
            }
            if (PMWeather.RANDOM.nextFloat() <= lightningChance * 0.5f) {
                Vec3 lPos = this.position.add((double)(PMWeather.RANDOM.nextFloat((float)(-ServerConfig.stormSize), (float)ServerConfig.stormSize) / 2.0f), 0.0, (double)(PMWeather.RANDOM.nextFloat((float)(-ServerConfig.stormSize), (float)ServerConfig.stormSize) / 2.0f));
                if (this.stormType == 1) {
                    Vec2 stormVel = new Vec2((float)this.velocity.x, (float)this.velocity.z);
                    Vec2 right = new Vec2(stormVel.y, -stormVel.x).normalized();
                    Vec2 fwd = stormVel.normalized();
                    right = Util.mulVec2(right, PMWeather.RANDOM.nextFloat((float)(-ServerConfig.stormSize), (float)ServerConfig.stormSize) * 5.0f);
                    fwd = Util.mulVec2(fwd, PMWeather.RANDOM.nextFloat((float)(-ServerConfig.stormSize), (float)ServerConfig.stormSize) / 2.0f);
                    lPos = this.position.add(new Vec3((double)right.x, 0.0, (double)right.y)).add(new Vec3((double)fwd.x, 0.0, (double)fwd.y));
                }
                int height = this.level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos((int)lPos.x, (int)lPos.y, (int)lPos.z)).getY();
                ((WeatherHandlerServer)this.weatherHandler).syncLightningNew(new Vec3(lPos.x, (double)height, lPos.z));
            }
        }
        int gs = this.growthSpeed / 2;
        if (this.stormType == 0 && this.stage < 3) {
            gs = (int)((float)gs / 1.5f);
        }
        if (this.tickCount % gs == 0) {
            if (!this.isDying) {
                int targetProgress = this.maxProgress;
                if (this.maxStage > this.stage) {
                    targetProgress = 100;
                }
                if (this.energy < targetProgress) {
                    ++this.energy;
                    if (this.stormType == 1) {
                        this.coldEnergy = Math.clamp((long)(this.coldEnergy + 1), 0, this.maxColdEnergy);
                    }
                }
                if (this.stage < 3 || this.stormType != 0) {
                    if (this.stage >= this.maxStage && this.energy >= targetProgress) {
                        this.isDying = true;
                        this.growthSpeed = PMWeather.RANDOM.nextInt(40, 80);
                        if (PMWeather.RANDOM.nextInt(2) == 0 || this.maxWidth > 200) {
                            this.maxWidth = Math.min(this.maxWidth, PMWeather.RANDOM.nextInt(5, 35));
                        }
                    }
                } else {
                    if (this.windspeed < this.maxWindspeed) {
                        ++this.windspeed;
                        this.occlusion = Math.clamp(this.occlusion - 0.025f, 0.0f, 1.0f);
                    }
                    if (this.windspeed >= this.maxWindspeed) {
                        this.isDying = true;
                        this.growthSpeed = PMWeather.RANDOM.nextInt(20, 70);
                    }
                }
                if (this.energy >= 100) {
                    this.energy = 0;
                    if (this.stormType == 0) {
                        if (this.stage < 3 && this.stage < this.maxStage) {
                            ++this.stage;
                            if (this.stage == 3) {
                                this.windspeed = 0;
                            }
                        }
                    } else if (this.stage < this.maxStage) {
                        ++this.stage;
                    }
                }
            } else if (this.ticksSinceDying > (this.stormType == 1 ? 2400 : 1200)) {
                if (this.stage < 3 || this.stormType != 0) {
                    --this.energy;
                    if (this.energy <= 0) {
                        this.energy = 100;
                        --this.stage;
                        if (this.stage < 0) {
                            this.energy = 0;
                            this.stage = 0;
                            if (this.coldEnergy > 0) {
                                --this.coldEnergy;
                            } else {
                                this.dead = true;
                            }
                        }
                    }
                } else {
                    if (this.windspeed < 85 && this.windspeed > 15) {
                        if (PMWeather.RANDOM.nextInt(2) == 0 && !this.level.isClientSide()) {
                            --this.windspeed;
                        }
                    } else {
                        --this.windspeed;
                    }
                    this.occlusion = Math.clamp(this.occlusion + 0.015f, 0.0f, 1.0f);
                    if (this.windspeed <= 0) {
                        this.windspeed = 0;
                        --this.stage;
                        this.energy = 100;
                    }
                }
            }
            if (Config.DEBUG) {
                PMWeather.LOGGER.debug("Stage: {}, Energy: {}, Windspeed: {}, Width: {}", new Object[]{this.stage, this.energy, this.windspeed, Float.valueOf(this.width)});
            }
        }
        this.width = Mth.lerp((float)0.025f, (float)this.width, (float)Math.max(5.0f, Math.clamp((float)this.windspeed / (float)this.maxWindspeed, 0.1f, 1.0f) * (float)this.maxWidth));
        Vec3 vel = this.velocity.multiply((double)0.05f, (double)0.05f, (double)0.05f).multiply(2.0, 0.0, 2.0);
        if (!this.aimedAtPlayer) {
            vel = vel.add(new Vec3(0.0, 0.0, -3.0).multiply((double)(0.05f * this.occlusion), (double)(0.05f * this.occlusion), (double)(0.05f * this.occlusion)));
        }
        this.position = this.position.add(vel);
        if (!this.aimedAtPlayer) {
            if (this.stormType != 1) {
                this.velocity = this.velocity.multiply((double)0.985f, (double)0.985f, (double)0.985f);
                Vec3 baseWind = WindEngine.getWind(new Vec3(this.position.x, (double)(this.level.getMaxBuildHeight() + 1), this.position.z), this.level, true, true, false);
                float factor = 0.018181818f;
                Vec3 velAdd = new Vec3(baseWind.x, 0.0, baseWind.z).multiply((double)factor, 0.0, (double)factor);
                this.velocity = this.velocity.add(velAdd.multiply((double)0.05f, (double)0.05f, (double)0.05f));
            }
            if (!this.level.isClientSide() && this.stage >= 3 && ServerConfig.aimAtPlayer && this.stormType == 0) {
                this.aimAtPlayer();
            }
        }
        if (!this.level.isClientSide() && this.tickCount % this.getUpdateRate() == 0) {
            WeatherHandlerServer weatherHandlerServer = (WeatherHandlerServer)this.weatherHandler;
            weatherHandlerServer.syncStormUpdate(this);
        }
        if (this.level.isClientSide()) {
            this.tickClient();
            return;
        }
        if (this.stage < 3 || this.stormType != 0) {
            return;
        }
        if (this.windspeed >= 40) {
            AABB aabb = new AABB(this.position.x, this.position.y, this.position.z, this.position.x, this.position.y, this.position.z);
            aabb = aabb.inflate((double)this.width / 2.0, 85.0, (double)this.width / 2.0);
            List entityList = this.level.getEntities(null, aabb);
            for (Entity entity : entityList) {
                Player player;
                if (entity instanceof Player && !(player = (Player)entity).isCreative() && !player.isSpectator()) {
                    this.pull(entity, 2.5f);
                    continue;
                }
                if (entity instanceof Player) continue;
                this.pull(entity, 2.5f);
            }
            int windfieldWidth = Math.max((int)this.width, 40);
            int numBlocks = Math.min(windfieldWidth * Math.max(windfieldWidth / 2, 20) + this.windspeed * 3 + 300, ServerConfig.maxBlocksDamagedPerTick);
            HashMap<Vec3i, Boolean> checkedMap = new HashMap<Vec3i, Boolean>();
            HashMap<ChunkPos, LevelChunk> chunkMap = new HashMap<ChunkPos, LevelChunk>();
            int damaged = 0;
            int damageMax = (500 + (int)this.width) / 3;
            for (int i = 0; i < numBlocks && damaged < damageMax; ++i) {
                MovingBlock movingBlock;
                BlockState aboveState;
                LevelChunk chunk;
                BlockPos blockPosTop;
                double windEffect;
                int z;
                int x = (int)(PMWeather.RANDOM.nextFloat() * (float)windfieldWidth * 2.0f - (float)windfieldWidth);
                Vec3i off = new Vec3i(x, 0, z = (int)(PMWeather.RANDOM.nextFloat() * (float)windfieldWidth * 2.0f - (float)windfieldWidth));
                if (checkedMap.containsKey(off)) continue;
                checkedMap.put(off, true);
                double dist = off.distSqr(Vec3i.ZERO);
                if (dist > (double)(windfieldWidth * windfieldWidth)) continue;
                float percAdj = 16.0f;
                BlockPos bPos = blockPos.offset(off.getX(), 60, off.getZ());
                if (!this.level.isInWorldBounds(bPos) || (windEffect = (double)this.getWind((blockPosTop = this.level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, bPos).below()).getCenter())) < 40.0) continue;
                ChunkPos chunkPos = new ChunkPos(SectionPos.blockToSectionCoord((int)blockPosTop.getX()), SectionPos.blockToSectionCoord((int)blockPosTop.getZ()));
                if (chunkMap.containsKey(chunkPos)) {
                    chunk = (LevelChunk)chunkMap.get(chunkPos);
                } else {
                    chunk = this.level.getChunk(chunkPos.x, chunkPos.z);
                    chunkMap.put(chunkPos, chunk);
                }
                BlockState state = chunk.getBlockState(blockPosTop);
                BlockPos randomDown = blockPosTop.below(PMWeather.RANDOM.nextInt(10));
                BlockState stateDown = chunk.getBlockState(randomDown);
                boolean downBlacklisted = false;
                for (TagKey<Block> tag : ServerConfig.blacklistedBlockTags) {
                    if (!stateDown.is(tag)) continue;
                    downBlacklisted = true;
                    break;
                }
                if (!downBlacklisted && !ServerConfig.blacklistedBlocks.contains(stateDown.getBlock())) {
                    if (stateDown.is(Tags.Blocks.GLASS_BLOCKS) || stateDown.is(Tags.Blocks.GLASS_PANES)) {
                        double percChance = Math.clamp((windEffect - 75.0) / 15.0, 0.0, 1.0);
                        if ((double)PMWeather.RANDOM.nextFloat() <= percChance * (double)(0.3f * percAdj) && Util.canWindAffect(randomDown.getCenter(), this.level)) {
                            this.level.removeBlock(randomDown, false);
                            this.level.playSound(null, randomDown, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1.0f, PMWeather.RANDOM.nextFloat(0.8f, 1.2f));
                        }
                    }
                    if (stateDown.is(BlockTags.LOGS) && !stateDown.is(Tags.Blocks.STRIPPED_LOGS) && ServerConfig.doDebarking) {
                        double percChance = Math.clamp((windEffect - 140.0) / 20.0, 0.0, 1.0);
                        if ((double)PMWeather.RANDOM.nextFloat() <= percChance * (double)(0.5f * percAdj) && Util.canWindAffect(randomDown.getCenter(), this.level)) {
                            Block replacement = Util.STRIPPED_VARIANTS.getOrDefault(stateDown.getBlock(), Blocks.STRIPPED_OAK_LOG);
                            this.level.setBlockAndUpdate(randomDown, (BlockState)replacement.defaultBlockState().trySetValue((Property)BlockStateProperties.AXIS, (Comparable)stateDown.getOptionalValue((Property)BlockStateProperties.AXIS).orElse(Direction.Axis.Y)));
                        }
                    }
                }
                if (!(aboveState = chunk.getBlockState(blockPosTop.above())).isAir()) {
                    Block aboveBlock = aboveState.getBlock();
                    float blockStrength = Storm.getBlockStrength(aboveBlock, this.level, blockPosTop.above());
                    double percChance = Math.clamp(Math.pow(Math.clamp(Math.max(windEffect - (double)blockStrength, 0.0) / 20.0, 0.0, 1.0), 4.0) + 0.02, 0.0, 1.0) * 0.05 * (double)percAdj;
                    if (windEffect < (double)blockStrength) {
                        percChance = 0.0;
                    }
                    if (aboveBlock.defaultDestroyTime() < 0.05f && aboveBlock.defaultDestroyTime() >= 0.0f && !ServerConfig.blacklistedBlocks.contains(aboveBlock) && (double)PMWeather.RANDOM.nextFloat() <= percChance) {
                        this.level.removeBlock(blockPosTop.above(), false);
                        continue;
                    }
                    boolean blacklisted = false;
                    for (TagKey<Block> tag : ServerConfig.blacklistedBlockTags) {
                        if (!aboveBlock.defaultBlockState().is(tag)) continue;
                        blacklisted = true;
                        break;
                    }
                    if (windEffect >= (double)blockStrength && aboveBlock.defaultDestroyTime() > 0.0f && !ServerConfig.blacklistedBlocks.contains(aboveBlock) && !blacklisted && state.getFluidState().isEmpty() && (double)PMWeather.RANDOM.nextFloat() <= percChance) {
                        this.level.removeBlock(blockPosTop.above(), false);
                    }
                }
                if (state.is(Blocks.GRASS_BLOCK) || state.is((Block)ModBlocks.SCOURED_GRASS.get())) {
                    double percChance = Math.clamp((windEffect - 140.0) / 80.0, 0.0, 1.0);
                    if (!((double)PMWeather.RANDOM.nextFloat() <= percChance * (double)(0.02f * percAdj))) continue;
                    this.level.setBlockAndUpdate(blockPosTop, Blocks.DIRT.defaultBlockState());
                    continue;
                }
                if (state.is(Blocks.DIRT)) {
                    double percChance = Math.clamp((windEffect - 170.0) / 40.0, 0.0, 1.0);
                    if (!((double)PMWeather.RANDOM.nextFloat() <= percChance * (double)(0.02f * percAdj))) continue;
                    this.level.setBlockAndUpdate(blockPosTop, ((Block)ModBlocks.MEDIUM_SCOURING.get()).defaultBlockState());
                    continue;
                }
                if (state.is((Block)ModBlocks.MEDIUM_SCOURING.get())) {
                    double percChance = Math.clamp((windEffect - 200.0) / 30.0, 0.0, 1.0);
                    if (!((double)PMWeather.RANDOM.nextFloat() <= percChance * (double)(0.02f * percAdj))) continue;
                    this.level.setBlockAndUpdate(blockPosTop, ((Block)ModBlocks.HEAVY_SCOURING.get()).defaultBlockState());
                    continue;
                }
                Block block = state.getBlock();
                float blockStrength = Storm.getBlockStrength(block, this.level, blockPosTop);
                if (state.is(Tags.Blocks.STRIPPED_LOGS)) {
                    blockStrength *= 2.0f;
                }
                if (ServerConfig.blockStrengths.containsKey(block)) {
                    blockStrength = ServerConfig.blockStrengths.get(block).floatValue();
                }
                double stretch = 35.0;
                if (state.is(BlockTags.LEAVES)) {
                    stretch = 70.0;
                } else if (state.is(BlockTags.LOGS) || state.is(BlockTags.PLANKS)) {
                    stretch = 50.0;
                }
                double percChance = Math.clamp(Math.pow(Math.clamp(Math.max(windEffect - (double)blockStrength, 0.0) / stretch, 0.0, 1.0), 4.0) + 0.02, 0.0, 1.0) * 0.05 * (double)percAdj;
                if (windEffect < (double)blockStrength) {
                    percChance = 0.0;
                }
                if (block.defaultDestroyTime() < 0.05f && block.defaultDestroyTime() >= 0.0f && !ServerConfig.blacklistedBlocks.contains(block) && (double)PMWeather.RANDOM.nextFloat() <= percChance) {
                    this.level.removeBlock(blockPosTop, false);
                    continue;
                }
                boolean blacklisted = false;
                for (TagKey<Block> tag : ServerConfig.blacklistedBlockTags) {
                    if (!block.defaultBlockState().is(tag)) continue;
                    blacklisted = true;
                    break;
                }
                if (!(windEffect >= (double)blockStrength) || !(block.defaultDestroyTime() > 0.0f) || ServerConfig.blacklistedBlocks.contains(block) || blacklisted || !state.getFluidState().isEmpty() || !((double)PMWeather.RANDOM.nextFloat() <= percChance) || (movingBlock = (MovingBlock)ModEntities.MOVING_BLOCK.get().create(this.level)) == null) continue;
                ++damaged;
                movingBlock.setStartPos(blockPosTop);
                movingBlock.setBlockState(state);
                movingBlock.setPos(blockPosTop.getX(), blockPosTop.getY(), blockPosTop.getZ());
                this.level.removeBlock(blockPosTop, false);
                Player nearest = this.level.getNearestPlayer((double)blockPosTop.getX(), (double)blockPosTop.getY(), (double)blockPosTop.getZ(), 128.0, false);
                if (PMWeather.RANDOM.nextInt(Math.max(1, windfieldWidth / 10)) == 0 && nearest != null && nearest.position().distanceTo(blockPosTop.getCenter()) < 128.0) {
                    if (this.level.isLoaded(blockPosTop)) {
                        this.level.addFreshEntity((Entity)movingBlock);
                        continue;
                    }
                    movingBlock.discard();
                    continue;
                }
                movingBlock.discard();
                ((WeatherHandlerServer)this.weatherHandler).syncBlockParticleNew(blockPosTop, state, this);
            }
        }
    }

    public float getRankine(double dist, int windfieldWidth) {
        float rankineWidth = (float)windfieldWidth / this.rankineFactor;
        float perc = 0.0f;
        if (dist <= (double)(rankineWidth / 2.0f)) {
            perc = (float)dist / (rankineWidth / 2.0f);
        } else if (dist <= (double)((float)windfieldWidth * 2.0f)) {
            perc = Math.clamp((float)Math.pow(1.0 - (dist - (double)(rankineWidth / 2.0f)) / (double)(((float)windfieldWidth * 2.0f - rankineWidth) / 2.0f), 1.5), 0.0f, 1.0f);
        }
        if (Float.isNaN(perc)) {
            perc = 0.0f;
        }
        return perc;
    }

    public float getWind(Vec3 pos) {
        int windfieldWidth = Math.max((int)this.width, 40);
        double dist = this.position.multiply(1.0, 0.0, 1.0).distanceTo(pos.multiply(1.0, 0.0, 1.0));
        float perc = this.getRankine(dist, windfieldWidth);
        float affectPerc = (float)Math.sqrt(1.0 - dist / (double)((float)windfieldWidth * 2.0f));
        Vec3 relativePos = pos.subtract(this.position);
        Vec3 rotational = new Vec3(relativePos.z, 0.0, -relativePos.x).normalize();
        Vec3 rPosNoise = this.rotateV3(relativePos, (double)this.tickCount / 60.0);
        double wNoise = this.FBM(new Vec3(rPosNoise.x / 100.0, rPosNoise.z / 100.0, (double)this.tickCount / 200.0), 5, 2.0f, 0.5f, 1.0f);
        double realWind = (double)this.windspeed * (1.0 + wNoise * 0.1);
        Vec3 motion = rotational.multiply(realWind * (double)perc, 0.0, realWind * (double)perc);
        motion = motion.add(this.velocity.multiply((double)(15.0f * affectPerc), 0.0, (double)(15.0f * affectPerc)));
        for (Vorticy vorticy : this.vorticies) {
            double d = vorticy.getPosition().multiply(1.0, 0.0, 1.0).distanceTo(pos.multiply(1.0, 0.0, 1.0));
            Vec3 rPos = pos.subtract(vorticy.getPosition());
            Vec3 rot = new Vec3(rPos.z, 0.0, -rPos.x).normalize();
            int windWid = (int)((float)windfieldWidth * vorticy.widthPerc);
            float p = this.getRankine(d, windWid);
            float wind = vorticy.windspeedMult * (float)this.windspeed;
            motion = motion.add(rot.multiply((double)(wind * p), 0.0, (double)(wind * p)));
        }
        return (float)motion.length();
    }

    public void initFirstTime() {
        this.ID = LastUsedStormID++;
    }

    public void pull(Particle particle, float multiplier) {
        int windfieldWidth = Math.max((int)this.width, 40);
        BlockPos blockPos = new BlockPos((int)particle.getPos().x, (int)particle.getPos().y, (int)particle.getPos().z);
        int worldHeight = this.level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, blockPos).getY();
        if (worldHeight > blockPos.getY()) {
            return;
        }
        double dist = particle.getPos().distanceTo(new Vec3(this.position.x, particle.getPos().y, this.position.z));
        if (dist > (double)windfieldWidth) {
            return;
        }
        Vec3 relativePos = particle.getPos().subtract(this.position);
        double heightDifference = particle.getPos().y - this.position.y;
        if (Math.abs(heightDifference) > 150.0) {
            return;
        }
        Vec3 inward = new Vec3(-relativePos.x, 0.0, -relativePos.z).normalize();
        Vec3 rotational = new Vec3(relativePos.z, 0.0, -relativePos.x).normalize();
        double windEffect = this.getWind(particle.getPos());
        double effectStrength = Math.clamp(windEffect / (double)Math.max((float)this.windspeed, 130.0f), 0.0, 1.0) * (double)multiplier;
        double pullFactor = 4.0;
        pullFactor -= Math.max(heightDifference, 0.0) / 100.0 * 3.0;
        pullFactor /= (double)Math.max(this.width / 100.0f, 1.0f);
        if (dist <= (double)(this.width / (this.rankineFactor * 2.0f))) {
            pullFactor = -1.5;
        }
        Vec3 add = inward.multiply(effectStrength * pullFactor, effectStrength * pullFactor, effectStrength * pullFactor).add(rotational.multiply(effectStrength, effectStrength, effectStrength));
        add = add.add(new Vec3(0.0, effectStrength, 0.0));
        if (particle instanceof ParticleData) {
            ParticleData particleData = (ParticleData)particle;
            particleData.addVelocity(add.multiply((double)0.05f, (double)0.05f, (double)0.05f));
        }
    }

    public void pull(Entity entity, float multiplier) {
        int windfieldWidth = Math.max((int)this.width, 40);
        int worldHeight = this.level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, entity.blockPosition()).getY();
        if (worldHeight > entity.blockPosition().getY()) {
            return;
        }
        double dist = entity.position().distanceTo(new Vec3(this.position.x, entity.position().y, this.position.z));
        if (dist > (double)windfieldWidth) {
            return;
        }
        Vec3 relativePos = entity.position().subtract(this.position);
        double heightDifference = entity.position().y - this.position.y;
        if (Math.abs(heightDifference) > 150.0) {
            return;
        }
        Vec3 inward = new Vec3(-relativePos.x, 0.0, -relativePos.z).normalize();
        Vec3 rotational = new Vec3(relativePos.z, 0.0, -relativePos.x).normalize();
        double windEffect = this.getWind(entity.position());
        if (windEffect < 60.0) {
            return;
        }
        double effectStrength = Math.clamp((windEffect - 60.0) / (double)Math.max((float)this.windspeed * 1.2f, 130.0f), 0.0, 1.0) * (double)multiplier * 1.5;
        double pullFactor = 4.0;
        pullFactor -= Math.max(heightDifference, 0.0) / 65.0 * 3.0;
        if (dist <= (double)(this.width / this.rankineFactor)) {
            pullFactor = -1.5;
        }
        Vec3 add = inward.multiply(effectStrength * pullFactor, effectStrength * pullFactor, effectStrength * pullFactor).add(rotational.multiply(effectStrength, effectStrength, effectStrength));
        add = add.add(new Vec3(0.0, effectStrength, 0.0));
        entity.addDeltaMovement(add.multiply((double)0.05f, (double)0.05f, (double)0.05f));
        Vec3 motion = entity.getDeltaMovement();
        if (motion.y > -0.25) {
            entity.fallDistance = 0.0f;
        }
    }

    @OnlyIn(value=Dist.CLIENT)
    public void tickClient() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && this.stormType == 0) {
            this.smoothWindspeed = Mth.lerp((float)0.1f, (float)this.smoothWindspeed, (float)this.windspeed);
            this.smoothWidth = Mth.lerp((float)0.05f, (float)this.smoothWidth, (float)this.width);
            if (this.stage >= 3) {
                if ((this.tornadicWind == null || this.tornadicWind.isStopped()) && !this.dead) {
                    this.tornadicWind = new MovingSoundStreamingSource(this, (SoundEvent)ModSounds.TORNADIC_WIND.value(), SoundSource.WEATHER, 0.1f, 1.0f, this.width, true, 1);
                    Minecraft.getInstance().getSoundManager().play((SoundInstance)this.tornadicWind);
                }
                if (this.windspeed >= 40 && !player.isCreative() && !player.isSpectator()) {
                    this.pull((Entity)player, 2.5f);
                }
            }
            if (this.stage >= 2 && (this.supercellWind == null || this.supercellWind.isStopped()) && !this.dead) {
                this.supercellWind = new MovingSoundStreamingSource(this, (SoundEvent)ModSounds.SUPERCELL_WIND.value(), SoundSource.WEATHER, 0.1f, 1.0f, this.width, true, 0);
                Minecraft.getInstance().getSoundManager().play((SoundInstance)this.supercellWind);
            }
            if (this.stage < 3 && this.tornadicWind != null) {
                this.tornadicWind.stopPlaying();
                this.tornadicWind = null;
            }
            if (this.stage < 2 && this.supercellWind != null) {
                this.supercellWind.stopPlaying();
                this.supercellWind = null;
            }
            for (int i = 0; i < this.listParticleDebris.size(); ++i) {
                EntityRotFX debris = this.listParticleDebris.get(i);
                if (!debris.isAlive()) {
                    this.listParticleDebris.remove((Object)debris);
                    continue;
                }
                this.pull((Particle)debris, 1.0f);
            }
        }
    }

    public void remove() {
        this.dead = true;
        if (EffectiveSide.get().equals((Object)LogicalSide.CLIENT)) {
            this.cleanupClient();
        }
        this.cleanup();
    }

    public void cleanup() {
        this.weatherHandler = null;
        if (!this.level.isClientSide()) {
            for (ChunkPos chunkPos : this.forceLoadedChunks) {
                ((ServerLevel)this.level).setChunkForced(chunkPos.x, chunkPos.z, false);
            }
        }
    }

    @OnlyIn(value=Dist.CLIENT)
    public void cleanupClient() {
        if (this.tornadicWind != null) {
            this.tornadicWind.stopPlaying();
            this.tornadicWind = null;
        }
        if (this.supercellWind != null) {
            this.supercellWind.stopPlaying();
            this.supercellWind = null;
        }
    }

    public void read() {
        this.nbtSyncFromServer();
    }

    public void write() {
        this.nbtSyncForClient();
    }

    public int getUpdateRate() {
        if (this.stormType == 0 && this.stage >= 3) {
            return 2;
        }
        return 40;
    }

    public void nbtSyncFromServer() {
        CachedNBTTagCompound nbt = this.getNBTCache();
        this.ID = nbt.getLong("ID");
        this.onWater = nbt.getBoolean("onWater");
        this.position = new Vec3(nbt.getDouble("positionX"), nbt.getDouble("positionY"), nbt.getDouble("positionZ"));
        this.velocity = new Vec3(nbt.getDouble("velocityX"), nbt.getDouble("velocityY"), nbt.getDouble("velocityZ"));
        this.windspeed = nbt.getInt("windspeed");
        this.width = nbt.getFloat("width");
        this.energy = nbt.getInt("energy");
        this.coldEnergy = nbt.getInt("coldEnergy");
        this.stormType = nbt.getInt("stormType");
        this.stage = nbt.getInt("stage");
        this.dead = nbt.getBoolean("dead");
        this.isDying = nbt.getBoolean("isDying");
        this.maxWidth = nbt.getInt("maxWidth");
        this.maxWindspeed = nbt.getInt("maxWindspeed");
        this.maxStage = nbt.getInt("maxStage");
        this.maxProgress = nbt.getInt("maxProgress");
        this.ticksSinceDying = nbt.getInt("ticksSinceDying");
        this.growthSpeed = nbt.getInt("growthSpeed");
        this.visualOnly = nbt.getBoolean("visualOnly");
        this.aimedAtPlayer = nbt.getBoolean("aimedAtPlayer");
        this.cirus = nbt.getBoolean("cirus");
        this.touchdownSpeed = nbt.getInt("touchdownSpeed");
        this.occlusion = nbt.getFloat("occlusion");
        CompoundTag vorticiesData = nbt.get("vorticies");
        int vorticyCount = vorticiesData.getInt("vorticyCount");
        this.vorticies.clear();
        for (int i = 0; i < vorticyCount; ++i) {
            CompoundTag vorticyData = vorticiesData.getCompound("vorticy" + i);
            Vorticy vorticy = new Vorticy(this, vorticyData.getFloat("maxWindspeedMult"), vorticyData.getFloat("widthPerc"), vorticyData.getFloat("distancePerc"), vorticyData.getInt("lifetime"));
            vorticy.dead = vorticyData.getBoolean("dead");
            vorticy.angle = vorticyData.getFloat("angle");
            vorticy.tickCount = vorticyData.getInt("tickCount");
            vorticy.windspeedMult = vorticyData.getFloat("windspeedMult");
            this.vorticies.add(vorticy);
        }
    }

    public void nbtSyncForClient() {
        CachedNBTTagCompound nbt = this.getNBTCache();
        CompoundTag vorticiesData = new CompoundTag();
        vorticiesData.putInt("vorticyCount", this.vorticies.size());
        for (int i = 0; i < this.vorticies.size(); ++i) {
            Vorticy vorticy = this.vorticies.get(i);
            CompoundTag vorticyData = new CompoundTag();
            vorticyData.putBoolean("dead", vorticy.dead);
            vorticyData.putFloat("windspeedMult", vorticy.windspeedMult);
            vorticyData.putFloat("maxWindspeedMult", vorticy.maxWindspeedMult);
            vorticyData.putFloat("widthPerc", vorticy.widthPerc);
            vorticyData.putFloat("distancePerc", vorticy.distancePerc);
            vorticyData.putFloat("angle", vorticy.angle);
            vorticyData.putInt("lifetime", vorticy.lifetime);
            vorticyData.putInt("tickCount", vorticy.tickCount);
            vorticiesData.put("vorticy" + i, (Tag)vorticyData);
        }
        nbt.put("vorticies", vorticiesData);
        nbt.putBoolean("onWater", this.onWater);
        nbt.putInt("touchdownSpeed", this.touchdownSpeed);
        nbt.putBoolean("cirus", this.cirus);
        nbt.putBoolean("aimedAtPlayer", this.aimedAtPlayer);
        nbt.putBoolean("visualOnly", this.visualOnly);
        nbt.putBoolean("isDying", this.isDying);
        nbt.putInt("maxWidth", this.maxWidth);
        nbt.putInt("maxWindspeed", this.maxWindspeed);
        nbt.putInt("maxStage", this.maxStage);
        nbt.putInt("maxProgress", this.maxProgress);
        nbt.putInt("ticksSinceDying", this.ticksSinceDying);
        nbt.putInt("growthSpeed", this.growthSpeed);
        nbt.putFloat("occlusion", this.occlusion);
        nbt.putDouble("positionX", this.position.x);
        nbt.putDouble("positionY", this.position.y);
        nbt.putDouble("positionZ", this.position.z);
        nbt.putDouble("velocityX", this.velocity.x);
        nbt.putDouble("velocityY", this.velocity.y);
        nbt.putDouble("velocityZ", this.velocity.z);
        nbt.putLong("ID", this.ID);
        nbt.getNewNBT().putLong("ID", this.ID);
        nbt.putInt("windspeed", this.windspeed);
        nbt.putFloat("width", this.width);
        nbt.putInt("energy", this.energy);
        nbt.putInt("coldEnergy", this.coldEnergy);
        nbt.putInt("stormType", this.stormType);
        nbt.putInt("stage", this.stage);
        nbt.putBoolean("dead", this.dead);
    }

    public CachedNBTTagCompound getNBTCache() {
        return this.nbtCache;
    }

    public static float getBlockStrength(Block block, Level level, @Nullable BlockPos blockPos) {
        ItemStack item = new ItemStack((ItemLike)Items.IRON_AXE);
        float destroySpeed = block.defaultBlockState().getDestroySpeed((BlockGetter)level, blockPos != null ? blockPos : BlockPos.ZERO);
        try {
            destroySpeed /= item.getDestroySpeed(block.defaultBlockState());
        }
        catch (Exception e) {
            PMWeather.LOGGER.warn(e.getMessage());
        }
        return 60.0f + Mth.sqrt((float)destroySpeed) * 60.0f;
    }
}

