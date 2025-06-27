/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.chat.Component
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.ItemInteractionResult
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.BaseEntityBlock
 *  net.minecraft.world.level.block.RenderShape
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.entity.BlockEntityTicker
 *  net.minecraft.world.level.block.entity.BlockEntityType
 *  net.minecraft.world.level.block.state.BlockBehaviour$Properties
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.BlockHitResult
 *  org.jetbrains.annotations.Nullable
 */
package dev.protomanly.pmweather.block;

import com.mojang.serialization.MapCodec;
import dev.protomanly.pmweather.block.entity.ModBlockEntities;
import dev.protomanly.pmweather.block.entity.WeatherPlatformBlockEntity;
import dev.protomanly.pmweather.item.ModItems;
import dev.protomanly.pmweather.item.component.ModComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class WeatherPlatformBlock
extends BaseEntityBlock {
    public static final MapCodec<WeatherPlatformBlock> CODEC = WeatherPlatformBlock.simpleCodec(WeatherPlatformBlock::new);

    protected WeatherPlatformBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.is((Item)ModItems.CONNECTOR.get())) {
            if (!level.isClientSide()) {
                stack.set(ModComponents.WEATHER_BALLOON_PLATFORM, (Object)pos);
                player.sendSystemMessage((Component)Component.literal((String)String.format("Connector is configured to %s, %s, %s", pos.getX(), pos.getY(), pos.getZ())));
            }
            return ItemInteractionResult.SUCCESS;
        }
        if (stack.is((Item)ModItems.WEATHER_BALLOON.get())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof WeatherPlatformBlockEntity) {
                WeatherPlatformBlockEntity weatherPlatformBlockEntity = (WeatherPlatformBlockEntity)blockEntity;
                if (!weatherPlatformBlockEntity.active && level.canSeeSky(pos.above())) {
                    if (!level.isClientSide()) {
                        stack.consume(1, (LivingEntity)player);
                        weatherPlatformBlockEntity.activate(level, pos, state);
                    }
                    return ItemInteractionResult.SUCCESS;
                }
            }
            if (!level.canSeeSky(pos.above()) && !level.isClientSide()) {
                player.sendSystemMessage((Component)Component.literal((String)"Platform cannot see sky!"));
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new WeatherPlatformBlockEntity(blockPos, blockState);
    }

    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return WeatherPlatformBlock.createTickerHelper(blockEntityType, ModBlockEntities.WEATHER_PLATFORM_BE.get(), (level1, blockPos, blockState, blockEntity) -> blockEntity.tick(level1, blockPos, blockState));
    }
}

