/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.phys.Vec3
 */
package dev.protomanly.pmweather.particle.behavior;

import dev.protomanly.pmweather.PMWeather;
import dev.protomanly.pmweather.particle.EntityRotFX;
import dev.protomanly.pmweather.particle.ParticleTexExtraRender;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class ParticleBehavior {
    public List<EntityRotFX> particles = new ArrayList<EntityRotFX>();
    public Vec3 coordSource;
    public Entity sourceEntity;
    public float rateDarken = 0.025f;
    public float rateBrighten = 0.01f;
    public float rateBrightenSlower = 0.003f;
    public float rateAlpha = 0.002f;
    public float rateScale = 0.1f;
    public int tickSmokifyTrigger = 40;
    float vanillaRainRed = 0.7f;
    float vanillaRainGreen = 0.7f;
    float vanillaRainBlue = 1.0f;

    public ParticleBehavior(Vec3 coordSource) {
        this.coordSource = coordSource;
    }

    public EntityRotFX initParticle(EntityRotFX particle) {
        particle.setPrevPosX(particle.getX());
        particle.setPrevPosY(particle.getY());
        particle.setPrevPosZ(particle.getZ());
        particle.setSize(0.01f, 0.01f);
        return particle;
    }

    public void initParticleRain(EntityRotFX particle, int extraRenderCount) {
        if (particle instanceof ParticleTexExtraRender) {
            ParticleTexExtraRender particleTexExtraRender = (ParticleTexExtraRender)particle;
            particleTexExtraRender.extraParticlesBaseAmount = extraRenderCount;
        }
        particle.killWhenUnderTopmostBlock = true;
        particle.setCanCollide(false);
        particle.killWhenUnderCameraAtLeast = 5;
        particle.dontRenderUnderTopmostBlock = true;
        particle.fastLight = true;
        particle.slantParticleToWind = true;
        particle.facePlayer = false;
        particle.setScale(0.3f);
        particle.isTransparent = true;
        particle.setGravity(1.8f);
        particle.setLifetime(50);
        particle.ticksFadeInMax = 5.0f;
        particle.ticksFadeOutMax = 5.0f;
        particle.ticksFadeOutMaxOnDeath = 3.0f;
        particle.setAlpha(0.0f);
        particle.rotationYaw = (float)PMWeather.RANDOM.nextInt(360) - 180.0f;
        particle.setMotionY(-0.5);
        particle.setColor(this.vanillaRainRed, this.vanillaRainGreen, this.vanillaRainBlue);
        particle.spawnAsWeatherEffect();
    }

    public void initParticleGroundSplash(EntityRotFX particle) {
        particle.killWhenUnderTopmostBlock = true;
        particle.setCanCollide(false);
        particle.killWhenUnderCameraAtLeast = 5;
        particle.facePlayer = true;
        particle.setScale(0.2f + PMWeather.RANDOM.nextFloat() * 0.05f);
        particle.setLifetime(15);
        particle.setGravity(1.0f);
        particle.ticksFadeInMax = 3.0f;
        particle.ticksFadeOutMax = 4.0f;
        particle.setAlpha(0.0f);
        particle.renderOrder = 2;
        particle.rotationYaw = (float)PMWeather.RANDOM.nextInt(360) - 180.0f;
        particle.rotationPitch = 90.0f;
        particle.setMotionY(PMWeather.RANDOM.nextFloat() * 0.2f);
        particle.setMotionX((PMWeather.RANDOM.nextFloat() - 0.5f) * 0.01f);
        particle.setMotionZ((PMWeather.RANDOM.nextFloat() - 0.5f) * 0.01f);
        particle.setColor(this.vanillaRainRed, this.vanillaRainGreen, this.vanillaRainBlue);
    }

    public void initParticleSnow(EntityRotFX particle, int extraRenderCount, float windSpeed) {
        if (particle instanceof ParticleTexExtraRender) {
            ParticleTexExtraRender particleTexExtraRender = (ParticleTexExtraRender)particle;
            particleTexExtraRender.extraParticlesBaseAmount = extraRenderCount;
        }
        float windScale = Math.max(0.1f, 1.0f - windSpeed);
        particle.setCanCollide(false);
        particle.ticksFadeOutMaxOnDeath = 5.0f;
        particle.dontRenderUnderTopmostBlock = true;
        particle.killWhenUnderTopmostBlock = true;
        particle.killWhenFarFromCameraAtLeast = 25;
        particle.setMotionX(0.0);
        particle.setMotionY(0.0);
        particle.setMotionZ(0.0);
        particle.setScale(0.19500001f + PMWeather.RANDOM.nextFloat() * 0.05f);
        particle.setGravity(0.05f + PMWeather.RANDOM.nextFloat() * 0.1f);
        particle.setLifetime((int)(1440.0f * windScale));
        particle.facePlayer = true;
        particle.ticksFadeInMax = 40.0f * windScale;
        particle.ticksFadeOutMax = 40.0f * windScale;
        particle.ticksFadeOutMaxOnDeath = 10.0f;
        particle.setAlpha(0.0f);
        particle.rotationYaw = (float)PMWeather.RANDOM.nextInt(360) - 180.0f;
    }

    public void initParticleSleet(EntityRotFX particle, int extraRenderCount) {
        if (particle instanceof ParticleTexExtraRender) {
            ParticleTexExtraRender particleTexExtraRender = (ParticleTexExtraRender)particle;
            particleTexExtraRender.extraParticlesBaseAmount = extraRenderCount;
        }
        particle.setCanCollide(true);
        particle.bounceOnVerticalImpact = true;
        particle.bounceOnVerticalImpactEnergy = 0.2f;
        particle.ticksFadeOutMaxOnDeath = 5.0f;
        particle.dontRenderUnderTopmostBlock = true;
        particle.killWhenFarFromCameraAtLeast = 25;
        particle.setMotionX(0.0);
        particle.setMotionY(0.0);
        particle.setMotionZ(0.0);
        particle.setScale(0.3f);
        particle.setGravity(1.2f);
        particle.setLifetime(50);
        particle.facePlayer = true;
        particle.ticksFadeInMax = 5.0f;
        particle.ticksFadeOutMax = 5.0f;
        particle.ticksFadeOutMaxOnDeath = 10.0f;
        particle.setAlpha(0.0f);
        particle.rotationYaw = (float)PMWeather.RANDOM.nextInt(360) - 180.0f;
    }

    public void initParticleHail(EntityRotFX particle) {
        particle.killWhenUnderTopmostBlock = false;
        particle.setCanCollide(true);
        particle.killOnCollide = true;
        particle.killWhenUnderCameraAtLeast = 5;
        particle.dontRenderUnderTopmostBlock = true;
        particle.rotationYaw = PMWeather.RANDOM.nextInt(360);
        particle.rotationPitch = PMWeather.RANDOM.nextInt(360);
        particle.fastLight = true;
        particle.slantParticleToWind = true;
        particle.windWeight = 1.5f;
        particle.ignoreWind = false;
        particle.spinFast = true;
        particle.spinFastRate = 10.0f;
        particle.facePlayer = false;
        particle.setScale(0.105000004f);
        particle.isTransparent = false;
        particle.setGravity(3.5f);
        particle.ticksFadeInMax = 5.0f;
        particle.ticksFadeOutMax = 5.0f;
        particle.ticksFadeOutMaxOnDeath = 50.0f;
        particle.fullAlphaTarget = 1.0f;
        particle.setAlpha(0.0f);
        particle.rotationYaw = PMWeather.RANDOM.nextInt(360) - 180;
        particle.setMotionY(-0.5);
        particle.setColor(0.9f, 0.9f, 0.9f);
        particle.bounceOnVerticalImpact = true;
        particle.bounceOnVerticalImpactEnergy = 0.3f;
    }

    public void initParticleCube(EntityRotFX particle) {
        particle.killWhenUnderTopmostBlock = false;
        particle.setCanCollide(true);
        particle.killOnCollide = true;
        particle.killOnCollideActivateAtAge = 30;
        particle.killWhenUnderCameraAtLeast = 0;
        particle.dontRenderUnderTopmostBlock = true;
        particle.rotationPitch = PMWeather.RANDOM.nextInt(360);
        particle.fastLight = true;
        particle.ignoreWind = true;
        particle.spinFast = true;
        particle.spinFastRate = 1.0f;
        particle.facePlayer = false;
        particle.setScale(0.45f);
        particle.isTransparent = false;
        particle.setGravity(0.5f);
        particle.setLifetime(400);
        particle.ticksFadeInMax = 5.0f;
        particle.ticksFadeOutMax = 5.0f;
        particle.ticksFadeOutMaxOnDeath = 20.0f;
        particle.fullAlphaTarget = 1.0f;
        particle.setAlpha(0.0f);
        particle.rotationYaw = PMWeather.RANDOM.nextInt(360) - 180;
        particle.vanillaMotionDampen = true;
        particle.bounceOnVerticalImpact = true;
        particle.bounceOnVerticalImpactEnergy = 0.3f;
    }
}

