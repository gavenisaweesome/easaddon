package com.burrows.easaddon.survey;

import com.burrows.easaddon.EASAddon;

import com.burrows.easaddon.network.SurveyNetworkPackets;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Server-side survey coordination and state management
 */
public class ServerSurveyManager {
    private static ServerSurveyManager instance;
    
    // Server-side survey state
    private final Map<Long, ServerSurveySession> activeSurveys = new ConcurrentHashMap<>();
    private final Map<Long, SurveyResults> completedSurveys = new ConcurrentHashMap<>();
    private final Set<ChunkPos> serverForceLoadedChunks = new HashSet<>();
    private static final int SERVER_CHUNK_LOAD_TIMEOUT_MS = 3000;
    
    public static class ServerSurveySession {
        public final long tornadoId;
        public final String playerName;
        public final UUID playerId;
        public final long startTime;
        public final Set<ChunkPos> surveyedChunks;
        public final Set<ChunkPos> availableChunks;
        public final Map<ChunkPos, ChunkSurveyData> chunkRatings; // ADDED: Track client-calculated ratings
        public int requiredSurveys;
       
        
        public static class ChunkSurveyData {
            public final int efRating;
            public final float windspeed;
            
            public ChunkSurveyData(int efRating, float windspeed) {
                this.efRating = efRating;
                this.windspeed = windspeed;
            }
        }
        
        public ServerSurveySession(long tornadoId, String playerName, UUID playerId, Set<ChunkPos> chunks) {
            this.tornadoId = tornadoId;
            this.playerName = playerName;
            this.playerId = playerId;
            this.startTime = System.currentTimeMillis();
            this.surveyedChunks = new HashSet<>();
            this.availableChunks = new HashSet<>(chunks);
            this.chunkRatings = new HashMap<>(); // ADDED
            this.requiredSurveys = Math.max(1, chunks.size() / 4); // 25% requirement
        }
        
        public boolean canFinish() {
            return surveyedChunks.size() >= requiredSurveys;
        }
        
        public float getProgress() {
            return availableChunks.isEmpty() ? 1.0f : (float) surveyedChunks.size() / requiredSurveys;
        }
        
        // ADDED: Methods to get final calculated ratings
        public int getFinalEFRating() {
            return chunkRatings.values().stream()
                    .mapToInt(data -> data.efRating)
                    .max()
                    .orElse(-1);
        }
        
        public float getFinalWindspeed() {
            return chunkRatings.values().stream()
                    .map(data -> data.windspeed)
                    .max(Float::compareTo)
                    .orElse(0.0f);
        }
    }
    
    public static class SurveyResults {
        public final long tornadoId;
        public final String surveyedBy;
        public final long surveyTime;
        public final int efRating;
        public final float maxWindspeed;
        
        public SurveyResults(long tornadoId, String surveyedBy, int efRating, float maxWindspeed) {
            this.tornadoId = tornadoId;
            this.surveyedBy = surveyedBy;
            this.surveyTime = System.currentTimeMillis();
            this.efRating = efRating;
            this.maxWindspeed = maxWindspeed;
        }
    }
    
    private ServerSurveyManager() {}
    
    public static ServerSurveyManager getInstance() {
        if (instance == null) {
            instance = new ServerSurveyManager();
        }
        return instance;
    }
    
    /**
     * NEW: Check if a survey would result in a rating downgrade
     */
    private boolean wouldDowngradeRating(long tornadoId, int newRating, String newSurveyor) {
        // Check if we have completed survey data
        SurveyResults existingResults = completedSurveys.get(tornadoId);
        if (existingResults == null) {
            return false; // No existing rating to downgrade from
        }
        
        int existingRating = existingResults.efRating;
        
        // Allow the same surveyor to re-survey regardless of rating
        if (newSurveyor.equals(existingResults.surveyedBy)) {
            EASAddon.LOGGER.info("Allowing same surveyor {} to re-survey tornado {} (EF{} -> EF{})", 
                newSurveyor, tornadoId, existingRating, newRating);
            return false;
        }
        
        // Allow re-survey if new rating is equal or higher
        if (newRating >= existingRating) {
            return false;
        }
        
        // This would be a downgrade
        EASAddon.LOGGER.warn("Server detected survey downgrade attempt: tornado {} from EF{} to EF{} by {} (original by {})", 
            tornadoId, existingRating, newRating, newSurveyor, existingResults.surveyedBy);
        return true;
    }
    
    /**
     * FIXED: Handle start survey request from client with actual damage chunks
     */
// Update the handleStartSurvey method in ServerSurveyManager.java
// Replace the existing method with this enhanced version:

/**
 * FIXED: Handle start survey request from client with validated damage chunks
 * Now both client and server use the same filtered chunk list
 */
public void handleStartSurvey(Player player, long tornadoId, List<ChunkPos> validatedChunks) {
    if (!(player instanceof ServerPlayer serverPlayer)) return;
    
    String playerName = player.getName().getString();
    UUID playerId = player.getUUID();
    
    // Check for existing survey
    if (activeSurveys.containsKey(tornadoId)) {
        ServerSurveySession existingSession = activeSurveys.get(tornadoId);
        if (existingSession.playerId.equals(playerId)) {
            serverPlayer.sendSystemMessage(Component.literal("§eYou are already surveying this tornado"));
        } else {
            serverPlayer.sendSystemMessage(Component.literal("§cThis tornado is already being surveyed by " + existingSession.playerName));
        }
        serverPlayer.sendSystemMessage(Component.literal("§c"));
        return;
    }
    
    Set<ChunkPos> validatedChunksSet = new HashSet<>(validatedChunks);
    
    if (validatedChunksSet.isEmpty()) {
        serverPlayer.sendSystemMessage(Component.literal("§cNo validated damage chunks received from client"));
        PacketDistributor.sendToPlayer(serverPlayer,
            new SurveyNetworkPackets.SurveyUpdatePacket(tornadoId, "error", playerName, 
                "No validated damage chunks received")
        );
        return;
    }
    
    EASAddon.LOGGER.info("SERVER: Starting survey for tornado {} with {} validated chunks", tornadoId, validatedChunksSet.size());
    
    // ENHANCED: Force load chunks on server side to ensure consistency
    ServerLevel serverLevel = (ServerLevel) player.level();
    Set<ChunkPos> chunksToLoad = new HashSet<>();
    
    for (ChunkPos chunk : validatedChunksSet) {
        if (!serverLevel.hasChunk(chunk.x, chunk.z)) {
            chunksToLoad.add(chunk);
        }
    }
    
    if (!chunksToLoad.isEmpty()) {
        EASAddon.LOGGER.info("SERVER: Force loading {} unloaded chunks for survey consistency", chunksToLoad.size());
        forceLoadChunksOnServer(chunksToLoad, serverLevel);
        
        // Wait briefly for chunks to load
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    // Create survey session with the same chunks the client validated
    ServerSurveySession session = new ServerSurveySession(tornadoId, playerName, playerId, validatedChunksSet);
    activeSurveys.put(tornadoId, session);
    
    // ENHANCED LOGGING: Verify client-server chunk sync
    EASAddon.LOGGER.info("SERVER SURVEY SESSION CREATED:");
    EASAddon.LOGGER.info("  Tornado ID: {}", tornadoId);
    EASAddon.LOGGER.info("  Player: {}", playerName);
    EASAddon.LOGGER.info("  Total validated chunks: {}", validatedChunksSet.size());
    EASAddon.LOGGER.info("  Chunks force loaded: {}", chunksToLoad.size());
    EASAddon.LOGGER.info("  Required surveys (25%): {}", session.requiredSurveys);
    EASAddon.LOGGER.info("  Chunk list: {}", validatedChunks.stream()
        .map(chunk -> "(" + chunk.x + "," + chunk.z + ")")
        .collect(java.util.stream.Collectors.joining(", ")));
    
    // FIXED: Send success update to all clients using correct API
    PacketDistributor.sendToAllPlayers(
        new SurveyNetworkPackets.SurveyUpdatePacket(tornadoId, "start", playerName, 
            String.format("{\"chunks\": %d, \"required\": %d}", validatedChunksSet.size(), session.requiredSurveys))
    );
    
    // Send confirmation to player
    serverPlayer.sendSystemMessage(Component.literal("§6Survey started for tornado " + tornadoId));
    serverPlayer.sendSystemMessage(Component.literal("§eValidated chunks: " + validatedChunksSet.size() + 
        " (need " + session.requiredSurveys + ")"));
    
    // Schedule cleanup of force-loaded chunks
    if (!chunksToLoad.isEmpty()) {
        scheduleServerChunkCleanup(chunksToLoad, serverLevel, 60000); // Clean up after 1 minute
    }
    
    EASAddon.LOGGER.info("Player {} started surveying tornado {} with {} validated chunks (need {})", 
        playerName, tornadoId, validatedChunksSet.size(), session.requiredSurveys);
}

/**
 * Force load chunks on server side for survey consistency
 */
private void forceLoadChunksOnServer(Set<ChunkPos> chunks, ServerLevel serverLevel) {
    for (ChunkPos chunk : chunks) {
        try {
            EASAddon.LOGGER.debug("SERVER: Force loading chunk ({}, {})", chunk.x, chunk.z);
            
            // Use server-side chunk force loading
            serverLevel.setChunkForced(chunk.x, chunk.z, true);
            serverForceLoadedChunks.add(chunk);
            
            EASAddon.LOGGER.debug("SERVER: Successfully force loaded chunk ({}, {})", chunk.x, chunk.z);
            
        } catch (Exception e) {
            EASAddon.LOGGER.error("SERVER: Failed to force load chunk ({}, {}): {}", chunk.x, chunk.z, e.getMessage());
        }
    }
}

/**
 * ENHANCED: Handle finish survey request with chunk cleanup
 * This replaces the existing handleFinishSurvey method
 */
public void handleFinishSurvey(Player player, long tornadoId, int finalRating, float finalWindspeed) {
    if (!(player instanceof ServerPlayer serverPlayer)) return;
    
    String playerName = player.getName().getString();
    ServerSurveySession session = activeSurveys.get(tornadoId);
    
    if (session == null) {
        serverPlayer.sendSystemMessage(Component.literal("§cNo active survey for this tornado"));
        return;
    }
    
    if (!session.playerId.equals(player.getUUID())) {
        serverPlayer.sendSystemMessage(Component.literal("§cYou are not the one surveying this tornado"));
        return;
    }
    
    // Calculate final results
    int totalChunks = session.availableChunks.size();
    int surveyedChunks = session.surveyedChunks.size();
    float completion = totalChunks > 0 ? ((float) surveyedChunks / totalChunks) * 100f : 0f;
    
    // ENHANCED: Clean up any force-loaded chunks for this survey
    ServerLevel serverLevel = (ServerLevel) player.level();
    cleanupSurveyChunks(session.availableChunks, serverLevel);
    
    // Remove session
    activeSurveys.remove(tornadoId);
    
    // Log completion
    EASAddon.LOGGER.info("SERVER: Survey completed for tornado {} by {}", tornadoId, playerName);
    EASAddon.LOGGER.info("  Final rating: EF{}", finalRating);
    EASAddon.LOGGER.info("  Final windspeed: {}mph", finalWindspeed);
    EASAddon.LOGGER.info("  Completion: {}/{} chunks ({}%)", surveyedChunks, totalChunks, String.format("%.1f", completion));
    
    // Send completion update to all clients
    PacketDistributor.sendToAllPlayers(
        new SurveyNetworkPackets.SurveyUpdatePacket(tornadoId, "complete", playerName,
            String.format("{\"rating\": %d, \"windspeed\": %.1f, \"completion\": %.1f, \"surveyed\": %d, \"total\": %d}",
                finalRating, finalWindspeed, completion, surveyedChunks, totalChunks))
    );
    
    // Send confirmation to player
    serverPlayer.sendSystemMessage(Component.literal("§a✓ Survey completed successfully!"));
    serverPlayer.sendSystemMessage(Component.literal("§eRating: EF" + finalRating + 
        " (" + finalWindspeed + " mph)"));
    serverPlayer.sendSystemMessage(Component.literal("§eCompletion: " + surveyedChunks + "/" + totalChunks + 
        " chunks (" + String.format("%.1f", completion) + "%)"));
}

/**
 * ENHANCED: Handle quit survey request with chunk cleanup
 * This replaces or enhances the existing handleQuitSurvey method
 */
public void handleQuitSurvey(Player player, long tornadoId) {
    if (!(player instanceof ServerPlayer serverPlayer)) return;
    
    String playerName = player.getName().getString();
    ServerSurveySession session = activeSurveys.get(tornadoId);
    
    if (session == null) {
        serverPlayer.sendSystemMessage(Component.literal("§cNo active survey for this tornado"));
        return;
    }
    
    if (!session.playerId.equals(player.getUUID())) {
        serverPlayer.sendSystemMessage(Component.literal("§cYou are not the one surveying this tornado"));
        return;
    }
    
    // ENHANCED: Clean up any force-loaded chunks for this survey
    ServerLevel serverLevel = (ServerLevel) player.level();
    cleanupSurveyChunks(session.availableChunks, serverLevel);
    
    // Remove session
    activeSurveys.remove(tornadoId);
    
    // Calculate what was completed
    int totalChunks = session.availableChunks.size();
    int surveyedChunks = session.surveyedChunks.size();
    float completion = totalChunks > 0 ? ((float) surveyedChunks / totalChunks) * 100f : 0f;
    
    EASAddon.LOGGER.info("SERVER: Survey quit for tornado {} by {} - {}/{} chunks completed ({}%)", 
        tornadoId, playerName, surveyedChunks, totalChunks, String.format("%.1f", completion));
    
    // Send quit update to all clients
    PacketDistributor.sendToAllPlayers(
        new SurveyNetworkPackets.SurveyUpdatePacket(tornadoId, "quit", playerName,
            String.format("{\"completion\": %.1f, \"surveyed\": %d, \"total\": %d}",
                completion, surveyedChunks, totalChunks))
    );
    
    // Send confirmation to player
    serverPlayer.sendSystemMessage(Component.literal("§6Survey ended"));
    if (surveyedChunks > 0) {
        serverPlayer.sendSystemMessage(Component.literal("§eProgress saved: " + surveyedChunks + "/" + totalChunks + 
            " chunks (" + String.format("%.1f", completion) + "%)"));
    }
}

/**
 * Clean up force-loaded chunks for a specific survey
 */
private void cleanupSurveyChunks(Set<ChunkPos> surveyChunks, ServerLevel serverLevel) {
    Set<ChunkPos> chunksToCleanup = new HashSet<>();
    
    // Find which chunks were force-loaded for this survey
    for (ChunkPos chunk : surveyChunks) {
        if (serverForceLoadedChunks.contains(chunk)) {
            chunksToCleanup.add(chunk);
        }
    }
    
    if (!chunksToCleanup.isEmpty()) {
        EASAddon.LOGGER.info("SERVER: Cleaning up {} force-loaded chunks from completed survey", chunksToCleanup.size());
        cleanupServerForceLoadedChunks(chunksToCleanup, serverLevel);
    }
}

/**
 * Schedule cleanup of server force-loaded chunks after delay
 */
private void scheduleServerChunkCleanup(Set<ChunkPos> chunksToClean, ServerLevel serverLevel, long delayMs) {
    if (chunksToClean.isEmpty()) return;
    
    EASAddon.LOGGER.info("SERVER: Scheduling cleanup of {} force-loaded chunks in {}ms", chunksToClean.size(), delayMs);
    
    // Use a separate thread to clean up after delay
    CompletableFuture.delayedExecutor(delayMs, TimeUnit.MILLISECONDS).execute(() -> {
        cleanupServerForceLoadedChunks(chunksToClean, serverLevel);
    });
}

/**
 * Clean up server force-loaded chunks
 */
private void cleanupServerForceLoadedChunks(Set<ChunkPos> chunksToClean, ServerLevel serverLevel) {
    EASAddon.LOGGER.info("SERVER: Cleaning up {} force-loaded chunks", chunksToClean.size());
    
    for (ChunkPos chunk : chunksToClean) {
        try {
            if (serverForceLoadedChunks.contains(chunk)) {
                serverLevel.setChunkForced(chunk.x, chunk.z, false);
                serverForceLoadedChunks.remove(chunk);
                EASAddon.LOGGER.debug("SERVER: Released force-loaded chunk ({}, {})", chunk.x, chunk.z);
            }
        } catch (Exception e) {
            EASAddon.LOGGER.error("SERVER: Error cleaning up chunk ({}, {}): {}", chunk.x, chunk.z, e.getMessage());
        }
    }
}

/**
 * Get all currently force-loaded chunks (for debugging/monitoring)
 */
public Set<ChunkPos> getForceLoadedChunks() {
    return new HashSet<>(serverForceLoadedChunks);
}

/**
 * ENHANCED: Shutdown method to clean up all force-loaded chunks
 */
public void shutdown() {
    EASAddon.LOGGER.info("SERVER: Shutting down server survey manager, cleaning up {} force-loaded chunks", 
        serverForceLoadedChunks.size());
    
    // Clean up all remaining force-loaded chunks
    if (!serverForceLoadedChunks.isEmpty()) {
        // Get any available server level for cleanup
        try {
            // This is a simplified approach - in practice you'd want to track per-level
            if (activeSurveys.values().iterator().hasNext()) {
                ServerSurveySession anySession = activeSurveys.values().iterator().next();
                // Find the level from any active session's player, but this is a fallback approach
                // In a real implementation, you'd want to track chunks per ServerLevel
            }
            
            // For now, just clear tracking and log
            Set<ChunkPos> chunksToLog = new HashSet<>(serverForceLoadedChunks);
            serverForceLoadedChunks.clear();
            
            EASAddon.LOGGER.warn("SERVER: Cleared tracking of {} force-loaded chunks on shutdown. " +
                "These chunks may remain force-loaded until server restart: {}", 
                chunksToLog.size(), 
                chunksToLog.stream()
                    .limit(10) // Show only first 10 to avoid spam
                    .map(chunk -> "(" + chunk.x + "," + chunk.z + ")")
                    .collect(java.util.stream.Collectors.joining(", "))
                    + (chunksToLog.size() > 10 ? "..." : ""));
                    
        } catch (Exception e) {
            EASAddon.LOGGER.error("SERVER: Error during shutdown cleanup: {}", e.getMessage());
        }
    }
    
    activeSurveys.clear();
}
    
    /**
     * FIXED: Handle survey chunk action from client with client-calculated rating and windspeed
     * REMOVED duplicate messages to prevent client-server message duplication
     */
    public void handleSurveyAction(Player player, long tornadoId, int chunkX, int chunkZ, int clientRating, float clientWindspeed) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        
        String playerName = player.getName().getString();
        ServerSurveySession session = activeSurveys.get(tornadoId);
        
        if (session == null) {
            // Only send error messages from server
            serverPlayer.sendSystemMessage(Component.literal("§cNo active survey for this tornado"));
            return;
        }
        
        if (!session.playerId.equals(player.getUUID())) {
            serverPlayer.sendSystemMessage(Component.literal("§cYou are not the one surveying this tornado"));
            return;
        }
        
        ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
        
        // FIXED: Use the actual available chunks from client data instead of hardcoded chunks
        if (!session.availableChunks.contains(chunkPos)) {
            serverPlayer.sendSystemMessage(Component.literal("§cThis chunk was not damaged by the tornado"));
            EASAddon.LOGGER.warn("Player {} tried to survey invalid chunk ({}, {}) for tornado {}. Available chunks: {}", 
                playerName, chunkX, chunkZ, tornadoId, session.availableChunks);
            return;
        }
        
        if (session.surveyedChunks.contains(chunkPos)) {
            serverPlayer.sendSystemMessage(Component.literal("§cThis chunk has already been surveyed"));
            return;
        }
        
        // FIXED: Use client-calculated rating and windspeed instead of server calculations
        int efRating = clientRating;
        float maxWindspeed = clientWindspeed;
        
        // Mark chunk as surveyed and store the client-calculated data
        session.surveyedChunks.add(chunkPos);
        session.chunkRatings.put(chunkPos, new ServerSurveySession.ChunkSurveyData(efRating, maxWindspeed));
        
        // REMOVED: Duplicate success messages - these are already sent by DamageSurveyManager on client
        // The client handles all user feedback, server only handles validation and state management
        
        // FIXED: Broadcast update to all clients using correct API (for multiplayer coordination only)
        PacketDistributor.sendToAllPlayers(
            new SurveyNetworkPackets.SurveyUpdatePacket(tornadoId, "chunk_surveyed", playerName, 
                String.format("{\"chunkX\": %d, \"chunkZ\": %d, \"rating\": %d, \"windspeed\": %.1f, \"progress\": %.2f}", 
                    chunkX, chunkZ, efRating, maxWindspeed, session.getProgress()))
        );
        
        EASAddon.LOGGER.info("Player {} surveyed chunk ({}, {}) for tornado {} - Rating: EF{}, Windspeed: {}mph (client-calculated)", 
            playerName, chunkX, chunkZ, tornadoId, efRating, maxWindspeed);
    }
    
    /**
     * Handle finish survey request from client
     */

    
    /**
     * Handle quit survey request from client
     */

    
    // Helper methods
    private String formatEFRating(int rating) {
        return rating < 0 ? "EFU" : "EF" + rating;
    }
    
    // Public accessors for state checking
    public boolean isTornadoBeingSurveyed(long tornadoId) {
        return activeSurveys.containsKey(tornadoId);
    }
    
    public String getSurveyorName(long tornadoId) {
        ServerSurveySession session = activeSurveys.get(tornadoId);
        return session != null ? session.playerName : null;
    }
    
    public SurveyResults getSurveyResults(long tornadoId) {
        return completedSurveys.get(tornadoId);
    }
}