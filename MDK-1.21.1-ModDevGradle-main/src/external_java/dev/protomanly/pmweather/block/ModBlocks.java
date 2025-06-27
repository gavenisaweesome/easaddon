/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.item.BlockItem
 *  net.minecraft.world.item.Item$Properties
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.IronBarsBlock
 *  net.minecraft.world.level.block.SnowLayerBlock
 *  net.minecraft.world.level.block.SoundType
 *  net.minecraft.world.level.block.TransparentBlock
 *  net.minecraft.world.level.block.state.BlockBehaviour
 *  net.minecraft.world.level.block.state.BlockBehaviour$Properties
 *  net.neoforged.neoforge.registries.DeferredBlock
 *  net.neoforged.neoforge.registries.DeferredRegister
 *  net.neoforged.neoforge.registries.DeferredRegister$Blocks
 */
package dev.protomanly.pmweather.block;

import dev.protomanly.pmweather.block.AnemometerBlock;
import dev.protomanly.pmweather.block.HeavyScourBlock;
import dev.protomanly.pmweather.block.MediumScourBlock;
import dev.protomanly.pmweather.block.MetarBlock;
import dev.protomanly.pmweather.block.RadarBlock;
import dev.protomanly.pmweather.block.RottedLogBlock;
import dev.protomanly.pmweather.block.ScouredGrassBlock;
import dev.protomanly.pmweather.block.SoundingViewerBlock;
import dev.protomanly.pmweather.block.TornadoSensorBlock;
import dev.protomanly.pmweather.block.TornadoSirenBlock;
import dev.protomanly.pmweather.block.WeatherPlatformBlock;
import dev.protomanly.pmweather.item.ModItems;
import dev.protomanly.pmweather.sound.ModSounds;
import java.util.function.Supplier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks((String)"pmweather");
    public static final DeferredBlock<Block> ANEMOMETER = ModBlocks.registerBlock("anemometer", () -> new AnemometerBlock(BlockBehaviour.Properties.ofFullCopy((BlockBehaviour)Blocks.OAK_PLANKS).strength(1.25f).noOcclusion().sound(SoundType.METAL)));
    public static final DeferredBlock<Block> RADAR = ModBlocks.registerBlock("radar", () -> new RadarBlock(BlockBehaviour.Properties.ofFullCopy((BlockBehaviour)Blocks.OAK_PLANKS).strength(1.0f).sound(SoundType.METAL)));
    public static final DeferredBlock<Block> TORNADO_SENSOR = ModBlocks.registerBlock("tornado_sensor", () -> new TornadoSensorBlock(BlockBehaviour.Properties.ofFullCopy((BlockBehaviour)Blocks.OAK_PLANKS).strength(1.0f).sound(SoundType.METAL)));
    public static final DeferredBlock<Block> TORNADO_SIREN = ModBlocks.registerBlock("tornado_siren", () -> new TornadoSirenBlock(BlockBehaviour.Properties.ofFullCopy((BlockBehaviour)Blocks.OAK_PLANKS).strength(1.0f).sound(SoundType.METAL)));
    public static final DeferredBlock<Block> METAR = ModBlocks.registerBlock("metar", () -> new MetarBlock(BlockBehaviour.Properties.ofFullCopy((BlockBehaviour)Blocks.OAK_PLANKS).strength(1.0f).sound(SoundType.METAL)));
    public static final DeferredBlock<Block> RADOME = ModBlocks.registerBlock("radome", () -> new Block(BlockBehaviour.Properties.ofFullCopy((BlockBehaviour)Blocks.WHITE_WOOL).strength(0.5f).sound(SoundType.STONE)));
    public static final DeferredBlock<Block> SCOURED_GRASS = ModBlocks.registerBlock("scoured_grass", () -> new ScouredGrassBlock(BlockBehaviour.Properties.ofFullCopy((BlockBehaviour)Blocks.GRASS_BLOCK).randomTicks()));
    public static final DeferredBlock<Block> MEDIUM_SCOURING = ModBlocks.registerBlock("medium_scouring", () -> new MediumScourBlock(BlockBehaviour.Properties.ofFullCopy((BlockBehaviour)Blocks.DIRT).randomTicks()));
    public static final DeferredBlock<Block> HEAVY_SCOURING = ModBlocks.registerBlock("heavy_scouring", () -> new HeavyScourBlock(BlockBehaviour.Properties.ofFullCopy((BlockBehaviour)Blocks.DIRT).randomTicks()));
    public static final DeferredBlock<Block> WEATHER_PLATFORM = ModBlocks.registerBlock("balloon_platform", () -> new WeatherPlatformBlock(BlockBehaviour.Properties.ofFullCopy((BlockBehaviour)Blocks.OAK_PLANKS).strength(1.0f).sound(SoundType.METAL).noOcclusion()));
    public static final DeferredBlock<Block> SOUNDING_VIEWER = ModBlocks.registerBlock("sounding_viewer", () -> new SoundingViewerBlock(BlockBehaviour.Properties.ofFullCopy((BlockBehaviour)Blocks.OAK_PLANKS).strength(1.0f).sound(SoundType.METAL).noOcclusion()));
    public static final DeferredBlock<Block> ICE_LAYER = ModBlocks.registerBlock("ice_layer", () -> new SnowLayerBlock(BlockBehaviour.Properties.ofFullCopy((BlockBehaviour)Blocks.ICE).noOcclusion()));
    public static final DeferredBlock<Block> SLEET_LAYER = ModBlocks.registerBlock("sleet_layer", () -> new SnowLayerBlock(BlockBehaviour.Properties.ofFullCopy((BlockBehaviour)Blocks.SNOW).sound((SoundType)ModSounds.SLEET_BLOCK).noOcclusion()));
    public static final DeferredBlock<Block> REINFORCED_GLASS = ModBlocks.registerBlock("reinforced_glass", () -> new TransparentBlock(BlockBehaviour.Properties.ofFullCopy((BlockBehaviour)Blocks.GLASS).strength(3.0f).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> REINFORCED_GLASS_PANE = ModBlocks.registerBlock("reinforced_glass_pane", () -> new IronBarsBlock(BlockBehaviour.Properties.ofFullCopy((BlockBehaviour)Blocks.GLASS).strength(1.5f).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> ROTTED_LOG = ModBlocks.registerBlock("rotted_log", () -> new RottedLogBlock(BlockBehaviour.Properties.ofFullCopy((BlockBehaviour)Blocks.OAK_LOG).sound(SoundType.GRAVEL).instabreak()));

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block) {
        DeferredBlock returnBlock = BLOCKS.register(name, block);
        ModBlocks.registerBlockItem(name, returnBlock);
        return returnBlock;
    }

    private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block) {
        ModItems.ITEMS.register(name, () -> new BlockItem((Block)block.get(), new Item.Properties()));
    }
}

