/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.NbtUtils
 *  net.minecraft.nbt.Tag
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.network.RegistryFriendlyByteBuf
 *  net.minecraft.network.codec.ByteBufCodecs
 *  net.minecraft.network.codec.StreamCodec
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload$Type
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.state.BlockState
 */
package dev.protomanly.pmweather.networking;

import dev.protomanly.pmweather.PMWeather;
import dev.protomanly.pmweather.block.MetarBlock;
import dev.protomanly.pmweather.block.ModBlocks;
import dev.protomanly.pmweather.block.entity.RadarBlockEntity;
import dev.protomanly.pmweather.block.entity.WeatherPlatformBlockEntity;
import dev.protomanly.pmweather.event.GameBusClientEvents;
import dev.protomanly.pmweather.weather.Sounding;
import dev.protomanly.pmweather.weather.ThermodynamicEngine;
import dev.protomanly.pmweather.weather.WeatherHandlerClient;
import dev.protomanly.pmweather.weather.WindEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public record PacketNBTFromServer(CompoundTag compoundTag) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<PacketNBTFromServer> TYPE = new CustomPacketPayload.Type(PMWeather.getPath("nbt_client"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketNBTFromServer> STREAM_CODEC = StreamCodec.composite((StreamCodec)ByteBufCodecs.COMPOUND_TAG, PacketNBTFromServer::compoundTag, PacketNBTFromServer::new);

    public PacketNBTFromServer(RegistryFriendlyByteBuf buf) {
        this(buf.readNbt());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeNbt((Tag)this.compoundTag);
    }

    public void handle(Player player) {
        try {
            String packetCommand = this.compoundTag.getString("packetCommand");
            String command = this.compoundTag.getString("command");
            GameBusClientEvents.getClientWeather();
            WeatherHandlerClient weatherHandler = (WeatherHandlerClient)GameBusClientEvents.weatherHandler;
            if (packetCommand.equals("WeatherData")) {
                weatherHandler.nbtSyncFromServer(this.compoundTag);
            } else if (packetCommand.equals("LevelData")) {
                if (command.equals("syncMisc")) {
                    weatherHandler.seed = this.compoundTag.getLong("seed");
                    WindEngine.init(weatherHandler);
                    ThermodynamicEngine.noise = WindEngine.simplexNoise;
                }
            } else if (packetCommand.equals("Radar")) {
                if (command.equals("syncBiomes")) {
                    BlockEntity blockEntity;
                    BlockPos blockPos = NbtUtils.readBlockPos((CompoundTag)this.compoundTag, (String)"blockPos").orElse(BlockPos.ZERO);
                    Level level = player.level();
                    BlockState state = level.getBlockState(blockPos);
                    if (state.is(ModBlocks.RADAR) && state.hasBlockEntity() && (blockEntity = level.getBlockEntity(blockPos)) instanceof RadarBlockEntity) {
                        RadarBlockEntity radarBlockEntity = (RadarBlockEntity)blockEntity;
                        radarBlockEntity.clientInit(level, this.compoundTag);
                    }
                }
            } else if (packetCommand.equals("WeatherPlatform")) {
                if (command.equals("sync")) {
                    BlockEntity blockEntity;
                    CompoundTag data = this.compoundTag.getCompound("data");
                    BlockPos blockPos = NbtUtils.readBlockPos((CompoundTag)data, (String)"blockPos").orElse(BlockPos.ZERO);
                    Level level = player.level();
                    BlockState state = level.getBlockState(blockPos);
                    if (state.is(ModBlocks.WEATHER_PLATFORM) && state.hasBlockEntity() && (blockEntity = level.getBlockEntity(blockPos)) instanceof WeatherPlatformBlockEntity) {
                        WeatherPlatformBlockEntity weatherPlatformBlockEntity = (WeatherPlatformBlockEntity)blockEntity;
                        weatherPlatformBlockEntity.deserializeNBT(data);
                    }
                } else if (command.equals("syncSounding")) {
                    BlockEntity blockEntity;
                    CompoundTag data = this.compoundTag.getCompound("data");
                    BlockPos blockPos = NbtUtils.readBlockPos((CompoundTag)this.compoundTag, (String)"blockPos").orElse(BlockPos.ZERO);
                    Level level = player.level();
                    BlockState state = level.getBlockState(blockPos);
                    if (state.is(ModBlocks.WEATHER_PLATFORM) && state.hasBlockEntity() && (blockEntity = level.getBlockEntity(blockPos)) instanceof WeatherPlatformBlockEntity) {
                        WeatherPlatformBlockEntity weatherPlatformBlockEntity = (WeatherPlatformBlockEntity)blockEntity;
                        weatherPlatformBlockEntity.sounding = new Sounding(GameBusClientEvents.weatherHandler, data, blockPos.getCenter());
                    }
                }
            } else if (packetCommand.equals("Metar") && command.equals("sendData")) {
                MetarBlock.sendMessage(this.compoundTag);
            }
        }
        catch (Exception e) {
            PMWeather.LOGGER.error(e.getMessage(), (Throwable)e);
        }
    }

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

