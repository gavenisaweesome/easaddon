/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.HolderGetter
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.NbtUtils
 *  net.minecraft.nbt.Tag
 *  net.minecraft.network.syncher.EntityDataAccessor
 *  net.minecraft.network.syncher.EntityDataSerializer
 *  net.minecraft.network.syncher.EntityDataSerializers
 *  net.minecraft.network.syncher.SynchedEntityData
 *  net.minecraft.network.syncher.SynchedEntityData$Builder
 *  net.minecraft.world.damagesource.DamageSource
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.Entity$MovementEmission
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.MoverType
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.levelgen.Heightmap$Types
 *  net.minecraft.world.phys.Vec3
 */
package dev.protomanly.pmweather.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

public class MovingBlock
extends Entity {
    public static EntityDataAccessor<BlockPos> DATA_START_POS = SynchedEntityData.defineId(MovingBlock.class, (EntityDataSerializer)EntityDataSerializers.BLOCK_POS);
    public static EntityDataAccessor<BlockState> DATA_BLOCK_STATE = SynchedEntityData.defineId(MovingBlock.class, (EntityDataSerializer)EntityDataSerializers.BLOCK_STATE);

    public MovingBlock(EntityType<? extends MovingBlock> entityType, Level level) {
        super(entityType, level);
        this.setBlockState(Blocks.STONE.defaultBlockState());
        this.setStartPos(BlockPos.ZERO);
    }

    public MovingBlock(EntityType<? extends MovingBlock> entityType, Level level, BlockState blockstate, BlockPos startPos) {
        super(entityType, level);
        this.setBlockState(blockstate);
        this.setStartPos(startPos);
    }

    public void tick() {
        super.tick();
        this.applyGravity();
        this.move(MoverType.SELF, this.getDeltaMovement());
        Vec3 motion = this.getDeltaMovement();
        if (!this.level().isClientSide()) {
            if (this.tickCount > 3600 || this.level().getNearestPlayer(this.getX(), this.getY(), this.getZ(), 96.0, false) == null) {
                this.discard();
                return;
            }
            if (this.onGround() && this.tickCount > 40) {
                if (this.level().getBlockState(this.blockPosition()).isAir()) {
                    this.level().setBlockAndUpdate(this.blockPosition(), this.getBlockState());
                }
                this.discard();
            }
        }
        if (this.onGround()) {
            BlockPos c = this.blockPosition();
            BlockPos n = c.north(2);
            BlockPos e = c.east(2);
            BlockPos s = c.south(2);
            BlockPos w = c.west(2);
            n = this.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, n);
            e = this.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, e);
            s = this.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, s);
            w = this.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, w);
            if (n.getY() < c.getY()) {
                c = n;
            }
            if (e.getY() < c.getY()) {
                c = e;
            }
            if (s.getY() < c.getY()) {
                c = s;
            }
            if (w.getY() < c.getY()) {
                c = w;
            }
            Vec3 off = this.getPosition(1.0f).subtract(c.getCenter()).multiply(1.0, 0.0, 1.0);
            motion = motion.add(off.multiply((double)0.05f, 0.0, (double)0.05f).multiply(1.0, 0.0, 1.0));
        }
        this.setDeltaMovement(motion.multiply((double)0.99f, (double)0.99f, (double)0.99f));
    }

    public void setStartPos(BlockPos pos) {
        this.entityData.set(DATA_START_POS, (Object)pos);
    }

    public BlockPos getStartPos() {
        return (BlockPos)this.entityData.get(DATA_START_POS);
    }

    public void setBlockState(BlockState state) {
        this.entityData.set(DATA_BLOCK_STATE, (Object)state);
    }

    public BlockState getBlockState() {
        return (BlockState)this.entityData.get(DATA_BLOCK_STATE);
    }

    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.NONE;
    }

    public boolean canBeCollidedWith() {
        return false;
    }

    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 327680.0;
    }

    public boolean isAttackable() {
        return false;
    }

    public boolean isPickable() {
        return false;
    }

    protected double getDefaultGravity() {
        return 0.04;
    }

    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_START_POS, (Object)BlockPos.ZERO);
        builder.define(DATA_BLOCK_STATE, (Object)Blocks.STONE.defaultBlockState());
    }

    protected void readAdditionalSaveData(CompoundTag compoundTag) {
        this.setBlockState(NbtUtils.readBlockState((HolderGetter)this.level().holderLookup(Registries.BLOCK), (CompoundTag)compoundTag.getCompound("blockstate")));
        this.setStartPos(NbtUtils.readBlockPos((CompoundTag)compoundTag, (String)"startPos").orElse(BlockPos.ZERO));
    }

    protected void addAdditionalSaveData(CompoundTag compoundTag) {
        compoundTag.put("blockstate", (Tag)NbtUtils.writeBlockState((BlockState)this.getBlockState()));
        compoundTag.put("startPos", NbtUtils.writeBlockPos((BlockPos)this.getStartPos()));
    }
}

