/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.NbtUtils
 *  net.minecraft.nbt.Tag
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.Vec3
 */
package dev.protomanly.pmweather.weather;

import dev.protomanly.pmweather.networking.ModNetworking;
import dev.protomanly.pmweather.util.CachedNBTTagCompound;
import dev.protomanly.pmweather.weather.Storm;
import dev.protomanly.pmweather.weather.WeatherHandler;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class WeatherHandlerServer
extends WeatherHandler {
    private final ServerLevel level;

    public WeatherHandlerServer(ServerLevel level) {
        super((ResourceKey<Level>)level.dimension());
        this.level = level;
        this.seed = level.getSeed();
    }

    @Override
    public Level getWorld() {
        return this.level;
    }

    public void syncStormRemove(Storm storm) {
        CompoundTag data = new CompoundTag();
        data.putString("packetCommand", "WeatherData");
        data.putString("command", "syncStormRemove");
        storm.nbtSyncForClient();
        data.put("data", (Tag)storm.getNBTCache().getNewNBT());
        data.getCompound("data").putBoolean("removed", true);
        ModNetworking.serverSendToClientDimension(data, this.getWorld());
    }

    public void syncStormNew(Storm storm) {
        this.syncStormNew(storm, null);
    }

    public void syncLightningNew(Vec3 pos) {
        CompoundTag data = new CompoundTag();
        data.putString("packetCommand", "WeatherData");
        data.putString("command", "syncLightningNew");
        CompoundTag compoundTag = new CompoundTag();
        compoundTag.putDouble("positionX", pos.x);
        compoundTag.putDouble("positionY", pos.y);
        compoundTag.putDouble("positionZ", pos.z);
        data.put("data", (Tag)compoundTag);
        ModNetworking.serverSendToClientNear(data, pos, 1024.0, (Level)this.level);
    }

    public void syncStormNew(Storm storm, @Nullable ServerPlayer player) {
        CompoundTag data = new CompoundTag();
        data.putString("packetCommand", "WeatherData");
        data.putString("command", "syncStormNew");
        CachedNBTTagCompound cache = storm.getNBTCache();
        cache.setUpdateForced(true);
        storm.nbtSyncForClient();
        cache.setUpdateForced(false);
        data.put("data", (Tag)cache.getNewNBT());
        if (player == null) {
            ModNetworking.serverSendToClientAll(data);
        } else {
            ModNetworking.serverSendToClientPlayer(data, (Player)player);
        }
    }

    public void syncStormUpdate(Storm storm) {
        CompoundTag data = new CompoundTag();
        data.putString("packetCommand", "WeatherData");
        data.putString("command", "syncStormUpdate");
        storm.getNBTCache().setNewNBT(new CompoundTag());
        storm.nbtSyncForClient();
        data.put("data", (Tag)storm.getNBTCache().getNewNBT());
        ModNetworking.serverSendToClientDimension(data, this.getWorld());
    }

    public void playerJoinedWorldSyncFull(ServerPlayer player) {
        Level lvl = this.getWorld();
        if (lvl instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)lvl;
            CompoundTag data = new CompoundTag();
            data.putString("packetCommand", "LevelData");
            data.putString("command", "syncMisc");
            data.putLong("seed", this.seed);
            ModNetworking.serverSendToClientPlayer(data, (Player)player);
            for (Storm storm : this.getStorms()) {
                this.syncStormNew(storm, player);
            }
        }
    }

    public void clearAllStorms() {
        for (Storm storm : this.getStorms()) {
            storm.remove();
            this.syncStormRemove(storm);
        }
        this.getStorms().clear();
        this.lookupStormByID.clear();
    }

    public void syncBlockParticleNew(BlockPos pos, BlockState state, Storm storm) {
        CompoundTag data = new CompoundTag();
        data.putString("packetCommand", "WeatherData");
        data.putString("command", "syncBlockParticleNew");
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("positionX", pos.getX());
        nbt.putInt("positionY", pos.getY());
        nbt.putInt("positionZ", pos.getZ());
        nbt.put("blockstate", (Tag)NbtUtils.writeBlockState((BlockState)state));
        nbt.putLong("stormID", storm.ID);
        data.put("data", (Tag)nbt);
        ModNetworking.serverSendToClientNear(data, new Vec3((double)pos.getX(), (double)pos.getY(), (double)pos.getZ()), 356.0, (Level)this.level);
    }
}

