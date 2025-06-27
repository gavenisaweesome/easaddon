/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.util.Mth
 *  net.minecraft.world.phys.Vec3
 */
package dev.protomanly.pmweather.weather;

import dev.protomanly.pmweather.PMWeather;
import dev.protomanly.pmweather.weather.Storm;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class Vorticy {
    public float windspeedMult = 0.0f;
    public float maxWindspeedMult;
    public float widthPerc;
    public float distancePerc;
    public float angle;
    public int lifetime;
    public int tickCount;
    public boolean dead = false;
    private Storm storm;

    public Vorticy(Storm storm, float maxWindspeedMult, float widthPerc, float distancePerc, int lifetime) {
        this.storm = storm;
        this.maxWindspeedMult = maxWindspeedMult;
        this.distancePerc = distancePerc;
        this.widthPerc = widthPerc;
        this.lifetime = lifetime;
        this.angle = PMWeather.RANDOM.nextFloat() * ((float)Math.PI * 2);
    }

    public void tick() {
        if (this.dead) {
            return;
        }
        ++this.tickCount;
        float lifeDelta = (float)this.tickCount / (float)this.lifetime;
        float wind = (float)this.storm.windspeed * (1.0f - this.distancePerc);
        float angleAdd = (float)Math.toRadians(wind / 300.0f);
        this.windspeedMult = (double)lifeDelta > 0.5 ? Mth.lerp((float)((lifeDelta - 0.5f) * 2.0f), (float)this.maxWindspeedMult, (float)0.0f) : Mth.lerp((float)(lifeDelta * 2.0f), (float)0.0f, (float)this.maxWindspeedMult);
        if (this.tickCount > this.lifetime) {
            this.dead = true;
        }
        this.angle += angleAdd;
        if (this.angle > (float)Math.PI * 2) {
            this.angle = 0.0f;
        }
    }

    public float getWidth() {
        return this.widthPerc * this.storm.width;
    }

    public float getDistance() {
        return this.distancePerc * this.storm.width;
    }

    public Vec3 getPosition() {
        float realDist = this.getDistance();
        return this.storm.position.add(new Vec3(Math.sin(this.angle) * (double)realDist, 0.0, Math.cos(this.angle) * (double)realDist));
    }
}

