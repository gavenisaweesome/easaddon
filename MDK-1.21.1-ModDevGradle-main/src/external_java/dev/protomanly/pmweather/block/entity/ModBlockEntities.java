/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.Registry
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.entity.BlockEntityType
 *  net.minecraft.world.level.block.entity.BlockEntityType$Builder
 *  net.neoforged.neoforge.registries.DeferredRegister
 */
package dev.protomanly.pmweather.block.entity;

import dev.protomanly.pmweather.block.ModBlocks;
import dev.protomanly.pmweather.block.entity.AnemometerBlockEntity;
import dev.protomanly.pmweather.block.entity.RadarBlockEntity;
import dev.protomanly.pmweather.block.entity.SoundingViewerBlockEntity;
import dev.protomanly.pmweather.block.entity.TornadoSensorBlockEntity;
import dev.protomanly.pmweather.block.entity.TornadoSirenBlockEntity;
import dev.protomanly.pmweather.block.entity.WeatherPlatformBlockEntity;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create((Registry)BuiltInRegistries.BLOCK_ENTITY_TYPE, (String)"pmweather");
    public static final Supplier<BlockEntityType<AnemometerBlockEntity>> ANEMOMETER_BE = BLOCK_ENTITIES.register("anemometer_be", () -> BlockEntityType.Builder.of(AnemometerBlockEntity::new, (Block[])new Block[]{(Block)ModBlocks.ANEMOMETER.get()}).build(null));
    public static final Supplier<BlockEntityType<RadarBlockEntity>> RADAR_BE = BLOCK_ENTITIES.register("radar_be", () -> BlockEntityType.Builder.of(RadarBlockEntity::new, (Block[])new Block[]{(Block)ModBlocks.RADAR.get()}).build(null));
    public static final Supplier<BlockEntityType<TornadoSensorBlockEntity>> TORNADO_SENSOR_BE = BLOCK_ENTITIES.register("tornado_sensor_be", () -> BlockEntityType.Builder.of(TornadoSensorBlockEntity::new, (Block[])new Block[]{(Block)ModBlocks.TORNADO_SENSOR.get()}).build(null));
    public static final Supplier<BlockEntityType<TornadoSirenBlockEntity>> TORNADO_SIREN_BE = BLOCK_ENTITIES.register("tornado_siren_be", () -> BlockEntityType.Builder.of(TornadoSirenBlockEntity::new, (Block[])new Block[]{(Block)ModBlocks.TORNADO_SIREN.get()}).build(null));
    public static final Supplier<BlockEntityType<WeatherPlatformBlockEntity>> WEATHER_PLATFORM_BE = BLOCK_ENTITIES.register("weather_platform_be", () -> BlockEntityType.Builder.of(WeatherPlatformBlockEntity::new, (Block[])new Block[]{(Block)ModBlocks.WEATHER_PLATFORM.get()}).build(null));
    public static final Supplier<BlockEntityType<SoundingViewerBlockEntity>> SOUNDING_VIEWER_BE = BLOCK_ENTITIES.register("sounding_viewer_be", () -> BlockEntityType.Builder.of(SoundingViewerBlockEntity::new, (Block[])new Block[]{(Block)ModBlocks.SOUNDING_VIEWER.get()}).build(null));
}

