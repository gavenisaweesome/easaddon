/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.chat.Component
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.Item$Properties
 *  net.minecraft.world.item.Item$TooltipContext
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.TooltipFlag
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.entity.BlockEntity
 */
package dev.protomanly.pmweather.item;

import dev.protomanly.pmweather.block.entity.WeatherPlatformBlockEntity;
import dev.protomanly.pmweather.item.component.ModComponents;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ConnectorItem
extends Item {
    public ConnectorItem(Item.Properties properties) {
        super(properties);
    }

    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        BlockPos pos;
        BlockEntity blockEntity;
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (stack.has(ModComponents.WEATHER_BALLOON_PLATFORM) && !level.isClientSide() && !((blockEntity = level.getBlockEntity(pos = (BlockPos)stack.get(ModComponents.WEATHER_BALLOON_PLATFORM))) instanceof WeatherPlatformBlockEntity)) {
            stack.remove(ModComponents.WEATHER_BALLOON_PLATFORM);
        }
    }

    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        if (stack.has(ModComponents.WEATHER_BALLOON_PLATFORM)) {
            BlockPos pos = (BlockPos)stack.get(ModComponents.WEATHER_BALLOON_PLATFORM);
            tooltipComponents.add((Component)Component.literal((String)String.format("Connected to: %s, %s, %s", pos.getX(), pos.getY(), pos.getZ())));
        } else {
            tooltipComponents.add((Component)Component.literal((String)"Unset"));
        }
    }
}

