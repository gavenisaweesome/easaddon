/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.HolderLookup$Provider
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.NbtUtils
 *  net.minecraft.network.protocol.Packet
 *  net.minecraft.network.protocol.game.ClientGamePacketListener
 *  net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.state.BlockState
 *  org.jetbrains.annotations.Nullable
 */
package dev.protomanly.pmweather.block.entity;

import dev.protomanly.pmweather.block.ModBlocks;
import dev.protomanly.pmweather.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class SoundingViewerBlockEntity
extends BlockEntity {
    public BlockPos connectedTo = BlockPos.ZERO;
    public boolean isConnected = false;

    public SoundingViewerBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.SOUNDING_VIEWER_BE.get(), pos, blockState);
    }

    public void connect(BlockPos to) {
        this.connectedTo = to;
        this.isConnected = true;
        this.setChanged();
    }

    public void tick(Level level, BlockPos blockPos, BlockState blockState) {
        BlockState stateAt;
        if (!level.isClientSide() && blockState.is(ModBlocks.SOUNDING_VIEWER) && this.isConnected && !(stateAt = level.getBlockState(this.connectedTo)).is(ModBlocks.WEATHER_PLATFORM)) {
            this.isConnected = false;
            this.setChanged();
        }
    }

    public void setChanged() {
        super.setChanged();
        this.getLevel().sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
    }

    @Nullable
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create((BlockEntity)this);
    }

    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("connectedTo", NbtUtils.writeBlockPos((BlockPos)this.connectedTo));
        tag.putBoolean("isConnected", this.isConnected);
    }

    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.connectedTo = NbtUtils.readBlockPos((CompoundTag)tag, (String)"connectedTo").orElse(BlockPos.ZERO);
        this.isConnected = tag.getBoolean("isConnected");
    }

    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag data = super.getUpdateTag(registries);
        this.saveAdditional(data, registries);
        return data;
    }
}

