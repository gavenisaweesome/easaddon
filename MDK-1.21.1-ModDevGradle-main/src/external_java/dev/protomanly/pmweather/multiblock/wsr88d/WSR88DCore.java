/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.world.item.context.BlockPlaceContext
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.state.BlockBehaviour$Properties
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.StateDefinition$Builder
 *  net.minecraft.world.level.block.state.properties.BlockStateProperties
 *  net.minecraft.world.level.block.state.properties.DirectionProperty
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.phys.shapes.CollisionContext
 *  net.minecraft.world.phys.shapes.Shapes
 *  net.minecraft.world.phys.shapes.VoxelShape
 *  org.jetbrains.annotations.Nullable
 */
package dev.protomanly.pmweather.multiblock.wsr88d;

import dev.protomanly.pmweather.block.ModBlocks;
import dev.protomanly.pmweather.multiblock.MultiBlock;
import dev.protomanly.pmweather.multiblock.MultiBlocks;
import dev.protomanly.pmweather.sound.ModSounds;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class WSR88DCore
extends MultiBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public WSR88DCore(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState((BlockState)this.defaultBlockState().setValue((Property)FACING, (Comparable)Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(new Property[]{FACING});
    }

    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0f;
    }

    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState blockstate = super.getStateForPlacement(context);
        if (blockstate == null) {
            blockstate = this.defaultBlockState();
        }
        return (BlockState)blockstate.setValue((Property)FACING, (Comparable)context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void completionChanged(boolean newValue, Level level, BlockState blockState, BlockPos pos) {
        super.completionChanged(newValue, level, blockState, pos);
        if (newValue) {
            level.playSound(null, pos, (SoundEvent)ModSounds.WSR88D_COMPLETED.value(), SoundSource.BLOCKS, 1.0f, 1.0f);
        } else {
            level.playSound(null, pos, (SoundEvent)ModSounds.WSR88D_DISMANTLED.value(), SoundSource.BLOCKS, 1.0f, 1.0f);
        }
    }

    @Override
    public Map<BlockPos, Block> getStructure() {
        return new HashMap<BlockPos, Block>(){
            {
                this.put(BlockPos.ZERO, (Block)MultiBlocks.WSR88D_CORE.get());
                this.put(new BlockPos(2, 0, -1), (Block)ModBlocks.RADOME.get());
                this.put(new BlockPos(2, 0, 0), (Block)ModBlocks.RADOME.get());
                this.put(new BlockPos(2, 0, 1), (Block)ModBlocks.RADOME.get());
                this.put(new BlockPos(2, 1, -1), (Block)ModBlocks.RADOME.get());
                this.put(new BlockPos(2, 1, 0), (Block)ModBlocks.RADOME.get());
                this.put(new BlockPos(2, 1, 1), (Block)ModBlocks.RADOME.get());
                this.put(new BlockPos(2, -1, -1), (Block)ModBlocks.RADOME.get());
                this.put(new BlockPos(2, -1, 0), (Block)ModBlocks.RADOME.get());
                this.put(new BlockPos(2, -1, 1), (Block)ModBlocks.RADOME.get());
                this.put(new BlockPos(-2, 0, -1), (Block)ModBlocks.RADOME.get());
                this.put(new BlockPos(-2, 0, 0), (Block)ModBlocks.RADOME.get());
                this.put(new BlockPos(-2, 0, 1), (Block)ModBlocks.RADOME.get());
                this.put(new BlockPos(-2, 1, -1), (Block)ModBlocks.RADOME.get());
                this.put(new BlockPos(-2, 1, 0), (Block)ModBlocks.RADOME.get());
                this.put(new BlockPos(-2, 1, 1), (Block)ModBlocks.RADOME.get());
                this.put(new BlockPos(-2, -1, -1), (Block)ModBlocks.RADOME.get());
                this.put(new BlockPos(-2, -1, 0), (Block)ModBlocks.RADOME.get());
                this.put(new BlockPos(-2, -1, 1), (Block)ModBlocks.RADOME.get());
                this.put(new BlockPos(-1, 0, 2), (Block)ModBlocks.RADOME.get());
                this.put(new BlockPos(0, 0, 2), (Block)ModBlocks.RADOME.get());
                this.put(new BlockPos(1, 0, 2), (Block)ModBlocks.RADOME.get());
                this.put(new BlockPos(-1, 1, 2), (Block)ModBlocks.RADOME.get());
                this.put(new BlockPos(0, 1, 2), (Block)ModBlocks.RADOME.get());
                this.put(new BlockPos(1, 1, 2), (Block)ModBlocks.RADOME.get());
                this.put(new BlockPos(-1, -1, 2), (Block)ModBlocks.RADOME.get());
                this.put(new BlockPos(0, -1, 2), (Block)ModBlocks.RADOME.get());
                this.put(new BlockPos(1, -1, 2), (Block)ModBlocks.RADOME.get());
                this.put(new BlockPos(-1, 0, -2), (Block)ModBlocks.RADOME.get());
                this.put(new BlockPos(0, 0, -2), (Block)ModBlocks.RADOME.get());
                this.put(new BlockPos(1, 0, -2), (Block)ModBlocks.RADOME.get());
                this.put(new BlockPos(-1, 1, -2), (Block)ModBlocks.RADOME.get());
                this.put(new BlockPos(0, 1, -2), (Block)ModBlocks.RADOME.get());
                this.put(new BlockPos(1, 1, -2), (Block)ModBlocks.RADOME.get());
                this.put(new BlockPos(-1, -1, -2), (Block)ModBlocks.RADOME.get());
                this.put(new BlockPos(0, -1, -2), (Block)ModBlocks.RADOME.get());
                this.put(new BlockPos(1, -1, -2), (Block)ModBlocks.RADOME.get());
                this.put(new BlockPos(-1, 2, 0), (Block)ModBlocks.RADOME.get());
                this.put(new BlockPos(0, 2, 0), (Block)ModBlocks.RADOME.get());
                this.put(new BlockPos(1, 2, 0), (Block)ModBlocks.RADOME.get());
                this.put(new BlockPos(-1, 2, 1), (Block)ModBlocks.RADOME.get());
                this.put(new BlockPos(0, 2, 1), (Block)ModBlocks.RADOME.get());
                this.put(new BlockPos(1, 2, 1), (Block)ModBlocks.RADOME.get());
                this.put(new BlockPos(-1, 2, -1), (Block)ModBlocks.RADOME.get());
                this.put(new BlockPos(0, 2, -1), (Block)ModBlocks.RADOME.get());
                this.put(new BlockPos(1, 2, -1), (Block)ModBlocks.RADOME.get());
                this.put(new BlockPos(-1, -2, 0), (Block)ModBlocks.RADOME.get());
                this.put(new BlockPos(1, -2, 0), (Block)ModBlocks.RADOME.get());
                this.put(new BlockPos(-1, -2, 1), (Block)ModBlocks.RADOME.get());
                this.put(new BlockPos(0, -2, 1), (Block)ModBlocks.RADOME.get());
                this.put(new BlockPos(1, -2, 1), (Block)ModBlocks.RADOME.get());
                this.put(new BlockPos(-1, -2, -1), (Block)ModBlocks.RADOME.get());
                this.put(new BlockPos(0, -2, -1), (Block)ModBlocks.RADOME.get());
                this.put(new BlockPos(1, -2, -1), (Block)ModBlocks.RADOME.get());
            }
        };
    }
}

