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
 *  com.mojang.math.Axis
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.renderer.GameRenderer
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.blockentity.BlockEntityRenderer
 *  net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider$Context
 *  net.minecraft.core.Direction
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.phys.Vec2
 *  org.joml.Matrix4fStack
 *  org.joml.Matrix4fc
 *  org.joml.Quaternionf
 *  org.joml.Quaternionfc
 *  org.joml.Vector3f
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
import com.mojang.math.Axis;
import dev.protomanly.pmweather.block.SoundingViewerBlock;
import dev.protomanly.pmweather.block.entity.SoundingViewerBlockEntity;
import dev.protomanly.pmweather.block.entity.WeatherPlatformBlockEntity;
import dev.protomanly.pmweather.weather.Sounding;
import dev.protomanly.pmweather.weather.ThermodynamicEngine;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec2;
import org.joml.Matrix4fStack;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;

public class SoundingViewerRenderer<T extends BlockEntity>
implements BlockEntityRenderer<T> {
    public int maxDisplayHeight = 16000;

    public SoundingViewerRenderer(BlockEntityRendererProvider.Context context) {
    }

    public void render(T blockEntity, float v, PoseStack poseStack, MultiBufferSource multiBufferSource, int j, int j1) {
        if (blockEntity instanceof SoundingViewerBlockEntity) {
            BlockEntity blockEntity1;
            SoundingViewerBlockEntity soundingViewerBlockEntity = (SoundingViewerBlockEntity)((Object)blockEntity);
            if (Minecraft.getInstance().player.position().distanceTo(blockEntity.getBlockPos().getCenter()) > 25.0) {
                return;
            }
            if (soundingViewerBlockEntity.isConnected && (blockEntity1 = blockEntity.getLevel().getBlockEntity(soundingViewerBlockEntity.connectedTo)) instanceof WeatherPlatformBlockEntity) {
                WeatherPlatformBlockEntity weatherPlatformBlockEntity = (WeatherPlatformBlockEntity)blockEntity1;
                if (weatherPlatformBlockEntity.sounding != null) {
                    BlockState state = blockEntity.getBlockState();
                    Direction direction = (Direction)state.getValue((Property)SoundingViewerBlock.FACING);
                    Matrix4fStack matrix4fStack = RenderSystem.getModelViewStack();
                    matrix4fStack.pushMatrix();
                    matrix4fStack.mul((Matrix4fc)poseStack.last().pose());
                    matrix4fStack.translate(0.5f, 0.5f, 0.5f);
                    Quaternionf rotation = switch (direction) {
                        case Direction.NORTH -> Axis.YP.rotationDegrees(180.0f);
                        case Direction.EAST -> Axis.YP.rotationDegrees(90.0f);
                        case Direction.WEST -> Axis.YP.rotationDegrees(270.0f);
                        default -> Axis.YP.rotationDegrees(0.0f);
                    };
                    matrix4fStack.rotate((Quaternionfc)rotation);
                    matrix4fStack.translate(0.0f, 0.0f, 0.55f);
                    RenderSystem.applyModelViewMatrix();
                    RenderSystem.enableBlend();
                    RenderSystem.depthMask((boolean)true);
                    RenderSystem.enableDepthTest();
                    RenderSystem.setShader(GameRenderer::getPositionColorShader);
                    RenderSystem.defaultBlendFunc();
                    Tesselator tesselator = Tesselator.getInstance();
                    BufferBuilder bufferBuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
                    float r = 0.0f;
                    float g = 0.0f;
                    float b = 0.0f;
                    float a = 1.0f;
                    Vector3f topLeft = new Vector3f(-1.0f, 1.0f, 0.0f).mul(1.0f);
                    Vector3f bottomLeft = new Vector3f(-1.0f, -1.0f, 0.0f).mul(1.0f);
                    Vector3f bottomRight = new Vector3f(1.0f, -1.0f, 0.0f).mul(1.0f);
                    Vector3f topRight = new Vector3f(1.0f, 1.0f, 0.0f).mul(1.0f);
                    bufferBuilder.addVertex(topLeft).setColor(r, g, b, a).addVertex(bottomLeft).setColor(r, g, b, a).addVertex(bottomRight).setColor(r, g, b, a).addVertex(topRight).setColor(r, g, b, a);
                    MeshData meshData = bufferBuilder.build();
                    if (meshData != null) {
                        BufferUploader.drawWithShader((MeshData)meshData);
                    }
                    Sounding sounding = weatherPlatformBlockEntity.sounding;
                    Sounding.Parcel parcel = sounding.getSBParcel();
                    List set = sounding.data.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList();
                    Vec2 lastTempPoint = null;
                    Vec2 lastVTempPoint = null;
                    Vec2 lastDewPoint = null;
                    Vec2 lastParcelPoint = null;
                    BufferBuilder lineBuilder = tesselator.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
                    float minPressure = 100.0f;
                    float maxPressure = 1050.0f;
                    for (int i = 100; i <= 1000; i += 100) {
                        Vec2 p = Sounding.getPosition(0.0f, i, minPressure, maxPressure, 40.0f);
                        lineBuilder.addVertex(-1.0f, p.y, 0.005f).setColor(0.4f, 0.4f, 0.4f, 1.0f);
                        lineBuilder.addVertex(1.0f, p.y, 0.005f).setColor(0.4f, 0.4f, 0.4f, 1.0f);
                    }
                    for (Map.Entry entry : set) {
                        int height = (Integer)entry.getKey();
                        ThermodynamicEngine.AtmosphericDataPoint atmosphericDataPoint = (ThermodynamicEngine.AtmosphericDataPoint)entry.getValue();
                        if (atmosphericDataPoint.pressure() < minPressure) break;
                        Vec2 p = Sounding.getPosition(atmosphericDataPoint.temperature(), atmosphericDataPoint.pressure(), minPressure, maxPressure, 40.0f);
                        if (lastTempPoint != null) {
                            lineBuilder.addVertex(lastTempPoint.x, lastTempPoint.y, 0.01f).setColor(1.0f, 0.0f, 0.0f, 1.0f);
                            lineBuilder.addVertex(p.x, p.y, 0.01f).setColor(1.0f, 0.0f, 0.0f, 1.0f);
                        }
                        lastTempPoint = p;
                        p = Sounding.getPosition(atmosphericDataPoint.virtualTemperature(), atmosphericDataPoint.pressure(), minPressure, maxPressure, 40.0f);
                        if (lastVTempPoint != null) {
                            lineBuilder.addVertex(lastVTempPoint.x, lastVTempPoint.y, 0.005f).setColor(0.3f, 0.0f, 0.0f, 1.0f);
                            lineBuilder.addVertex(p.x, p.y, 0.005f).setColor(0.3f, 0.0f, 0.0f, 1.0f);
                        }
                        lastVTempPoint = p;
                        p = Sounding.getPosition(atmosphericDataPoint.dewpoint(), atmosphericDataPoint.pressure(), minPressure, maxPressure, 40.0f);
                        if (lastDewPoint != null) {
                            lineBuilder.addVertex(lastDewPoint.x, lastDewPoint.y, 0.01f).setColor(0.0f, 1.0f, 0.0f, 1.0f);
                            lineBuilder.addVertex(p.x, p.y, 0.01f).setColor(0.0f, 1.0f, 0.0f, 1.0f);
                        }
                        lastDewPoint = p;
                        if (parcel == null) continue;
                        Float t = parcel.profile.getOrDefault(Float.valueOf(atmosphericDataPoint.pressure()), null);
                        p = Sounding.getPosition(t.floatValue(), atmosphericDataPoint.pressure(), minPressure, maxPressure, 40.0f);
                        if (lastParcelPoint != null && p.x > -1.0f) {
                            lineBuilder.addVertex(lastParcelPoint.x, lastParcelPoint.y, 0.02f).setColor(0.8f, 0.8f, 0.8f, 1.0f);
                            lineBuilder.addVertex(p.x, p.y, 0.02f).setColor(0.8f, 0.8f, 0.8f, 1.0f);
                        }
                        lastParcelPoint = p;
                    }
                    if (parcel != null) {
                        Vec2 p;
                        if (parcel.lclP > 0.0f) {
                            p = Sounding.getPosition(0.0f, parcel.lclP, minPressure, maxPressure, 40.0f);
                            lineBuilder.addVertex(0.95f, p.y, 0.01f).setColor(0.0f, 1.0f, 0.0f, 1.0f);
                            lineBuilder.addVertex(1.0f, p.y, 0.01f).setColor(0.0f, 1.0f, 0.0f, 1.0f);
                        }
                        if (parcel.lfcP > 0.0f) {
                            p = Sounding.getPosition(0.0f, parcel.lfcP, minPressure, maxPressure, 40.0f);
                            lineBuilder.addVertex(0.95f, p.y, 0.01f).setColor(1.0f, 1.0f, 0.0f, 1.0f);
                            lineBuilder.addVertex(1.0f, p.y, 0.01f).setColor(1.0f, 1.0f, 0.0f, 1.0f);
                        }
                        if (parcel.elP > 0.0f) {
                            p = Sounding.getPosition(0.0f, parcel.elP, minPressure, maxPressure, 40.0f);
                            lineBuilder.addVertex(0.95f, p.y, 0.01f).setColor(1.0f, 0.0f, 1.0f, 1.0f);
                            lineBuilder.addVertex(1.0f, p.y, 0.01f).setColor(1.0f, 0.0f, 1.0f, 1.0f);
                        }
                    }
                    matrix4fStack.mul((Matrix4fc)poseStack.last().pose().invert());
                    matrix4fStack.translate(-0.5f, -0.5f, -0.5f);
                    matrix4fStack.rotate((Quaternionfc)rotation.invert());
                    matrix4fStack.translate(0.0f, 0.0f, -0.55f);
                    matrix4fStack.popMatrix();
                    meshData = lineBuilder.build();
                    if (meshData != null) {
                        BufferUploader.drawWithShader((MeshData)meshData);
                    }
                    RenderSystem.applyModelViewMatrix();
                    RenderSystem.disableBlend();
                    RenderSystem.defaultBlendFunc();
                }
            }
        }
    }
}

