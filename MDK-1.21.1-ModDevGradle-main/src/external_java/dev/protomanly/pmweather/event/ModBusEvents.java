/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.data.DataGenerator
 *  net.minecraft.data.DataProvider
 *  net.minecraft.data.PackOutput
 *  net.neoforged.bus.api.SubscribeEvent
 *  net.neoforged.fml.common.EventBusSubscriber
 *  net.neoforged.fml.common.EventBusSubscriber$Bus
 *  net.neoforged.neoforge.common.data.ExistingFileHelper
 *  net.neoforged.neoforge.data.event.GatherDataEvent
 *  net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent
 */
package dev.protomanly.pmweather.event;

import dev.protomanly.pmweather.networking.ModNetworking;
import dev.protomanly.pmweather.particle.ParticleRegistry;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid="pmweather", bus=EventBusSubscriber.Bus.MOD)
public class ModBusEvents {
    @SubscribeEvent
    public static void registerPayload(RegisterPayloadHandlersEvent event) {
        ModNetworking.register(event.registrar("1"));
    }

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        if (event.includeClient()) {
            DataGenerator gen = event.getGenerator();
            PackOutput packOutput = gen.getPackOutput();
            ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
            gen.addProvider(event.includeClient(), (DataProvider)new ParticleRegistry(packOutput, event.getLookupProvider(), "pmweather", existingFileHelper));
        }
    }
}

