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
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.player.Player
 */
package dev.protomanly.pmweather.networking;

import dev.protomanly.pmweather.PMWeather;
import dev.protomanly.pmweather.block.entity.RadarBlockEntity;
import dev.protomanly.pmweather.event.GameBusEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public record PacketNBTFromClient(CompoundTag compoundTag) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<PacketNBTFromClient> TYPE = new CustomPacketPayload.Type(PMWeather.getPath("nbt_server"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketNBTFromClient> STREAM_CODEC = StreamCodec.composite((StreamCodec)ByteBufCodecs.COMPOUND_TAG, PacketNBTFromClient::compoundTag, PacketNBTFromClient::new);

    public PacketNBTFromClient(RegistryFriendlyByteBuf buf) {
        this(buf.readNbt());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeNbt((Tag)this.compoundTag);
    }

    public void handle(Player player) {
        try {
            if (player instanceof ServerPlayer) {
                ServerPlayer serverPlayer = (ServerPlayer)player;
                String packetCommand = this.compoundTag.getString("packetCommand");
                String command = this.compoundTag.getString("command");
                if (packetCommand.equals("WeatherData")) {
                    if (command.equals("syncFull")) {
                        GameBusEvents.playerRequestsFullSync(serverPlayer);
                    }
                } else if (packetCommand.equals("Radar") && command.equals("syncBiomes")) {
                    RadarBlockEntity.playerRequestsSync(serverPlayer, NbtUtils.readBlockPos((CompoundTag)this.compoundTag, (String)"blockPos").orElse(BlockPos.ZERO));
                }
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

