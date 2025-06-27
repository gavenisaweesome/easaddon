/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.renderer.texture.TextureAtlasSprite
 */
package dev.protomanly.pmweather.particle;

import dev.protomanly.pmweather.particle.EntityRotFX;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

public class ParticleTexFX
extends EntityRotFX {
    public ParticleTexFX(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, TextureAtlasSprite sprite) {
        super(level, x, y, z, xSpeed, ySpeed - 0.5, zSpeed);
        this.setSprite(sprite);
        this.setColor(1.0f, 1.0f, 1.0f);
        this.gravity = 1.0f;
        this.quadSize = 0.15f;
        this.setLifetime(100);
        this.setCanCollide(false);
    }
}

