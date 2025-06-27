/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.level.LevelAccessor
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.state.BlockState
 *  net.neoforged.bus.api.SubscribeEvent
 *  net.neoforged.fml.common.EventBusSubscriber
 *  net.neoforged.fml.common.EventBusSubscriber$Bus
 *  net.neoforged.neoforge.event.level.BlockEvent$NeighborNotifyEvent
 */
package dev.protomanly.pmweather.multiblock;

import dev.protomanly.pmweather.PMWeather;
import dev.protomanly.pmweather.multiblock.MultiBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid="pmweather", bus=EventBusSubscriber.Bus.GAME)
public class MultiBlockHandler {
    public static int searchRange = 6;
    public static boolean isDirty = false;

    public static void update(BlockPos blockPos, LevelAccessor level) {
        if (level.isClientSide()) {
            return;
        }
        for (int x = -searchRange; x <= searchRange; ++x) {
            block1: for (int y = -searchRange; y <= searchRange; ++y) {
                for (int z = -searchRange; z <= searchRange; ++z) {
                    BlockState state = level.getBlockState(blockPos.offset(x, y, z));
                    Block block = state.getBlock();
                    if (!(block instanceof MultiBlock)) continue;
                    MultiBlock multiblock = (MultiBlock)block;
                    level.scheduleTick(blockPos.offset(x, y, z), (Block)multiblock, 0);
                    PMWeather.LOGGER.debug("Scheduled a tick at {} for {}", (Object)blockPos.offset(x, y, z), (Object)state);
                    continue block1;
                }
            }
        }
    }

    @SubscribeEvent
    public static void onUpdate(BlockEvent.NeighborNotifyEvent event) {
        MultiBlockHandler.update(event.getPos(), event.getLevel());
    }
}

