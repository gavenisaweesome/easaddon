/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.util.RandomSource
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.state.BlockBehaviour$Properties
 *  net.minecraft.world.level.block.state.BlockState
 */
package dev.protomanly.pmweather.block;

import dev.protomanly.pmweather.PMWeather;
import dev.protomanly.pmweather.block.ModBlocks;
import dev.protomanly.pmweather.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class HeavyScourBlock
extends Block {
    public HeavyScourBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.randomTick(state, level, pos, random);
        Util.checkLogs(state, level, pos, random.nextInt(4) - 1);
        for (int i = 0; i < 4; ++i) {
            BlockPos randomSample = pos.offset(PMWeather.RANDOM.nextInt(-2, 3), PMWeather.RANDOM.nextInt(-2, 3), PMWeather.RANDOM.nextInt(-2, 3));
            BlockState state1 = level.getBlockState(randomSample);
            if (!state1.is(Blocks.GRASS_BLOCK) && !state1.is((Block)ModBlocks.SCOURED_GRASS.get())) continue;
            level.setBlockAndUpdate(pos, ((Block)ModBlocks.SCOURED_GRASS.get()).defaultBlockState());
        }
    }
}

