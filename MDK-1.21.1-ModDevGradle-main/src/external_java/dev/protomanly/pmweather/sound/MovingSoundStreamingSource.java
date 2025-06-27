/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.client.resources.sounds.AbstractTickableSoundInstance
 *  net.minecraft.client.resources.sounds.SoundInstance
 *  net.minecraft.core.BlockPos
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.Vec3
 */
package dev.protomanly.pmweather.sound;

import dev.protomanly.pmweather.config.ServerConfig;
import dev.protomanly.pmweather.weather.Storm;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class MovingSoundStreamingSource
extends AbstractTickableSoundInstance {
    public Storm storm;
    public Vec3 realSource;
    public float cutOffRange = 128.0f;
    public boolean lockToPlayer = false;
    private float extraVolumeAdjForDistScale = 1.0f;
    private Block block;
    private BlockPos blockPos;
    private Level level;
    private int mode = 0;

    public MovingSoundStreamingSource(Vec3 pos, SoundEvent soundEvent, SoundSource soundSource, float volume, float pitch, boolean lockToPlayer) {
        super(soundEvent, soundSource, SoundInstance.createUnseededRandom());
        this.looping = false;
        this.volume = 0.1f;
        this.extraVolumeAdjForDistScale = volume;
        this.pitch = pitch;
        this.realSource = pos;
        this.lockToPlayer = lockToPlayer;
        this.tick();
    }

    public MovingSoundStreamingSource(Level level, BlockState block, BlockPos blockPos, SoundEvent soundEvent, SoundSource soundSource, float volume, float pitch, float cutOffRange) {
        super(soundEvent, soundSource, SoundInstance.createUnseededRandom());
        this.looping = false;
        this.volume = 0.1f;
        this.extraVolumeAdjForDistScale = volume;
        this.pitch = pitch;
        this.cutOffRange = cutOffRange;
        this.realSource = blockPos.getCenter();
        this.block = block.getBlock();
        this.blockPos = blockPos;
        this.level = level;
        this.tick();
    }

    public MovingSoundStreamingSource(Vec3 pos, SoundEvent soundEvent, SoundSource soundSource, float volume, float pitch, float cutOffRange) {
        super(soundEvent, soundSource, SoundInstance.createUnseededRandom());
        this.looping = false;
        this.volume = 0.1f;
        this.extraVolumeAdjForDistScale = volume;
        this.pitch = pitch;
        this.cutOffRange = cutOffRange;
        this.realSource = pos;
        this.tick();
    }

    public MovingSoundStreamingSource(Storm storm, SoundEvent soundEvent, SoundSource soundSource, float volume, float pitch, float cutOffRange, boolean looping, int mode) {
        super(soundEvent, soundSource, SoundInstance.createUnseededRandom());
        this.volume = 0.1f;
        this.extraVolumeAdjForDistScale = volume;
        this.pitch = pitch;
        this.cutOffRange = cutOffRange;
        this.storm = storm;
        this.looping = looping;
        this.mode = mode;
        this.tick();
    }

    public void stopPlaying() {
        this.stop();
    }

    public boolean canStartSilent() {
        return true;
    }

    public void tick() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            this.x = (float)player.getX();
            this.y = (float)player.getY();
            this.z = (float)player.getZ();
            this.volume = this.extraVolumeAdjForDistScale;
        }
        if (this.storm != null) {
            this.realSource = this.storm.position;
            if (this.mode == 0) {
                this.cutOffRange = (float)ServerConfig.stormSize / 1.5f;
                this.extraVolumeAdjForDistScale = this.storm.stage == 2 ? (float)this.storm.energy / 100.0f * 2.0f : 2.0f;
            } else if (this.mode == 1) {
                this.cutOffRange = Math.max(this.storm.width, 45.0f) * 0.85f;
                this.extraVolumeAdjForDistScale = (float)this.storm.windspeed / 40.0f;
            }
        }
        if (this.storm != null && this.storm.dead) {
            this.stop();
        }
        if (this.block != null && this.blockPos != null && this.level != null && !this.level.getBlockState(this.blockPos).is(this.block)) {
            this.stop();
        }
        if (!this.lockToPlayer && player != null) {
            double dist = this.realSource.distanceTo(player.position());
            this.volume = dist > (double)this.cutOffRange ? 0.0f : (float)(1.0 - dist / (double)this.cutOffRange) * this.extraVolumeAdjForDistScale;
        }
    }
}

