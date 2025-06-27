/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.world.item.CreativeModeTab
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.ItemLike
 *  net.neoforged.neoforge.registries.DeferredRegister
 */
package dev.protomanly.pmweather.creative;

import dev.protomanly.pmweather.block.ModBlocks;
import dev.protomanly.pmweather.item.ModItems;
import dev.protomanly.pmweather.multiblock.MultiBlocks;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create((ResourceKey)Registries.CREATIVE_MODE_TAB, (String)"pmweather");
    public static final Supplier<CreativeModeTab> PMWEATHER_TAB = CREATIVE_MODE_TABS.register("pmweather_tab", () -> CreativeModeTab.builder().icon(() -> new ItemStack((ItemLike)ModBlocks.RADAR.get())).title((Component)Component.translatable((String)"creativetab.pmweather.main")).displayItems((itemDisplayParameters, output) -> {
        output.accept(ModBlocks.REINFORCED_GLASS);
        output.accept(ModBlocks.REINFORCED_GLASS_PANE);
        output.accept(ModBlocks.RADAR);
        output.accept(MultiBlocks.WSR88D_CORE);
        output.accept(ModBlocks.RADOME);
        output.accept(ModBlocks.TORNADO_SENSOR);
        output.accept(ModBlocks.TORNADO_SIREN);
        output.accept(ModBlocks.ANEMOMETER);
        output.accept(ModBlocks.METAR);
        output.accept(ModBlocks.SOUNDING_VIEWER);
        output.accept(ModBlocks.WEATHER_PLATFORM);
        output.accept(ModItems.CONNECTOR);
        output.accept(ModItems.WEATHER_BALLOON);
        output.accept(ModBlocks.ICE_LAYER);
        output.accept(ModBlocks.SLEET_LAYER);
    }).build());
}

