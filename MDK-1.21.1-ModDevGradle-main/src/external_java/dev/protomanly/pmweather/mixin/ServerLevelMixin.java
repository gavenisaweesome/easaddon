/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.LevelAccessor
 *  net.minecraft.world.level.LevelReader
 *  net.minecraft.world.level.LightLayer
 *  net.minecraft.world.level.biome.Biome$Precipitation
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.LiquidBlock
 *  net.minecraft.world.level.block.SnowLayerBlock
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.level.levelgen.Heightmap$Types
 *  net.minecraft.world.level.material.FluidState
 *  net.minecraft.world.level.material.Fluids
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package dev.protomanly.pmweather.mixin;

import dev.protomanly.pmweather.PMWeather;
import dev.protomanly.pmweather.block.ModBlocks;
import dev.protomanly.pmweather.config.ServerConfig;
import dev.protomanly.pmweather.event.GameBusEvents;
import dev.protomanly.pmweather.weather.ThermodynamicEngine;
import dev.protomanly.pmweather.weather.WeatherHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={ServerLevel.class})
public class ServerLevelMixin {
    public boolean shouldFreeze(ServerLevel level, BlockPos water, boolean mustBeAtEdge) {
        if (water.getY() >= level.getMinBuildHeight() && water.getY() < level.getMaxBuildHeight() && level.getBrightness(LightLayer.BLOCK, water) < 10) {
            BlockState blockstate = level.getBlockState(water);
            FluidState fluidstate = level.getFluidState(water);
            if (fluidstate.getType() == Fluids.WATER && blockstate.getBlock() instanceof LiquidBlock) {
                boolean flag;
                if (!mustBeAtEdge) {
                    return true;
                }
                boolean bl = flag = level.isWaterAt(water.west()) && level.isWaterAt(water.east()) && level.isWaterAt(water.north()) && level.isWaterAt(water.south());
                if (!flag) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean shouldSnow(ServerLevel level, BlockPos pos) {
        BlockState blockstate;
        return pos.getY() >= level.getMinBuildHeight() && pos.getY() < level.getMaxBuildHeight() && ((blockstate = level.getBlockState(pos)).isAir() || blockstate.is(Blocks.SNOW)) && Blocks.SNOW.defaultBlockState().canSurvive((LevelReader)level, pos);
    }

    public boolean shouldIce(ServerLevel level, BlockPos pos) {
        BlockState blockstate;
        return pos.getY() >= level.getMinBuildHeight() && pos.getY() < level.getMaxBuildHeight() && ((blockstate = level.getBlockState(pos)).isAir() || blockstate.is((Block)ModBlocks.ICE_LAYER.get())) && ((Block)ModBlocks.ICE_LAYER.get()).defaultBlockState().canSurvive((LevelReader)level, pos);
    }

    public boolean shouldSleet(ServerLevel level, BlockPos pos) {
        BlockState blockstate;
        return pos.getY() >= level.getMinBuildHeight() && pos.getY() < level.getMaxBuildHeight() && ((blockstate = level.getBlockState(pos)).isAir() || blockstate.is((Block)ModBlocks.SLEET_LAYER.get())) && ((Block)ModBlocks.SLEET_LAYER.get()).defaultBlockState().canSurvive((LevelReader)level, pos);
    }

    @Inject(method={"tickPrecipitation"}, at={@At(value="HEAD")}, cancellable=true)
    public void editTickPrecipitation(BlockPos blockPos, CallbackInfo callbackInfo) {
        ServerLevel level = (ServerLevel)this;
        BlockPos blockpos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, blockPos);
        BlockPos blockpos1 = blockpos.below();
        WeatherHandler weatherHandler = GameBusEvents.MANAGERS.get(level.dimension());
        ThermodynamicEngine.AtmosphericDataPoint dataPoint = ThermodynamicEngine.samplePoint(weatherHandler, blockpos.getCenter(), (Level)level, null, 0);
        float rain = weatherHandler.getPrecipitation(blockpos.getCenter());
        if (level.isAreaLoaded(blockpos1, 1) && dataPoint.temperature() <= 0.0f && this.shouldFreeze(level, blockpos1, true)) {
            level.setBlockAndUpdate(blockpos1, Blocks.ICE.defaultBlockState());
        }
        BlockState blockstate = level.getBlockState(blockpos);
        int c = 20;
        if (dataPoint.temperature() > 2.0f) {
            c = 10;
        }
        if (dataPoint.temperature() > 4.0f) {
            c = 4;
        }
        if (dataPoint.temperature() > 6.0f) {
            c = 1;
        }
        if ((blockstate.is(Blocks.SNOW) || blockstate.is((Block)ModBlocks.SLEET_LAYER.get()) || blockstate.is((Block)ModBlocks.ICE_LAYER.get())) && dataPoint.temperature() > 0.0f && PMWeather.RANDOM.nextInt(c) == 0) {
            int j = (Integer)blockstate.getValue((Property)SnowLayerBlock.LAYERS);
            if (j > 1) {
                BlockState blockstate1 = (BlockState)blockstate.setValue((Property)SnowLayerBlock.LAYERS, (Comparable)Integer.valueOf(j - 1));
                Block.pushEntitiesUp((BlockState)blockstate, (BlockState)blockstate1, (LevelAccessor)level, (BlockPos)blockpos);
                level.setBlockAndUpdate(blockpos, blockstate1);
            } else {
                level.removeBlock(blockpos, false);
            }
        }
        if (rain > 0.1f) {
            BlockState blockstate1;
            int j;
            BlockState blockstateW;
            int i = ServerConfig.snowAccumulationHeight;
            ThermodynamicEngine.Precipitation precip = ThermodynamicEngine.getPrecipitationType(weatherHandler, blockpos.getCenter(), (Level)level, 500);
            if (precip == ThermodynamicEngine.Precipitation.WINTRY_MIX) {
                switch (PMWeather.RANDOM.nextInt(3)) {
                    case 0: {
                        ThermodynamicEngine.Precipitation precipitation = ThermodynamicEngine.Precipitation.SNOW;
                        break;
                    }
                    case 1: {
                        ThermodynamicEngine.Precipitation precipitation = ThermodynamicEngine.Precipitation.SLEET;
                        break;
                    }
                    case 2: {
                        ThermodynamicEngine.Precipitation precipitation = ThermodynamicEngine.Precipitation.FREEZING_RAIN;
                        break;
                    }
                    default: {
                        ThermodynamicEngine.Precipitation precipitation = precip = ThermodynamicEngine.Precipitation.WINTRY_MIX;
                    }
                }
            }
            if (precip == ThermodynamicEngine.Precipitation.SNOW && i > 0 && this.shouldSnow(level, blockpos)) {
                blockstateW = level.getBlockState(blockpos);
                if (blockstateW.is(Blocks.SNOW)) {
                    j = (Integer)blockstateW.getValue((Property)SnowLayerBlock.LAYERS);
                    if (j < Math.min(i, 8)) {
                        blockstate1 = (BlockState)blockstateW.setValue((Property)SnowLayerBlock.LAYERS, (Comparable)Integer.valueOf(j + 1));
                        Block.pushEntitiesUp((BlockState)blockstateW, (BlockState)blockstate1, (LevelAccessor)level, (BlockPos)blockpos);
                        level.setBlockAndUpdate(blockpos, blockstate1);
                    }
                } else {
                    level.setBlockAndUpdate(blockpos, Blocks.SNOW.defaultBlockState());
                }
            }
            if (precip == ThermodynamicEngine.Precipitation.SLEET && i > 0 && this.shouldSleet(level, blockpos)) {
                blockstateW = level.getBlockState(blockpos);
                if (blockstateW.is((Block)ModBlocks.SLEET_LAYER.get())) {
                    j = (Integer)blockstateW.getValue((Property)SnowLayerBlock.LAYERS);
                    if (j < Math.min(i, 8)) {
                        blockstate1 = (BlockState)blockstateW.setValue((Property)SnowLayerBlock.LAYERS, (Comparable)Integer.valueOf(j + 1));
                        Block.pushEntitiesUp((BlockState)blockstateW, (BlockState)blockstate1, (LevelAccessor)level, (BlockPos)blockpos);
                        level.setBlockAndUpdate(blockpos, blockstate1);
                    }
                } else {
                    level.setBlockAndUpdate(blockpos, ((Block)ModBlocks.SLEET_LAYER.get()).defaultBlockState());
                }
            }
            if (precip == ThermodynamicEngine.Precipitation.FREEZING_RAIN && i > 0 && this.shouldIce(level, blockpos)) {
                blockstateW = level.getBlockState(blockpos);
                if (blockstateW.is((Block)ModBlocks.ICE_LAYER.get())) {
                    j = (Integer)blockstateW.getValue((Property)SnowLayerBlock.LAYERS);
                    if (j < Math.min(i, 8) && PMWeather.RANDOM.nextInt(3) == 0) {
                        blockstate1 = (BlockState)blockstateW.setValue((Property)SnowLayerBlock.LAYERS, (Comparable)Integer.valueOf(j + 1));
                        Block.pushEntitiesUp((BlockState)blockstateW, (BlockState)blockstate1, (LevelAccessor)level, (BlockPos)blockpos);
                        level.setBlockAndUpdate(blockpos, blockstate1);
                    }
                } else {
                    level.setBlockAndUpdate(blockpos, ((Block)ModBlocks.ICE_LAYER.get()).defaultBlockState());
                }
            }
            Biome.Precipitation biome$precipitation = Biome.Precipitation.NONE;
            if (rain > 0.1f) {
                biome$precipitation = Biome.Precipitation.RAIN;
                if (dataPoint.temperature() <= 0.0f) {
                    biome$precipitation = Biome.Precipitation.SNOW;
                }
            }
            if (biome$precipitation != Biome.Precipitation.NONE) {
                BlockState blockstate2 = level.getBlockState(blockpos1);
                blockstate2.getBlock().handlePrecipitation(blockstate2, (Level)level, blockpos1, biome$precipitation);
            }
        }
        callbackInfo.cancel();
    }
}

