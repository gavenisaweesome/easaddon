/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonSyntaxException
 *  com.mojang.blaze3d.Blaze3D
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.minecraft.client.Camera
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.client.renderer.EffectInstance
 *  net.minecraft.client.renderer.PostChain
 *  net.minecraft.client.renderer.PostPass
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.packs.resources.ResourceProvider
 *  net.minecraft.util.Mth
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.Vec2
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Matrix4f
 */
package dev.protomanly.pmweather.shaders;

import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.Blaze3D;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.protomanly.pmweather.PMWeather;
import dev.protomanly.pmweather.config.ClientConfig;
import dev.protomanly.pmweather.config.ServerConfig;
import dev.protomanly.pmweather.event.GameBusClientEvents;
import dev.protomanly.pmweather.mixin.PostChainMixin;
import dev.protomanly.pmweather.weather.Lightning;
import dev.protomanly.pmweather.weather.Storm;
import dev.protomanly.pmweather.weather.ThermodynamicEngine;
import dev.protomanly.pmweather.weather.WeatherHandler;
import dev.protomanly.pmweather.weather.WeatherHandlerClient;
import dev.protomanly.pmweather.weather.WindEngine;
import java.io.IOException;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class ModShaders {
    private static PostChain clouds;
    private static Vec2 lastScroll;
    public static Vec2 scroll;
    private static int lastWidth;
    private static int lastHeight;
    private static float snow;
    private static float lastSnow;
    private static Integer noiseTextureID;

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        WeatherHandler weatherHandler = GameBusClientEvents.weatherHandler;
        if (player != null && weatherHandler != null && Minecraft.getInstance().level != null) {
            Vec3 wind = WindEngine.getWind(new Vec3(player.position().x, (double)minecraft.level.getMaxBuildHeight(), player.position().z), (Level)minecraft.level, true, true, false);
            Vec3 wind2 = wind.multiply(0.03, 0.03, 0.03);
            lastScroll = scroll;
            scroll = scroll.add(new Vec2(-((float)wind2.x), -((float)wind2.z)));
            for (Storm storm : weatherHandler.getStorms()) {
                storm.lastPosition = storm.lastPosition == null ? storm.position : storm.lastPosition.lerp(storm.position, (double)0.05f);
                storm.lastSpin = storm.spin;
                storm.spin += storm.smoothWindspeed * 0.01f / Math.max(storm.smoothWidth, 20.0f);
            }
            ThermodynamicEngine.Precipitation precip = ThermodynamicEngine.getPrecipitationType(weatherHandler, player.position(), (Level)minecraft.level, 0);
            lastSnow = snow;
            if (precip == ThermodynamicEngine.Precipitation.SNOW || precip == ThermodynamicEngine.Precipitation.WINTRY_MIX) {
                float rain = weatherHandler.getPrecipitation(player.position());
                float snowBlindness = (float)Math.clamp(Math.pow(wind.length() / 60.0, 2.0) * (double)rain, 0.0, 1.0);
                snow = Mth.lerp((float)0.05f, (float)snow, (float)snowBlindness);
            } else {
                snow = Mth.lerp((float)0.05f, (float)snow, (float)0.0f);
            }
        }
    }

    public static void renderShaders(float partialTicks, Camera camera, Matrix4f projMat, Matrix4f modelMat) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        WeatherHandler weatherHandler = GameBusClientEvents.weatherHandler;
        if (clouds != null && player != null && weatherHandler != null && Minecraft.getInstance().level != null && ServerConfig.validDimensions != null && ServerConfig.validDimensions.contains(Minecraft.getInstance().level.dimension())) {
            int width = minecraft.getWindow().getWidth();
            int height = minecraft.getWindow().getHeight();
            if (width != lastWidth || height != lastHeight) {
                lastWidth = width;
                lastHeight = height;
                ModShaders.updateShaderGroupSize(clouds);
            }
            float gameTime = (float)Blaze3D.getTime();
            RenderSystem.enableDepthTest();
            RenderSystem.resetTextureMatrix();
            RenderSystem.disableBlend();
            RenderSystem.depthMask((boolean)false);
            PostChain postChain = clouds;
            if (postChain instanceof PostChainMixin) {
                PostChainMixin data = (PostChainMixin)postChain;
                List<PostPass> passes = data.getPasses();
                EffectInstance effect = passes.getFirst().getEffect();
                effect.safeGetUniform("OutSize").set((float)width, (float)height);
                Vec3 camPos = camera.getPosition();
                effect.safeGetUniform("pos").set((float)camPos.x, (float)camPos.y, (float)camPos.z);
                effect.safeGetUniform("scroll").set(Mth.lerp((float)partialTicks, (float)ModShaders.lastScroll.x, (float)ModShaders.scroll.x), Mth.lerp((float)partialTicks, (float)ModShaders.lastScroll.y, (float)ModShaders.scroll.y));
                effect.safeGetUniform("maxSteps").set(800);
                effect.safeGetUniform("stepSize").set(0.01f);
                effect.safeGetUniform("fogStart").set(RenderSystem.getShaderFogStart() * 4.0f);
                effect.safeGetUniform("fogEnd").set(RenderSystem.getShaderFogEnd() * 4.0f);
                effect.safeGetUniform("proj").set(projMat.invert());
                effect.safeGetUniform("viewmat").set(modelMat.invert());
                effect.safeGetUniform("time").set((float)player.tickCount + partialTicks);
                long seed = 0L;
                if (GameBusClientEvents.weatherHandler != null) {
                    seed = GameBusClientEvents.weatherHandler.seed;
                }
                effect.safeGetUniform("worldTime").set((float)player.level().getGameTime() + (float)seed / 1.0E14f + partialTicks);
                effect.safeGetUniform("layer0height").set((float)ServerConfig.layer0Height);
                effect.safeGetUniform("layerCheight").set((float)ServerConfig.layerCHeight);
                effect.safeGetUniform("stormSize").set((float)ServerConfig.stormSize * 2.0f);
                float sunAng = Minecraft.getInstance().level.getSunAngle(partialTicks);
                Vec3 sunDir = new Vec3(-Math.sin(sunAng), Math.cos(sunAng), 0.0);
                effect.safeGetUniform("sunDir").set((float)sunDir.x, (float)sunDir.y, (float)sunDir.z);
                effect.safeGetUniform("lightIntensity").set((float)Math.pow((Math.cos(sunAng) + 1.0) / 2.0, 3.0));
                effect.safeGetUniform("downsample").set((float)ClientConfig.volumetricsDownsample);
                passes.get(1).getEffect().safeGetUniform("downsample").set((float)ClientConfig.volumetricsDownsample);
                passes.get(1).getEffect().safeGetUniform("glowFix").set(ClientConfig.glowFix ? 1.0f : 0.0f);
                passes.get(1).getEffect().safeGetUniform("doBlur").set(ClientConfig.volumetricsBlur ? 1.0f : 0.0f);
                effect.safeGetUniform("simpleLighting").set(ClientConfig.simpleLighting ? 0.0f : 1.0f);
                if (weatherHandler instanceof WeatherHandlerClient) {
                    WeatherHandlerClient whc = (WeatherHandlerClient)weatherHandler;
                    effect.safeGetUniform("rain").set(whc.getPrecipitation());
                    effect.safeGetUniform("snow").set(Mth.lerp((float)partialTicks, (float)lastSnow, (float)snow));
                }
                List<Storm> storms = weatherHandler.getStorms();
                float[] stormPositions = new float[48];
                float[] stormVelocites = new float[32];
                float[] stormStages = new float[16];
                float[] stormEnergies = new float[16];
                float[] stormTypes = new float[16];
                float[] tornadoWindspeeds = new float[16];
                float[] tornadoWidths = new float[16];
                float[] tornadoTouchdownSpeeds = new float[16];
                float[] visualOnlys = new float[16];
                float[] stormSpins = new float[16];
                float[] stormDyings = new float[16];
                float[] tornadoShapes = new float[16];
                float[] stormOcclusions = new float[16];
                float[] lightningStrikes = new float[192];
                if (GameBusClientEvents.weatherHandler != null) {
                    float[] lightningBrightness = new float[64];
                    List<Lightning> lightnings = ((WeatherHandlerClient)GameBusClientEvents.weatherHandler).lightnings;
                    for (int i = 0; i < lightnings.size(); ++i) {
                        if (i >= 64) continue;
                        Lightning lightning = lightnings.get(i);
                        lightningStrikes[i * 3] = (float)lightning.position.x;
                        lightningStrikes[i * 3 + 1] = (float)lightning.position.y;
                        lightningStrikes[i * 3 + 2] = (float)lightning.position.z;
                        float p = Math.clamp(((float)lightning.ticks + partialTicks) / (float)lightning.lifetime, 0.0f, 1.0f);
                        lightningBrightness[i] = (float)Math.abs(Math.cos(Math.sqrt(p) * Math.PI * 3.0)) * (1.0f - p);
                    }
                    effect.safeGetUniform("lightningStrikes").set(lightningStrikes);
                    effect.safeGetUniform("lightningCount").set(lightnings.size());
                    effect.safeGetUniform("lightningBrightness").set(lightningBrightness);
                }
                int count = 0;
                for (int i = 0; i < storms.size(); ++i) {
                    if (i >= 16) continue;
                    Storm storm = storms.get(i);
                    if (storm.position.multiply(1.0, 0.0, 1.0).distanceTo(camera.getPosition().multiply(1.0, 0.0, 1.0)) > 8000.0 || storm.stage <= 0 && storm.energy <= 0 || storm.lastPosition == null) continue;
                    Vec3 pos = storm.lastPosition;
                    stormPositions[count * 3] = (float)pos.x;
                    stormPositions[count * 3 + 1] = (float)pos.y;
                    stormPositions[count * 3 + 2] = (float)pos.z;
                    Vec3 vel = storm.velocity;
                    stormVelocites[count * 2] = (float)vel.x;
                    stormVelocites[count * 2 + 1] = (float)vel.z;
                    stormStages[count] = storm.stage;
                    stormEnergies[count] = storm.energy;
                    tornadoWindspeeds[count] = storm.smoothWindspeed;
                    tornadoWidths[count] = storm.smoothWidth;
                    tornadoTouchdownSpeeds[count] = storm.touchdownSpeed;
                    stormSpins[count] = Mth.lerp((float)partialTicks, (float)storm.lastSpin, (float)storm.spin);
                    tornadoShapes[count] = storm.tornadoShape;
                    stormTypes[count] = storm.stormType;
                    stormOcclusions[count] = storm.occlusion;
                    visualOnlys[count] = storm.visualOnly ? 1.0f : -1.0f;
                    stormDyings[count] = storm.isDying ? 1.0f : -1.0f;
                    ++count;
                }
                effect.safeGetUniform("stormCount").set(count);
                effect.safeGetUniform("stormPositions").set(stormPositions);
                effect.safeGetUniform("stormVelocities").set(stormVelocites);
                effect.safeGetUniform("stormStages").set(stormStages);
                effect.safeGetUniform("stormEnergies").set(stormEnergies);
                effect.safeGetUniform("stormTypes").set(stormTypes);
                effect.safeGetUniform("tornadoWindspeeds").set(tornadoWindspeeds);
                effect.safeGetUniform("tornadoWidths").set(tornadoWidths);
                effect.safeGetUniform("tornadoTouchdownSpeeds").set(tornadoTouchdownSpeeds);
                effect.safeGetUniform("visualOnlys").set(visualOnlys);
                effect.safeGetUniform("stormSpins").set(stormSpins);
                effect.safeGetUniform("stormDyings").set(stormDyings);
                effect.safeGetUniform("tornadoShapes").set(tornadoShapes);
                effect.safeGetUniform("stormOcclusions").set(stormOcclusions);
                effect.safeGetUniform("overcastPerc").set((float)ServerConfig.overcastPercent);
                effect.safeGetUniform("rainStrength").set((float)ServerConfig.rainStrength);
                Vec3 sampPos = camera.getPosition().multiply(1.0, 0.0, 1.0).add(0.0, ServerConfig.layer0Height, 0.0);
                Vec3 lightingColor = new Vec3(1.0, 1.0, 1.0);
                lightingColor = lightingColor.lerp(new Vec3(0.741, 0.318, 0.227), Math.pow(1.0 - sunDir.y, 2.5));
                lightingColor = lightingColor.lerp(new Vec3(0.314, 0.408, 0.525), Math.clamp((sunDir.y + 0.1) / -0.1, 0.0, 1.0));
                Vec3 skyColor = Minecraft.getInstance().level.getSkyColor(sampPos, partialTicks);
                effect.safeGetUniform("lightingColor").set((float)lightingColor.x, (float)lightingColor.y, (float)lightingColor.z);
                effect.safeGetUniform("skyColor").set((float)skyColor.x, (float)skyColor.y, (float)skyColor.z);
                int quality = switch (ClientConfig.volumetricsQuality.ordinal()) {
                    default -> throw new MatchException(null, null);
                    case 0 -> 0;
                    case 1 -> 1;
                    case 2 -> 2;
                    case 3 -> 3;
                    case 4 -> 4;
                };
                effect.safeGetUniform("quality").set(quality);
                effect.safeGetUniform("nearPlane").set(0.05f);
                effect.safeGetUniform("farPlane").set((float)((Integer)Minecraft.getInstance().options.renderDistance().get()).intValue() * 4.0f * 16.0f);
                effect.safeGetUniform("renderDistance").set(6000.0f);
                clouds.process(partialTicks);
            }
            minecraft.getMainRenderTarget().bindWrite(false);
            RenderSystem.depthMask((boolean)true);
            projMat.invert();
            modelMat.invert();
        }
    }

    public static PostChain createShader(ResourceLocation resourceLocation) {
        try {
            Minecraft minecraft = Minecraft.getInstance();
            return new PostChain(minecraft.getTextureManager(), (ResourceProvider)minecraft.getResourceManager(), minecraft.getMainRenderTarget(), resourceLocation);
        }
        catch (IOException e) {
            PMWeather.LOGGER.error("Failed to load shader: {}", (Object)resourceLocation, (Object)e);
        }
        catch (JsonSyntaxException e) {
            PMWeather.LOGGER.error("Failed to parse shader: {}", (Object)resourceLocation, (Object)e);
        }
        return null;
    }

    public static void createShaders() {
        if (clouds == null) {
            clouds = ModShaders.createShader(PMWeather.getPath("shaders/post/clouds.json"));
        }
    }

    public static void reload() {
        if (clouds != null) {
            clouds.close();
        }
        clouds = null;
        ModShaders.createShaders();
        ModShaders.updateShaderGroupSize(clouds);
        PMWeather.LOGGER.info("Loaded PMWeather Shaders");
    }

    private static void updateShaderGroupSize(PostChain shaderGroup) {
        if (shaderGroup != null) {
            Minecraft minecraft = Minecraft.getInstance();
            int width = minecraft.getWindow().getWidth();
            int height = minecraft.getWindow().getHeight();
            shaderGroup.resize(width, height);
        }
    }

    static {
        lastScroll = Vec2.ZERO;
        scroll = Vec2.ZERO;
        lastWidth = 0;
        lastHeight = 0;
        snow = 0.0f;
        lastSnow = 0.0f;
    }

    public static enum Quality {
        POTATO,
        LOW,
        MEDIUM,
        HIGH,
        PC_KILLER;

    }
}

