/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.bus.api.SubscribeEvent
 *  net.neoforged.fml.common.EventBusSubscriber
 *  net.neoforged.fml.common.EventBusSubscriber$Bus
 *  net.neoforged.neoforge.client.event.RenderLevelStageEvent
 *  net.neoforged.neoforge.client.event.RenderLevelStageEvent$Stage
 */
package dev.protomanly.pmweather.render;

import dev.protomanly.pmweather.event.GameBusClientEvents;
import dev.protomanly.pmweather.particle.ParticleManager;
import dev.protomanly.pmweather.render.CustomLightningRenderer;
import dev.protomanly.pmweather.shaders.ModShaders;
import dev.protomanly.pmweather.weather.Lightning;
import dev.protomanly.pmweather.weather.WeatherHandlerClient;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(modid="pmweather", bus=EventBusSubscriber.Bus.GAME, value={Dist.CLIENT})
public class RenderEvents {
    public static List<Color> lightningColors = new ArrayList<Color>(){
        {
            this.add(new Color(0xFFFFFF));
            this.add(new Color(15587698));
            this.add(new Color(13041578));
            this.add(new Color(10778102));
            this.add(new Color(15106690));
            this.add(new Color(7203548));
        }
    };

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_WEATHER) {
            float partialTicks = event.getPartialTick().getGameTimeDeltaTicks();
            WeatherHandlerClient weatherHandlerClient = (WeatherHandlerClient)GameBusClientEvents.weatherHandler;
            if (weatherHandlerClient != null) {
                List<Lightning> lightnings = weatherHandlerClient.lightnings;
                for (int i = 0; i < lightnings.size(); ++i) {
                    Lightning lightning = lightnings.get(i);
                    if (lightning == null) continue;
                    Random rand = new Random(lightning.seed);
                    Color color = lightningColors.get(rand.nextInt(lightningColors.size()));
                    float p = Math.clamp(((float)lightning.ticks + partialTicks) / (float)lightning.lifetime, 0.0f, 1.0f);
                    float alpha = (float)Math.abs(Math.cos(Math.sqrt(p) * Math.PI * 3.0)) * (1.0f - p);
                    color = new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.clamp((long)((int)(alpha * 255.0f)), 0, 255));
                    CustomLightningRenderer.render(lightning.position, lightning.seed, event.getCamera(), color);
                }
                ModShaders.renderShaders(event.getPartialTick().getGameTimeDeltaTicks(), event.getCamera(), event.getProjectionMatrix(), event.getModelViewMatrix());
                ParticleManager pm = GameBusClientEvents.particleManager;
                if (pm != null) {
                    pm.render(event.getPoseStack(), null, Minecraft.getInstance().gameRenderer.lightTexture(), event.getCamera(), event.getPartialTick().getGameTimeDeltaPartialTick(false), event.getFrustum());
                }
            }
        }
    }
}

