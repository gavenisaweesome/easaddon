/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.world.item.BlockItem
 *  net.minecraft.world.item.CreativeModeTab
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.Item$Properties
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.entity.BlockEntityType
 *  net.minecraft.world.level.block.entity.BlockEntityType$Builder
 *  net.minecraft.world.level.block.state.BlockBehaviour$Properties
 *  net.neoforged.bus.api.IEventBus
 *  net.neoforged.neoforge.registries.DeferredHolder
 *  net.neoforged.neoforge.registries.DeferredRegister
 *  net.neoforged.neoforge.registries.RegisterEvent
 */
package com.burrows.easaddon;

import com.burrows.easaddon.EASAddon;
import com.burrows.easaddon.EASBlock;
import com.burrows.easaddon.EASBlockEntity;
import com.burrows.easaddon.EasTransmitterItem;
import com.burrows.easaddon.DamageSurveyorItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegisterEvent;
import com.burrows.easaddon.RadarOverlayBlock;
import com.burrows.easaddon.RadarOverlayBlockEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class RegistryHandler {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create((ResourceKey)Registries.BLOCK, (String)"easaddon");
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create((ResourceKey)Registries.ITEM, (String)"easaddon");
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create((ResourceKey)Registries.BLOCK_ENTITY_TYPE, (String)"easaddon");
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create((ResourceKey)Registries.CREATIVE_MODE_TAB, (String)"easaddon");
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create((ResourceKey)Registries.SOUND_EVENT, (String)"easaddon");

    public static final DeferredHolder<Block, EASBlock> EAS_BLOCK =
    	    BLOCKS.register("eas_block", () ->
    	        new EASBlock(BlockBehaviour.Properties
    	            .of()                             // default material
    	            .strength(1.5f)                   // hardness = 2.5, blast resistance = 2.5
    	            .lightLevel(s -> 3)               // still emits light level 3
    	        )
    	    );
    
    

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EASBlockEntity>> EAS_BLOCK_ENTITY = BLOCK_ENTITIES.register("eas_block_entity", () -> BlockEntityType.Builder.of(EASBlockEntity::new, (Block[])new Block[]{(Block)EAS_BLOCK.get()}).build(null));
    public static final DeferredHolder<Item, Item> EAS_BLOCK_ITEM = ITEMS.register("eas_block", () -> new BlockItem((Block)EAS_BLOCK.get(), new Item.Properties()));
    public static final DeferredHolder<Item, EasTransmitterItem> EAS_TRANSMITTER = ITEMS.register("eas_transmitter", () -> new EasTransmitterItem(new Item.Properties().stacksTo(1)));
    
    // New Damage Surveyor Item
    public static final DeferredHolder<Item, DamageSurveyorItem> DAMAGE_SURVEYOR = ITEMS.register("damage_surveyor", () -> new DamageSurveyorItem(new Item.Properties().stacksTo(1)));
    
    public static final DeferredHolder<SoundEvent, SoundEvent> EAS_ALERT = SOUNDS.register("eas_alert", () -> SoundEvent.createVariableRangeEvent((ResourceLocation)ResourceLocation.parse((String)"easaddon:eas_alert")));
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB = CREATIVE_TABS.register("main", () -> CreativeModeTab.builder().title((Component)Component.translatable((String)"itemGroup.easaddon.main")).icon(() -> new ItemStack((ItemLike)EAS_BLOCK_ITEM.get())).displayItems((params, output) -> {
        output.accept((ItemLike)EAS_BLOCK_ITEM.get());
        output.accept((ItemLike)EAS_TRANSMITTER.get());
        output.accept((ItemLike)DAMAGE_SURVEYOR.get()); // Add Damage Surveyor to creative tab
    }).build());
    
    public static final DeferredHolder<Block, RadarOverlayBlock> RADAR_OVERLAY_BLOCK =
    	    BLOCKS.register("radar_overlay", 
    	        () -> new RadarOverlayBlock(
    	            BlockBehaviour.Properties
    	                .of()
    	                .noCollission()        // so you can walk through it
    	                .strength(0.1f)        // nearly instant break
    	                .noOcclusion()         // invisible to light
    	                .lightLevel(s -> 0)    // no light
    	        )
    	    );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RadarOverlayBlockEntity>> RADAR_OVERLAY_BE =
    	    BLOCK_ENTITIES.register("radar_overlay",
    	        () -> BlockEntityType.Builder
    	                .of(RadarOverlayBlockEntity::new, RADAR_OVERLAY_BLOCK.get())
    	                .build(null)
    	    );
    public static final DeferredHolder<Item, BlockItem> RADAR_OVERLAY_ITEM =
    	    ITEMS.register("radar_overlay",
    	        () -> new BlockItem(RADAR_OVERLAY_BLOCK.get(),
    	                            new Item.Properties() /* no tab() or group() for now */)
    	    );
    
    
    
    private static boolean blocksRegistered = false;
    private static boolean itemsRegistered = false;
    private static boolean blockEntitiesRegistered = false;

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
        SOUNDS.register(modEventBus);
        modEventBus.addListener(RegistryHandler::onRegistryEvent);
    }

    private static void onRegistryEvent(RegisterEvent event) {
        if (event.getRegistryKey().equals(Registries.BLOCK)) {
            blocksRegistered = true;
           
        }
        if (event.getRegistryKey().equals(Registries.ITEM)) {
            itemsRegistered = true;
            
        }
        if (event.getRegistryKey().equals(Registries.BLOCK_ENTITY_TYPE)) {
            blockEntitiesRegistered = true;
            
        }
    }

    public static void verifyRegistration() {
    }
}