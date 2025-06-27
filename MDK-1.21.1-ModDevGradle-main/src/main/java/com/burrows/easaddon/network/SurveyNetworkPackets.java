package com.burrows.easaddon.network;

import com.burrows.easaddon.EASAddon;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs; // FIXED: Correct import
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Network packets for survey system coordination
 */
public class SurveyNetworkPackets {
    
    // Packet IDs
    public static final ResourceLocation START_SURVEY_ID = ResourceLocation.fromNamespaceAndPath(EASAddon.MODID, "start_survey");
    public static final ResourceLocation QUIT_SURVEY_ID = ResourceLocation.fromNamespaceAndPath(EASAddon.MODID, "quit_survey");
    public static final ResourceLocation FINISH_SURVEY_ID = ResourceLocation.fromNamespaceAndPath(EASAddon.MODID, "finish_survey");
    public static final ResourceLocation SURVEY_ACTION_ID = ResourceLocation.fromNamespaceAndPath(EASAddon.MODID, "survey_action");
    public static final ResourceLocation SURVEY_UPDATE_ID = ResourceLocation.fromNamespaceAndPath(EASAddon.MODID, "survey_update");
    
    // === START SURVEY PACKET (UPDATED with damage chunks) ===
    public record StartSurveyPacket(long tornadoId, List<ChunkPos> damagedChunks) implements CustomPacketPayload {
        public static final Type<StartSurveyPacket> TYPE = new Type<>(START_SURVEY_ID);
        
        // FIXED: Custom codec for ChunkPos list
        public static final StreamCodec<FriendlyByteBuf, StartSurveyPacket> STREAM_CODEC = new StreamCodec<FriendlyByteBuf, StartSurveyPacket>() {
            @Override
            public void encode(FriendlyByteBuf buffer, StartSurveyPacket packet) {
                buffer.writeVarLong(packet.tornadoId);
                buffer.writeVarInt(packet.damagedChunks.size());
                for (ChunkPos chunk : packet.damagedChunks) {
                    buffer.writeVarInt(chunk.x);
                    buffer.writeVarInt(chunk.z);
                }
            }
            
            @Override
            public StartSurveyPacket decode(FriendlyByteBuf buffer) {
                long tornadoId = buffer.readVarLong();
                int chunkCount = buffer.readVarInt();
                List<ChunkPos> chunks = new ArrayList<>();
                for (int i = 0; i < chunkCount; i++) {
                    int x = buffer.readVarInt();
                    int z = buffer.readVarInt();
                    chunks.add(new ChunkPos(x, z));
                }
                return new StartSurveyPacket(tornadoId, chunks);
            }
        };
        
        @Override
        public Type<StartSurveyPacket> type() {
            return TYPE;
        }
        
        public static void handle(StartSurveyPacket packet, IPayloadContext context) {
            context.enqueueWork(() -> {
                // Server-side handling
                com.burrows.easaddon.survey.ServerSurveyManager.getInstance()
                    .handleStartSurvey(context.player(), packet.tornadoId, packet.damagedChunks);
            });
        }
    }
    
    // === QUIT SURVEY PACKET ===
    public record QuitSurveyPacket(long tornadoId) implements CustomPacketPayload {
        public static final Type<QuitSurveyPacket> TYPE = new Type<>(QUIT_SURVEY_ID);
        
        // FIXED: Use ByteBufCodecs
        public static final StreamCodec<FriendlyByteBuf, QuitSurveyPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, QuitSurveyPacket::tornadoId,
            QuitSurveyPacket::new
        );
        
        @Override
        public Type<QuitSurveyPacket> type() {
            return TYPE;
        }
        
        public static void handle(QuitSurveyPacket packet, IPayloadContext context) {
            context.enqueueWork(() -> {
                com.burrows.easaddon.survey.ServerSurveyManager.getInstance()
                    .handleQuitSurvey(context.player(), packet.tornadoId);
            });
        }
    }
    
    // === FINISH SURVEY PACKET ===
    public record FinishSurveyPacket(long tornadoId, int finalRating, float finalWindspeed) implements CustomPacketPayload {
        public static final Type<FinishSurveyPacket> TYPE = new Type<>(FINISH_SURVEY_ID);
        
        // FIXED: Use ByteBufCodecs for all primitive types
        public static final StreamCodec<FriendlyByteBuf, FinishSurveyPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, FinishSurveyPacket::tornadoId,
            ByteBufCodecs.VAR_INT, FinishSurveyPacket::finalRating,
            ByteBufCodecs.FLOAT, FinishSurveyPacket::finalWindspeed,
            FinishSurveyPacket::new
        );
        
        @Override
        public Type<FinishSurveyPacket> type() {
            return TYPE;
        }
        
        public static void handle(FinishSurveyPacket packet, IPayloadContext context) {
            context.enqueueWork(() -> {
                com.burrows.easaddon.survey.ServerSurveyManager.getInstance()
                    .handleFinishSurvey(context.player(), packet.tornadoId, packet.finalRating, packet.finalWindspeed);
            });
        }
    }
    
    // === SURVEY ACTION PACKET (UPDATED with client-calculated data) ===
    public record SurveyActionPacket(long tornadoId, int chunkX, int chunkZ, int clientRating, float clientWindspeed) implements CustomPacketPayload {
        public static final Type<SurveyActionPacket> TYPE = new Type<>(SURVEY_ACTION_ID);
        
        // FIXED: Include client-calculated rating and windspeed
        public static final StreamCodec<FriendlyByteBuf, SurveyActionPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, SurveyActionPacket::tornadoId,
            ByteBufCodecs.VAR_INT, SurveyActionPacket::chunkX,
            ByteBufCodecs.VAR_INT, SurveyActionPacket::chunkZ,
            ByteBufCodecs.VAR_INT, SurveyActionPacket::clientRating,
            ByteBufCodecs.FLOAT, SurveyActionPacket::clientWindspeed,
            SurveyActionPacket::new
        );
        
        @Override
        public Type<SurveyActionPacket> type() {
            return TYPE;
        }
        
        public static void handle(SurveyActionPacket packet, IPayloadContext context) {
            context.enqueueWork(() -> {
                com.burrows.easaddon.survey.ServerSurveyManager.getInstance()
                    .handleSurveyAction(context.player(), packet.tornadoId, packet.chunkX, packet.chunkZ, packet.clientRating, packet.clientWindspeed);
            });
        }
    }
    
    // === SURVEY UPDATE PACKET (Server -> Client) ===
    public record SurveyUpdatePacket(
        long tornadoId, 
        String action, // "start", "quit", "finish", "chunk_surveyed", "error"
        String playerName,
        String data // JSON data for additional info
    ) implements CustomPacketPayload {
        public static final Type<SurveyUpdatePacket> TYPE = new Type<>(SURVEY_UPDATE_ID);
        
        // FIXED: Use ByteBufCodecs.STRING_UTF8 for strings
        public static final StreamCodec<FriendlyByteBuf, SurveyUpdatePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, SurveyUpdatePacket::tornadoId,
            ByteBufCodecs.STRING_UTF8, SurveyUpdatePacket::action,
            ByteBufCodecs.STRING_UTF8, SurveyUpdatePacket::playerName,
            ByteBufCodecs.STRING_UTF8, SurveyUpdatePacket::data,
            SurveyUpdatePacket::new
        );
        
        @Override
        public Type<SurveyUpdatePacket> type() {
            return TYPE;
        }
        
        public static void handle(SurveyUpdatePacket packet, IPayloadContext context) {
            context.enqueueWork(() -> {
                // Client-side handling
                com.burrows.easaddon.survey.ClientSurveyManager.getInstance()
                    .handleSurveyUpdate(packet.tornadoId, packet.action, packet.playerName, packet.data);
            });
        }
    }
}