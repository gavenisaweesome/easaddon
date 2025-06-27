/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  com.mojang.math.Axis
 *  net.minecraft.client.Camera
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.particle.ParticleRenderType
 *  net.minecraft.client.renderer.block.BlockRenderDispatcher
 *  net.minecraft.client.renderer.block.model.BakedQuad
 *  net.minecraft.client.renderer.texture.TextureAtlasSprite
 *  net.minecraft.client.resources.model.BakedModel
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.util.Mth
 *  net.minecraft.util.RandomSource
 *  net.minecraft.world.level.BlockAndTintGetter
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Quaternionf
 *  org.joml.Quaternionfc
 *  org.joml.Vector3f
 */
package dev.protomanly.pmweather.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.protomanly.pmweather.PMWeather;
import dev.protomanly.pmweather.particle.ParticleRegistry;
import dev.protomanly.pmweather.particle.ParticleTexFX;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;

public class ParticleCube
extends ParticleTexFX {
    public ParticleCube(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, BlockState state) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed, ParticleRegistry.rain);
        TextureAtlasSprite sprite1 = this.getSpriteFromState(state);
        if (sprite1 != null) {
            this.setSprite(sprite1);
        } else {
            PMWeather.LOGGER.warn("Unable to find sprite from block {}", (Object)state);
            sprite1 = this.getSpriteFromState(Blocks.OAK_PLANKS.defaultBlockState());
            if (sprite1 != null) {
                this.setSprite(sprite1);
            }
        }
        int multiplier = Minecraft.getInstance().getBlockColors().getColor(state, (BlockAndTintGetter)this.level, new BlockPos((int)x, (int)y, (int)z), 0);
        float mr = (float)(multiplier >>> 16 & 0xFF) / 255.0f;
        float mg = (float)(multiplier >>> 8 & 0xFF) / 255.0f;
        float mb = (float)(multiplier & 0xFF) / 255.0f;
        this.setColor(mr, mg, mb);
    }

    public TextureAtlasSprite getSpriteFromState(BlockState state) {
        BlockRenderDispatcher blockRenderDispatcher = Minecraft.getInstance().getBlockRenderer();
        BakedModel model = blockRenderDispatcher.getBlockModel(state);
        int n = 0;
        Direction[] directionArray = Direction.values();
        int n2 = directionArray.length;
        if (n < n2) {
            Direction direction = directionArray[n];
            List list = model.getQuads(state, direction, RandomSource.create());
            if (!list.isEmpty()) {
                return ((BakedQuad)list.getFirst()).getSprite();
            }
            return model.getParticleIcon();
        }
        return null;
    }

    @Override
    public void render(VertexConsumer buffer, Camera renderInfo, float partialTicks) {
        Quaternionf quaternion;
        Vec3 vec3d = renderInfo.getPosition();
        float f = (float)(Mth.lerp((double)partialTicks, (double)this.xo, (double)this.x) - vec3d.x());
        float f1 = (float)(Mth.lerp((double)partialTicks, (double)this.yo, (double)this.y) - vec3d.y());
        float f2 = (float)(Mth.lerp((double)partialTicks, (double)this.zo, (double)this.z) - vec3d.z());
        if (this.facePlayer || this.rotationPitch == 0.0f && this.rotationYaw == 0.0f) {
            quaternion = renderInfo.rotation();
        } else {
            quaternion = new Quaternionf(0.0f, 0.0f, 0.0f, 1.0f);
            if (this.facePlayerYaw) {
                quaternion.mul((Quaternionfc)Axis.YP.rotationDegrees(-renderInfo.getYRot()));
            } else {
                quaternion.mul((Quaternionfc)Axis.YP.rotationDegrees(Mth.lerp((float)this.rotationSpeedAroundCenter, (float)this.prevRotationYaw, (float)this.rotationYaw)));
            }
            quaternion.mul((Quaternionfc)Axis.XP.rotationDegrees(Mth.lerp((float)partialTicks, (float)this.prevRotationPitch, (float)this.rotationPitch)));
        }
        ArrayList<Vector3f[]> faces = new ArrayList<Vector3f[]>();
        Vector3f[] face = new Vector3f[]{new Vector3f(-1.0f, -1.0f, -1.0f), new Vector3f(-1.0f, 1.0f, -1.0f), new Vector3f(1.0f, 1.0f, -1.0f), new Vector3f(1.0f, -1.0f, -1.0f)};
        faces.add(face);
        face = new Vector3f[]{new Vector3f(-1.0f, -1.0f, 1.0f), new Vector3f(-1.0f, 1.0f, 1.0f), new Vector3f(1.0f, 1.0f, 1.0f), new Vector3f(1.0f, -1.0f, 1.0f)};
        faces.add(face);
        face = new Vector3f[]{new Vector3f(-1.0f, -1.0f, -1.0f), new Vector3f(-1.0f, 1.0f, -1.0f), new Vector3f(-1.0f, 1.0f, 1.0f), new Vector3f(-1.0f, -1.0f, 1.0f)};
        faces.add(face);
        face = new Vector3f[]{new Vector3f(1.0f, -1.0f, -1.0f), new Vector3f(1.0f, 1.0f, -1.0f), new Vector3f(1.0f, 1.0f, 1.0f), new Vector3f(1.0f, -1.0f, 1.0f)};
        faces.add(face);
        face = new Vector3f[]{new Vector3f(-1.0f, -1.0f, -1.0f), new Vector3f(-1.0f, -1.0f, 1.0f), new Vector3f(1.0f, -1.0f, 1.0f), new Vector3f(1.0f, -1.0f, -1.0f)};
        faces.add(face);
        face = new Vector3f[]{new Vector3f(-1.0f, 1.0f, -1.0f), new Vector3f(-1.0f, 1.0f, 1.0f), new Vector3f(1.0f, 1.0f, 1.0f), new Vector3f(1.0f, 1.0f, -1.0f)};
        faces.add(face);
        float f4 = this.getQuadSize(partialTicks);
        for (Vector3f[] entryFace : faces) {
            for (int i = 0; i < 4; ++i) {
                entryFace[i].rotate((Quaternionfc)quaternion);
                entryFace[i].mul(f4);
                entryFace[i].add(f, f1, f2);
            }
        }
        float u0 = this.getU0();
        float u1 = this.getU1();
        float v0 = this.getV0();
        float v1 = this.getV1();
        int j = this.getLightColor(partialTicks);
        if (j > 0) {
            this.lastNonZeroBrightness = j;
        } else {
            j = this.lastNonZeroBrightness;
        }
        for (Vector3f[] entryFace : faces) {
            buffer.addVertex(entryFace[0].x(), entryFace[0].y(), entryFace[0].z()).setUv(u1, v1).setColor(this.rCol, this.gCol, this.bCol, this.alpha).setLight(j);
            buffer.addVertex(entryFace[1].x(), entryFace[1].y(), entryFace[1].z()).setUv(u1, v0).setColor(this.rCol, this.gCol, this.bCol, this.alpha).setLight(j);
            buffer.addVertex(entryFace[2].x(), entryFace[2].y(), entryFace[2].z()).setUv(u0, v0).setColor(this.rCol, this.gCol, this.bCol, this.alpha).setLight(j);
            buffer.addVertex(entryFace[3].x(), entryFace[3].y(), entryFace[3].z()).setUv(u0, v1).setColor(this.rCol, this.gCol, this.bCol, this.alpha).setLight(j);
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return SORTED_OPAQUE_BLOCK;
    }
}

