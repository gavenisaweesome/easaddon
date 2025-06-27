/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.CommandDispatcher
 *  it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap
 *  net.minecraft.commands.CommandSourceStack
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.tags.TagKey
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.LevelAccessor
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.levelgen.Heightmap$Types
 *  net.minecraft.world.phys.Vec3
 *  net.neoforged.bus.api.SubscribeEvent
 *  net.neoforged.fml.common.EventBusSubscriber
 *  net.neoforged.fml.common.EventBusSubscriber$Bus
 *  net.neoforged.neoforge.common.Tags$Blocks
 *  net.neoforged.neoforge.event.RegisterCommandsEvent
 *  net.neoforged.neoforge.event.level.LevelEvent$Load
 *  net.neoforged.neoforge.event.level.LevelEvent$Unload
 *  net.neoforged.neoforge.event.tick.EntityTickEvent$Post
 *  net.neoforged.neoforge.event.tick.LevelTickEvent$Post
 *  net.neoforged.neoforge.event.tick.ServerTickEvent$Pre
 *  net.neoforged.neoforge.server.command.ConfigCommand
 */
package dev.protomanly.pmweather.event;

import com.mojang.brigadier.CommandDispatcher;
import dev.protomanly.pmweather.PMWeather;
import dev.protomanly.pmweather.command.WeatherCommands;
import dev.protomanly.pmweather.config.ServerConfig;
import dev.protomanly.pmweather.entity.ModEntities;
import dev.protomanly.pmweather.entity.MovingBlock;
import dev.protomanly.pmweather.util.Util;
import dev.protomanly.pmweather.weather.Sounding;
import dev.protomanly.pmweather.weather.Storm;
import dev.protomanly.pmweather.weather.ThermodynamicEngine;
import dev.protomanly.pmweather.weather.WeatherHandler;
import dev.protomanly.pmweather.weather.WeatherHandlerServer;
import dev.protomanly.pmweather.weather.WindEngine;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.server.command.ConfigCommand;

@EventBusSubscriber(modid="pmweather", bus=EventBusSubscriber.Bus.GAME)
public class GameBusEvents {
    public static final Map<ResourceKey<Level>, WeatherHandler> MANAGERS = new Reference2ObjectOpenHashMap();
    public static final Map<String, WeatherHandler> MANAGERSLOOKUP = new HashMap<String, WeatherHandler>();

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        Level level = entity.level();
        if (!level.isClientSide() && entity instanceof MovingBlock) {
            Vec3 wind = WindEngine.getWind(entity.getPosition(1.0f), level, false, true, false);
            entity.addDeltaMovement(wind.multiply((double)0.05f, 0.0, (double)0.05f).multiply((double)0.01f, 0.0, (double)0.01f));
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        Level level = event.getLevel();
        if (!level.isClientSide() && ServerConfig.validDimensions != null && level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            if (ServerConfig.validDimensions.contains(level.dimension())) {
                WeatherHandlerServer weatherHandler = (WeatherHandlerServer)MANAGERS.get(level.dimension());
                if (PMWeather.RANDOM.nextInt(2) == 0) {
                    ArrayList<ServerPlayer> validPlayers = new ArrayList<ServerPlayer>();
                    List plrs = serverLevel.players();
                    Collections.shuffle(plrs);
                    for (ServerPlayer player : plrs) {
                        boolean isTooNear = false;
                        for (ServerPlayer existing : validPlayers) {
                            if (!(existing.distanceTo((Entity)player) <= 64.0f)) continue;
                            isTooNear = true;
                            break;
                        }
                        if (isTooNear) continue;
                        validPlayers.add(player);
                    }
                    for (ServerPlayer player : validPlayers) {
                        for (int i = 0; i < 60; ++i) {
                            MovingBlock movingBlock;
                            BlockPos check = player.blockPosition().offset(new Vec3i(PMWeather.RANDOM.nextInt(-64, 65), 50, PMWeather.RANDOM.nextInt(-64, 65)));
                            float wind = (float)WindEngine.getWind(check = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, check).below(), level, false, true, false).length();
                            if (!(wind > 45.0f)) continue;
                            check = check.below(PMWeather.RANDOM.nextInt(3));
                            BlockState state = level.getBlockState(check);
                            Block block = state.getBlock();
                            float blockStrength = Storm.getBlockStrength(block, level, check);
                            if (ServerConfig.blockStrengths.containsKey(block)) {
                                blockStrength = ServerConfig.blockStrengths.get(block).floatValue();
                            }
                            blockStrength *= 0.9f;
                            boolean blacklisted = false;
                            for (TagKey<Block> tag : ServerConfig.blacklistedBlockTags) {
                                if (!block.defaultBlockState().is(tag)) continue;
                                blacklisted = true;
                                break;
                            }
                            if (blacklisted || ServerConfig.blacklistedBlocks.contains(block) || !Util.canWindAffect(check.getCenter(), level)) continue;
                            if (state.is(Tags.Blocks.GLASS_BLOCKS) || state.is(Tags.Blocks.GLASS_PANES)) {
                                double percChance = Math.clamp((wind - 55.0f) / 15.0f, 0.0f, 1.0f);
                                if (!((double)PMWeather.RANDOM.nextFloat() <= percChance) || !Util.canWindAffect(check.getCenter(), level)) continue;
                                level.removeBlock(check, false);
                                level.playSound(null, check, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1.0f, PMWeather.RANDOM.nextFloat(0.8f, 1.2f));
                                continue;
                            }
                            double percChance = Math.clamp(Math.pow(Math.clamp(Math.max((double)(wind - blockStrength), 0.0) / 20.0, 0.0, 1.0), 2.0) + 0.02, 0.0, 1.0);
                            if (wind < blockStrength) {
                                percChance = 0.0;
                            }
                            if (block.defaultDestroyTime() < 0.05f && block.defaultDestroyTime() >= 0.0f && (double)PMWeather.RANDOM.nextFloat() <= percChance && Util.canWindAffect(check.getCenter(), level)) {
                                level.removeBlock(check, false);
                                continue;
                            }
                            if (!((double)PMWeather.RANDOM.nextFloat() <= percChance) || !Util.canWindAffect(check.getCenter(), level) || (movingBlock = (MovingBlock)ModEntities.MOVING_BLOCK.get().create(level)) == null) continue;
                            movingBlock.setStartPos(check);
                            movingBlock.setBlockState(state);
                            movingBlock.setPos(check.getX(), check.getY(), check.getZ());
                            level.removeBlock(check, false);
                            if (level.isLoaded(check)) {
                                level.addFreshEntity((Entity)movingBlock);
                                continue;
                            }
                            movingBlock.discard();
                        }
                    }
                }
                int storms = weatherHandler.getStorms().size();
                if (level.getGameTime() % 1200L == 0L) {
                    PMWeather.LOGGER.debug("Checking for storm/cloud spawns");
                    ArrayList<ServerPlayer> validPlayers = new ArrayList<ServerPlayer>();
                    List plrs = serverLevel.players();
                    Collections.shuffle(plrs);
                    for (ServerPlayer player : plrs) {
                        boolean isTooNear = false;
                        for (ServerPlayer existing : validPlayers) {
                            if (!(existing.distanceTo((Entity)player) <= (float)ServerConfig.spawnRange / 2.0f)) continue;
                            isTooNear = true;
                            break;
                        }
                        if (isTooNear) continue;
                        validPlayers.add(player);
                    }
                    PMWeather.LOGGER.debug("{} players available to spawn around", (Object)validPlayers.size());
                    for (ServerPlayer player : validPlayers) {
                        boolean squall;
                        Vec3 pos = new Vec3(player.getX(), (double)level.getMaxBuildHeight(), player.getZ()).add((double)PMWeather.RANDOM.nextInt(-ServerConfig.spawnRange, ServerConfig.spawnRange + 1), 0.0, (double)PMWeather.RANDOM.nextInt(-ServerConfig.spawnRange, ServerConfig.spawnRange + 1));
                        Vec3 wind = WindEngine.getWind(new Vec3(player.getX(), (double)(level.getMaxBuildHeight() + 1), player.getZ()), level, true, true, false);
                        boolean bl = squall = PMWeather.RANDOM.nextInt(ServerConfig.chanceInOneSquall) == 0;
                        if (squall) {
                            PMWeather.LOGGER.debug("Checking for squall spawn");
                            dist = PMWeather.RANDOM.nextFloat(256.0f, 512.0f) * 6.0f;
                            pos = pos.add(wind.normalize().multiply((double)(-dist), 0.0, (double)(-dist)));
                        } else {
                            dist = PMWeather.RANDOM.nextFloat(256.0f, 512.0f) * 4.0f;
                            pos = pos.add(wind.normalize().multiply((double)(-dist), 0.0, (double)(-dist)));
                        }
                        PMWeather.LOGGER.debug("Checking storm spawns around {} at {}, {}", new Object[]{player.getDisplayName().getString(), (int)pos.x, (int)pos.z});
                        double spawnChance = ServerConfig.stormSpawnChancePerMinute;
                        Vec3 sfcPos = serverLevel.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, player.blockPosition()).getCenter();
                        Sounding sounding = new Sounding(weatherHandler, sfcPos, level, 250, 16000);
                        ThermodynamicEngine.AtmosphericDataPoint sfc = ThermodynamicEngine.samplePoint(weatherHandler, sfcPos, level, null, 0);
                        float riskV = sounding.getRisk(0);
                        if (ServerConfig.environmentSystem) {
                            spawnChance = squall ? (spawnChance *= Math.pow(riskV, 0.75) + (double)0.035f) : (spawnChance *= Math.pow(riskV, 0.75) * 1.25 + 0.02);
                            if (sfc.temperature() < 3.0f) {
                                spawnChance += (double)(Math.clamp((sfc.temperature() - 3.0f) / -6.0f, 0.0f, 1.0f) * 0.035f);
                            }
                            PMWeather.LOGGER.debug("W/ spawn chance: {}%\nRisk: {}", (Object)((int)(spawnChance * 100.0)), (Object)((int)(riskV * 100.0f)));
                        }
                        if ((double)PMWeather.RANDOM.nextFloat() <= spawnChance && storms < ServerConfig.maxStorms) {
                            if (squall) {
                                if (sfc.temperature() < 3.0f) {
                                    riskV += Math.clamp((sfc.temperature() - 3.0f) / -6.0f, 0.0f, 1.0f) * 0.25f;
                                }
                                storm = new Storm(weatherHandler, level, Float.valueOf(riskV), 1);
                                storm.width = 0.0f;
                                storm.windspeed = 0;
                                storm.stormType = 1;
                                storm.stage = 0;
                                if (ServerConfig.environmentSystem) {
                                    if (PMWeather.RANDOM.nextFloat() <= riskV * 2.5f) {
                                        storm.maxStage = Math.max(storm.maxStage, 1);
                                    }
                                    if (PMWeather.RANDOM.nextFloat() <= riskV * 2.0f) {
                                        storm.maxStage = Math.max(storm.maxStage, 2);
                                    }
                                    if (PMWeather.RANDOM.nextFloat() <= riskV * 1.5f) {
                                        storm.maxStage = Math.max(storm.maxStage, 3);
                                    }
                                    storm.recalc(Float.valueOf(riskV));
                                }
                                if (wind.length() < 6.0) {
                                    wind = wind.normalize().multiply(6.0, 0.0, 6.0);
                                }
                                storm.position = pos;
                                storm.velocity = wind.multiply(0.1, 0.0, 0.1);
                                storm.energy = 0;
                                storm.initFirstTime();
                                weatherHandler.addStorm(storm);
                                weatherHandler.syncStormNew(storm);
                                ++storms;
                            } else {
                                storm = new Storm(weatherHandler, level, Float.valueOf(riskV), 0);
                                storm.width = 0.0f;
                                storm.windspeed = 0;
                                storm.stormType = 0;
                                storm.stage = 0;
                                if (ServerConfig.environmentSystem) {
                                    if (PMWeather.RANDOM.nextFloat() <= riskV * 2.5f) {
                                        storm.maxStage = Math.max(storm.maxStage, 1);
                                    }
                                    if (PMWeather.RANDOM.nextFloat() <= riskV * 2.0f) {
                                        storm.maxStage = Math.max(storm.maxStage, 2);
                                    }
                                    if (PMWeather.RANDOM.nextFloat() <= riskV * 1.5f) {
                                        storm.maxStage = Math.max(storm.maxStage, 3);
                                    }
                                    storm.recalc(Float.valueOf(riskV));
                                }
                                storm.position = pos;
                                storm.velocity = Vec3.ZERO;
                                storm.energy = 0;
                                storm.initFirstTime();
                                weatherHandler.addStorm(storm);
                                weatherHandler.syncStormNew(storm);
                                ++storms;
                            }
                            PMWeather.LOGGER.debug("Spawned storm at {}, {}", (Object)((int)pos.x), (Object)((int)pos.z));
                            continue;
                        }
                        PMWeather.LOGGER.debug("Storm spawn failed, rolled bad number or too many storms");
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        LevelAccessor level = event.getLevel();
        if (!level.isClientSide() && level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            ResourceKey dimension = serverLevel.dimension();
            WeatherHandlerServer weatherHandler = new WeatherHandlerServer(serverLevel);
            weatherHandler.read();
            MANAGERS.put((ResourceKey<Level>)dimension, weatherHandler);
            MANAGERSLOOKUP.put(dimension.location().toString(), weatherHandler);
            if (WindEngine.simplexNoise == null) {
                WindEngine.init(weatherHandler);
            }
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        LevelAccessor level = event.getLevel();
        if (!level.isClientSide() && level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            MANAGERS.remove(serverLevel.dimension());
            MANAGERSLOOKUP.remove(serverLevel.dimension().toString());
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Pre event) {
        if (!event.getServer().isPaused()) {
            for (WeatherHandler weatherHandler : MANAGERS.values()) {
                weatherHandler.tick();
            }
        }
    }

    @SubscribeEvent
    public static void onCommandsRegister(RegisterCommandsEvent event) {
        new WeatherCommands((CommandDispatcher<CommandSourceStack>)event.getDispatcher(), event.getBuildContext());
        ConfigCommand.register((CommandDispatcher)event.getDispatcher());
    }

    public static void playerRequestsFullSync(ServerPlayer player) {
        WeatherHandler weatherHandler = MANAGERS.get(player.level().dimension());
        if (weatherHandler instanceof WeatherHandlerServer) {
            WeatherHandlerServer weatherHandlerServer = (WeatherHandlerServer)weatherHandler;
            weatherHandlerServer.playerJoinedWorldSyncFull(player);
        }
    }
}

