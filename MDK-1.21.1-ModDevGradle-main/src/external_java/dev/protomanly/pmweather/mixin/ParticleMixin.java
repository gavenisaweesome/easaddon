/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.particle.Particle
 *  net.minecraft.world.phys.Vec3
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 */
package dev.protomanly.pmweather.mixin;

import dev.protomanly.pmweather.interfaces.ParticleData;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value={Particle.class})
public class ParticleMixin
implements ParticleData {
    @Shadow
    protected double xd;
    @Shadow
    protected double yd;
    @Shadow
    protected double zd;

    @Override
    public Vec3 getVelocity() {
        return new Vec3(this.xd, this.yd, this.zd);
    }

    @Override
    public void addVelocity(Vec3 vec3) {
        this.xd += vec3.x;
        this.yd += vec3.y;
        this.zd += vec3.z;
    }

    @Override
    public void setVelocity(Vec3 vec3) {
        this.xd = vec3.x;
        this.yd = vec3.y;
        this.zd = vec3.z;
    }
}

