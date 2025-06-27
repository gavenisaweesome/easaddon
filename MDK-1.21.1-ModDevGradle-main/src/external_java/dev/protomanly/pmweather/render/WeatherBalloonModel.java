/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  net.minecraft.client.model.HierarchicalModel
 *  net.minecraft.client.model.geom.ModelLayerLocation
 *  net.minecraft.client.model.geom.ModelPart
 *  net.minecraft.client.model.geom.PartPose
 *  net.minecraft.client.model.geom.builders.CubeDeformation
 *  net.minecraft.client.model.geom.builders.CubeListBuilder
 *  net.minecraft.client.model.geom.builders.LayerDefinition
 *  net.minecraft.client.model.geom.builders.MeshDefinition
 *  net.minecraft.client.model.geom.builders.PartDefinition
 *  net.minecraft.world.entity.Entity
 */
package dev.protomanly.pmweather.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.protomanly.pmweather.PMWeather;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.Entity;

public class WeatherBalloonModel<T extends Entity>
extends HierarchicalModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(PMWeather.getPath("weather_balloon"), "main");
    public final ModelPart root;

    public WeatherBalloonModel(ModelPart root) {
        this.root = root.getChild("root");
    }

    public ModelPart root() {
        return this.root;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create().texOffs(64, 64).addBox(-8.0f, 37.0f, -8.0f, 16.0f, 9.0f, 16.0f, new CubeDeformation(0.0f)).texOffs(128, 39).addBox(-1.5f, 14.0f, -1.5f, 3.0f, 20.0f, 3.0f, new CubeDeformation(0.0f)).texOffs(0, 64).addBox(-8.0f, 7.0f, -8.0f, 16.0f, 30.0f, 16.0f, new CubeDeformation(0.0f)).texOffs(0, 110).addBox(-8.0f, 0.0f, -8.0f, 16.0f, 7.0f, 16.0f, new CubeDeformation(0.2f)).texOffs(64, 112).addBox(-8.0f, 37.0f, -8.0f, 16.0f, 4.0f, 16.0f, new CubeDeformation(0.2f)).texOffs(0, 0).addBox(-16.0f, -32.0f, -16.0f, 32.0f, 32.0f, 32.0f, new CubeDeformation(0.0f)).texOffs(64, 89).addBox(-8.0f, 0.0f, -8.0f, 16.0f, 7.0f, 16.0f, new CubeDeformation(0.0f)), PartPose.offset((float)0.0f, (float)24.0f, (float)0.0f));
        PartDefinition knot_r1 = root.addOrReplaceChild("knot_r1", CubeListBuilder.create().texOffs(128, 71).addBox(0.0f, -2.5f, -3.0f, 0.0f, 3.0f, 6.0f, new CubeDeformation(0.0f)).texOffs(128, 0).addBox(0.0f, -2.5f, -11.5f, 0.0f, 8.0f, 23.0f, new CubeDeformation(0.0f)).texOffs(128, 31).addBox(-11.5f, -2.5f, 0.0f, 23.0f, 8.0f, 0.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)0.0f, (float)9.5f, (float)0.0f, (float)0.0f, (float)0.7854f, (float)0.0f));
        PartDefinition knot_r2 = root.addOrReplaceChild("knot_r2", CubeListBuilder.create().texOffs(128, 62).addBox(0.0f, -2.5f, -3.0f, 0.0f, 3.0f, 6.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)0.0f, (float)9.5f, (float)0.0f, (float)0.0f, (float)-0.7854f, (float)0.0f));
        return LayerDefinition.create((MeshDefinition)meshdefinition, (int)176, (int)176);
    }

    public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
    }

    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
        this.root.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
}

