/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Maps
 *  com.mojang.blaze3d.vertex.PoseStack
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.model.Model
 *  net.minecraft.client.model.geom.ModelPart
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.blockentity.BlockEntityRenderer
 *  net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider$Context
 *  net.minecraft.client.renderer.texture.TextureAtlas
 *  net.minecraft.client.resources.model.Material
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 */
package dev.protomanly.pmweather.render;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.protomanly.pmweather.PMWeather;
import dev.protomanly.pmweather.block.entity.WeatherPlatformBlockEntity;
import dev.protomanly.pmweather.render.WeatherBalloonModel;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class WeatherPlatformRenderer<T extends BlockEntity>
implements BlockEntityRenderer<T> {
    private static Map<String, ResourceLocation> resLocMap = Maps.newHashMap();
    private static Map<String, Material> materialMap = Maps.newHashMap();
    protected final WeatherBalloonModel model = new WeatherBalloonModel(Minecraft.getInstance().getEntityModels().bakeLayer(WeatherBalloonModel.LAYER_LOCATION));

    public static Material getMaterial(String path) {
        return materialMap.computeIfAbsent(path, m -> WeatherPlatformRenderer.createMaterial(path));
    }

    public boolean shouldRenderOffScreen(T blockEntity) {
        return true;
    }

    public AABB getRenderBoundingBox(T blockEntity) {
        return super.getRenderBoundingBox(blockEntity).inflate(256.0);
    }

    public int getViewDistance() {
        return 256;
    }

    public static Material createMaterial(String path) {
        return new Material(TextureAtlas.LOCATION_BLOCKS, WeatherPlatformRenderer.getTexture(path));
    }

    public static ResourceLocation getTexture(String path) {
        return resLocMap.computeIfAbsent(path, k -> PMWeather.getPath(String.format("textures/blockentity/%s.png", path)));
    }

    public static void renderModel(Material material, Model model, PoseStack poseStack, MultiBufferSource buffer, int combinedLightIn, int combinedOverlayIn) {
        model.renderToBuffer(poseStack, buffer.getBuffer(model.renderType(material.texture())), combinedLightIn, combinedOverlayIn, -1);
    }

    public WeatherPlatformRenderer(BlockEntityRendererProvider.Context context) {
    }

    public void render(T blockEntity, float partialTicks, PoseStack poseStack, MultiBufferSource multiBufferSource, int combinedLightIn, int combinedOverlayIn) {
        if (blockEntity instanceof WeatherPlatformBlockEntity) {
            WeatherPlatformBlockEntity weatherPlatformBlockEntity = (WeatherPlatformBlockEntity)((Object)blockEntity);
            if (weatherPlatformBlockEntity.active) {
                this.model.root.getAllParts().forEach(ModelPart::resetPose);
                ModelPart mp = this.model.root;
                Vec3 offset = weatherPlatformBlockEntity.position.subtract(weatherPlatformBlockEntity.getBlockPos().getCenter());
                mp.x += ((float)offset.x + 0.5f) * 16.0f;
                mp.z += ((float)offset.z + 0.5f) * 16.0f;
                mp.xRot += (float)Math.toRadians(180.0);
                mp.yRot += (float)Math.toRadians(offset.y);
                mp.y += ((float)offset.y + 2.5f) * 16.0f;
                mp.xScale = 1.25f;
                mp.yScale = 1.25f;
                mp.zScale = 1.25f;
                WeatherPlatformRenderer.renderModel(WeatherPlatformRenderer.getMaterial("weather_balloon"), (Model)this.model, poseStack, multiBufferSource, combinedLightIn, combinedOverlayIn);
            }
        }
    }
}

