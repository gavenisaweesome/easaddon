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

public class AnemometerModel<T extends Entity>
extends HierarchicalModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(PMWeather.getPath("anemometer"), "main");
    public final ModelPart tower;
    public final ModelPart shaft;

    public AnemometerModel(ModelPart root) {
        this.tower = root.getChild("tower");
        this.shaft = root.getChild("shaft");
    }

    public ModelPart root() {
        return this.shaft;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        PartDefinition tower = partdefinition.addOrReplaceChild("tower", CubeListBuilder.create().texOffs(0, 17).addBox(-1.0f, 7.0f, -1.0f, 2.0f, 6.0f, 2.0f, new CubeDeformation(0.0f)).texOffs(22, 21).addBox(-1.0f, 0.0f, 0.0f, 2.0f, 7.0f, 0.0f, new CubeDeformation(0.0f)).texOffs(18, 17).addBox(0.0f, 0.0f, -1.0f, 0.0f, 7.0f, 2.0f, new CubeDeformation(0.0f)), PartPose.offset((float)0.0f, (float)11.0f, (float)0.0f));
        PartDefinition shaft = partdefinition.addOrReplaceChild("shaft", CubeListBuilder.create().texOffs(22, 17).addBox(-1.0f, -2.0f, -1.0f, 2.0f, 2.0f, 2.0f, new CubeDeformation(0.0f)).texOffs(-1, 16).addBox(-8.0f, -1.0f, -0.5f, 16.0f, 0.0f, 1.0f, new CubeDeformation(0.0f)).texOffs(-1, 0).addBox(-0.5f, -1.0f, -8.0f, 1.0f, 0.0f, 16.0f, new CubeDeformation(0.0f)).texOffs(8, 17).addBox(-2.5f, -2.5f, 5.0f, 2.0f, 3.0f, 3.0f, new CubeDeformation(0.0f)), PartPose.offset((float)0.0f, (float)11.0f, (float)0.0f));
        PartDefinition cup_r1 = shaft.addOrReplaceChild("cup_r1", CubeListBuilder.create().texOffs(8, 17).addBox(-2.5f, -2.0f, 5.0f, 2.0f, 3.0f, 3.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)0.0f, (float)-0.5f, (float)0.0f, (float)0.0f, (float)1.5708f, (float)0.0f));
        PartDefinition cup_r2 = shaft.addOrReplaceChild("cup_r2", CubeListBuilder.create().texOffs(8, 17).addBox(-2.5f, -2.0f, 5.0f, 2.0f, 3.0f, 3.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)0.0f, (float)-0.5f, (float)0.0f, (float)0.0f, (float)-1.5708f, (float)0.0f));
        PartDefinition cup_r3 = shaft.addOrReplaceChild("cup_r3", CubeListBuilder.create().texOffs(8, 17).addBox(-2.5f, -2.0f, 5.0f, 2.0f, 3.0f, 3.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)0.0f, (float)-0.5f, (float)0.0f, (float)0.0f, (float)3.1416f, (float)0.0f));
        return LayerDefinition.create((MeshDefinition)meshdefinition, (int)32, (int)32);
    }

    public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
    }

    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
        this.tower.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        this.shaft.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
}

