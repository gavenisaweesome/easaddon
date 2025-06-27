/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.renderer.entity.EntityRenderers
 *  net.minecraft.client.renderer.item.ItemProperties
 *  net.minecraft.client.renderer.item.ItemPropertyFunction
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.packs.resources.PreparableReloadListener
 *  net.minecraft.server.packs.resources.ResourceManager
 *  net.minecraft.server.packs.resources.SimplePreparableReloadListener
 *  net.minecraft.util.profiling.ProfilerFiller
 *  net.minecraft.world.item.Item
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.bus.api.SubscribeEvent
 *  net.neoforged.fml.common.EventBusSubscriber
 *  net.neoforged.fml.common.EventBusSubscriber$Bus
 *  net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
 *  net.neoforged.neoforge.client.event.EntityRenderersEvent$RegisterLayerDefinitions
 *  net.neoforged.neoforge.client.event.EntityRenderersEvent$RegisterRenderers
 *  net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent
 */
package dev.protomanly.pmweather.event;

import dev.protomanly.pmweather.PMWeather;
import dev.protomanly.pmweather.block.entity.ModBlockEntities;
import dev.protomanly.pmweather.entity.ModEntities;
import dev.protomanly.pmweather.entity.client.MovingBlockRenderer;
import dev.protomanly.pmweather.item.ModItems;
import dev.protomanly.pmweather.item.component.ModComponents;
import dev.protomanly.pmweather.render.AnemometerModel;
import dev.protomanly.pmweather.render.AnemometerRenderer;
import dev.protomanly.pmweather.render.RadarRenderer;
import dev.protomanly.pmweather.render.SoundingViewerRenderer;
import dev.protomanly.pmweather.render.WeatherBalloonModel;
import dev.protomanly.pmweather.render.WeatherPlatformRenderer;
import dev.protomanly.pmweather.shaders.ModShaders;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.item.ItemPropertyFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

@EventBusSubscriber(modid="pmweather", bus=EventBusSubscriber.Bus.MOD, value={Dist.CLIENT})
public class ModBusClientEvents {
    @SubscribeEvent
    public static void reloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((PreparableReloadListener)new SimplePreparableReloadListener<Object>(){

            protected Object prepare(ResourceManager resourceManager, ProfilerFiller profilerFiller) {
                return null;
            }

            protected void apply(Object o, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
                ModShaders.reload();
            }
        });
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        EntityRenderers.register(ModEntities.MOVING_BLOCK.get(), MovingBlockRenderer::new);
        ModBusClientEvents.registerItemProp(event, (Item)ModItems.CONNECTOR.get(), PMWeather.getPath("connected"), (itemStack, clientLevel, livingEntity, i) -> {
            if (itemStack.has(ModComponents.WEATHER_BALLOON_PLATFORM)) {
                return 1.0f;
            }
            return 0.0f;
        });
    }

    public static void registerItemProp(FMLClientSetupEvent event, Item item, ResourceLocation propertyID, ItemPropertyFunction function) {
        event.enqueueWork(() -> ItemProperties.register((Item)item, (ResourceLocation)propertyID, (ItemPropertyFunction)function));
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(AnemometerModel.LAYER_LOCATION, AnemometerModel::createBodyLayer);
        event.registerLayerDefinition(WeatherBalloonModel.LAYER_LOCATION, WeatherBalloonModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.ANEMOMETER_BE.get(), AnemometerRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.RADAR_BE.get(), RadarRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.WEATHER_PLATFORM_BE.get(), WeatherPlatformRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.SOUNDING_VIEWER_BE.get(), SoundingViewerRenderer::new);
    }
}

