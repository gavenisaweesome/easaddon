/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.chat.Component
 *  net.minecraft.world.InteractionResult
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.BaseEntityBlock
 *  net.minecraft.world.level.block.RenderShape
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.entity.BlockEntityTicker
 *  net.minecraft.world.level.block.entity.BlockEntityType
 *  net.minecraft.world.level.block.state.BlockBehaviour$Properties
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraft.world.phys.Vec3
 *  net.minecraft.world.phys.shapes.CollisionContext
 *  net.minecraft.world.phys.shapes.VoxelShape
 *  org.jetbrains.annotations.Nullable
 */
package dev.protomanly.pmweather.block;

import com.mojang.serialization.MapCodec;
import dev.protomanly.pmweather.block.entity.AnemometerBlockEntity;
import dev.protomanly.pmweather.block.entity.ModBlockEntities;
import dev.protomanly.pmweather.config.ClientConfig;
import dev.protomanly.pmweather.weather.WindEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class AnemometerBlock
extends BaseEntityBlock {
    public static final MapCodec<AnemometerBlock> CODEC = AnemometerBlock.simpleCodec(AnemometerBlock::new);
    public static final VoxelShape SHAPE = AnemometerBlock.box((double)6.0, (double)0.0, (double)6.0, (double)10.0, (double)14.0, (double)10.0);

    protected AnemometerBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            Vec3 wind = WindEngine.getWind(pos, level);
            double windspeed = wind.length();
            if (ClientConfig.metric) {
                player.sendSystemMessage((Component)Component.literal((String)String.format("%s km/h", (int)Math.floor(windspeed * 1.609))));
            } else {
                player.sendSystemMessage((Component)Component.literal((String)String.format("%s MPH", (int)Math.floor(windspeed))));
            }
        }
        return InteractionResult.SUCCESS_NO_ITEM_USED;
    }

    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new AnemometerBlockEntity(blockPos, blockState);
    }

    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return AnemometerBlock.createTickerHelper(blockEntityType, ModBlockEntities.ANEMOMETER_BE.get(), (level1, blockPos, blockState, blockEntity) -> blockEntity.tick(level1, blockPos, blockState));
    }
}

