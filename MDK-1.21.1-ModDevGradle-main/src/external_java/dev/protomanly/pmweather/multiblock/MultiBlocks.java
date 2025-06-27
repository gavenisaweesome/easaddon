/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.item.BlockItem
 *  net.minecraft.world.item.Item$Properties
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.state.BlockBehaviour
 *  net.minecraft.world.level.block.state.BlockBehaviour$Properties
 *  net.neoforged.neoforge.registries.DeferredBlock
 */
package dev.protomanly.pmweather.multiblock;

import dev.protomanly.pmweather.block.ModBlocks;
import dev.protomanly.pmweather.item.ModItems;
import dev.protomanly.pmweather.multiblock.wsr88d.WSR88DCore;
import java.util.function.Supplier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

public class MultiBlocks {
    public static final DeferredBlock<Block> WSR88D_CORE = MultiBlocks.registerBlock("wsr88d_core", () -> new WSR88DCore(BlockBehaviour.Properties.ofFullCopy((BlockBehaviour)Blocks.IRON_BLOCK).noOcclusion()));

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block) {
        DeferredBlock returnBlock = ModBlocks.BLOCKS.register(name, block);
        MultiBlocks.registerBlockItem(name, returnBlock);
        return returnBlock;
    }

    private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block) {
        ModItems.ITEMS.register(name, () -> new BlockItem((Block)block.get(), new Item.Properties()));
    }

    public static void register() {
    }
}

