/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.Vec3
 */
package dev.protomanly.pmweather.block.entity;

import dev.protomanly.pmweather.block.entity.ModBlockEntities;
import dev.protomanly.pmweather.weather.WindEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class AnemometerBlockEntity
extends BlockEntity {
    public float smoothAngle = 0.0f;
    public float prevSmoothAngle = 0.0f;
    public float smoothAngleRotationalVel = 0.0f;

    public AnemometerBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.ANEMOMETER_BE.get(), pos, blockState);
    }

    public void tick(Level level, BlockPos blockPos, BlockState blockState) {
        if (level.isClientSide()) {
            double rotMax;
            Vec3 wind = WindEngine.getWind(blockPos, level);
            double windspeed = wind.length();
            double maxSpeed = windspeed / 30.0 * (rotMax = 50.0);
            if ((double)this.smoothAngleRotationalVel < maxSpeed) {
                this.smoothAngleRotationalVel += (float)windspeed / 100.0f;
            }
            if ((double)this.smoothAngleRotationalVel > rotMax) {
                this.smoothAngleRotationalVel = (float)rotMax;
            }
            if (this.smoothAngle >= 180.0f) {
                this.smoothAngle -= 360.0f;
            }
            this.prevSmoothAngle = this.smoothAngle;
            this.smoothAngle += this.smoothAngleRotationalVel;
            this.smoothAngleRotationalVel -= 0.01f;
            this.smoothAngleRotationalVel *= 0.99f;
            if (this.smoothAngleRotationalVel <= 0.0f) {
                this.smoothAngleRotationalVel = 0.0f;
            }
        }
    }
}

