/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.phys.Vec3
 */
package dev.protomanly.pmweather.interfaces;

import net.minecraft.world.phys.Vec3;

public interface ParticleData {
    public Vec3 getVelocity();

    public void addVelocity(Vec3 var1);

    public void setVelocity(Vec3 var1);
}

