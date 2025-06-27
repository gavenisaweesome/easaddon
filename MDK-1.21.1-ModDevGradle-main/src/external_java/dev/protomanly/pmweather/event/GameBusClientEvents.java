/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.particle.Particle
 *  net.minecraft.client.particle.ParticleRenderType
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.client.renderer.texture.TextureAtlasSprite
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.util.Mth
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.levelgen.Heightmap$Types
 *  net.minecraft.world.level.material.MapColor
 *  net.minecraft.world.phys.Vec3
 *  net.minecraft.world.phys.shapes.VoxelShape
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.bus.api.SubscribeEvent
 *  net.neoforged.fml.common.EventBusSubscriber
 *  net.neoforged.fml.common.EventBusSubscriber$Bus
 *  net.neoforged.neoforge.client.event.ClientTickEvent$Pre
 *  net.neoforged.neoforge.client.event.RenderLevelStageEvent
 *  net.neoforged.neoforge.client.event.RenderLevelStageEvent$Stage
 *  net.neoforged.neoforge.client.event.ViewportEvent$RenderFog
 */
package dev.protomanly.pmweather.event;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.protomanly.pmweather.PMWeather;
import dev.protomanly.pmweather.config.ClientConfig;
import dev.protomanly.pmweather.config.ServerConfig;
import dev.protomanly.pmweather.interfaces.ParticleData;
import dev.protomanly.pmweather.networking.ModNetworking;
import dev.protomanly.pmweather.particle.EntityRotFX;
import dev.protomanly.pmweather.particle.ParticleCube;
import dev.protomanly.pmweather.particle.ParticleManager;
import dev.protomanly.pmweather.particle.ParticleRegistry;
import dev.protomanly.pmweather.particle.ParticleTexExtraRender;
import dev.protomanly.pmweather.particle.ParticleTexFX;
import dev.protomanly.pmweather.particle.behavior.ParticleBehavior;
import dev.protomanly.pmweather.shaders.ModShaders;
import dev.protomanly.pmweather.sound.ModSounds;
import dev.protomanly.pmweather.util.ChunkCoordinatesBlock;
import dev.protomanly.pmweather.weather.ThermodynamicEngine;
import dev.protomanly.pmweather.weather.WeatherHandler;
import dev.protomanly.pmweather.weather.WeatherHandlerClient;
import dev.protomanly.pmweather.weather.WindEngine;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

@EventBusSubscriber(modid="pmweather", bus=EventBusSubscriber.Bus.GAME, value={Dist.CLIENT})
public class GameBusClientEvents {
    public static Level lastLevel;
    public static WeatherHandler weatherHandler;
    public static ParticleManager particleManager;
    public static ParticleManager particleManagerDebris;
    public static ParticleBehavior particleBehavior;
    public static List<Block> LEAVES_BLOCKS;
    public static ArrayList<ChunkCoordinatesBlock> soundLocations;
    public static HashMap<ChunkCoordinatesBlock, Long> soundTimeLocations;
    public static long lastAmbientTick;
    public static long lastAmbientTickThreaded;
    public static long lastWindSoundTick;

    @SubscribeEvent
    public static void fogEvent(ViewportEvent.RenderFog event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level != null && ClientConfig.baseGameFog) {
            RenderSystem.setShaderFogStart((float)10000.0f);
            RenderSystem.setShaderFogEnd((float)40000.0f);
        }
    }

    @SubscribeEvent
    public static void onStageRenderTick(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES && weatherHandler != null) {
            particleManagerDebris.render(event.getPoseStack(), null, Minecraft.getInstance().gameRenderer.lightTexture(), event.getCamera(), event.getPartialTick().getGameTimeDeltaPartialTick(false), event.getFrustum());
        }
    }

    public static void doSnowParticles(float precip, Minecraft minecraft, Level level) {
        int spawnsNeeded = (int)(precip * 80.0f);
        int spawns = 0;
        int spawnAreaSize = 50;
        for (int i = 0; i < 60; ++i) {
            BlockPos pos = minecraft.player.blockPosition().offset(PMWeather.RANDOM.nextInt(spawnAreaSize) - spawnAreaSize / 2, -5 + PMWeather.RANDOM.nextInt(25), PMWeather.RANDOM.nextInt(spawnAreaSize) - spawnAreaSize / 2);
            if (!GameBusClientEvents.canPrecipitateAt(level, pos)) continue;
            TextureAtlasSprite particle = switch (PMWeather.RANDOM.nextInt(4)) {
                case 1 -> ParticleRegistry.snow1;
                case 2 -> ParticleRegistry.snow2;
                case 3 -> ParticleRegistry.snow3;
                default -> ParticleRegistry.snow;
            };
            ParticleTexExtraRender snow = new ParticleTexExtraRender((ClientLevel)level, pos.getX(), pos.getY(), pos.getZ(), 0.0, 0.0, 0.0, particle);
            snow.fullAlphaTarget = 1.0f;
            snow.renderOrder = 3;
            particleBehavior.initParticleSnow(snow, Math.max((int)(5.0f * precip), 1), (float)(WindEngine.getWind(pos, level, false, false, true).length() / 45.0));
            snow.setScale(Math.max(precip * 0.08f + (PMWeather.RANDOM.nextFloat() - PMWeather.RANDOM.nextFloat()) * 0.02f, 0.01f));
            snow.windWeight = 0.15f;
            snow.renderOrder = 3;
            snow.spawnAsWeatherEffect();
            if (++spawns > spawnsNeeded) break;
        }
    }

    public static void doSleetParticles(float precip, Minecraft minecraft, Level level) {
        int spawnsNeeded = (int)(precip * 300.0f);
        int spawns = 0;
        int spawnAreaSize = 30;
        for (int i = 0; i < 60; ++i) {
            BlockPos pos = minecraft.player.blockPosition().offset(PMWeather.RANDOM.nextInt(spawnAreaSize) - spawnAreaSize / 2, -5 + PMWeather.RANDOM.nextInt(25), PMWeather.RANDOM.nextInt(spawnAreaSize) - spawnAreaSize / 2);
            if (!GameBusClientEvents.canPrecipitateAt(level, pos)) continue;
            ParticleTexExtraRender sleet = new ParticleTexExtraRender((ClientLevel)level, pos.getX(), pos.getY(), pos.getZ(), 0.0, 0.0, 0.0, ParticleRegistry.sleet);
            sleet.fullAlphaTarget = 1.0f;
            sleet.renderOrder = 3;
            particleBehavior.initParticleSleet(sleet, Math.max((int)(20.0f * precip), 1));
            sleet.setScale(Math.max(precip * 0.08f + (PMWeather.RANDOM.nextFloat() - PMWeather.RANDOM.nextFloat()) * 0.02f, 0.02f) * 0.3f);
            sleet.renderOrder = 3;
            sleet.spawnAsWeatherEffect();
            if (++spawns > spawnsNeeded) break;
        }
    }

    public static void doRainParticles(float precip, Minecraft minecraft, Level level) {
        BlockPos pos;
        int i;
        int spawnsNeeded = (int)(precip * 300.0f);
        int spawns = 0;
        int spawnAreaSize = 30;
        for (i = 0; i < 60; ++i) {
            pos = minecraft.player.blockPosition().offset(PMWeather.RANDOM.nextInt(spawnAreaSize) - spawnAreaSize / 2, -5 + PMWeather.RANDOM.nextInt(25), PMWeather.RANDOM.nextInt(spawnAreaSize) - spawnAreaSize / 2);
            if (!GameBusClientEvents.canPrecipitateAt(level, pos)) continue;
            ParticleTexExtraRender rain = new ParticleTexExtraRender((ClientLevel)level, pos.getX(), pos.getY(), pos.getZ(), 0.0, 0.0, 0.0, ParticleRegistry.rain);
            rain.fullAlphaTarget = Mth.lerp((float)precip, (float)0.3f, (float)1.0f);
            rain.renderOrder = 3;
            particleBehavior.initParticleRain(rain, Math.max((int)(20.0f * precip), 1));
            if (++spawns > spawnsNeeded) break;
        }
        spawnAreaSize = 40;
        i = 0;
        while ((float)i < 200.0f * precip) {
            pos = minecraft.player.blockPosition().offset(PMWeather.RANDOM.nextInt(spawnAreaSize) - spawnAreaSize / 2, -5 + PMWeather.RANDOM.nextInt(25), PMWeather.RANDOM.nextInt(spawnAreaSize) - spawnAreaSize / 2);
            pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos).below();
            BlockState state = level.getBlockState(pos);
            double maxY = 0.0;
            double minY = 0.0;
            VoxelShape shape = state.getShape((BlockGetter)level, pos);
            if (!shape.isEmpty()) {
                minY = shape.bounds().minY;
                maxY = shape.bounds().maxY;
            }
            if (!(pos.distSqr((Vec3i)minecraft.player.blockPosition()) > (double)spawnAreaSize / 2.0 * ((double)spawnAreaSize / 2.0)) && GameBusClientEvents.canPrecipitateAt(level, pos.above())) {
                if (level.getBlockState(pos).getBlock().defaultMapColor() == MapColor.WATER) {
                    pos = pos.offset(0, 1, 0);
                }
                ParticleTexFX rain = new ParticleTexFX((ClientLevel)level, (float)pos.getX() + PMWeather.RANDOM.nextFloat(), (double)pos.getY() + 0.01 + maxY, (float)pos.getZ() + PMWeather.RANDOM.nextFloat(), 0.0, 0.0, 0.0, ParticleRegistry.splash);
                rain.fullAlphaTarget = Mth.lerp((float)precip, (float)0.2f, (float)0.8f) / 2.0f;
                rain.renderOrder = 4;
                particleBehavior.initParticleGroundSplash(rain);
                rain.spawnAsWeatherEffect();
            }
            ++i;
        }
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level != null && !minecraft.isPaused()) {
            GameBusClientEvents.getClientWeather();
            GameBusClientEvents.tryAmbientSounds();
            GameBusClientEvents.trySounds();
            weatherHandler.tick();
            particleManager.tick();
            particleManagerDebris.tick();
            ModShaders.tick();
            WeatherHandlerClient weatherHandlerClient = (WeatherHandlerClient)weatherHandler;
            if (minecraft.player != null) {
                minecraft.particleEngine.iterateParticles(arg_0 -> GameBusClientEvents.lambda$onTick$0((Level)level, arg_0));
                particleManager.getParticles().forEach((arg_0, arg_1) -> GameBusClientEvents.lambda$onTick$1((Level)level, arg_0, arg_1));
                particleManagerDebris.getParticles().forEach((arg_0, arg_1) -> GameBusClientEvents.lambda$onTick$2((Level)level, arg_0, arg_1));
                float hail = weatherHandlerClient.getHail();
                float precip = weatherHandlerClient.getPrecipitation();
                if (precip > 0.0f) {
                    ThermodynamicEngine.Precipitation precipType = ThermodynamicEngine.getPrecipitationType(weatherHandlerClient, minecraft.player.position(), (Level)level, 0);
                    if (precipType == ThermodynamicEngine.Precipitation.RAIN || precipType == ThermodynamicEngine.Precipitation.FREEZING_RAIN || precipType == ThermodynamicEngine.Precipitation.WINTRY_MIX) {
                        GameBusClientEvents.doRainParticles(precip, minecraft, (Level)level);
                    }
                    if (precipType == ThermodynamicEngine.Precipitation.SLEET || precipType == ThermodynamicEngine.Precipitation.WINTRY_MIX) {
                        GameBusClientEvents.doSleetParticles(precip, minecraft, (Level)level);
                    }
                    if (precipType == ThermodynamicEngine.Precipitation.SNOW || precipType == ThermodynamicEngine.Precipitation.WINTRY_MIX) {
                        GameBusClientEvents.doSnowParticles(precip, minecraft, (Level)level);
                    }
                }
                if (hail > 0.0f) {
                    int spawnsNeeded = (int)(hail * 80.0f);
                    int spawns = 0;
                    int spawnAreaSize = 30;
                    for (int i = 0; i < 15; ++i) {
                        BlockPos pos = minecraft.player.blockPosition().offset(PMWeather.RANDOM.nextInt(spawnAreaSize) - spawnAreaSize / 2, -5 + PMWeather.RANDOM.nextInt(25), PMWeather.RANDOM.nextInt(spawnAreaSize) - spawnAreaSize / 2);
                        if (!GameBusClientEvents.canPrecipitateAt((Level)level, pos)) continue;
                        ParticleCube hailP = new ParticleCube(level, (double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), 0.0, 0.0, 0.0, Blocks.PACKED_ICE.defaultBlockState());
                        particleBehavior.initParticleHail(hailP);
                        hailP.setScale(0.01f + PMWeather.RANDOM.nextFloat() * hail * 0.08f);
                        hailP.renderOrder = 3;
                        hailP.spawnAsDebrisEffect();
                        if (++spawns >= spawnsNeeded) break;
                    }
                }
            }
        }
    }

    public static boolean canPrecipitateAt(Level level, BlockPos pos) {
        if ((double)pos.getY() > ServerConfig.layer0Height) {
            return false;
        }
        return level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos).getY() <= pos.getY();
    }

    public static void resetClientWeather() {
        weatherHandler = null;
    }

    public static WeatherHandlerClient getClientWeather() {
        try {
            ClientLevel level = Minecraft.getInstance().level;
            if (weatherHandler == null || level != lastLevel) {
                GameBusClientEvents.init((Level)level);
            }
        }
        catch (Exception e) {
            PMWeather.LOGGER.error(e.getMessage(), (Throwable)e);
        }
        return (WeatherHandlerClient)weatherHandler;
    }

    public static void trySounds() {
        try {
            int chance;
            Minecraft minecraft = Minecraft.getInstance();
            ClientLevel level = minecraft.level;
            LocalPlayer player = minecraft.player;
            if (player == null || level == null) {
                return;
            }
            float hail = ((WeatherHandlerClient)weatherHandler).getHail();
            if (hail > 0.0f && PMWeather.RANDOM.nextInt(chance = (int)Mth.lerp((float)hail, (float)20.0f, (float)2.0f)) == 0) {
                BlockPos pos = player.blockPosition().offset(PMWeather.RANDOM.nextInt(-15, 16), 15, PMWeather.RANDOM.nextInt(-15, 16));
                if (GameBusClientEvents.canPrecipitateAt((Level)level, pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos)) && pos.distSqr((Vec3i)player.blockPosition()) < 225.0) {
                    level.playLocalSound(pos, (SoundEvent)ModSounds.HAIL.value(), SoundSource.WEATHER, hail * 3.5f, 2.0f + PMWeather.RANDOM.nextFloat() * 0.5f, false);
                }
            }
            if (lastWindSoundTick < System.currentTimeMillis()) {
                lastWindSoundTick = System.currentTimeMillis() + 4000L + (long)PMWeather.RANDOM.nextInt(0, 3000);
                Vec3 wind = WindEngine.getWind(player.position(), (Level)level);
                double windspeed = wind.length();
                if (windspeed > 55.0) {
                    ModSounds.playPlayerLockedSound(player.position(), (SoundEvent)ModSounds.WIND_STRONG.value(), (float)(windspeed / 200.0), 0.9f + PMWeather.RANDOM.nextFloat() * 0.2f);
                }
                if (windspeed > 35.0) {
                    ModSounds.playPlayerLockedSound(player.position(), (SoundEvent)ModSounds.WIND_MED.value(), (float)(windspeed / 200.0), 0.9f + PMWeather.RANDOM.nextFloat() * 0.2f);
                }
                if (windspeed > 5.0) {
                    ModSounds.playPlayerLockedSound(player.position(), (SoundEvent)ModSounds.WIND_CALM.value(), Math.min((float)(windspeed / 100.0), 0.1f), 0.9f + PMWeather.RANDOM.nextFloat() * 0.2f);
                }
            }
            if (lastAmbientTick < System.currentTimeMillis()) {
                lastAmbientTick = System.currentTimeMillis() + 500L;
                int size = 32;
                int hSize = size / 2;
                BlockPos curBlockPos = player.blockPosition();
                for (int i = 0; i < soundLocations.size(); ++i) {
                    ChunkCoordinatesBlock chunkCoord = soundLocations.get(i);
                    if (Math.sqrt(chunkCoord.distSqr((Vec3i)curBlockPos)) > (double)size) {
                        soundLocations.remove(i--);
                        soundTimeLocations.remove((Object)chunkCoord);
                        continue;
                    }
                    Block block = level.getBlockState((BlockPos)chunkCoord).getBlock();
                    if (block == null || block.defaultMapColor() != MapColor.WATER && block.defaultMapColor() != MapColor.PLANT) {
                        soundLocations.remove(i);
                        soundTimeLocations.remove((Object)chunkCoord);
                        continue;
                    }
                    long lastPlayTime = 0L;
                    float soundMuffle = 0.6f;
                    if (soundTimeLocations.containsKey((Object)chunkCoord)) {
                        lastPlayTime = soundTimeLocations.get((Object)chunkCoord);
                    }
                    float maxLeavesVolume = 1.0f;
                    soundMuffle *= (float)ClientConfig.leavesVolume;
                    if (lastPlayTime >= System.currentTimeMillis() || !LEAVES_BLOCKS.contains(chunkCoord.block)) continue;
                    Vec3 wind = WindEngine.getWind(curBlockPos, (Level)level, false, false, false);
                    double windspeed = wind.length();
                    soundTimeLocations.put(chunkCoord, System.currentTimeMillis() + 12000L + (long)PMWeather.RANDOM.nextInt(50));
                    minecraft.level.playLocalSound((BlockPos)chunkCoord, (SoundEvent)ModSounds.CALM_AMBIENCE.value(), SoundSource.AMBIENT, (float)Math.min((double)maxLeavesVolume, windspeed * (double)soundMuffle * (double)0.05f), 0.9f + PMWeather.RANDOM.nextFloat() * 0.2f, false);
                }
            }
        }
        catch (Exception e) {
            PMWeather.LOGGER.error(e.getMessage(), (Throwable)e);
        }
    }

    public static void tryAmbientSounds() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        if (lastAmbientTickThreaded < System.currentTimeMillis() && ClientConfig.leavesVolume > 0.0) {
            lastAmbientTickThreaded = System.currentTimeMillis() + 500L;
            int size = 32;
            int hSize = size / 2;
            BlockPos curBlockPos = player.blockPosition();
            for (int x = curBlockPos.getX() - hSize; x < curBlockPos.getX() + hSize; ++x) {
                for (int y = curBlockPos.getY() - hSize; y < curBlockPos.getY() + hSize; ++y) {
                    for (int z = curBlockPos.getZ() - hSize; z < curBlockPos.getZ() + hSize; ++z) {
                        Block block = level.getBlockState(new BlockPos(x, y, z)).getBlock();
                        if (block.defaultMapColor() != MapColor.PLANT) continue;
                        boolean proxFail = false;
                        for (ChunkCoordinatesBlock soundLocation : soundLocations) {
                            BlockPos blockPos = new BlockPos(x, y, z);
                            if (!(Math.sqrt(soundLocation.distSqr((Vec3i)blockPos)) < 15.0)) continue;
                            proxFail = true;
                            break;
                        }
                        if (proxFail) continue;
                        soundLocations.add(new ChunkCoordinatesBlock(x, y, z, block));
                    }
                }
            }
        }
    }

    public static void init(Level level) {
        lastLevel = level;
        if (level == null) {
            return;
        }
        weatherHandler = new WeatherHandlerClient((ResourceKey<Level>)level.dimension());
        Minecraft minecraft = Minecraft.getInstance();
        if (particleManager == null) {
            particleManager = new ParticleManager(minecraft.level, minecraft.getTextureManager());
        } else {
            particleManager.setLevel((ClientLevel)level);
        }
        if (particleManagerDebris == null) {
            particleManagerDebris = new ParticleManager(minecraft.level, minecraft.getTextureManager());
        } else {
            particleManagerDebris.setLevel((ClientLevel)level);
        }
        CompoundTag data = new CompoundTag();
        data.putString("command", "syncFull");
        data.putString("packetCommand", "WeatherData");
        ModNetworking.clientSendToSever(data);
    }

    private static /* synthetic */ void lambda$onTick$2(Level level, ParticleRenderType particleRenderType, Queue particles) {
        for (Particle particle : particles) {
            if (!(particle instanceof ParticleData)) continue;
            ParticleData particleData = (ParticleData)particle;
            float affect = 1.0f;
            if (particle instanceof EntityRotFX) {
                EntityRotFX entityRotFX = (EntityRotFX)particle;
                affect = entityRotFX.ignoreWind ? 0.0f : entityRotFX.windWeight;
            }
            if (!(affect > 0.0f)) continue;
            Vec3 wind = WindEngine.getWind(particle.getPos(), level, false, false, false);
            particleData.addVelocity(wind.multiply((double)0.05f, (double)0.05f, (double)0.05f).multiply((double)0.04f, (double)0.04f, (double)0.04f).multiply((double)affect, (double)affect, (double)affect));
        }
    }

    private static /* synthetic */ void lambda$onTick$1(Level level, ParticleRenderType particleRenderType, Queue particles) {
        for (Particle particle : particles) {
            if (!(particle instanceof ParticleData)) continue;
            ParticleData particleData = (ParticleData)particle;
            float affect = 1.0f;
            if (particle instanceof EntityRotFX) {
                EntityRotFX entityRotFX = (EntityRotFX)particle;
                affect = entityRotFX.ignoreWind ? 0.0f : entityRotFX.windWeight;
            }
            if (!(affect > 0.0f)) continue;
            Vec3 wind = WindEngine.getWind(particle.getPos(), level, false, false, false);
            particleData.addVelocity(wind.multiply((double)0.05f, (double)0.05f, (double)0.05f).multiply((double)0.04f, (double)0.04f, (double)0.04f).multiply((double)affect, (double)affect, (double)affect));
            double l = wind.length() * 0.01;
            if (!(particleData.getVelocity().length() < l)) continue;
            particleData.setVelocity(particleData.getVelocity().normalize().multiply(l, l, l));
        }
    }

    private static /* synthetic */ void lambda$onTick$0(Level level, Particle particle) {
        if (particle instanceof ParticleData) {
            ParticleData particleData = (ParticleData)particle;
            boolean affect = true;
            if (particle instanceof EntityRotFX) {
                EntityRotFX entityRotFX = (EntityRotFX)particle;
                boolean bl = affect = !entityRotFX.ignoreWind;
            }
            if (affect) {
                Vec3 wind = WindEngine.getWind(particle.getPos(), level, false, false, false);
                particleData.addVelocity(wind.multiply((double)0.05f, (double)0.05f, (double)0.05f).multiply((double)0.04f, (double)0.04f, (double)0.04f));
                double l = wind.length() * 0.01;
                if (particleData.getVelocity().length() < l) {
                    particleData.setVelocity(particleData.getVelocity().normalize().multiply(l, l, l));
                }
            }
        }
    }

    static {
        particleBehavior = new ParticleBehavior(null);
        LEAVES_BLOCKS = new ArrayList<Block>(){
            {
                this.add(Blocks.ACACIA_LEAVES);
                this.add(Blocks.AZALEA_LEAVES);
                this.add(Blocks.BIRCH_LEAVES);
                this.add(Blocks.DARK_OAK_LEAVES);
                this.add(Blocks.CHERRY_LEAVES);
                this.add(Blocks.FLOWERING_AZALEA_LEAVES);
                this.add(Blocks.MANGROVE_LEAVES);
                this.add(Blocks.OAK_LEAVES);
                this.add(Blocks.JUNGLE_LEAVES);
                this.add(Blocks.SPRUCE_LEAVES);
            }
        };
        soundLocations = new ArrayList();
        soundTimeLocations = new HashMap();
    }
}

