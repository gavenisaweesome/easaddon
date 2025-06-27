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
 *  net.minecraft.util.Mth
 *  net.minecraft.world.level.block.entity.BlockEntity
 */
package dev.protomanly.pmweather.render;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.protomanly.pmweather.PMWeather;
import dev.protomanly.pmweather.block.entity.AnemometerBlockEntity;
import dev.protomanly.pmweather.render.AnemometerModel;
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
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;

public class AnemometerRenderer<T extends BlockEntity>
implements BlockEntityRenderer<T> {
    private static Map<String, ResourceLocation> resLocMap = Maps.newHashMap();
    private static Map<String, Material> materialMap = Maps.newHashMap();
    protected final AnemometerModel model = new AnemometerModel(Minecraft.getInstance().getEntityModels().bakeLayer(AnemometerModel.LAYER_LOCATION));

    public static Material getMaterial(String path) {
        return materialMap.computeIfAbsent(path, m -> AnemometerRenderer.createMaterial(path));
    }

    public static Material createMaterial(String path) {
        return new Material(TextureAtlas.LOCATION_BLOCKS, AnemometerRenderer.getTexture(path));
    }

    public static ResourceLocation getTexture(String path) {
        return resLocMap.computeIfAbsent(path, k -> PMWeather.getPath(String.format("textures/block/%s.png", path)));
    }

    public static void renderModel(Material material, Model model, PoseStack poseStack, MultiBufferSource buffer, int combinedLightIn, int combinedOverlayIn) {
        model.renderToBuffer(poseStack, buffer.getBuffer(model.renderType(material.texture())), combinedLightIn, combinedOverlayIn, -1);
    }

    public AnemometerRenderer(BlockEntityRendererProvider.Context context) {
    }

    public void render(T blockEntity, float partialTicks, PoseStack poseStack, MultiBufferSource multiBufferSource, int combinedLightIn, int combinedOverlayIn) {
        this.model.tower.getAllParts().forEach(ModelPart::resetPose);
        this.model.shaft.getAllParts().forEach(ModelPart::resetPose);
        ModelPart mp = this.model.tower;
        mp.x += 8.0f;
        mp.z += 8.0f;
        mp.xRot += (float)Math.toRadians(180.0);
        mp.yRot += (float)Math.toRadians(180.0);
        mp.y += 2.0f;
        mp = this.model.shaft;
        mp.x += 8.0f;
        mp.z += 8.0f;
        mp.xRot += (float)Math.toRadians(180.0);
        mp.yRot += (float)Math.toRadians(180.0);
        mp.y += 2.0f;
        if (blockEntity instanceof AnemometerBlockEntity) {
            AnemometerBlockEntity anemometerBlockEntity = (AnemometerBlockEntity)((Object)blockEntity);
            float lerpAngle = Mth.lerp((float)partialTicks, (float)anemometerBlockEntity.prevSmoothAngle, (float)anemometerBlockEntity.smoothAngle);
            this.model.shaft.yRot = -((float)Math.toRadians(lerpAngle));
        }
        AnemometerRenderer.renderModel(AnemometerRenderer.getMaterial("anemometer"), (Model)this.model, poseStack, multiBufferSource, combinedLightIn, combinedOverlayIn);
    }
}

