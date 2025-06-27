/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.util.RandomSource
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.state.BlockBehaviour$Properties
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.StateDefinition$Builder
 *  net.minecraft.world.level.block.state.properties.BooleanProperty
 *  net.minecraft.world.level.block.state.properties.Property
 */
package dev.protomanly.pmweather.multiblock;

import dev.protomanly.pmweather.PMWeather;
import dev.protomanly.pmweather.multiblock.MultiBlockHandler;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;

public class MultiBlock
extends Block {
    public static BooleanProperty COMPLETED = BooleanProperty.create((String)"completed");

    public MultiBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState((BlockState)this.defaultBlockState().setValue((Property)COMPLETED, (Comparable)Boolean.valueOf(false)));
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(new Property[]{COMPLETED});
    }

    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.tick(state, level, pos, random);
        boolean completed = true;
        for (BlockPos blockPos : this.getStructure().keySet()) {
            Block goal = this.getStructure().get(blockPos);
            if (level.getBlockState(pos.offset((Vec3i)blockPos)).is(goal)) continue;
            completed = false;
        }
        if (completed != this.isComplete(state)) {
            level.setBlockAndUpdate(pos, (BlockState)state.setValue((Property)COMPLETED, (Comparable)Boolean.valueOf(completed)));
            MultiBlockHandler.isDirty = true;
            if (completed) {
                PMWeather.LOGGER.debug("MultiBlock structure {} marked complete", (Object)state);
            } else {
                PMWeather.LOGGER.debug("MultiBlock structure {} marked dismantled", (Object)state);
            }
            this.completionChanged(completed, (Level)level, state, pos);
        }
    }

    public void completionChanged(boolean newValue, Level level, BlockState blockState, BlockPos pos) {
    }

    public boolean isComplete(BlockState blockState) {
        return (Boolean)blockState.getValue((Property)COMPLETED);
    }

    public Map<BlockPos, Block> getStructure() {
        return new HashMap<BlockPos, Block>();
    }
}

