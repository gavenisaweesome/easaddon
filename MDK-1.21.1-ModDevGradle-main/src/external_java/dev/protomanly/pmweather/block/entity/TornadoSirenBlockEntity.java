/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.state.BlockState
 */
package dev.protomanly.pmweather.block.entity;

import dev.protomanly.pmweather.block.entity.ModBlockEntities;
import dev.protomanly.pmweather.config.ClientConfig;
import dev.protomanly.pmweather.config.ServerConfig;
import dev.protomanly.pmweather.event.GameBusClientEvents;
import dev.protomanly.pmweather.sound.ModSounds;
import dev.protomanly.pmweather.weather.Storm;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class TornadoSirenBlockEntity
extends BlockEntity {
    private long lastSirenSound = 0L;

    public TornadoSirenBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.TORNADO_SIREN_BE.get(), pos, blockState);
    }

    public void tick(Level level, BlockPos blockPos, BlockState blockState) {
        if (level.getGameTime() % 20L == 0L && level.isClientSide() && System.currentTimeMillis() > this.lastSirenSound) {
            boolean nearTornado = false;
            for (Storm storm : GameBusClientEvents.weatherHandler.getStorms()) {
                double dist;
                if (level != storm.level || !((dist = blockPos.getCenter().multiply(1.0, 0.0, 1.0).distanceTo(storm.position.multiply(1.0, 0.0, 1.0))) < ServerConfig.stormSize * (double)1.15f) || storm.stage < 3 || storm.stormType != 0) continue;
                nearTornado = true;
                break;
            }
            if (nearTornado) {
                this.lastSirenSound = System.currentTimeMillis() + 120000L;
                ModSounds.playBlockSound(level, blockState, blockPos, (SoundEvent)ModSounds.SIREN.value(), (float)ClientConfig.sirenVolume, 1.0f, 120.0f);
            }
        }
    }
}

