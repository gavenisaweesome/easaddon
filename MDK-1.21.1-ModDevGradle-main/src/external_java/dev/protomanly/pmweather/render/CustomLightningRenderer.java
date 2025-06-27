/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.GlStateManager$DestFactor
 *  com.mojang.blaze3d.platform.GlStateManager$SourceFactor
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.blaze3d.vertex.BufferBuilder
 *  com.mojang.blaze3d.vertex.BufferUploader
 *  com.mojang.blaze3d.vertex.DefaultVertexFormat
 *  com.mojang.blaze3d.vertex.MeshData
 *  com.mojang.blaze3d.vertex.Tesselator
 *  com.mojang.blaze3d.vertex.VertexFormat$Mode
 *  net.minecraft.client.Camera
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.renderer.GameRenderer
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Matrix4fStack
 */
package dev.protomanly.pmweather.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.protomanly.pmweather.config.ServerConfig;
import java.awt.Color;
import java.util.Random;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fStack;

public class CustomLightningRenderer {
    public static void render(Vec3 pos, long seed, Camera camera, Color color) {
        if (Minecraft.getInstance().player == null) {
            return;
        }
        Random rand = new Random(seed);
        Vec3 camPos = camera.getPosition();
        Vec3 offset = pos.subtract(camPos);
        Matrix4fStack matrix4fStack = RenderSystem.getModelViewStack();
        matrix4fStack.pushMatrix();
        matrix4fStack.translate((float)offset.x, (float)offset.y, (float)offset.z);
        RenderSystem.applyModelViewMatrix();
        RenderSystem.enableBlend();
        RenderSystem.depthMask((boolean)true);
        RenderSystem.enableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.blendFunc((GlStateManager.SourceFactor)GlStateManager.SourceFactor.SRC_ALPHA, (GlStateManager.DestFactor)GlStateManager.DestFactor.ONE);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        Vec3 lastPos = new Vec3(0.0, ServerConfig.layer0Height * 2.0 - pos.y, 0.0);
        float r = 3.0f;
        while (lastPos.y > 0.0 && r > 0.1f) {
            float r2;
            Vec3 newPos = lastPos.add((double)rand.nextFloat(-6.0f, 6.0f), (double)rand.nextFloat(-20.0f, 0.0f), (double)rand.nextFloat(-6.0f, 6.0f));
            CustomLightningRenderer.box(bufferBuilder, color, newPos.add((double)(-1.0f * r), 0.0, (double)(-1.0f * r)), newPos.add((double)(-1.0f * r), 0.0, (double)(1.0f * r)), newPos.add((double)(1.0f * r), 0.0, (double)(1.0f * r)), newPos.add((double)(1.0f * r), 0.0, (double)(-1.0f * r)), lastPos.add((double)(-1.0f * r), 0.0, (double)(-1.0f * r)), lastPos.add((double)(-1.0f * r), 0.0, (double)(1.0f * r)), lastPos.add((double)(1.0f * r), 0.0, (double)(1.0f * r)), lastPos.add((double)(1.0f * r), 0.0, (double)(-1.0f * r)));
            lastPos = newPos;
            if (rand.nextInt(8) != 0 || !((r2 = r * 0.8f) > 0.1f)) continue;
            r *= 0.6f;
            Vec3 lP = lastPos;
            int n = rand.nextInt(3, 10);
            for (int i = 0; i < n; ++i) {
                Vec3 nP = lP.add((double)(rand.nextFloat(-50.0f, 50.0f) / (float)(i + 1)), (double)rand.nextFloat(-10.0f, 5.0f), (double)(rand.nextFloat(-50.0f, 50.0f) / (float)(i + 1)));
                CustomLightningRenderer.box(bufferBuilder, color, nP.add((double)(-1.0f * r2), 0.0, (double)(-1.0f * r2)), nP.add((double)(-1.0f * r2), 0.0, (double)(1.0f * r2)), nP.add((double)(1.0f * r2), 0.0, (double)(1.0f * r2)), nP.add((double)(1.0f * r2), 0.0, (double)(-1.0f * r2)), lP.add((double)(-1.0f * r2), 0.0, (double)(-1.0f * r2)), lP.add((double)(-1.0f * r2), 0.0, (double)(1.0f * r2)), lP.add((double)(1.0f * r2), 0.0, (double)(1.0f * r2)), lP.add((double)(1.0f * r2), 0.0, (double)(-1.0f * r2)));
                lP = nP;
            }
        }
        matrix4fStack.translate(-((float)offset.x), -((float)offset.y), -((float)offset.z));
        matrix4fStack.popMatrix();
        MeshData meshData = bufferBuilder.build();
        if (meshData != null) {
            BufferUploader.drawWithShader((MeshData)meshData);
        }
        RenderSystem.applyModelViewMatrix();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    }

    public static void box(BufferBuilder bufferBuilder, Color color, Vec3 btl, Vec3 bbl, Vec3 bbr, Vec3 btr, Vec3 ttl, Vec3 tbl, Vec3 tbr, Vec3 ttr) {
        CustomLightningRenderer.quad(bufferBuilder, color, btl, bbl, bbr, btr, true);
        CustomLightningRenderer.quad(bufferBuilder, color, ttl, tbl, tbr, ttr, false);
        CustomLightningRenderer.quad(bufferBuilder, color, ttl, btl, btr, ttr, false);
        CustomLightningRenderer.quad(bufferBuilder, color, ttl, btl, bbl, tbl, false);
        CustomLightningRenderer.quad(bufferBuilder, color, tbr, bbr, btr, ttr, false);
        CustomLightningRenderer.quad(bufferBuilder, color, tbr, bbr, bbl, tbl, false);
    }

    public static void quad(BufferBuilder bufferBuilder, Color color, Vec3 tl, Vec3 bl, Vec3 br, Vec3 tr, boolean clockwise) {
        float r = (float)color.getRed() / 255.0f;
        float g = (float)color.getGreen() / 255.0f;
        float b = (float)color.getBlue() / 255.0f;
        float a = (float)color.getAlpha() / 255.0f;
        if (clockwise) {
            bufferBuilder.addVertex(tr.toVector3f()).setColor(r, g, b, a);
            bufferBuilder.addVertex(br.toVector3f()).setColor(r, g, b, a);
            bufferBuilder.addVertex(bl.toVector3f()).setColor(r, g, b, a);
            bufferBuilder.addVertex(tl.toVector3f()).setColor(r, g, b, a);
        } else {
            bufferBuilder.addVertex(tl.toVector3f()).setColor(r, g, b, a);
            bufferBuilder.addVertex(bl.toVector3f()).setColor(r, g, b, a);
            bufferBuilder.addVertex(br.toVector3f()).setColor(r, g, b, a);
            bufferBuilder.addVertex(tr.toVector3f()).setColor(r, g, b, a);
        }
    }
}

