/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.bus.api.SubscribeEvent
 *  net.neoforged.fml.common.EventBusSubscriber
 *  net.neoforged.fml.common.EventBusSubscriber$Bus
 *  net.neoforged.fml.event.config.ModConfigEvent
 *  net.neoforged.fml.event.config.ModConfigEvent$Unloading
 *  net.neoforged.neoforge.common.ModConfigSpec
 *  net.neoforged.neoforge.common.ModConfigSpec$BooleanValue
 *  net.neoforged.neoforge.common.ModConfigSpec$Builder
 *  net.neoforged.neoforge.common.ModConfigSpec$DoubleValue
 *  net.neoforged.neoforge.common.ModConfigSpec$EnumValue
 *  net.neoforged.neoforge.common.ModConfigSpec$IntValue
 */
package dev.protomanly.pmweather.config;

import dev.protomanly.pmweather.PMWeather;
import dev.protomanly.pmweather.shaders.ModShaders;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid="pmweather", bus=EventBusSubscriber.Bus.MOD, value={Dist.CLIENT})
public class ClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    private static final ModConfigSpec.BooleanValue METRIC = BUILDER.comment("Whether to use the metric system over imperial").define("metric", false);
    public static boolean metric;
    private static final ModConfigSpec.DoubleValue LEAVES_VOLUME;
    public static double leavesVolume;
    private static final ModConfigSpec.DoubleValue SIREN_VOLUME;
    public static double sirenVolume;
    private static final ModConfigSpec.BooleanValue BASE_GAME_FOG;
    public static boolean baseGameFog;
    private static final ModConfigSpec.EnumValue<ModShaders.Quality> VOLUMETRICS_QUALITY;
    public static ModShaders.Quality volumetricsQuality;
    private static final ModConfigSpec.DoubleValue VOLUMETRICS_DOWNSAMPLE;
    public static double volumetricsDownsample;
    private static final ModConfigSpec.BooleanValue GLOW_FIX;
    public static boolean glowFix;
    private static final ModConfigSpec.BooleanValue SIMPLE_LIGHTING;
    public static boolean simpleLighting;
    private static final ModConfigSpec.BooleanValue VOLUMETRICS_BLUR;
    public static boolean volumetricsBlur;
    public static int stormParticleSpawnDelay;
    public static int cloudParticleSpawnDelay;
    private static final ModConfigSpec.IntValue MAX_PARTICLE_SPAWN_DISTANCE_FROM_PLAYER;
    public static int maxParticleSpawnDistanceFromPlayer;
    public static double tornadoParticleDensity;
    private static final ModConfigSpec.DoubleValue DEBRIS_PARTICLE_DENSITY;
    public static double debrisParticleDensity;
    private static final ModConfigSpec.BooleanValue CUSTOM_PARTICLES;
    public static boolean customParticles;
    private static final ModConfigSpec.IntValue RADAR_RESOLUTION;
    public static int radarResolution;
    private static final ModConfigSpec.BooleanValue RADAR_DEBUGGING;
    public static boolean radarDebugging;
    private static final ModConfigSpec.EnumValue<RadarMode> RADAR_MODE;
    public static RadarMode radarMode;
    public static final ModConfigSpec SPEC;

    @SubscribeEvent
    private static void onLoad(ModConfigEvent event) {
        if (event.getConfig().getSpec() == SPEC && !(event instanceof ModConfigEvent.Unloading)) {
            PMWeather.LOGGER.info("Loading Client PMWeather Configs");
            glowFix = (Boolean)GLOW_FIX.get();
            simpleLighting = (Boolean)SIMPLE_LIGHTING.get();
            volumetricsBlur = (Boolean)VOLUMETRICS_BLUR.get();
            volumetricsDownsample = (Double)VOLUMETRICS_DOWNSAMPLE.get();
            volumetricsQuality = (ModShaders.Quality)((Object)VOLUMETRICS_QUALITY.get());
            metric = (Boolean)METRIC.get();
            radarDebugging = (Boolean)RADAR_DEBUGGING.get();
            radarMode = (RadarMode)((Object)RADAR_MODE.get());
            radarResolution = (Integer)RADAR_RESOLUTION.get();
            leavesVolume = (Double)LEAVES_VOLUME.get();
            sirenVolume = (Double)SIREN_VOLUME.get();
            maxParticleSpawnDistanceFromPlayer = (Integer)MAX_PARTICLE_SPAWN_DISTANCE_FROM_PLAYER.get();
            customParticles = (Boolean)CUSTOM_PARTICLES.get();
            debrisParticleDensity = (Double)DEBRIS_PARTICLE_DENSITY.get();
            baseGameFog = (Boolean)BASE_GAME_FOG.get();
        }
    }

    static {
        LEAVES_VOLUME = BUILDER.comment("Volume of leaves in wind").defineInRange("leavesvolume", 0.0, 0.0, 1.0);
        SIREN_VOLUME = BUILDER.comment("Volume of tornado sirens").defineInRange("sirenvolume", 0.75, 0.0, 1.0);
        BASE_GAME_FOG = BUILDER.comment("Whether the mod will disable Minecraft's base game fog system.").define("basegamefog", true);
        VOLUMETRICS_QUALITY = BUILDER.comment("Quality of volumetric clouds").defineEnum("volumetricsquality", (Enum)ModShaders.Quality.MEDIUM);
        VOLUMETRICS_DOWNSAMPLE = BUILDER.comment("Render scale of volumetric clouds (Causes artifacting but greatly improves performance)").defineInRange("volumetricsdownsample", 2.5, 1.0, 4.0);
        GLOW_FIX = BUILDER.comment("Whether the mod will attempt to fix bleeding when downsampled").define("glowfix", true);
        SIMPLE_LIGHTING = BUILDER.comment("Whether the sun will cast light on clouds, turning this off will improved performance at the cost of visuals").define("simplelighting", true);
        VOLUMETRICS_BLUR = BUILDER.comment("Whether the mod will blur the output of the volumetrics shader").define("volumetricsblur", true);
        stormParticleSpawnDelay = 2;
        cloudParticleSpawnDelay = 3;
        MAX_PARTICLE_SPAWN_DISTANCE_FROM_PLAYER = BUILDER.comment("Max distance particles will spawn from the player").defineInRange("maxparticlespawndistancefromplayer", 1024, 256, 2048);
        tornadoParticleDensity = 0.35;
        DEBRIS_PARTICLE_DENSITY = BUILDER.comment("Density of debris particles, lower values are more performant").defineInRange("debrisparticledensity", 0.35, 0.0, 1.0);
        CUSTOM_PARTICLES = BUILDER.comment("Whether the mod will use it's built in particle system over vanilla minecraft's").define("customparticles", true);
        RADAR_RESOLUTION = BUILDER.comment("Radar resolution (will be double this number in game)").defineInRange("radarresolution", 50, 1, 100);
        RADAR_DEBUGGING = BUILDER.comment("Whether to use radar debugging").define("radardebugging", false);
        RADAR_MODE = BUILDER.comment("Radar mode when radar debugging is enabled").defineEnum("radarmode", (Enum)RadarMode.TEMPERATURE);
        SPEC = BUILDER.build();
    }

    public static enum RadarMode {
        TEMPERATURE,
        WINDFIELDS,
        CAPE,
        CAPE3KM,
        CINH,
        LAPSERATE03,
        LAPSERATE36;

    }
}

