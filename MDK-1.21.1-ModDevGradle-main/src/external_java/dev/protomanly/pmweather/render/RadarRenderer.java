/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.blaze3d.vertex.BufferBuilder
 *  com.mojang.blaze3d.vertex.BufferUploader
 *  com.mojang.blaze3d.vertex.DefaultVertexFormat
 *  com.mojang.blaze3d.vertex.MeshData
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.Tesselator
 *  com.mojang.blaze3d.vertex.VertexFormat$Mode
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.renderer.GameRenderer
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.blockentity.BlockEntityRenderer
 *  net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider$Context
 *  net.minecraft.core.BlockPos
 *  net.minecraft.util.Mth
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.Vec2
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Matrix4fStack
 *  org.joml.Matrix4fc
 *  org.joml.Vector3f
 *  org.joml.Vector3fc
 */
package dev.protomanly.pmweather.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.protomanly.pmweather.PMWeather;
import dev.protomanly.pmweather.block.RadarBlock;
import dev.protomanly.pmweather.block.entity.RadarBlockEntity;
import dev.protomanly.pmweather.config.ClientConfig;
import dev.protomanly.pmweather.config.ServerConfig;
import dev.protomanly.pmweather.event.GameBusClientEvents;
import dev.protomanly.pmweather.multiblock.wsr88d.WSR88DCore;
import dev.protomanly.pmweather.util.ColorTables;
import dev.protomanly.pmweather.util.Util;
import dev.protomanly.pmweather.weather.Sounding;
import dev.protomanly.pmweather.weather.Storm;
import dev.protomanly.pmweather.weather.ThermodynamicEngine;
import dev.protomanly.pmweather.weather.WindEngine;
import java.awt.Color;
import java.util.ArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fStack;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class RadarRenderer<T extends BlockEntity>
implements BlockEntityRenderer<T> {
    public RadarRenderer(BlockEntityRendererProvider.Context context) {
    }

    public void render(T blockEntity, float partialTicks, PoseStack poseStack, MultiBufferSource multiBufferSource, int combinedLightIn, int combinedOverlayIn) {
        if (blockEntity instanceof RadarBlockEntity) {
            RadarBlockEntity radarBlockEntity = (RadarBlockEntity)((Object)blockEntity);
            if (Minecraft.getInstance().player.position().distanceTo(blockEntity.getBlockPos().getCenter()) > 25.0) {
                return;
            }
            boolean canRender = true;
            BlockPos pos = radarBlockEntity.getBlockPos();
            float sizeRenderDiameter = 3.0f;
            float simSize = 2048.0f;
            int resolution = ClientConfig.radarResolution;
            Matrix4fStack matrix4fStack = RenderSystem.getModelViewStack();
            matrix4fStack.pushMatrix();
            matrix4fStack.mul((Matrix4fc)poseStack.last().pose());
            matrix4fStack.translate(0.5f, 1.05f, 0.5f);
            RenderSystem.applyModelViewMatrix();
            RenderSystem.enableBlend();
            RenderSystem.depthMask((boolean)true);
            RenderSystem.enableDepthTest();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            RenderSystem.defaultBlendFunc();
            Tesselator tesselator = Tesselator.getInstance();
            BufferBuilder bufferBuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            ArrayList<Storm> storms = new ArrayList<Storm>(radarBlockEntity.storms);
            boolean update = false;
            ClientConfig.RadarMode radarMode = ClientConfig.radarMode;
            if (radarBlockEntity.lastUpdate < radarBlockEntity.tickCount) {
                radarBlockEntity.lastUpdate = radarBlockEntity.tickCount + 60;
                update = true;
            }
            if (ServerConfig.requireWSR88D && update) {
                canRender = false;
                int searchrange = 64;
                Level level = blockEntity.getLevel();
                for (int x = -searchrange; x <= searchrange && !canRender; ++x) {
                    block15: for (int y = -searchrange; y <= searchrange && !canRender; ++y) {
                        for (int z = -searchrange * 2; z <= searchrange * 2; ++z) {
                            WSR88DCore core;
                            BlockState state = level.getBlockState(pos.offset(x, y, z));
                            Block block = state.getBlock();
                            if (!(block instanceof WSR88DCore) || !(core = (WSR88DCore)block).isComplete(state)) continue;
                            canRender = true;
                            continue block15;
                        }
                    }
                }
            }
            float size = sizeRenderDiameter / (float)resolution;
            for (int x = -resolution; x <= resolution; ++x) {
                for (int z = -resolution; z <= resolution; ++z) {
                    float r = 0.0f;
                    float g = 0.0f;
                    float b = 0.0f;
                    float a = 0.0f;
                    String id = String.format("%s,%s", x, z);
                    float dbz = radarBlockEntity.reflectivityMap.getOrDefault(id, Float.valueOf(0.0f)).floatValue();
                    float temp = radarBlockEntity.temperatureMap.getOrDefault(id, Float.valueOf(15.0f)).floatValue();
                    float vel = radarBlockEntity.velocityMap.getOrDefault(id, Float.valueOf(0.0f)).floatValue();
                    Color dbg = radarBlockEntity.debugMap.getOrDefault(id, new Color(0, 0, 0));
                    Vector3f pixelPos = new Vector3f((float)x, 0.0f, (float)z).mul(1.0f / (float)resolution).mul(sizeRenderDiameter / 2.0f);
                    Vec3 worldPos = new Vec3((double)x, 0.0, (double)z).multiply((double)(1.0f / (float)resolution), 0.0, (double)(1.0f / (float)resolution)).multiply((double)simSize, 0.0, (double)simSize).add(pos.getCenter());
                    if (update) {
                        dbz = 0.0f;
                        temp = 0.0f;
                        Vec2 f = new Vec2((float)x, (float)z).normalized();
                        Vec3 wind = WindEngine.getWind(new Vec3(worldPos.x, (double)(blockEntity.getLevel().getMaxBuildHeight() + 1), worldPos.z), blockEntity.getLevel(), false, false, false);
                        Vec2 w = new Vec2((float)wind.x, (float)wind.z);
                        vel = f.dot(w);
                        for (Storm storm : storms) {
                            if (storm.visualOnly) continue;
                            double stormSize = ServerConfig.stormSize * 2.0;
                            if (storm.stormType == 0) {
                                stormSize *= 1.5;
                            }
                            double scale = stormSize / 1200.0;
                            double shapeNoise = radarBlockEntity.noise.getValue((double)((float)radarBlockEntity.tickCount / 1200.0f), worldPos.x / (750.0 * scale), worldPos.z / (750.0 * scale));
                            double shapeNoise2 = radarBlockEntity.noise.getValue((double)((float)radarBlockEntity.tickCount / 1200.0f), worldPos.z / (750.0 * scale), worldPos.x / (750.0 * scale));
                            double shapeNoise4 = radarBlockEntity.noise.getValue((double)((float)radarBlockEntity.tickCount / 1200.0f), worldPos.z / (250.0 * scale), worldPos.x / (250.0 * scale));
                            shapeNoise *= 0.5;
                            shapeNoise2 *= 0.5;
                            shapeNoise4 *= 0.5;
                            shapeNoise += 0.5;
                            shapeNoise2 += 0.5;
                            shapeNoise4 += 0.5;
                            float localDBZ = 0.0f;
                            float smoothStage = (float)storm.stage + (float)storm.energy / 100.0f;
                            if (storm.stormType == 1) {
                                float p;
                                float intensity;
                                double rawDist = worldPos.distanceTo(storm.position.multiply(1.0, 0.0, 1.0));
                                Vec2 v2fWorldPos = new Vec2((float)worldPos.x, (float)worldPos.z);
                                Vec2 stormVel = new Vec2((float)storm.velocity.x, (float)storm.velocity.z);
                                Vec2 v2fStormPos = new Vec2((float)storm.position.x, (float)storm.position.z);
                                Vec2 right = new Vec2(stormVel.y, -stormVel.x).normalized();
                                Vec2 fwd = stormVel.normalized();
                                Vec2 le = Util.mulVec2(right, -3000.0f * (float)scale);
                                Vec2 ri = Util.mulVec2(right, 3000.0f * (float)scale);
                                Vec2 off = Util.mulVec2(fwd, -((float)Math.pow(Mth.clamp((double)(rawDist / (3000.0 * scale)), (double)0.0, (double)1.0), 2.0)) * (900.0f * (float)scale));
                                le = le.add(off);
                                ri = ri.add(off);
                                le = le.add(v2fStormPos);
                                ri = ri.add(v2fStormPos);
                                float dist = Util.minimumDistance(le, ri, v2fWorldPos);
                                switch (storm.stage) {
                                    case 1: {
                                        float f2 = 0.1f + (float)storm.energy / 100.0f * 0.7f;
                                        break;
                                    }
                                    case 2: {
                                        float f2 = 0.8f + (float)storm.energy / 100.0f * 0.4f;
                                        break;
                                    }
                                    case 3: {
                                        float f2 = 1.2f + (float)storm.energy / 100.0f;
                                        break;
                                    }
                                    default: {
                                        float f2 = intensity = (float)storm.energy / 100.0f * 0.1f;
                                    }
                                }
                                if (intensity > 0.8f) {
                                    intensity = 0.8f + (intensity - 0.8f) / 1.5f;
                                }
                                Vec2 nearPoint = Util.nearestPoint(le, ri, v2fWorldPos);
                                Vec2 facing = v2fWorldPos.add(nearPoint.negated());
                                float behind = -facing.dot(fwd);
                                behind += (float)shapeNoise * 600.0f * (float)scale * 0.2f;
                                float sze = 600.0f * (float)scale * 1.5f * 3.0f;
                                if ((behind += (float)stormSize / 2.0f) > 0.0f) {
                                    float start;
                                    sze *= Mth.lerp((float)Mth.clamp((float)(smoothStage - 1.0f), (float)0.0f, (float)1.0f), (float)1.0f, (float)4.0f);
                                    p = Mth.clamp((float)(Math.abs(behind) / sze), (float)0.0f, (float)1.0f);
                                    if (p <= (start = 0.06f)) {
                                        localDBZ += (float)Math.pow(p /= start, 2.0);
                                    } else {
                                        p = 1.0f - (p - start) / (1.0f - start);
                                        localDBZ += (float)Math.pow(p, 4.0);
                                    }
                                }
                                localDBZ *= Mth.sqrt((float)(1.0f - Mth.clamp((float)(dist / sze), (float)0.0f, (float)1.0f)));
                                if (smoothStage > 3.0f) {
                                    p = Mth.clamp((float)((smoothStage - 3.0f) / 2.0f), (float)0.0f, (float)0.5f);
                                    localDBZ *= 0.8f + (float)shapeNoise2 * 0.4f * (1.0f - p);
                                    localDBZ *= 0.8f + (float)shapeNoise * 0.4f * (1.0f - p);
                                    localDBZ *= 1.0f + p * 0.25f;
                                } else {
                                    localDBZ *= 0.8f + (float)shapeNoise2 * 0.4f;
                                    localDBZ *= 0.8f + (float)shapeNoise * 0.4f;
                                }
                                localDBZ *= Mth.sqrt((float)intensity);
                            }
                            if (storm.stormType == 0) {
                                float windspeed;
                                float intensity;
                                double dist = worldPos.distanceTo(storm.position.multiply(1.0, 0.0, 1.0));
                                if (dist > stormSize * 4.0) continue;
                                switch (storm.stage) {
                                    case 1: {
                                        float f3 = 0.1f + (float)storm.energy / 100.0f * 0.7f;
                                        break;
                                    }
                                    case 2: {
                                        float f3 = 0.8f + (float)storm.energy / 100.0f * 0.4f;
                                        break;
                                    }
                                    case 3: {
                                        float f3 = 1.2f + (float)storm.windspeed / 100.0f;
                                        break;
                                    }
                                    default: {
                                        float f3 = intensity = (float)Math.pow((float)storm.energy / 100.0f, 2.0) * 0.1f;
                                    }
                                }
                                if (intensity > 0.8f) {
                                    intensity = 0.8f + (intensity - 0.8f) / 4.0f;
                                }
                                switch (storm.stage) {
                                    case 2: {
                                        float f4 = (float)storm.energy / 100.0f * 40.0f;
                                        break;
                                    }
                                    case 3: {
                                        float f4 = 40.0f + (float)storm.windspeed;
                                        break;
                                    }
                                    default: {
                                        float f4 = windspeed = 0.0f;
                                    }
                                }
                                if (windspeed > 60.0f) {
                                    windspeed -= (windspeed - 60.0f) * 0.2f;
                                }
                                Vec3 torPos = storm.position.multiply(1.0, 0.0, 1.0);
                                Vec3 corePos = torPos.add(100.0 * scale * 2.5 * (double)Math.clamp(intensity * 1.5f, 0.0f, 1.0f), 0.0, -350.0 * scale * 2.5 * (double)Math.clamp(intensity * 1.5f, 0.0f, 1.0f));
                                float xM = 1.75f;
                                if (worldPos.x > corePos.x) {
                                    xM = 1.0f;
                                }
                                double coreDist = Math.sqrt(Math.pow((worldPos.x - corePos.x) * (double)xM, 2.0) + Math.pow((worldPos.z - corePos.z) * 1.5, 2.0)) / scale;
                                coreDist *= 0.9 + shapeNoise * 0.3;
                                Vec3 relPos = torPos.subtract(worldPos).multiply(scale, 0.0, scale);
                                double d = 150.0 + (dist /= scale) / 3.0;
                                double d2 = 75.0 + dist / 3.0;
                                double angle = Math.atan2(relPos.z, relPos.x) - dist / d;
                                double angle2 = Math.atan2(relPos.z, relPos.x) - dist / d2;
                                double angle3 = Math.atan2(relPos.z, relPos.x) - dist / d2 / 2.0;
                                angle += Math.toRadians(180.0);
                                angle2 += Math.toRadians(180.0);
                                angle3 += Math.toRadians(180.0);
                                double angleMod = Math.toRadians(40.0) * (1.0 - Math.clamp(Math.pow((double)windspeed / 100.0, 2.0), 0.0, 0.9));
                                double noise = (shapeNoise4 - 0.5) * Math.toRadians(10.0);
                                angle2 += angleMod + noise;
                                angle3 += angleMod + noise;
                                double inflow = Math.sin((angle += angleMod + noise) - Math.toRadians(15.0));
                                inflow = Math.pow(Math.abs(inflow), 0.5) * Math.sin(inflow);
                                if ((inflow *= 1.0 - Math.clamp(dist / 2400.0, 0.0, 1.0)) < 0.0) {
                                    localDBZ += (float)(inflow * 2.0 * Math.pow(Math.clamp((double)(windspeed - 15.0f) / 50.0, 0.0, 1.0), 2.0));
                                }
                                double surge = Math.sin(angle2 - Math.toRadians(60.0));
                                surge = Math.abs(surge) * Math.sin(surge);
                                if ((surge *= (1.0 - Math.pow(Math.clamp(dist / 1200.0, 0.0, 1.0), 1.5)) * (1.0 - Math.clamp(dist / 200.0, 0.0, 0.3))) > 0.0) {
                                    double n = 0.8 * (1.0 - Math.clamp(Math.pow((double)windspeed / 80.0, 2.0), 0.0, 1.0));
                                    double m = 1.0 - shapeNoise4 * n;
                                    localDBZ += (float)(surge * 1.5 * Math.clamp(dist / 500.0, 0.0, 1.0) * Math.sqrt(Math.clamp((double)(windspeed - 20.0f) / 50.0, 0.0, 1.0)) * m);
                                }
                                double shield = Math.sin(angle3 - Math.toRadians(60.0));
                                shield = Math.abs(shield) * Math.sin(shield);
                                if ((shield *= 1.0 - Math.pow(Math.clamp(dist / 2400.0, 0.0, 1.0), 2.0)) > 0.0) {
                                    localDBZ -= (float)(shield * 2.0 * Math.clamp(dist / 1000.0, 0.0, 1.0) * Math.sqrt(Math.clamp((double)(windspeed - 30.0f) / 80.0, 0.0, 1.0)));
                                }
                                double coreIntensity = (1.0 - Math.clamp(coreDist / 1800.0, 0.0, 1.0)) * (1.5 - shapeNoise2 * 0.5) * Math.sqrt(Math.clamp((double)intensity / 2.0, 0.0, 1.0)) * Math.clamp(dist / 300.0, 0.5, 1.0) * 1.2;
                                localDBZ += (float)Math.pow(coreIntensity, 0.65);
                            }
                            dbz = Math.max(dbz, localDBZ);
                        }
                        dbz += (PMWeather.RANDOM.nextFloat() - 0.5f) * 5.0f / 60.0f;
                        vel += (PMWeather.RANDOM.nextFloat() - 0.5f) * 3.0f;
                        if (dbz > 1.0f) {
                            dbz = (dbz - 1.0f) / 3.0f + 1.0f;
                        }
                        if (!canRender) {
                            dbz = PMWeather.RANDOM.nextFloat() * 1.2f;
                            vel = (PMWeather.RANDOM.nextFloat() - 0.5f) * 300.0f;
                            temp = 15.0f;
                        } else {
                            temp = ThermodynamicEngine.samplePoint(GameBusClientEvents.weatherHandler, worldPos, blockEntity.getLevel(), radarBlockEntity, 0).temperature();
                        }
                        radarBlockEntity.reflectivityMap.put(id, Float.valueOf(dbz));
                        radarBlockEntity.temperatureMap.put(id, Float.valueOf(temp));
                        radarBlockEntity.velocityMap.put(id, Float.valueOf(vel));
                    }
                    float rdbz = dbz * 60.0f;
                    Color color = ColorTables.getReflectivity(rdbz);
                    RadarBlock.Mode mode = (RadarBlock.Mode)((Object)blockEntity.getBlockState().getValue(RadarBlock.RADAR_MODE));
                    if (mode == RadarBlock.Mode.VELOCITY) {
                        color = new Color(0, 0, 0);
                        color = ColorTables.lerp(Mth.clamp((float)(Math.max(rdbz, (Mth.abs((float)(vel /= 1.75f)) - 18.0f) / 0.65f) / 12.0f), (float)0.0f, (float)1.0f), color, ColorTables.getVelocity(vel));
                    }
                    if (ClientConfig.radarDebugging && update) {
                        Sounding sounding;
                        if (radarMode == ClientConfig.RadarMode.TEMPERATURE) {
                            float t = ThermodynamicEngine.samplePoint(GameBusClientEvents.weatherHandler, worldPos, blockEntity.getLevel(), radarBlockEntity, 0).temperature();
                            dbg = t <= 0.0f ? ColorTables.lerp(Math.clamp(t / -40.0f, 0.0f, 1.0f), new Color(153, 226, 251, 255), new Color(29, 53, 221, 255)) : (t < 15.0f ? ColorTables.lerp(Math.clamp(t / 15.0f, 0.0f, 1.0f), new Color(255, 255, 255, 255), new Color(225, 174, 46, 255)) : ColorTables.lerp(Math.clamp((t - 15.0f) / 25.0f, 0.0f, 1.0f), new Color(225, 174, 46, 255), new Color(232, 53, 14, 255)));
                        }
                        if (radarMode == ClientConfig.RadarMode.WINDFIELDS && GameBusClientEvents.weatherHandler != null) {
                            Vec3 wP = new Vec3((double)x, 0.0, (double)z).multiply((double)(1.0f / (float)resolution), 0.0, (double)(1.0f / (float)resolution)).multiply(256.0, 0.0, 256.0).add(pos.getCenter());
                            float wind = 0.0f;
                            for (Storm storm : storms) {
                                wind += storm.getWind(wP);
                            }
                            dbg = ColorTables.getWindspeed(wind);
                        }
                        if (radarMode == ClientConfig.RadarMode.CAPE) {
                            sounding = new Sounding(GameBusClientEvents.weatherHandler, worldPos, blockEntity.getLevel(), 500, 12000, radarBlockEntity);
                            Sounding.CAPE CAPE2 = sounding.getCAPE(sounding.getSBParcel());
                            dbg = ColorTables.lerp(Mth.clamp((float)(CAPE2.CAPE() / 6000.0f), (float)0.0f, (float)1.0f), new Color(0, 0, 0), new Color(255, 0, 0));
                        }
                        if (radarMode == ClientConfig.RadarMode.CAPE3KM) {
                            sounding = new Sounding(GameBusClientEvents.weatherHandler, worldPos, blockEntity.getLevel(), 250, 4000, radarBlockEntity);
                            Sounding.CAPE CAPE3 = sounding.getCAPE(sounding.getSBParcel());
                            dbg = ColorTables.lerp(Mth.clamp((float)(CAPE3.CAPE3() / 1000.0f), (float)0.0f, (float)1.0f), new Color(0, 0, 0), new Color(255, 0, 0));
                        }
                        if (radarMode == ClientConfig.RadarMode.CINH) {
                            sounding = new Sounding(GameBusClientEvents.weatherHandler, worldPos, blockEntity.getLevel(), 500, 12000, radarBlockEntity);
                            Sounding.CAPE CAPE4 = sounding.getCAPE(sounding.getSBParcel());
                            dbg = ColorTables.lerp(Mth.clamp((float)(CAPE4.CINH() / -250.0f), (float)0.0f, (float)1.0f), new Color(0, 0, 0), new Color(0, 0, 255));
                        }
                        if (radarMode == ClientConfig.RadarMode.LAPSERATE03) {
                            sounding = new Sounding(GameBusClientEvents.weatherHandler, worldPos, blockEntity.getLevel(), 250, 4000, radarBlockEntity);
                            float lapse = (float)Math.floor(sounding.getLapseRate(0, 3000).floatValue() * 2.0f) / 2.0f;
                            dbg = lapse > 5.0f ? ColorTables.lerp(Mth.clamp((float)((lapse - 5.0f) / 5.0f), (float)0.0f, (float)1.0f), new Color(255, 255, 0), new Color(255, 0, 0)) : ColorTables.lerp(Mth.clamp((float)(lapse / 5.0f), (float)0.0f, (float)1.0f), new Color(0, 255, 0), new Color(255, 255, 0));
                        }
                        if (radarMode == ClientConfig.RadarMode.LAPSERATE36) {
                            sounding = new Sounding(GameBusClientEvents.weatherHandler, worldPos, blockEntity.getLevel(), 250, 7000, radarBlockEntity);
                            float lapse = (float)Math.floor(sounding.getLapseRate(3000, 6000).floatValue() * 2.0f) / 2.0f;
                            dbg = lapse > 5.0f ? ColorTables.lerp(Mth.clamp((float)((lapse - 5.0f) / 5.0f), (float)0.0f, (float)1.0f), new Color(255, 255, 0), new Color(255, 0, 0)) : ColorTables.lerp(Mth.clamp((float)(lapse / 5.0f), (float)0.0f, (float)1.0f), new Color(0, 255, 0), new Color(255, 255, 0));
                        }
                        radarBlockEntity.debugMap.put(id, dbg);
                    }
                    if (ClientConfig.radarDebugging) {
                        color = dbg;
                    }
                    r = (float)color.getRed() / 255.0f;
                    g = (float)color.getGreen() / 255.0f;
                    b = (float)color.getBlue() / 255.0f;
                    a = (float)color.getAlpha() / 255.0f * 0.75f + 0.25f;
                    Vector3f topLeft = new Vector3f(-1.0f, 0.0f, -1.0f).mul(size / 4.0f).add((Vector3fc)pixelPos);
                    Vector3f bottomLeft = new Vector3f(-1.0f, 0.0f, 1.0f).mul(size / 4.0f).add((Vector3fc)pixelPos);
                    Vector3f bottomRight = new Vector3f(1.0f, 0.0f, 1.0f).mul(size / 4.0f).add((Vector3fc)pixelPos);
                    Vector3f topRight = new Vector3f(1.0f, 0.0f, -1.0f).mul(size / 4.0f).add((Vector3fc)pixelPos);
                    bufferBuilder.addVertex(topLeft).setColor(r, g, b, a).addVertex(bottomLeft).setColor(r, g, b, a).addVertex(bottomRight).setColor(r, g, b, a).addVertex(topRight).setColor(r, g, b, a);
                }
            }
            float r = 1.0f;
            float g = 0.0f;
            float b = 0.0f;
            float a = 1.0f;
            Vector3f topLeft = new Vector3f(-1.0f, 0.0f, -1.0f).mul(0.015f).add(0.0f, 0.01f, 0.0f);
            Vector3f bottomLeft = new Vector3f(-1.0f, 0.0f, 1.0f).mul(0.015f).add(0.0f, 0.01f, 0.0f);
            Vector3f bottomRight = new Vector3f(1.0f, 0.0f, 1.0f).mul(0.015f).add(0.0f, 0.01f, 0.0f);
            Vector3f topRight = new Vector3f(1.0f, 0.0f, -1.0f).mul(0.015f).add(0.0f, 0.01f, 0.0f);
            bufferBuilder.addVertex(topLeft).setColor(r, g, b, a).addVertex(bottomLeft).setColor(r, g, b, a).addVertex(bottomRight).setColor(r, g, b, a).addVertex(topRight).setColor(r, g, b, a);
            matrix4fStack.mul((Matrix4fc)poseStack.last().pose().invert());
            matrix4fStack.translate(-0.5f, -1.05f, -0.5f);
            matrix4fStack.popMatrix();
            MeshData meshData = bufferBuilder.build();
            if (meshData != null) {
                BufferUploader.drawWithShader((MeshData)meshData);
            }
            RenderSystem.applyModelViewMatrix();
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
        }
    }
}

