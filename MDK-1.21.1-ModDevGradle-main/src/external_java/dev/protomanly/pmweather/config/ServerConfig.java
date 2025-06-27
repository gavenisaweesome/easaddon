/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.tags.TagKey
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Block
 *  net.neoforged.bus.api.SubscribeEvent
 *  net.neoforged.fml.common.EventBusSubscriber
 *  net.neoforged.fml.common.EventBusSubscriber$Bus
 *  net.neoforged.fml.event.config.ModConfigEvent
 *  net.neoforged.fml.event.config.ModConfigEvent$Unloading
 *  net.neoforged.neoforge.common.ModConfigSpec
 *  net.neoforged.neoforge.common.ModConfigSpec$BooleanValue
 *  net.neoforged.neoforge.common.ModConfigSpec$Builder
 *  net.neoforged.neoforge.common.ModConfigSpec$ConfigValue
 *  net.neoforged.neoforge.common.ModConfigSpec$DoubleValue
 *  net.neoforged.neoforge.common.ModConfigSpec$IntValue
 */
package dev.protomanly.pmweather.config;

import dev.protomanly.pmweather.PMWeather;
import dev.protomanly.pmweather.util.Util;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid="pmweather", bus=EventBusSubscriber.Bus.MOD)
public class ServerConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    private static final ModConfigSpec.BooleanValue REQUIRE_WSR88D = BUILDER.comment("Whether radar blocks require a completed WSR-88D to be nearby for display").define("requirewsr88d", false);
    public static boolean requireWSR88D;
    private static final ModConfigSpec.IntValue SPAWN_RANGE;
    public static int spawnRange;
    private static final ModConfigSpec.IntValue CHANCE_IN_ONE_SQUALL;
    public static int chanceInOneSquall;
    private static final ModConfigSpec.IntValue CHANCE_IN_ONE_STAGE_1;
    public static int chanceInOneStage1;
    private static final ModConfigSpec.IntValue CHANCE_IN_ONE_STAGE_2;
    public static int chanceInOneStage2;
    private static final ModConfigSpec.IntValue CHANCE_IN_ONE_STAGE_3;
    public static int chanceInOneStage3;
    private static final ModConfigSpec.DoubleValue RISK_CURVE;
    public static double riskCurve;
    private static final ModConfigSpec.DoubleValue SQUALL_STRENGTH_MULTIPLIER;
    public static double squallStrengthMultiplier;
    private static final ModConfigSpec.DoubleValue STORM_SPAWN_CHANCE_PER_MINUTE;
    public static double stormSpawnChancePerMinute;
    private static final ModConfigSpec.BooleanValue ENVIRONMENT_SYSTEM;
    public static boolean environmentSystem;
    private static final ModConfigSpec.IntValue MAX_STORMS;
    public static int maxStorms;
    private static final ModConfigSpec.IntValue SNOW_ACCUMULATION_HEIGHT;
    public static int snowAccumulationHeight;
    public static int maxClouds;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> BLOCK_STENGTHS;
    public static Map<Block, Float> blockStrengths;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> BLACKLISTED_BLOCKS;
    public static List<Block> blacklistedBlocks;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> BLACKLISTED_BLOCKTAGS;
    public static List<TagKey<Block>> blacklistedBlockTags;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> VALID_DIMENSIONS;
    public static List<ResourceKey<Level>> validDimensions;
    private static final ModConfigSpec.DoubleValue STORM_SIZE;
    public static double stormSize;
    private static final ModConfigSpec.DoubleValue OVERCAST_PERCENT;
    public static double overcastPercent;
    private static final ModConfigSpec.DoubleValue RAIN_STRENGTH;
    public static double rainStrength;
    private static final ModConfigSpec.DoubleValue LAYER_0_HEIGHT;
    public static double layer0Height;
    private static final ModConfigSpec.DoubleValue LAYER_C_HEIGHT;
    public static double layerCHeight;
    private static final ModConfigSpec.IntValue MAX_TORNADO_WIDTH;
    public static double maxTornadoWidth;
    private static final ModConfigSpec.BooleanValue AIM_AT_PLAYER;
    public static boolean aimAtPlayer;
    private static final ModConfigSpec.DoubleValue AIM_AT_PLAYER_OFFSET;
    public static double aimAtPlayerOffset;
    private static final ModConfigSpec.IntValue MAX_BLOCKS_DAMAGED_PER_TICK;
    public static int maxBlocksDamagedPerTick;
    private static final ModConfigSpec.BooleanValue DO_DEBARKING;
    public static boolean doDebarking;
    public static final ModConfigSpec SPEC;

    @SubscribeEvent
    private static void onLoad(ModConfigEvent event) {
        if (event.getConfig().getSpec() == SPEC && !(event instanceof ModConfigEvent.Unloading)) {
            Block block;
            ResourceLocation resourceLocation;
            PMWeather.LOGGER.info("Loading Server PMWeather Configs");
            requireWSR88D = (Boolean)REQUIRE_WSR88D.get();
            chanceInOneSquall = (Integer)CHANCE_IN_ONE_SQUALL.get();
            chanceInOneStage1 = (Integer)CHANCE_IN_ONE_STAGE_1.get();
            chanceInOneStage2 = (Integer)CHANCE_IN_ONE_STAGE_2.get();
            chanceInOneStage3 = (Integer)CHANCE_IN_ONE_STAGE_3.get();
            environmentSystem = (Boolean)ENVIRONMENT_SYSTEM.get();
            aimAtPlayer = (Boolean)AIM_AT_PLAYER.get();
            aimAtPlayerOffset = (Double)AIM_AT_PLAYER_OFFSET.get();
            spawnRange = (Integer)SPAWN_RANGE.get();
            stormSpawnChancePerMinute = (Double)STORM_SPAWN_CHANCE_PER_MINUTE.get();
            maxStorms = (Integer)MAX_STORMS.get();
            overcastPercent = (Double)OVERCAST_PERCENT.get();
            rainStrength = (Double)RAIN_STRENGTH.get();
            riskCurve = (Double)RISK_CURVE.get();
            squallStrengthMultiplier = (Double)SQUALL_STRENGTH_MULTIPLIER.get();
            maxTornadoWidth = ((Integer)MAX_TORNADO_WIDTH.get()).intValue();
            maxBlocksDamagedPerTick = (Integer)MAX_BLOCKS_DAMAGED_PER_TICK.get();
            doDebarking = (Boolean)DO_DEBARKING.get();
            blockStrengths = new HashMap<Block, Float>();
            List bstrngths = (List)BLOCK_STENGTHS.get();
            for (Object string : bstrngths) {
                String[] args = ((String)string).split("=");
                if (args.length == 2) {
                    resourceLocation = ResourceLocation.parse((String)args[0]);
                    if (BuiltInRegistries.BLOCK.containsKey(resourceLocation) && Util.isInteger(args[1])) {
                        block = (Block)BuiltInRegistries.BLOCK.get(resourceLocation);
                        int strength = Integer.parseInt(args[1]);
                        PMWeather.LOGGER.debug("Setup Block {} with strength {} mph", (Object)block, (Object)strength);
                        blockStrengths.put(block, Float.valueOf(strength));
                        continue;
                    }
                    PMWeather.LOGGER.warn("Invalid blockstrengths config: {}", string);
                    continue;
                }
                PMWeather.LOGGER.warn("Invalid blockstrengths config: {}", string);
            }
            blacklistedBlocks = new ArrayList<Block>();
            List blbs = (List)BLACKLISTED_BLOCKS.get();
            for (Object string : blbs) {
                resourceLocation = ResourceLocation.parse((String)string);
                if (BuiltInRegistries.BLOCK.containsKey(resourceLocation)) {
                    block = (Block)BuiltInRegistries.BLOCK.get(resourceLocation);
                    PMWeather.LOGGER.debug("Inserted Block {}", (Object)block);
                    blacklistedBlocks.add(block);
                    continue;
                }
                PMWeather.LOGGER.warn("Invalid block within config blacklistedblocks: {}", string);
            }
            blacklistedBlockTags = new ArrayList<TagKey<Block>>();
            List blbts = (List)BLACKLISTED_BLOCKTAGS.get();
            for (String string : blbts) {
                ResourceLocation resourceLocation2 = ResourceLocation.parse((String)string);
                TagKey tagKey = TagKey.create((ResourceKey)Registries.BLOCK, (ResourceLocation)resourceLocation2);
                PMWeather.LOGGER.debug("Inserted BlockTag {}", (Object)tagKey);
                blacklistedBlockTags.add((TagKey<Block>)tagKey);
            }
            validDimensions = new ArrayList<ResourceKey<Level>>();
            List vds = (List)VALID_DIMENSIONS.get();
            for (String string : vds) {
                ResourceLocation resourceLocation3 = ResourceLocation.parse((String)string);
                ResourceKey dimension = ResourceKey.create((ResourceKey)Registries.DIMENSION, (ResourceLocation)resourceLocation3);
                validDimensions.add((ResourceKey<Level>)dimension);
            }
            stormSize = (Double)STORM_SIZE.get();
            layer0Height = (Double)LAYER_0_HEIGHT.get();
            layerCHeight = (Double)LAYER_C_HEIGHT.get();
            snowAccumulationHeight = (Integer)SNOW_ACCUMULATION_HEIGHT.get();
        }
    }

    static {
        SPAWN_RANGE = BUILDER.comment("Range within which clouds and storms will spawn from players").defineInRange("spawnrange", 384, 256, 2048);
        CHANCE_IN_ONE_SQUALL = BUILDER.comment("1 in x chance that a spawning storm will spawn as a squall").defineInRange("chanceinonesquall", 10, 1, 100);
        CHANCE_IN_ONE_STAGE_1 = BUILDER.comment("1 in x chance that a storm will progress to stage 1").defineInRange("chanceinonestage1", 2, 1, 100);
        CHANCE_IN_ONE_STAGE_2 = BUILDER.comment("1 in x chance that a storm will progress to stage 2").defineInRange("chanceinonestage2", 3, 1, 100);
        CHANCE_IN_ONE_STAGE_3 = BUILDER.comment("1 in x chance that a storm will progress to stage 3 / tornado").defineInRange("chanceinonestage3", 5, 1, 100);
        RISK_CURVE = BUILDER.comment("Risk curve, higher is rarer").defineInRange("riskcurve", 1.0, 0.5, 2.0);
        SQUALL_STRENGTH_MULTIPLIER = BUILDER.comment("Multiplier of squall windspeeds").defineInRange("squallstrengthmultiplier", 1.25, 0.0, 2.0);
        STORM_SPAWN_CHANCE_PER_MINUTE = BUILDER.comment("Chance a storm will spawn each minute").defineInRange("stormspawnchanceperminute", 0.2, 0.0, 1.0);
        ENVIRONMENT_SYSTEM = BUILDER.comment("Whether chance to spawn storms and chance for storms to progress is affected by the game environment").define("environmentsystem", true);
        MAX_STORMS = BUILDER.comment("Maximum number of active storms allowed to spawn").defineInRange("maxstorms", 5, 1, 10);
        SNOW_ACCUMULATION_HEIGHT = BUILDER.comment("Maximum precipitation accumulation, 0 = off").defineInRange("snowaccumulationheight", 6, 0, 8);
        maxClouds = 0;
        BLOCK_STENGTHS = BUILDER.comment("List of blocks and respective windspeed at which they get damaged").defineListAllowEmpty("blockstrengths", () -> new ArrayList<String>(){
            {
                this.add("minecraft:acacia_leaves=55");
                this.add("minecraft:azalea_leaves=55");
                this.add("minecraft:birch_leaves=50");
                this.add("minecraft:dark_oak_leaves=55");
                this.add("minecraft:cherry_leaves=55");
                this.add("minecraft:flowering_azalea_leaves=55");
                this.add("minecraft:mangrove_leaves=65");
                this.add("minecraft:oak_leaves=55");
                this.add("minecraft:jungle_leaves=55");
                this.add("minecraft:chain=75");
                this.add("minecraft:lantern=75");
                this.add("minecraft:soul_lantern=75");
                this.add("minecraft:white_wool=60");
                this.add("minecraft:orange_wool=60");
                this.add("minecraft:magenta_wool=60");
                this.add("minecraft:light_blue_wool=60");
                this.add("minecraft:yellow_wool=60");
                this.add("minecraft:lime_wool=60");
                this.add("minecraft:pink_wool=60");
                this.add("minecraft:gray_wool=60");
                this.add("minecraft:light_gray_wool=60");
                this.add("minecraft:cyan_wool=60");
                this.add("minecraft:purple_wool=60");
                this.add("minecraft:blue_wool=60");
                this.add("minecraft:brown_wool=60");
                this.add("minecraft:green_wool=60");
                this.add("minecraft:red_wool=60");
                this.add("minecraft:black_wool=60");
            }
        }, () -> "pmweather", e -> {
            String string;
            return e instanceof String && (string = (String)e).contains("=") && string.split("=").length == 2 && Objects.nonNull(ResourceLocation.tryParse((String)string.split("=")[0])) && BuiltInRegistries.BLOCK.containsKey(ResourceLocation.parse((String)string.split("=")[0])) && Util.isInteger(string.split("=")[1]);
        });
        BLACKLISTED_BLOCKS = BUILDER.comment("List of blocks not allowed to be damaged").defineListAllowEmpty("blacklistedblocks", () -> new ArrayList<String>(){
            {
                this.add("minecraft:gravel");
                this.add("minecraft:farmland");
                this.add("minecraft:dirt_path");
            }
        }, () -> "pmweather", e -> {
            String string;
            return e instanceof String && Objects.nonNull(ResourceLocation.tryParse((String)(string = (String)e))) && BuiltInRegistries.BLOCK.containsKey(ResourceLocation.parse((String)string));
        });
        BLACKLISTED_BLOCKTAGS = BUILDER.comment("List of blocktags not allowed to be damaged").defineListAllowEmpty("blacklistedblocktags", () -> new ArrayList<String>(){
            {
                this.add("minecraft:dirt");
                this.add("minecraft:base_stone_overworld");
                this.add("minecraft:terracotta");
                this.add("minecraft:badlands_terracotta");
                this.add("minecraft:ice");
                this.add("minecraft:sand");
            }
        }, () -> "pmweather", e -> {
            if (e instanceof String) {
                String string = (String)e;
                ResourceLocation path = ResourceLocation.tryParse((String)string);
                if (Objects.isNull(path)) {
                    return false;
                }
                TagKey tagKey = TagKey.create((ResourceKey)Registries.BLOCK, (ResourceLocation)path);
                BuiltInRegistries.BLOCK.getTagOrEmpty(tagKey);
                return BuiltInRegistries.BLOCK.getTagOrEmpty(tagKey).iterator().hasNext();
            }
            return false;
        });
        VALID_DIMENSIONS = BUILDER.comment("List of valid dimensions for spawning weather").defineListAllowEmpty("validdimensions", () -> new ArrayList<String>(){
            {
                this.add("minecraft:overworld");
            }
        }, () -> "pmweather", e -> e instanceof String);
        STORM_SIZE = BUILDER.comment("Size of storms").defineInRange("stormsize", 300.0, 128.0, 512.0);
        OVERCAST_PERCENT = BUILDER.comment("Overcast Modifier").defineInRange("overcastpercent", 0.75, 0.0, 2.0);
        RAIN_STRENGTH = BUILDER.comment("Rain Modifier").defineInRange("rainstrength", 0.8, 0.0, 2.0);
        LAYER_0_HEIGHT = BUILDER.comment("Height of first cloud layer").defineInRange("layer0height", 315.0, 150.0, 450.0);
        LAYER_C_HEIGHT = BUILDER.comment("Height of cirrus cloud layer").defineInRange("layerCheight", 2000.0, 1000.0, 3000.0);
        MAX_TORNADO_WIDTH = BUILDER.comment("Maximum width of tornadoes").defineInRange("maxtornadowidth", 225, 100, 800);
        AIM_AT_PLAYER = BUILDER.comment("Whether storms will aim at the player whenever strengthening into a tornado").define("aimatplayer", false);
        AIM_AT_PLAYER_OFFSET = BUILDER.comment("Random range of blocks that storms will aim at around players").defineInRange("aimatplayeroffset", 248.0, 0.0, 1024.0);
        MAX_BLOCKS_DAMAGED_PER_TICK = BUILDER.comment("Maximum number of blocks allowed to be damaged by a tornado per tick").defineInRange("maxblocksdamagedpertick", 12500, 5000, 40000);
        DO_DEBARKING = BUILDER.comment("Whether debarking will be applied to logs").define("dodebarking", true);
        SPEC = BUILDER.build();
    }
}

