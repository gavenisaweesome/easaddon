/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.Vec3
 */
package dev.protomanly.pmweather.weather;

import dev.protomanly.pmweather.PMWeather;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class Lightning {
    public long seed;
    public Vec3 position;
    public Level level;
    public boolean dead;
    public int ticks = 0;
    public int lifetime;

    public Lightning(Vec3 position, Level level) {
        this.position = position;
        this.level = level;
        this.seed = PMWeather.RANDOM.nextLong();
        this.dead = false;
        this.lifetime = PMWeather.RANDOM.nextInt(5, 20);
    }

    public void tick() {
        if (!this.dead) {
            ++this.ticks;
            if (this.ticks > this.lifetime) {
                this.dead = true;
            }
        }
    }
}

