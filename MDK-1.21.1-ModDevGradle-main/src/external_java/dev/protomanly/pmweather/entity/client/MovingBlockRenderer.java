/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.math.Axis
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.client.renderer.block.BlockRenderDispatcher
 *  net.minecraft.client.renderer.entity.EntityRenderer
 *  net.minecraft.client.renderer.entity.EntityRendererProvider$Context
 *  net.minecraft.client.renderer.texture.OverlayTexture
 *  net.minecraft.client.renderer.texture.TextureAtlas
 *  net.minecraft.client.resources.model.BakedModel
 *  net.minecraft.core.BlockPos
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.util.RandomSource
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.level.BlockAndTintGetter
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.RenderShape
 *  net.minecraft.world.level.block.state.BlockState
 *  net.neoforged.neoforge.client.RenderTypeHelper
 *  net.neoforged.neoforge.client.model.data.ModelData
 *  org.jetbrains.annotations.NotNull
 */
package dev.protomanly.pmweather.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.protomanly.pmweather.entity.MovingBlock;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;

public class MovingBlockRenderer
extends EntityRenderer<MovingBlock> {
    private final BlockRenderDispatcher dispatcher;

    public MovingBlockRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5f;
        this.dispatcher = context.getBlockRenderDispatcher();
    }

    public void render(MovingBlock entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        Level level;
        BlockState blockstate = entity.getBlockState();
        float age = ((float)entity.tickCount + partialTicks) * 5.0f;
        if (blockstate.getRenderShape() == RenderShape.MODEL && blockstate != (level = entity.level()).getBlockState(entity.blockPosition()) && blockstate.getRenderShape() != RenderShape.INVISIBLE) {
            poseStack.pushPose();
            BlockPos pos = BlockPos.containing((double)entity.getX(), (double)entity.getBoundingBox().maxY, (double)entity.getZ());
            poseStack.mulPose(Axis.XP.rotationDegrees(age * 2.0f));
            poseStack.mulPose(Axis.YP.rotationDegrees(age * 2.0f));
            poseStack.mulPose(Axis.ZP.rotationDegrees(age * 2.0f));
            poseStack.translate(-0.5, 0.0, -0.5);
            BakedModel model = this.dispatcher.getBlockModel(blockstate);
            for (RenderType renderType : model.getRenderTypes(blockstate, RandomSource.create((long)blockstate.getSeed(entity.getStartPos())), ModelData.EMPTY)) {
                this.dispatcher.getModelRenderer().tesselateBlock((BlockAndTintGetter)level, model, blockstate, pos, poseStack, bufferSource.getBuffer(RenderTypeHelper.getMovingBlockRenderType((RenderType)renderType)), false, RandomSource.create(), blockstate.getSeed(entity.getStartPos()), OverlayTexture.NO_OVERLAY, ModelData.EMPTY, renderType);
            }
            poseStack.popPose();
            super.render((Entity)entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
        }
    }

    @NotNull
    public ResourceLocation getTextureLocation(@NotNull MovingBlock movingBlock) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}

