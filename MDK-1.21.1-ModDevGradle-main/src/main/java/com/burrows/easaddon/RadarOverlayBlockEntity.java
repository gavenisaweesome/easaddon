package com.burrows.easaddon;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.HolderLookup;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public class RadarOverlayBlockEntity extends BlockEntity {
    
    public RadarOverlayBlockEntity(BlockPos pos, BlockState state) {
        super(RegistryHandler.RADAR_OVERLAY_BE.get(), pos, state);
    }

    // --- 1) write out polygons on the server ---
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookupProvider) {
        super.saveAdditional(tag, lookupProvider);
        AlertPolygonManager.writePolygons(this.worldPosition, tag);
    }

    // --- 2) read them back in (both world‐load and client‐sync) ---
    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider lookupProvider) {
        super.loadAdditional(tag, lookupProvider);

        if (tag.contains("polygons")) {
            AlertPolygonManager.readPolygons(this.worldPosition, tag);
        } else {
        }
    }

    // --- 3) include our data in the client‐sync tag ---
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider lookupProvider) {
        CompoundTag tag = super.getUpdateTag(lookupProvider);
        saveAdditional(tag, lookupProvider);
        
        // Debug: Check if polygons were actually written
        if (tag.contains("polygons")) {

        } else {

        }
        
        return tag;
    }

    // --- 4) when that packet arrives on the client, read our polygons back in ---
    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider lookupProvider) {
        super.onDataPacket(net, pkt, lookupProvider);

        CompoundTag tag = pkt.getTag();
        if (tag != null && tag.contains("polygons")) {

            AlertPolygonManager.readPolygons(this.worldPosition, tag);
            
            // Debug print current state after reading
            AlertPolygonManager.debugPrintState();
        } else {
            // FIXED: Clear polygons when server sends empty data
            AlertPolygonManager.clearPolygons(this.worldPosition);
        }
    }
    // --- helper to ask the server to re-send us the packet whenever something changes ---
    public void requestClientUpdate() {
        if (level != null) {
            // Debug: Check current polygon state before sending update
            int polyCount = AlertPolygonManager.getPolygonsAt(this.worldPosition).size();
            
            // use Block.UPDATE_CLIENTS to tell the engine to send the data packet
            level.sendBlockUpdated(this.worldPosition,
                                  this.getBlockState(),
                                  this.getBlockState(),
                                  Block.UPDATE_CLIENTS);

        } else {
        }
    }
    
    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);

        if (tag.contains("polygons")) {

            AlertPolygonManager.readPolygons(getBlockPos(), tag);
        } else {

            // FIXED: Clear polygons when server sends empty data
            AlertPolygonManager.clearPolygons(getBlockPos());
        }
    }
    @Override
    public void setRemoved() {
        super.setRemoved();

        AlertPolygonManager.clearPolygons(getBlockPos());
    }

    // --- ensure getUpdatePacket is wired up ---
    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {

        return ClientboundBlockEntityDataPacket.create(this);
    }
}