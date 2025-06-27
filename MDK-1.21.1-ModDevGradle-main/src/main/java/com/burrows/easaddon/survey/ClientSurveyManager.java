package com.burrows.easaddon.survey;

import com.burrows.easaddon.EASAddon;
import com.burrows.easaddon.network.SurveyNetworkPackets;
import com.burrows.easaddon.tornado.TornadoData;
import com.burrows.easaddon.tornado.TornadoTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side survey manager that coordinates with server
 */
@OnlyIn(Dist.CLIENT)
public class ClientSurveyManager {
    private static ClientSurveyManager instance;
    
    // Client-side survey state (synced from server)
    private final Map<Long, ClientSurveyInfo> activeSurveys = new ConcurrentHashMap<>();
    private final Map<Long, SurveyResult> completedSurveys = new ConcurrentHashMap<>();
    
    public static class ClientSurveyInfo {
        public final long tornadoId;
        public final String surveyorName;
        public final long startTime;
        public int totalChunks;
        public int requiredChunks;
        public int surveyedChunks;
        public boolean canFinish;
        
        public ClientSurveyInfo(long tornadoId, String surveyorName) {
            this.tornadoId = tornadoId;
            this.surveyorName = surveyorName;
            this.startTime = System.currentTimeMillis();
        }
        
        public float getProgress() {
            return requiredChunks == 0 ? 0.0f : (float) surveyedChunks / requiredChunks;
        }
    }
    
    public static class SurveyResult {
        public final long tornadoId;
        public final String surveyedBy;
        public final long surveyTime;
        public final int efRating;
        public final float maxWindspeed;
        
        public SurveyResult(long tornadoId, String surveyedBy, int efRating, float maxWindspeed) {
            this.tornadoId = tornadoId;
            this.surveyedBy = surveyedBy;
            this.surveyTime = System.currentTimeMillis();
            this.efRating = efRating;
            this.maxWindspeed = maxWindspeed;
        }
    }
    
    private ClientSurveyManager() {}
    
    public static ClientSurveyManager getInstance() {
        if (instance == null) {
            instance = new ClientSurveyManager();
        }
        return instance;
    }
    
    // Update the startSurvey method in ClientSurveyManager.java

/**
 * Request to start surveying a tornado
 * FIXED: Accept filtered chunk list instead of tornado data to ensure client-server sync
 */
public boolean startSurvey(long tornadoId, List<ChunkPos> validChunks, Player player) {
    // Check if tornado is already being surveyed
    if (activeSurveys.containsKey(tornadoId)) {
        String surveyorName = activeSurveys.get(tornadoId).surveyorName;
        player.sendSystemMessage(Component.literal("§cTornado is already being surveyed by " + surveyorName));
        return false;
    }
    
    // Check if player is already surveying another tornado
    String playerName = player.getName().getString();
    for (ClientSurveyInfo survey : activeSurveys.values()) {
        if (survey.surveyorName.equals(playerName)) {
            player.sendSystemMessage(Component.literal("§cYou are already surveying another tornado. Use /survey quit to cancel."));
            return false;
        }
    }
    
    // FIXED: Send the actual filtered chunks that the client validated
    PacketDistributor.sendToServer(new SurveyNetworkPackets.StartSurveyPacket(tornadoId, validChunks));
    
    EASAddon.LOGGER.info("Sent start survey request for tornado {} with {} validated chunks", tornadoId, validChunks.size());
    return true; // Server will validate and respond
}

// DEPRECATED: Keep old method for backward compatibility but redirect to new one
@Deprecated
public boolean startSurvey(TornadoData tornadoData, Player player) {
    // Extract chunks from tornado data as fallback
    Set<ChunkPos> damagedChunks = tornadoData.getDamagedChunks();
    List<ChunkPos> chunkList = new ArrayList<>(damagedChunks);
    return startSurvey(tornadoData.getId(), chunkList, player);
}
    
    /**
     * Handle a survey action at a specific position
     * NOTE: This is now primarily used for validation - the actual network call is made from DamageSurveyManager
     */
    public boolean handleSurveyAction(Player player, BlockPos pos) {
        String playerName = player.getName().getString();
        
        // Find active survey for this player
        ClientSurveyInfo activeSurvey = null;
        for (ClientSurveyInfo survey : activeSurveys.values()) {
            if (survey.surveyorName.equals(playerName)) {
                activeSurvey = survey;
                break;
            }
        }
        
        if (activeSurvey == null) {
            player.sendSystemMessage(Component.literal("§cYou are not currently surveying a tornado"));
            return false;
        }
        
        // NOTE: The actual network packet with rating/windspeed is sent from DamageSurveyManager
        // This method now just validates that the player has an active survey
        return true;
    }
    
    /**
     * Finish the current survey
     * NOTE: This is now primarily used for validation - the actual network call is made from DamageSurveyManager
     */
    public boolean finishSurvey(Player player) {
        String playerName = player.getName().getString();
        
        // Find active survey for this player
        ClientSurveyInfo activeSurvey = null;
        for (ClientSurveyInfo survey : activeSurveys.values()) {
            if (survey.surveyorName.equals(playerName)) {
                activeSurvey = survey;
                break;
            }
        }
        
        if (activeSurvey == null) {
            player.sendSystemMessage(Component.literal("§cYou are not currently surveying a tornado"));
            return false;
        }
        
        if (!activeSurvey.canFinish) {
            player.sendSystemMessage(Component.literal("§cYou must survey more chunks before finishing"));
            return false;
        }
        
        // NOTE: The actual network packet with calculated rating/windspeed is sent from DamageSurveyManager
        // This method now just validates that the player can finish the survey
        return true;
    }
    
    /**
     * Quit the current survey
     */
    public boolean quitSurvey(Player player) {
        String playerName = player.getName().getString();
        
        // Find active survey for this player
        ClientSurveyInfo activeSurvey = null;
        for (ClientSurveyInfo survey : activeSurveys.values()) {
            if (survey.surveyorName.equals(playerName)) {
                activeSurvey = survey;
                break;
            }
        }
        
        if (activeSurvey == null) {
            player.sendSystemMessage(Component.literal("§cYou are not currently surveying a tornado"));
            return false;
        }
        
        // FIXED: Send quit request to server using correct API
        PacketDistributor.sendToServer(new SurveyNetworkPackets.QuitSurveyPacket(activeSurvey.tornadoId));
        
        EASAddon.LOGGER.info("Sent quit survey request for tornado {}", activeSurvey.tornadoId);
        return true;
    }
    
    /**
     * Handle survey updates from server
     */
    public void handleSurveyUpdate(long tornadoId, String action, String playerName, String data) {
        EASAddon.LOGGER.info("Received survey update: tornado={}, action={}, player={}, data={}", 
            tornadoId, action, playerName, data);
        
        switch (action) {
            case "start" -> {
                ClientSurveyInfo survey = new ClientSurveyInfo(tornadoId, playerName);
                // Parse data to get chunk counts
                try {
                    // Simple JSON parsing for chunk data
                    if (data.contains("chunks")) {
                        String chunksStr = data.substring(data.indexOf("\"chunks\": ") + 10);
                        chunksStr = chunksStr.substring(0, chunksStr.indexOf(","));
                        survey.totalChunks = Integer.parseInt(chunksStr);
                        
                        String requiredStr = data.substring(data.indexOf("\"required\": ") + 12);
                        requiredStr = requiredStr.substring(0, requiredStr.indexOf("}"));
                        survey.requiredChunks = Integer.parseInt(requiredStr);
                    }
                } catch (Exception e) {
                    EASAddon.LOGGER.warn("Failed to parse survey start data: {}", e.getMessage());
                }
                
                activeSurveys.put(tornadoId, survey);
                
                // Update GUI if needed
                Player localPlayer = Minecraft.getInstance().player;
                if (localPlayer != null && playerName.equals(localPlayer.getName().getString())) {
                    localPlayer.sendSystemMessage(Component.literal("§6Survey started for tornado " + tornadoId));
                }
            }
            
            case "chunk_surveyed" -> {
                ClientSurveyInfo survey = activeSurveys.get(tornadoId);
                if (survey != null) {
                    survey.surveyedChunks++;
                    survey.canFinish = survey.surveyedChunks >= survey.requiredChunks;
                }
            }
            
            case "finish" -> {
                ClientSurveyInfo survey = activeSurveys.remove(tornadoId);
                if (survey != null) {
                    // Parse final results
                    try {
                        int rating = 0;
                        float windspeed = 0.0f;
                        
                        if (data.contains("rating")) {
                            String ratingStr = data.substring(data.indexOf("\"rating\": ") + 10);
                            ratingStr = ratingStr.substring(0, ratingStr.indexOf(","));
                            rating = Integer.parseInt(ratingStr);
                        }
                        
                        if (data.contains("windspeed")) {
                            String windspeedStr = data.substring(data.indexOf("\"windspeed\": ") + 13);
                            windspeedStr = windspeedStr.substring(0, windspeedStr.indexOf(","));
                            windspeed = Float.parseFloat(windspeedStr);
                        }
                        
                        SurveyResult result = new SurveyResult(tornadoId, playerName, rating, windspeed);
                        completedSurveys.put(tornadoId, result);
                        
                        // FIXED: Update tornado data with survey results ON ALL CLIENTS
                        TornadoData tornado = TornadoTracker.getInstance().getTornadoData(tornadoId);
                        if (tornado != null) {
                            EASAddon.LOGGER.info("Updating tornado {} survey results on client: EF{}, {}mph by {}", 
                                tornadoId, rating, windspeed, playerName);
                            tornado.setSurveyResults(playerName, rating, windspeed);
                            TornadoTracker.getInstance().forceSave();
                        } else {
                            // FIXED: If tornado doesn't exist locally, create a basic entry to store survey results
                            EASAddon.LOGGER.warn("Tornado {} not found locally, creating minimal entry for survey results", tornadoId);
                            TornadoData newTornado = new TornadoData(tornadoId);
                            newTornado.setActive(false); // Mark as inactive since we didn't track it live
                            newTornado.setSurveyResults(playerName, rating, windspeed);
                            // Force the rating to be set after survey results
                            newTornado.setRating("EF" + rating);
                            newTornado.setMaxWindspeed(Math.round(windspeed));
                            
                            // Add to tracker
                            TornadoTracker.getInstance().addOrUpdateTornadoData(newTornado);
                            TornadoTracker.getInstance().forceSave();
                            
                            EASAddon.LOGGER.info("Created new tornado entry {} with survey results: EF{}, {}mph", 
                                tornadoId, rating, windspeed);
                        }
                        
                        // FIXED: Show notification to all clients about survey completion
                        Player localPlayer = Minecraft.getInstance().player;
                        if (localPlayer != null) {
                            if (playerName.equals(localPlayer.getName().getString())) {
                                localPlayer.sendSystemMessage(Component.literal("§a§lSurvey completed successfully!"));
                            } else {
                                localPlayer.sendSystemMessage(Component.literal("§6" + playerName + " completed survey of tornado " + tornadoId + " - Rating: EF" + rating));
                            }
                        }
                        
                        EASAddon.LOGGER.info("Survey completed for tornado {} by {}: EF{}, {}mph", 
                            tornadoId, playerName, rating, windspeed);
                        
                    } catch (Exception e) {
                        EASAddon.LOGGER.warn("Failed to parse survey finish data: {}", e.getMessage());
                    }
                }
            }
            
            case "quit" -> {
                activeSurveys.remove(tornadoId);
                
                Player localPlayer = Minecraft.getInstance().player;
                if (localPlayer != null) {
                    if (playerName.equals(localPlayer.getName().getString())) {
                        localPlayer.sendSystemMessage(Component.literal("§6Survey cancelled."));
                    } else {
                        localPlayer.sendSystemMessage(Component.literal("§6" + playerName + " cancelled survey of tornado " + tornadoId));
                    }
                }
            }
            
            case "error" -> {
                Player localPlayer = Minecraft.getInstance().player;
                if (localPlayer != null && playerName.equals(localPlayer.getName().getString())) {
                    localPlayer.sendSystemMessage(Component.literal("§cSurvey error: " + data));
                }
            }
        }
    }
    
    // Public accessors for GUI
    public ClientSurveyInfo getActiveSurvey(String playerName) {
        return activeSurveys.values().stream()
                .filter(survey -> survey.surveyorName.equals(playerName))
                .findFirst()
                .orElse(null);
    }
    
    public boolean isTornadoBeingSurveyed(long tornadoId) {
        return activeSurveys.containsKey(tornadoId);
    }
    
    public String getSurveyorName(long tornadoId) {
        ClientSurveyInfo survey = activeSurveys.get(tornadoId);
        return survey != null ? survey.surveyorName : null;
    }
    
    public SurveyResult getSurveyResults(long tornadoId) {
        return completedSurveys.get(tornadoId);
    }
}