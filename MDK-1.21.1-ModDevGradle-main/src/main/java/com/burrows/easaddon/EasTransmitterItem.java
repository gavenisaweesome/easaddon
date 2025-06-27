package com.burrows.easaddon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class EasTransmitterItem extends Item {
    public EasTransmitterItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level world = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        Player player = ctx.getPlayer();

        if (!world.isClientSide() && world.getBlockState(pos).getBlock() instanceof EASBlock) {
            ItemStack stack = ctx.getItemInHand();

            // Get or create component data
            CustomData component = stack.get(DataComponents.CUSTOM_DATA);
            CompoundTag tag = component != null ? component.copyTag() : new CompoundTag();

            // Update NBT data
            tag.putLong("boundPos", pos.asLong());
            tag.putString("boundDim", world.dimension().location().toString());

            // Set updated component back to item
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

            if (player != null) {
                player.sendSystemMessage(
                    Component.literal("§6Transmitter bound to EAS at " + pos)
                );
            }
            return InteractionResult.CONSUME;
        }

        return super.useOn(ctx);
    }
}