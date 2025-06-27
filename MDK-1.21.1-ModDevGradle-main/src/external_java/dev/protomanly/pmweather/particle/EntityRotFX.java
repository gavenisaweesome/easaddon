/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.blaze3d.vertex.BufferBuilder
 *  com.mojang.blaze3d.vertex.DefaultVertexFormat
 *  com.mojang.blaze3d.vertex.Tesselator
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  com.mojang.blaze3d.vertex.VertexFormat$Mode
 *  com.mojang.math.Axis
 *  net.minecraft.client.Camera
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.particle.Particle
 *  net.minecraft.client.particle.ParticleRenderType
 *  net.minecraft.client.particle.TextureSheetParticle
 *  net.minecraft.client.renderer.GameRenderer
 *  net.minecraft.client.renderer.texture.TextureAtlas
 *  net.minecraft.client.renderer.texture.TextureAtlasSprite
 *  net.minecraft.client.renderer.texture.TextureManager
 *  net.minecraft.core.BlockPos
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.util.Mth
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.levelgen.Heightmap$Types
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.NotNull
 *  org.joml.Quaternionf
 *  org.joml.Quaternionfc
 *  org.joml.Vector3f
 */
package dev.protomanly.pmweather.particle;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import dev.protomanly.pmweather.PMWeather;
import dev.protomanly.pmweather.config.ClientConfig;
import dev.protomanly.pmweather.event.GameBusClientEvents;
import dev.protomanly.pmweather.particle.behavior.ParticleBehavior;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;

public class EntityRotFX
extends TextureSheetParticle {
    public static final ParticleRenderType SORTED_TRANSLUCENT = new ParticleRenderType(){

        @NotNull
        public BufferBuilder begin(Tesselator tesselator, @NotNull TextureManager textureManager) {
            RenderSystem.disableCull();
            RenderSystem.depthMask((boolean)false);
            RenderSystem.setShaderTexture((int)0, (ResourceLocation)TextureAtlas.LOCATION_PARTICLES);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            return tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }

        public String toString() {
            return "PARTICLE_SHEET_SORTED_TRANSLUCENT";
        }
    };
    public static final ParticleRenderType SORTED_OPAQUE_BLOCK = new ParticleRenderType(){

        @NotNull
        public BufferBuilder begin(Tesselator tesselator, @NotNull TextureManager textureManager) {
            RenderSystem.disableBlend();
            RenderSystem.depthMask((boolean)true);
            RenderSystem.setShader(GameRenderer::getParticleShader);
            RenderSystem.setShaderTexture((int)0, (ResourceLocation)TextureAtlas.LOCATION_BLOCKS);
            return tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }

        public String toString() {
            return "PARTICLE_BLOCK_SHEET_SORTED_OPAQUE";
        }
    };
    public float renderRange = 128.0f;
    public int renderOrder = 0;
    public float windWeight = 1.0f;
    public int entityID = 0;
    public float prevRotationYaw;
    public float rotationYaw;
    public float prevRotationPitch;
    public float rotationPitch;
    public float rotationRoll;
    public boolean isTransparent = true;
    public boolean weatherEffect = false;
    public boolean killOnCollide = false;
    public int killOnCollideActivateAtAge = 0;
    public boolean dontRenderUnderTopmostBlock = false;
    public boolean killWhenUnderTopmostBlock = false;
    public int killWhenUnderTopmostBlock_ScanAheadRange = 0;
    public int killWhenUnderCameraAtLeast = 0;
    public int killWhenFarFromCameraAtLeast = 0;
    public boolean facePlayer = false;
    public boolean facePlayerYaw = false;
    public boolean spinFast = false;
    public float spinFastRate = 10.0f;
    public boolean spinTowardsMotionDirection = false;
    public float ticksFadeInMax = 0.0f;
    public float ticksFadeOutMax = 0.0f;
    public float ticksFadeOutMaxOnDeath = -1.0f;
    public float ticksFadeOutCurOnDeath = 0.0f;
    public boolean fadingOut = false;
    public float fullAlphaTarget = 1.0f;
    public float rotationAroundCenter = 0.0f;
    public float rotationSpeedAroundCenter = 0.0f;
    public boolean slantParticleToWind = false;
    public boolean fastLight = false;
    protected int lastNonZeroBrightness = 0xF00000;
    public ParticleBehavior particleBehavior = null;
    public boolean useCustomBBForRenderCulling = false;
    public static final AABB INITIAL_AABB = new AABB(Vec3.ZERO, Vec3.ZERO);
    public AABB bbRender = INITIAL_AABB;
    public boolean vanillaMotionDampen = true;
    public boolean collisionSpeedDampen = true;
    public boolean collidingHorizontally = false;
    public boolean collidingDownwards = false;
    public boolean collidingUpwards = false;
    public boolean markCollided = false;
    public boolean bounceOnVerticalImpact = false;
    public float bounceOnVerticalImpactEnergy = 0.3f;
    public boolean ignoreWind = false;
    public ParticleRenderType renderType = SORTED_TRANSLUCENT;

    public EntityRotFX(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.setSize(0.3f, 0.3f);
        this.entityID = PMWeather.RANDOM.nextInt(100000);
    }

    public ParticleRenderType getRenderType() {
        return this.renderType;
    }

    public void remove() {
        if (this.particleBehavior != null) {
            this.particleBehavior.particles.remove((Object)this);
        }
        super.remove();
    }

    public void tick() {
        super.tick();
        this.prevRotationPitch = this.rotationPitch;
        this.prevRotationYaw = this.rotationYaw;
        Entity cam = Minecraft.getInstance().getCameraEntity();
        if (!this.vanillaMotionDampen) {
            this.xd /= (double)0.98f;
            this.yd /= (double)0.98f;
            this.zd /= (double)0.98f;
        }
        if (!this.removed && !this.fadingOut) {
            int height;
            if (this.killOnCollide && (this.killOnCollideActivateAtAge == 0 || this.age > this.killOnCollideActivateAtAge) && this.isColliding()) {
                this.startDeath();
            }
            BlockPos pos = new BlockPos((int)this.x, (int)this.y, (int)this.z);
            if (this.killWhenUnderTopmostBlock && this.y - (double)this.killWhenUnderTopmostBlock_ScanAheadRange <= (double)(height = this.level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos).getY())) {
                this.startDeath();
            }
            if (this.killWhenUnderCameraAtLeast != 0 && cam != null && this.y < cam.getY() - (double)this.killWhenUnderCameraAtLeast) {
                this.startDeath();
            }
            if (this.killWhenFarFromCameraAtLeast != 0 && cam != null && this.age > 20 && this.age % 5 == 0 && cam.distanceToSqr(this.x, this.y, this.z) > (double)(this.killWhenFarFromCameraAtLeast * this.killWhenFarFromCameraAtLeast)) {
                this.startDeath();
            }
        }
        if (!this.collisionSpeedDampen && this.onGround) {
            this.xd /= (double)0.7f;
            this.zd /= (double)0.7f;
        }
        double speedXZ = Math.sqrt(this.getMotionX() * this.getMotionX() + this.getMotionZ() * this.getMotionZ());
        double spinFastRateAdj = (double)this.spinFastRate * speedXZ * 10.0;
        if (this.spinFast) {
            this.rotationPitch += (float)(this.entityID % 2 == 0 ? spinFastRateAdj : -spinFastRateAdj);
            this.rotationYaw += (float)(this.entityID % 2 == 0 ? -spinFastRateAdj : spinFastRateAdj);
        }
        float angleToMovement = (float)Math.toDegrees(Math.atan2(this.xd, this.zd));
        if (this.spinTowardsMotionDirection) {
            this.rotationYaw = angleToMovement;
            this.rotationPitch += this.spinFastRate;
        }
        if (!this.fadingOut) {
            if (this.ticksFadeInMax > 0.0f && (float)this.age < this.ticksFadeInMax) {
                this.setAlpha((float)this.age / this.ticksFadeInMax * this.fullAlphaTarget);
            } else if (this.ticksFadeOutMax > 0.0f && (float)this.age > (float)this.lifetime - this.ticksFadeOutMax) {
                float count = (float)this.getAge() - ((float)this.getLifetime() - this.ticksFadeOutMax);
                float val = (this.ticksFadeOutMax - count) / this.ticksFadeOutMax;
                this.setAlpha(val * this.fullAlphaTarget);
            } else if (this.ticksFadeInMax > 0.0f || this.ticksFadeOutMax > 0.0f) {
                this.setAlpha(this.fullAlphaTarget);
            }
        } else {
            if (this.ticksFadeOutCurOnDeath < this.ticksFadeOutMaxOnDeath) {
                this.ticksFadeOutCurOnDeath += 1.0f;
            } else {
                this.remove();
            }
            float val = 1.0f - this.ticksFadeOutCurOnDeath / this.ticksFadeOutMaxOnDeath;
            this.setAlpha(val * this.fullAlphaTarget);
        }
        this.rotationAroundCenter += this.rotationSpeedAroundCenter;
        this.rotationAroundCenter %= 360.0f;
        this.tickExtraRotations();
    }

    public void tickExtraRotations() {
        if (this.slantParticleToWind) {
            double motionXZ = Math.sqrt(this.xd * this.xd + this.zd * this.zd);
            this.rotationPitch = (float)Math.atan2(this.yd, motionXZ);
        }
    }

    public void render(VertexConsumer buffer, Camera renderInfo, float partialTicks) {
        Quaternionf quaternion;
        Vec3 vec3d = renderInfo.getPosition();
        float f = (float)(Mth.lerp((double)partialTicks, (double)this.xo, (double)this.x) - vec3d.x());
        float f1 = (float)(Mth.lerp((double)partialTicks, (double)this.yo, (double)this.y) - vec3d.y());
        float f2 = (float)(Mth.lerp((double)partialTicks, (double)this.zo, (double)this.z) - vec3d.z());
        if (this.facePlayer || this.rotationPitch == 0.0f && this.rotationYaw == 0.0f) {
            try {
                quaternion = (Quaternionf)renderInfo.rotation().clone();
                quaternion.mul((Quaternionfc)Axis.ZP.rotationDegrees(this.rotationRoll));
            }
            catch (CloneNotSupportedException e) {
                quaternion = renderInfo.rotation();
            }
        } else {
            quaternion = new Quaternionf(0.0f, 0.0f, 0.0f, 1.0f);
            if (this.facePlayerYaw) {
                quaternion.mul((Quaternionfc)Axis.YP.rotationDegrees(-renderInfo.getYRot()));
            } else {
                quaternion.mul((Quaternionfc)Axis.YP.rotationDegrees(Mth.lerp((float)partialTicks, (float)this.prevRotationYaw, (float)this.rotationYaw)));
            }
            quaternion.mul((Quaternionfc)Axis.XP.rotationDegrees(Mth.lerp((float)partialTicks, (float)this.prevRotationPitch, (float)this.rotationPitch)));
        }
        Vector3f[] v3f = new Vector3f[]{new Vector3f(-1.0f, -1.0f, 0.0f), new Vector3f(-1.0f, 1.0f, 0.0f), new Vector3f(1.0f, 1.0f, 0.0f), new Vector3f(1.0f, -1.0f, 0.0f)};
        float scale = this.getQuadSize(partialTicks);
        for (int i = 0; i < 4; ++i) {
            Vector3f vector3f = v3f[i];
            vector3f.rotate((Quaternionfc)quaternion);
            vector3f.mul(scale);
            vector3f.add(f, f1, f2);
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
        buffer.addVertex(v3f[0].x, v3f[0].y, v3f[0].z).setUv(u1, v1).setColor(this.rCol, this.gCol, this.bCol, this.alpha).setLight(j);
        buffer.addVertex(v3f[1].x, v3f[1].y, v3f[1].z).setUv(u1, v0).setColor(this.rCol, this.gCol, this.bCol, this.alpha).setLight(j);
        buffer.addVertex(v3f[2].x, v3f[2].y, v3f[2].z).setUv(u0, v0).setColor(this.rCol, this.gCol, this.bCol, this.alpha).setLight(j);
        buffer.addVertex(v3f[3].x, v3f[3].y, v3f[3].z).setUv(u0, v1).setColor(this.rCol, this.gCol, this.bCol, this.alpha).setLight(j);
    }

    public void move(double x, double y, double z) {
        double xx = x;
        double yy = y;
        double zz = z;
        if (this.hasPhysics && (x != 0.0 || y != 0.0 || z != 0.0)) {
            Vec3 vec3d = Entity.collideBoundingBox(null, (Vec3)new Vec3(x, y, z), (AABB)this.getBoundingBox(), (Level)this.level, List.of());
            x = vec3d.x;
            y = vec3d.y;
            z = vec3d.z;
        }
        if (x != 0.0 || y != 0.0 || z != 0.0) {
            this.setBoundingBox(this.getBoundingBox().move(x, y, z));
            if (this.useCustomBBForRenderCulling) {
                this.bbRender = this.getBoundingBoxForRender().move(x, y, z);
            }
            this.setLocationFromBoundingbox();
        }
        this.onGround = yy != y && yy < 0.0;
        this.collidingHorizontally = xx != x || zz != z;
        this.collidingDownwards = yy < y;
        boolean bl = this.collidingUpwards = yy > y;
        if (xx != x) {
            this.xd = 0.0;
        }
        if (zz != z) {
            this.zd = 0.0;
        }
        if (!this.markCollided) {
            if (this.onGround || this.collidingDownwards || this.collidingHorizontally || this.collidingUpwards) {
                this.onHit();
                this.markCollided = true;
            }
            if (this.bounceOnVerticalImpact && (this.onGround || this.collidingDownwards)) {
                this.setMotionY(-this.getMotionY() * (double)this.bounceOnVerticalImpactEnergy);
            }
        }
    }

    public void onHit() {
    }

    public void setSprite(@NotNull TextureAtlasSprite sprite) {
        this.sprite = sprite;
    }

    public void spawnAsWeatherEffect() {
        this.weatherEffect = true;
        if (ClientConfig.customParticles) {
            GameBusClientEvents.particleManager.add((Particle)this);
        } else {
            Minecraft.getInstance().particleEngine.add((Particle)this);
        }
    }

    public void spawnAsDebrisEffect() {
        this.weatherEffect = true;
        if (ClientConfig.customParticles) {
            GameBusClientEvents.particleManagerDebris.add((Particle)this);
        } else {
            Minecraft.getInstance().particleEngine.add((Particle)this);
        }
    }

    public AABB getBoundingBoxForRender() {
        if (this.useCustomBBForRenderCulling) {
            return this.bbRender;
        }
        return this.getBoundingBox();
    }

    public void setSizeForRenderCulling(float width, float height) {
        if (width != this.bbWidth || height != this.bbHeight) {
            this.bbWidth = width;
            this.bbHeight = height;
            AABB aabb = this.getBoundingBox();
            double d0 = (aabb.minX + aabb.maxX - (double)width) / 2.0;
            double d1 = (aabb.minZ + aabb.maxZ - (double)width) / 2.0;
            this.bbRender = new AABB(d0, aabb.minY, d1, d0 + (double)this.bbWidth, aabb.minY + (double)this.bbHeight, d1 + (double)this.bbWidth);
        }
    }

    public void startDeath() {
        if (this.ticksFadeOutMaxOnDeath > 0.0f) {
            this.ticksFadeOutCurOnDeath = 0.0f;
            this.fadingOut = true;
        } else {
            this.remove();
        }
    }

    public boolean isColliding() {
        return this.onGround || this.collidingHorizontally;
    }

    public Vec3 getPivotedPosition() {
        return Vec3.ZERO;
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public double getZ() {
        return this.z;
    }

    public int getAge() {
        return this.age;
    }

    public void setMotionX(double val) {
        this.xd = val;
    }

    public void setMotionY(double val) {
        this.yd = val;
    }

    public void setMotionZ(double val) {
        this.zd = val;
    }

    public double getMotionX() {
        return this.xd;
    }

    public double getMotionY() {
        return this.yd;
    }

    public double getMotionZ() {
        return this.zd;
    }

    public void setGravity(float val) {
        this.gravity = val;
    }

    public void setAlpha(float val) {
        this.alpha = val;
    }

    public void setScale(float val) {
        this.setSizeForRenderCulling(val, val);
        this.quadSize = val;
    }

    public void setPrevPosX(double val) {
        this.xo = val;
    }

    public void setPrevPosY(double val) {
        this.yo = val;
    }

    public void setPrevPosZ(double val) {
        this.zo = val;
    }

    public void setSize(float width, float height) {
        super.setSize(width, height);
        this.setPos(this.x, this.y, this.z);
    }

    public void setCanCollide(boolean val) {
        this.hasPhysics = val;
    }
}

