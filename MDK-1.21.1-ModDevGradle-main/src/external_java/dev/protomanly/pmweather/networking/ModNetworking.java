/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.network.codec.StreamCodec
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload$Type
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.Vec3
 *  net.neoforged.neoforge.network.PacketDistributor
 *  net.neoforged.neoforge.network.handling.IPayloadHandler
 *  net.neoforged.neoforge.network.registration.PayloadRegistrar
 */
package dev.protomanly.pmweather.networking;

import dev.protomanly.pmweather.PMWeather;
import dev.protomanly.pmweather.networking.PacketNBTFromClient;
import dev.protomanly.pmweather.networking.PacketNBTFromServer;
import java.util.function.BiConsumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModNetworking {
    public static final ResourceLocation NBT_PACKET_ID = PMWeather.getPath("nbt");

    public static void register(Object ... args) {
        ModNetworking.registerClientboundPacket(PacketNBTFromServer.TYPE, PacketNBTFromServer.STREAM_CODEC, PacketNBTFromServer::handle, args);
        ModNetworking.registerServerboundPacket(PacketNBTFromClient.TYPE, PacketNBTFromClient.STREAM_CODEC, PacketNBTFromClient::handle, args);
    }

    public static <T extends CustomPacketPayload, B extends FriendlyByteBuf> void registerServerboundPacket(CustomPacketPayload.Type<T> type, StreamCodec<B, T> codec, BiConsumer<T, Player> handler, Object ... args) {
        PayloadRegistrar registrar = (PayloadRegistrar)args[0];
        IPayloadHandler serverHandler = (packet, context) -> context.enqueueWork(() -> handler.accept(packet, context.player()));
        registrar.playToServer(type, codec, serverHandler);
    }

    public static <T extends CustomPacketPayload, B extends FriendlyByteBuf> void registerClientboundPacket(CustomPacketPayload.Type<T> type, StreamCodec<B, T> codec, BiConsumer<T, Player> handler, Object ... args) {
        PayloadRegistrar registrar = (PayloadRegistrar)args[0];
        IPayloadHandler clientHandler = (packet, context) -> context.enqueueWork(() -> handler.accept(packet, context.player()));
        registrar.playToClient(type, codec, clientHandler);
    }

    public static void clientSendToSever(CompoundTag data) {
        PacketDistributor.sendToServer((CustomPacketPayload)new PacketNBTFromClient(data), (CustomPacketPayload[])new CustomPacketPayload[0]);
    }

    public static void serverSendToClientAll(CompoundTag data) {
        PacketDistributor.sendToAllPlayers((CustomPacketPayload)new PacketNBTFromServer(data), (CustomPacketPayload[])new CustomPacketPayload[0]);
    }

    public static void serverSendToClientPlayer(CompoundTag data, Player player) {
        PacketDistributor.sendToPlayer((ServerPlayer)((ServerPlayer)player), (CustomPacketPayload)new PacketNBTFromServer(data), (CustomPacketPayload[])new CustomPacketPayload[0]);
    }

    public static void serverSendToClientNear(CompoundTag data, Vec3 position, double distance, Level level) {
        PacketDistributor.sendToPlayersNear((ServerLevel)((ServerLevel)level), null, (double)position.x, (double)position.y, (double)position.z, (double)distance, (CustomPacketPayload)new PacketNBTFromServer(data), (CustomPacketPayload[])new CustomPacketPayload[0]);
    }

    public static void serverSendToClientDimension(CompoundTag data, Level level) {
        PacketDistributor.sendToPlayersInDimension((ServerLevel)((ServerLevel)level), (CustomPacketPayload)new PacketNBTFromServer(data), (CustomPacketPayload[])new CustomPacketPayload[0]);
    }
}

