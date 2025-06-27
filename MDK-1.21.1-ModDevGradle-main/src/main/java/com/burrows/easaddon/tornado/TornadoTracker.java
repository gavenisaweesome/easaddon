package com.burrows.easaddon.tornado;

import com.burrows.easaddon.EASAddon;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import com.burrows.easaddon.survey.ChunkDamageData;
import com.burrows.easaddon.survey.DamageSurveyManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.minecraft.util.Mth;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.level.ChunkEvent;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import net.minecraft.util.Mth;




@OnlyIn(Dist.CLIENT)
public class TornadoTracker {
    private static TornadoTracker instance;
    private final Map<Long, TornadoData> trackedTornadoes = new ConcurrentHashMap<>();
    private int tickCounter = 0;
    private String currentDimension = null;
    private String currentWorldId = null;
    
    // Persistence
    private boolean dataLoaded = false;
    
    // Reflection cache
    private Class<?> weatherHandlerClass;
    private Class<?> stormClass;
    private Field stormPositionField;
    private Field stormWindspeedField;
    private Field stormWidthField;
    private Field stormStageField;
    private Field stormIDField;
    private Field stormDeadField;
    private Field stormTypeField;
    private Method getStormsMethod;
    private boolean reflectionInitialized = false;
    
    private TornadoTracker() {
        initializeReflection();
    }
    
    public static TornadoTracker getInstance() {
        if (instance == null) {
            instance = new TornadoTracker();
        }
        return instance;
    }
    
    private void initializeReflection() {
        if (!EASAddon.isPMWeatherAvailable()) {
            return;
        }
        
        try {
            // Get PMWeather classes
            weatherHandlerClass = Class.forName("dev.protomanly.pmweather.weather.WeatherHandler");
            stormClass = Class.forName("dev.protomanly.pmweather.weather.Storm");
            
            // Get fields from Storm class
            stormPositionField = stormClass.getDeclaredField("position");
            stormPositionField.setAccessible(true);
            
            stormWindspeedField = stormClass.getDeclaredField("windspeed");
            stormWindspeedField.setAccessible(true);
            
            stormWidthField = stormClass.getDeclaredField("width");
            stormWidthField.setAccessible(true);
            
            stormStageField = stormClass.getDeclaredField("stage");
            stormStageField.setAccessible(true);
            
            stormIDField = stormClass.getDeclaredField("ID");
            stormIDField.setAccessible(true);
            
            stormDeadField = stormClass.getDeclaredField("dead");
            stormDeadField.setAccessible(true);
            
            stormTypeField = stormClass.getDeclaredField("stormType");
            stormTypeField.setAccessible(true);
            
            // Get the getStorms() method instead of accessing field directly
            getStormsMethod = weatherHandlerClass.getDeclaredMethod("getStorms");
            getStormsMethod.setAccessible(true);
            
            reflectionInitialized = true;
            EASAddon.LOGGER.info("Tornado tracker initialized successfully");
            
        } catch (Exception e) {
            EASAddon.LOGGER.error("Failed to initialize tornado tracker reflection", e);
            reflectionInitialized = false;
        }
    }
    
    @SubscribeEvent
    public void onLevelLoad(LevelEvent.Load event) {
        // Load data when level loads (client side only)
        if (event.getLevel().isClientSide() && event.getLevel() instanceof Level level) {
            String newWorldId = ClientTornadoPersistence.getCurrentWorldIdentifier();
            
            EASAddon.LOGGER.info("Level load detected - Current world: {}, New world: {}", currentWorldId, newWorldId);
            
            // Check if this is a completely different world (not just dimension change)
            if (ClientTornadoPersistence.isNewWorldSession(currentWorldId, newWorldId)) {
                EASAddon.LOGGER.info("NEW WORLD SESSION DETECTED - Clearing all tornado data");
                
                // Save current data before switching (if we have any)
                if (currentWorldId != null && !trackedTornadoes.isEmpty()) {
                    EASAddon.LOGGER.info("Saving {} tornado records for previous world: {}", 
                        trackedTornadoes.size(), currentWorldId);
                    saveCurrentWorldData();
                }
                
                // Clear all in-memory data for new world
                clearAllInMemoryData();
                
                // Update world tracking
                currentWorldId = newWorldId;
                currentDimension = level.dimension().location().toString();
                dataLoaded = false;
                
                EASAddon.LOGGER.info("Switched to new world session: {}", currentWorldId);
            }
            
            // Load data for current world
            loadDataForLevel(level);
        }
    }
    
    @SubscribeEvent
    public void onLevelUnload(LevelEvent.Unload event) {
        // Save data when level unloads (client side only)
        if (event.getLevel().isClientSide() && event.getLevel() instanceof Level level) {
            EASAddon.LOGGER.info("Level unload detected for world: {}", currentWorldId);
            saveDataForLevel(level);
            
            // DON'T clear currentWorldId here - we might just be changing dimensions
            // Let onLevelLoad handle world vs dimension detection
        }
    }
    
    
    private void clearAllInMemoryData() {
        EASAddon.LOGGER.info("Clearing all in-memory tornado data (world switch)");
        trackedTornadoes.clear();
        
        // Also clear any survey data that might be world-specific
        try {
            DamageSurveyManager.getInstance().clearWorldData();
        } catch (Exception e) {
            EASAddon.LOGGER.warn("Failed to clear survey data: {}", e.getMessage());
        }
        
        dataLoaded = false;
    }
    
    // ADDED: Save data for current world without clearing
    private void saveCurrentWorldData() {
        try {
            if (currentDimension != null && !trackedTornadoes.isEmpty()) {
                ClientTornadoPersistence.saveTornadoData(currentDimension, trackedTornadoes);
                EASAddon.LOGGER.info("Saved tornado data for world: {} (dimension: {})", currentWorldId, currentDimension);
            }
        } catch (Exception e) {
            EASAddon.LOGGER.error("Failed to save current world data: {}", e.getMessage());
        }
    }
    
    private void checkDimensionChange(Level level) {
        String newDimension = level.dimension().location().toString();
        String newWorldId = ClientTornadoPersistence.getCurrentWorldIdentifier();
        
        // Check for world change first
        if (ClientTornadoPersistence.isNewWorldSession(currentWorldId, newWorldId)) {
            EASAddon.LOGGER.info("World change detected during dimension check: {} -> {}", currentWorldId, newWorldId);
            
            // Save current data
            saveCurrentWorldData();
            
            // Clear and switch to new world
            clearAllInMemoryData();
            currentWorldId = newWorldId;
            currentDimension = newDimension;
            dataLoaded = false;
            
            // Load new world data
            loadDataForLevel(level);
            return;
        }
        
        // Handle dimension change within same world
        if (currentDimension == null) {
            currentDimension = newDimension;
            currentWorldId = newWorldId;
            dataLoaded = false; // Force reload
        } else if (!currentDimension.equals(newDimension)) {
            EASAddon.LOGGER.info("Dimension changed within same world: {} -> {} (world: {})", 
                currentDimension, newDimension, currentWorldId);
            
            // Save current dimension data
            saveDataForLevel(level);
            
            // Switch to new dimension (but same world)
            currentDimension = newDimension;
            dataLoaded = false; // Force reload for new dimension
            
            // Load new dimension data
            loadDataForLevel(level);
        }
    }
    
    // UPDATED: Enhanced data loading with world isolation
    private void loadDataForLevel(Level level) {
        try {
            String newDimension = level.dimension().location().toString();
            String newWorldId = ClientTornadoPersistence.getCurrentWorldIdentifier();
            
            EASAddon.LOGGER.info("Loading data for level - World: {}, Dimension: {}, Currently loaded: {}", 
                newWorldId, newDimension, dataLoaded);
            
            // Ensure we're tracking the current world
            if (currentWorldId == null || !currentWorldId.equals(newWorldId)) {
                EASAddon.LOGGER.info("World ID mismatch - updating tracking");
                currentWorldId = newWorldId;
                currentDimension = newDimension;
                dataLoaded = false;
            }
            
            // Only load if we haven't loaded data for this world/dimension yet
            if (!dataLoaded || !newDimension.equals(currentDimension)) {
                EASAddon.LOGGER.info("Loading tornado data for world: {} (dimension: {})", currentWorldId, newDimension);
                
                // Clear current data before loading
                trackedTornadoes.clear();
                
                // Load data for this specific dimension in this world
                Map<Long, TornadoData> loadedData = ClientTornadoPersistence.loadTornadoData(newDimension);
                trackedTornadoes.putAll(loadedData);
                
                currentDimension = newDimension;
                dataLoaded = true;
                
                EASAddon.LOGGER.info("Loaded {} tornado records for world: {} (dimension: {})", 
                    loadedData.size(), currentWorldId, newDimension);
            } else {
                EASAddon.LOGGER.info("Data already loaded for current world/dimension - skipping");
            }
            
        } catch (Exception e) {
            EASAddon.LOGGER.error("Failed to load tornado data: {}", e.getMessage());
            dataLoaded = true; // Continue anyway to prevent infinite reload attempts
        }
    }
    
    
 // Add this new method to TornadoTracker.java class

    /**
     * FIXED: Handle chunk loading events to retroactively calculate tornado damage
     * This solves the major bug where tornadoes beyond render distance don't get damage calculated
     */
    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        // Only process on client side where tornado tracking occurs
        if (!event.getLevel().isClientSide() || !reflectionInitialized) {
            return;
        }
        
        Level level = (Level) event.getLevel();
        ChunkPos chunkPos = event.getChunk().getPos();
        
        // Skip if this is initial world loading or if we don't have any tornado data yet
        if (!dataLoaded || trackedTornadoes.isEmpty()) {
            return;
        }
        
        try {
            // Check all active and recently inactive tornadoes (within last 5 minutes)
            long currentTime = System.currentTimeMillis();
            long recentCutoff = currentTime - 300000; // 5 minutes ago
            
            boolean foundRelevantTornado = false;
            
            for (TornadoData tornadoData : trackedTornadoes.values()) {
                // Skip very old inactive tornadoes
                if (!tornadoData.isActive() && tornadoData.getLastSeenTime() < recentCutoff) {
                    continue;
                }
                
                // Check if this tornado was close enough to damage this chunk
                if (shouldCalculateRetroactiveDamage(tornadoData, chunkPos)) {
                    EASAddon.LOGGER.info("Chunk ({}, {}) loaded - calculating retroactive damage for tornado {}", 
                        chunkPos.x, chunkPos.z, tornadoData.getId());
                    
                    calculateRetroactiveDamageForChunk(tornadoData, chunkPos, level);
                    foundRelevantTornado = true;
                }
            }
            
            if (foundRelevantTornado) {
                EASAddon.LOGGER.debug("Completed retroactive damage calculation for chunk ({}, {})", 
                    chunkPos.x, chunkPos.z);
            }
            
            // Also check if DamageSurveyManager has pending retroactive analysis for this chunk
            for (TornadoData tornadoData : trackedTornadoes.values()) {
                try {
                    if (DamageSurveyManager.getInstance().processRetroactiveDamageForChunk(
                            tornadoData.getId(), chunkPos, level)) {
                        EASAddon.LOGGER.debug("Processed pending retroactive damage for tornado {} in chunk ({}, {})", 
                            tornadoData.getId(), chunkPos.x, chunkPos.z);
                    }
                } catch (Exception e) {
                    // Continue processing other tornadoes
                }
            }
            
        } catch (Exception e) {
            EASAddon.LOGGER.error("Error during retroactive damage calculation for chunk ({}, {}): {}", 
                chunkPos.x, chunkPos.z, e.getMessage());
        }
    }

    /**
     * ADDED: Check if we should calculate retroactive damage for a tornado/chunk combination
     */
    private boolean shouldCalculateRetroactiveDamage(TornadoData tornadoData, ChunkPos chunkPos) {
        // Check if we already have damage data for this chunk
        if (tornadoData.getDamagedChunks().contains(chunkPos)) {
            return false; // Already tracked
        }
        
        // Calculate chunk center
        Vec3 chunkCenter = new Vec3(chunkPos.x * 16 + 8, 0, chunkPos.z * 16 + 8);
        
        // Check if tornado was ever close enough to damage this chunk
        double maxDamageRange = Math.max(tornadoData.getMaxWidth() * 2.0, 100.0);
        
        for (TornadoData.PositionRecord record : tornadoData.getPositionHistory()) {
            // Only consider records when tornado was at damaging strength
            if (record.windspeed >= 40) {
                double distance = record.position.distanceTo(chunkCenter);
                if (distance <= maxDamageRange) {
                    EASAddon.LOGGER.debug("Tornado {} was within range of chunk ({}, {}) - distance: {}m, max range: {}m", 
                        tornadoData.getId(), chunkPos.x, chunkPos.z, Math.round(distance), Math.round(maxDamageRange));
                    return true;
                }
            }
        }
        
        return false;
    }

    /**
     * ADDED: Calculate retroactive damage for a newly loaded chunk
     */
    private void calculateRetroactiveDamageForChunk(TornadoData tornadoData, ChunkPos chunkPos, Level level) {
        try {
            // Find the closest tornado approach to this chunk
            Vec3 chunkCenter = new Vec3(chunkPos.x * 16 + 8, 0, chunkPos.z * 16 + 8);
            TornadoData.PositionRecord closestRecord = null;
            double minDistance = Double.MAX_VALUE;
            
            for (TornadoData.PositionRecord record : tornadoData.getPositionHistory()) {
                if (record.windspeed >= 40) {
                    double distance = record.position.distanceTo(chunkCenter);
                    if (distance < minDistance) {
                        minDistance = distance;
                        closestRecord = record;
                    }
                }
            }
            
            if (closestRecord == null) {
                return;
            }
            
            // Use PMWeather's damage calculation logic
            int windfieldWidth = Math.max((int)closestRecord.width, 40);
            float damageRadius = windfieldWidth * 2.0f;
            
            // Only proceed if chunk is within damage radius
            if (minDistance > damageRadius) {
                return;
            }
            
            // Check if this chunk would have destroyable blocks
            if (checkChunkForDestroyableBlocks(chunkPos, closestRecord.position, 
                    closestRecord.width, closestRecord.windspeed, windfieldWidth, level)) {
                
                // Add chunk to tornado's damaged chunks list
                tornadoData.addDamagedChunk(chunkPos);
                
                // Create accurate damage evidence using existing method
                createAccurateDamageEvidence(tornadoData.getId(), chunkPos, 
                    closestRecord.windspeed, closestRecord.position, level, windfieldWidth);
                
                EASAddon.LOGGER.info("Retroactive damage calculated for tornado {} in chunk ({}, {}) - windspeed: {}mph, distance: {}m", 
                    tornadoData.getId(), chunkPos.x, chunkPos.z, closestRecord.windspeed, Math.round(minDistance));
            }
            
        } catch (Exception e) {
            EASAddon.LOGGER.error("Error calculating retroactive damage for tornado {} in chunk ({}, {}): {}", 
                tornadoData.getId(), chunkPos.x, chunkPos.z, e.getMessage());
        }
    }
    
    
    
    
    
    
    
    
    
    
    
    // UPDATED: Clear method with world session reset
    public void clearData() {
        EASAddon.LOGGER.info("Manual clear requested - clearing data for world: {}", currentWorldId);
        
        trackedTornadoes.clear();
        
        // Also clear the saved data for current dimension if we have world info
        if (currentDimension != null) {
            ClientTornadoPersistence.saveTornadoData(currentDimension, trackedTornadoes);
        }
        
        // Clear survey data too
        try {
            DamageSurveyManager.getInstance().clearWorldData();
        } catch (Exception e) {
            EASAddon.LOGGER.warn("Failed to clear survey data during manual clear: {}", e.getMessage());
        }
        
        EASAddon.LOGGER.info("Cleared all tornado tracking data for world: {}", currentWorldId);
    }
    
    // ADDED: Get current world identifier
    public String getCurrentWorldId() {
        return currentWorldId;
    }
    
    // ADDED: Force a world session reset (for debugging/admin use)
    public void forceWorldSessionReset() {
        EASAddon.LOGGER.info("FORCE RESET: Clearing world session and all data");
        clearAllInMemoryData();
        currentWorldId = null;
        currentDimension = null;
        dataLoaded = false;
    }
    
    
    @SubscribeEvent
    public void onLevelTick(LevelTickEvent.Pre event) {
        // Only track on client side and only every 20 ticks to reduce performance impact
        if (event.getLevel().isClientSide() && reflectionInitialized && ++tickCounter % 20 == 0) {
            // Check if dimension changed and load/save data if so
            checkDimensionChange(event.getLevel());
            
            // Ensure data is loaded for current level
            if (!dataLoaded) {
                loadDataForLevel(event.getLevel());
            }
            
            updateTornadoTracking(event.getLevel());
            
            // Auto-save every 30 seconds (600 ticks) to prevent data loss
            if (tickCounter % 600 == 0) {
                saveDataForLevel(event.getLevel());
            }
            
            // FIXED: Additional save on significant changes
            if (tickCounter % 20 == 0) { // Every second, check for changes
                checkAndSaveIfNeeded(event.getLevel());
            }
        }
    }
    
 
    
    private void checkAndSaveIfNeeded(Level level) {
        // Track if significant changes occurred
        int currentActiveCount = (int) trackedTornadoes.values().stream().filter(TornadoData::isActive).count();
        
        // Save if tornado count changed significantly or if any tornado became inactive
        boolean shouldSave = false;
        for (TornadoData tornado : trackedTornadoes.values()) {
            if (!tornado.isActive() && tornado.getLastSeenTime() > System.currentTimeMillis() - 30000) {
                // Tornado became inactive in the last 30 seconds
                shouldSave = true;
                break;
            }
        }
        
        if (shouldSave) {
            saveDataForLevel(level);
        }
    }
    
    private void saveDataForLevel(Level level) {
        try {
            if (currentDimension != null) {
                ClientTornadoPersistence.saveTornadoData(currentDimension, trackedTornadoes);
            }
        } catch (Exception e) {
            EASAddon.LOGGER.error("Failed to save tornado data: {}", e.getMessage());
        }
    }
    
  
    
private void updateTornadoTracking(Level level) {
    try {
        // Get the weather handler from the level
        Object weatherHandler = getWeatherHandler(level);
        if (weatherHandler == null) {
            return;
        }
        
        // Get the storms list using the getStorms() method
        List<?> storms = (List<?>) getStormsMethod.invoke(weatherHandler);
        if (storms == null) {
            return;
        }
        
        // FIXED: Track all storms that exist, not just stage 3+
        Set<Long> allExistingStormIds = new HashSet<>();
        
        // Process each storm
        for (Object storm : storms) {
            if (storm == null) continue;
            
            try {
                // Get storm data using reflection
                int stormType = (Integer) stormTypeField.get(storm);
                
                // Only track tornadoes (stormType 0)
                if (stormType != 0) continue;
                
                long stormId = (Long) stormIDField.get(storm);
                Vec3 position = (Vec3) stormPositionField.get(storm);
                int windspeed = (Integer) stormWindspeedField.get(storm);
                float width = (Float) stormWidthField.get(storm);
                int stage = (Integer) stormStageField.get(storm);
                boolean dead = (Boolean) stormDeadField.get(storm);
                
                // FIXED: Add all non-dead storms to existing list
                if (!dead) {
                    allExistingStormIds.add(stormId);
                }
                
                // Only start tracking if storm has reached stage 3 (confirmed tornado)
                if (stage >= 3) {
                    // Get or create tornado data
                    TornadoData tornadoData = trackedTornadoes.computeIfAbsent(stormId, TornadoData::new);
                    
                    // Update tornado data
                    if (!dead && windspeed > 0) { // Only record if tornado is alive and has wind
                        tornadoData.updateData(windspeed, width, stage, position);
                        trackDamagedChunks(storm, tornadoData);
                    } else if (dead || windspeed == 0) {
                        tornadoData.markInactive();
                    }
                } else if (trackedTornadoes.containsKey(stormId)) {
                    // FIXED: If we were tracking this storm but it's no longer stage 3, 
                    // continue updating but don't mark as inactive unless actually dead
                    TornadoData existingData = trackedTornadoes.get(stormId);
                    
                    if (!dead) {
                        // Storm is still alive, just not fully formed - keep it active
                        // Update with current data (even if windspeed is 0 temporarily)
                        existingData.updateData(windspeed, width, stage, position);
                        existingData.setLastSeenTime(System.currentTimeMillis());
                    } else {
                        // Storm is actually dead - mark inactive
                        existingData.markInactive();
                    }
                }
                
            } catch (Exception e) {
                // Skip individual storm errors
            }
        }
        
        // FIXED: Only mark storms as inactive if they're completely missing from the storms list
        // or if we haven't seen them in a while (safety check)
        long currentTime = System.currentTimeMillis();
        for (TornadoData tornado : trackedTornadoes.values()) {
            if (tornado.isActive()) {
                // Mark inactive only if:
                // 1. Storm ID is not in the current storms list, AND
                // 2. We haven't seen it for more than 30 seconds (safety buffer)
                if (!allExistingStormIds.contains(tornado.getId()) && 
                    (currentTime - tornado.getLastSeenTime()) > 30000) {
                    tornado.markInactive();
                    EASAddon.LOGGER.info("Marked tornado {} as inactive (missing from storms list for >30s)", 
                                       tornado.getId());
                }
            }
        }
        
    } catch (Exception e) {
        // Skip tracking errors
    }
}


// Replace the trackDamagedChunks method in TornadoTracker.java with this enhanced version

/**
 * FIXED: Track all chunks in tornado's damage radius, creating evidence for both loaded and unloaded chunks
 */
/**
 * ENHANCED: Track all chunks in tornado's damage radius, creating evidence for both loaded and unloaded chunks
 * This replaces the existing trackDamagedChunks method in TornadoTracker.java
 */
private void trackDamagedChunks(Object storm, TornadoData tornadoData) {
    try {
        int windspeed = (Integer) stormWindspeedField.get(storm);
        int stage = (Integer) stormStageField.get(storm);
        Vec3 position = (Vec3) stormPositionField.get(storm);
        float width = (Float) stormWidthField.get(storm);
        
        if (stage >= 3 && windspeed >= 40 && width >= 5.0f) {
            
            // Use PMWeather's actual damage radius calculation
            int windfieldWidth = Math.max((int)width, 40);
            float damageRadius = windfieldWidth * 2.0f; // PMWeather's real damage range
            
            // Cap at reasonable maximum to prevent excessive chunk checking
            damageRadius = Math.min(damageRadius, 200.0f);
            
            int chunkRadius = (int) Math.ceil(damageRadius / 16.0);
            ChunkPos centerChunk = new ChunkPos(new BlockPos((int)position.x, (int)position.y, (int)position.z));
            
            Level level = Minecraft.getInstance().level;
            if (level == null) return;
            
            // Track chunks that would have damage
            int chunksWithDamage = 0;
            int chunksProcessedRealTime = 0;
            int chunksMarkedForRetroactive = 0;
            
            for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
                for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                    ChunkPos chunkPos = new ChunkPos(centerChunk.x + dx, centerChunk.z + dz);
                    
                    // Calculate distance from chunk center to tornado
                    double chunkCenterX = chunkPos.x * 16 + 8;
                    double chunkCenterZ = chunkPos.z * 16 + 8;
                    double chunkDistance = position.multiply(1, 0, 1).distanceTo(new Vec3(chunkCenterX, 0, chunkCenterZ));
                    
                    // Only process chunks actually within damage radius
                    if (chunkDistance > damageRadius) continue;
                    
                    // ENHANCED: Check for destroyable blocks whether chunk is loaded or not
                    boolean hasDestroyableBlocks = checkChunkForDestroyableBlocks(
                        chunkPos, position, width, windspeed, windfieldWidth, level);
                    
                    if (hasDestroyableBlocks) {
                        // Add to tornado's damaged chunks list (this is crucial for later retroactive analysis)
                        tornadoData.addDamagedChunk(chunkPos);
                        chunksWithDamage++;
                        
                        if (level.hasChunkAt(chunkPos.getMinBlockX(), chunkPos.getMinBlockZ())) {
                            // LOADED CHUNK: Create damage evidence immediately
                            createAccurateDamageEvidence(tornadoData.getId(), chunkPos, 
                                windspeed, position, level, windfieldWidth);
                            chunksProcessedRealTime++;
                            
                            EASAddon.LOGGER.debug("Real-time damage processing for tornado {} in loaded chunk ({}, {})", 
                                tornadoData.getId(), chunkPos.x, chunkPos.z);
                        } else {
                            // UNLOADED CHUNK: Mark for retroactive processing when chunk loads
                            markChunkForRetroactiveDamage(tornadoData.getId(), chunkPos, 
                                windspeed, position, windfieldWidth);
                            chunksMarkedForRetroactive++;
                            
                            EASAddon.LOGGER.debug("Marked unloaded chunk ({}, {}) for retroactive damage calculation (tornado {})", 
                                chunkPos.x, chunkPos.z, tornadoData.getId());
                        }
                    }
                }
            }
            
            if (chunksWithDamage > 0) {
                EASAddon.LOGGER.info("Tornado {} damage tracking: {} total chunks, {} processed real-time, {} marked for retroactive", 
                    tornadoData.getId(), chunksWithDamage, chunksProcessedRealTime, chunksMarkedForRetroactive);
            }
        }
    } catch (Exception e) {
        EASAddon.LOGGER.error("Error tracking damaged chunks: {}", e.getMessage());
    }
}

/**
 * ADDED: Mark a chunk for retroactive damage calculation when it loads
 */
private void markChunkForRetroactiveDamage(long tornadoId, ChunkPos chunkPos, 
                                          int windspeed, Vec3 tornadoPos, int windfieldWidth) {
    try {
        // Create a placeholder damage record that indicates this chunk needs retroactive processing
        // This ensures the chunk is tracked even when unloaded
        DamageSurveyManager.getInstance().markChunkForRetroactiveAnalysis(
            tornadoId, chunkPos, windspeed, tornadoPos, windfieldWidth);
        
        EASAddon.LOGGER.debug("Marked chunk ({}, {}) for retroactive damage analysis (tornado {}, windspeed {}mph)", 
            chunkPos.x, chunkPos.z, tornadoId, windspeed);
            
    } catch (Exception e) {
        EASAddon.LOGGER.error("Error marking chunk for retroactive damage: {}", e.getMessage());
    }
}

/**
 * ADDED: Check if a chunk would have damage based on tornado parameters
 */
private boolean checkIfChunkWouldHaveDamage(ChunkPos chunkPos, Vec3 tornadoPos, 
                                           float width, int windspeed, int windfieldWidth, Level level) {
    // Sample several positions in the chunk
    int samplesWithDamage = 0;
    
    for (int sx = 4; sx < 16; sx += 4) {
        for (int sz = 4; sz < 16; sz += 4) {
            int worldX = chunkPos.x * 16 + sx;
            int worldZ = chunkPos.z * 16 + sz;
            
            // Calculate wind effect at this position
            Vec3 samplePos = new Vec3(worldX, tornadoPos.y, worldZ);
            double windEffect = calculateWindEffectAtPosition(
                samplePos, tornadoPos, windfieldWidth, windspeed);
            
            // If wind is strong enough to damage typical blocks
            if (windEffect >= 50) { // Grass/dirt threshold
                samplesWithDamage++;
                if (samplesWithDamage >= 2) {
                    return true; // At least 2 positions would have damage
                }
            }
        }
    }
    
    return samplesWithDamage > 0;
}

private boolean checkChunkForDestroyableBlocks(ChunkPos chunkPos, Vec3 tornadoPos, 
        float width, int windspeed, int windfieldWidth, Level level) {
// If chunk is loaded, we can check actual blocks
if (level.hasChunkAt(chunkPos.getMinBlockX(), chunkPos.getMinBlockZ())) {
int destroyableBlocks = 0;

// Sample positions in the chunk
for (int x = 2; x < 16; x += 4) {
for (int z = 2; z < 16; z += 4) {
BlockPos worldPos = new BlockPos(chunkPos.x * 16 + x, 0, chunkPos.z * 16 + z);
BlockPos surfacePos = level.getHeightmapPos(
net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, worldPos);

// Skip if outside reasonable height bounds
if (surfacePos.getY() < level.getMinBuildHeight() || 
surfacePos.getY() > level.getMaxBuildHeight()) {
continue;
}

// Check blocks at and above surface
for (int dy = -1; dy <= 2; dy++) {
BlockPos checkPos = surfacePos.offset(0, dy, 0);
BlockState blockState = level.getBlockState(checkPos);

if (blockState.isAir() || !blockState.getFluidState().isEmpty()) {
continue;
}

// Calculate wind effect at this position
double windEffect = calculateWindEffectAtPosition(
checkPos.getCenter(), tornadoPos, windfieldWidth, windspeed);

// Calculate block strength
float blockStrength = getBlockStrengthPMWeather(blockState.getBlock(), level);

// Check if tornado would destroy this block
if (windEffect >= blockStrength) {
destroyableBlocks++;

// If we find enough destroyable blocks, this chunk has real damage potential
if (destroyableBlocks >= 2) {
return true;
}
}
}
}
}

return destroyableBlocks > 0; // At least one block would be destroyed
} else {
// For unloaded chunks, estimate based on tornado strength and distance
double chunkCenterX = chunkPos.x * 16 + 8;
double chunkCenterZ = chunkPos.z * 16 + 8;
Vec3 chunkCenter = new Vec3(chunkCenterX, tornadoPos.y, chunkCenterZ);

// Calculate average wind effect in the chunk
double avgWindEffect = calculateWindEffectAtPosition(chunkCenter, tornadoPos, windfieldWidth, windspeed);

// Assume chunk has destroyable blocks if wind is strong enough
// This threshold matches typical grass/vegetation strength
return avgWindEffect >= 50.0;
}
}

/**
 * ADDED: Create damage evidence for a chunk (works for both loaded and unloaded)
 */
private void createDamageEvidenceForChunk(long tornadoId, ChunkPos chunkPos, int windspeed,
                                          Vec3 tornadoPos, int windfieldWidth, Level level) {
    // Get or create damage data for this tornado/chunk
    Map<ChunkPos, ChunkDamageData> damageMap = DamageSurveyManager.getInstance()
        .getTornadoDamageData(tornadoId);
    
    if (damageMap != null && damageMap.containsKey(chunkPos)) {
        // Already has damage data (from real damage or previous calculation)
        return;
    }
    
    // Sample positions and create evidence
    int sampleCount = Math.min(8, 3 + (windspeed / 50));
    
    for (int i = 0; i < sampleCount; i++) {
        int x = chunkPos.x * 16 + level.random.nextInt(16);
        int z = chunkPos.z * 16 + level.random.nextInt(16);
        
        BlockPos surfacePos;
        if (level.hasChunkAt(new BlockPos(x, 0, z))) {
            // Loaded chunk - get actual surface
            surfacePos = level.getHeightmapPos(
                net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, 
                new BlockPos(x, 0, z));
        } else {
            // Unloaded chunk - assume typical height
            surfacePos = new BlockPos(x, 64 + level.random.nextInt(10), z);
        }
        
        // Calculate wind effect
        double windEffect = calculateWindEffectAtPosition(
            surfacePos.getCenter(), tornadoPos, windfieldWidth, windspeed);
        
        // Create appropriate damage evidence
        if (windEffect >= 50) {
            createBlockDamageEvidence(tornadoId, chunkPos, surfacePos, windEffect, windspeed, level);
        }
        
        if (windEffect >= 140) {
            createDebarkingEvidence(tornadoId, chunkPos, surfacePos, windEffect, level);
        }
        
        if (windEffect >= 140) {
            createScouringEvidence(tornadoId, chunkPos, surfacePos, windEffect, level);
        }
    }
}

/**
 * Create block damage evidence
 */
private void createBlockDamageEvidence(long tornadoId, ChunkPos chunkPos, BlockPos surfacePos,
                                       double windEffect, int tornadoWindspeed, Level level) {
    // Check multiple heights
    for (int dy = -1; dy <= 2; dy++) {
        BlockPos checkPos = surfacePos.offset(0, dy, 0);
        
        if (level.hasChunkAt(checkPos)) {
            BlockState state = level.getBlockState(checkPos);
            if (!state.isAir() && state.getFluidState().isEmpty()) {
                float blockStrength = getBlockStrengthPMWeather(state.getBlock(), level);
                
                if (windEffect >= blockStrength) {
                    DamageSurveyManager.getInstance().addDamage(
                        tornadoId, chunkPos, checkPos, state,
                        net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),
                        tornadoWindspeed, level);
                    return;
                }
            }
        } else {
            // Unloaded chunk - simulate typical damage
            if (dy == 0 && windEffect >= 50) {
                DamageSurveyManager.getInstance().addDamage(
                    tornadoId, chunkPos, checkPos,
                    net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState(),
                    net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),
                    tornadoWindspeed, level);
                return;
            }
        }
    }
}

/**
 * Create debarking evidence
 */
private void createDebarkingEvidence(long tornadoId, ChunkPos chunkPos, BlockPos surfacePos,
                                     double windEffect, Level level) {
    // Check area for logs
    for (int dx = -2; dx <= 2; dx++) {
        for (int dy = 0; dy <= 3; dy++) {
            for (int dz = -2; dz <= 2; dz++) {
                BlockPos checkPos = surfacePos.offset(dx, dy, dz);
                
                if (level.hasChunkAt(checkPos)) {
                    BlockState state = level.getBlockState(checkPos);
                    if (state.is(net.minecraft.tags.BlockTags.LOGS)) {
                        double probability = Math.min((windEffect - 140.0) / 20.0, 1.0);
                        if (level.random.nextDouble() < probability * 0.3) {
                            DamageSurveyManager.getInstance().addDebarkingEvidence(
                                tornadoId, chunkPos, checkPos);
                            return;
                        }
                    }
                } else if (!level.hasChunkAt(checkPos) && dy > 0 && dy < 3) {
                    // Unloaded chunk - assume some logs in forest
                    if (windEffect >= 160 && level.random.nextDouble() < 0.1) {
                        DamageSurveyManager.getInstance().addDebarkingEvidence(
                            tornadoId, chunkPos, checkPos);
                        return;
                    }
                }
            }
        }
    }
}

/**
 * Create scouring evidence
 */
private void createScouringEvidence(long tornadoId, ChunkPos chunkPos, BlockPos surfacePos,
                                    double windEffect, Level level) {
    if (level.hasChunkAt(surfacePos)) {
        BlockState surfaceState = level.getBlockState(surfacePos);
        
        if (surfaceState.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK) && windEffect >= 140) {
            double probability = Math.min((windEffect - 140.0) / 80.0, 1.0);
            if (level.random.nextDouble() < probability * 0.15) {
                DamageSurveyManager.getInstance().addScouringEvidence(
                    tornadoId, chunkPos, surfacePos, ChunkDamageData.ScouringLevel.GRASS_TO_DIRT);
            }
        } else if (surfaceState.is(net.minecraft.world.level.block.Blocks.DIRT) && windEffect >= 170) {
            double probability = Math.min((windEffect - 170.0) / 40.0, 1.0);
            if (level.random.nextDouble() < probability * 0.1) {
                DamageSurveyManager.getInstance().addScouringEvidence(
                    tornadoId, chunkPos, surfacePos, ChunkDamageData.ScouringLevel.DIRT_TO_MEDIUM);
            }
        }
    } else {
        // Unloaded chunk - simulate scouring
        if (windEffect >= 180 && level.random.nextDouble() < 0.1) {
            DamageSurveyManager.getInstance().addScouringEvidence(
                tornadoId, chunkPos, surfacePos, ChunkDamageData.ScouringLevel.DIRT_TO_MEDIUM);
        }
    }
}

 /**
  * Check if a chunk actually contains blocks that would be destroyed by this tornado
  * This prevents false positives by only including chunks with destroyable content
  */
 

 /**
  * Create accurate damage evidence based on blocks that would actually be destroyed
  */
 /**
  * Calculate wind effect using PMWeather's logic from Storm.getWind()
  */
 private double calculateWindEffectAtPosition(Vec3 pos, Vec3 tornadoPos, int windfieldWidth, int windspeed) {
     
     // Distance calculation (2D)
     double dist = tornadoPos.multiply(1.0, 0.0, 1.0).distanceTo(pos.multiply(1.0, 0.0, 1.0));
     
     // Rankine vortex calculation (from Storm.getRankine())
     float rankineFactor = 4.5f;
     float rankineWidth = (float)windfieldWidth / rankineFactor;
     float perc = 0.0f;
     
     if (dist <= (double)(rankineWidth / 2.0f)) {
         perc = (float)dist / (rankineWidth / 2.0f);
     } else if (dist <= (double)((float)windfieldWidth * 2.0f)) {
         double denominator = (((float)windfieldWidth * 2.0f - rankineWidth) / 2.0f);
         if (denominator > 0) {
             perc = (float)Math.pow(1.0 - (dist - (double)(rankineWidth / 2.0f)) / denominator, 1.5);
             perc = Math.max(0.0f, Math.min(1.0f, perc));
         }
     }
     
     if (Float.isNaN(perc)) {
         perc = 0.0f;
     }
     
     // Rotational component
     Vec3 relativePos = pos.subtract(tornadoPos);
     Vec3 rotational = new Vec3(relativePos.z, 0.0, -relativePos.x).normalize();
     Vec3 motion = rotational.multiply((double)windspeed * (double)perc, 0.0, (double)windspeed * (double)perc);
     
     // Translational component (simplified)
     float affectPerc = (float)Math.sqrt(Math.max(0, 1.0 - dist / (double)((float)windfieldWidth * 2.0f)));
     motion = motion.add(new Vec3(15.0 * affectPerc, 0.0, 15.0 * affectPerc));
     
     return motion.length();
 }

 /**
  * PMWeather's exact block strength calculation
  */
 private static float getBlockStrengthPMWeather(net.minecraft.world.level.block.Block block, Level level) {
     net.minecraft.world.item.ItemStack item = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_AXE);
     float destroySpeed = block.defaultBlockState().getDestroySpeed(level, BlockPos.ZERO);
     
     try {
         destroySpeed /= item.getDestroySpeed(block.defaultBlockState());
     } catch (Exception e) {
         destroySpeed = 1.0f;
     }
     
     return 60.0f + net.minecraft.util.Mth.sqrt(destroySpeed) * 60.0f;
 }

// REMOVED: All the theoretical damage creation methods
// - calculateMaxWindEffectForChunk
// - calculateWindEffectAtPosition  
// - createRealisticDamageEvidence
// - getBlockStrengthPMWeather

// These were creating fake damage records instead of tracking real damage

/**
 * Get block strength using PMWeather's exact calculation from Storm.getBlockStrength()


/**
 * Create damage evidence for chunks affected by tornado
 * This simulates the damage that PMWeather tornadoes would cause
 */


private Object getWeatherHandler(Level level) {
        try {
            // Method 1: Look for WeatherHandlerClient class specifically (since we're on client side)
            try {
                Class<?> weatherHandlerClientClass = Class.forName("dev.protomanly.pmweather.weather.WeatherHandlerClient");
                
                // Look for static instances or fields
                Field[] staticFields = weatherHandlerClientClass.getDeclaredFields();
                for (Field field : staticFields) {
                    if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                        try {
                            field.setAccessible(true);
                            Object fieldValue = field.get(null);
                            if (fieldValue != null && weatherHandlerClass.isInstance(fieldValue)) {
                                return fieldValue;
                            }
                        } catch (Exception e) {
                            // Continue trying
                        }
                    }
                }
                
                // Try to find getInstance methods
                try {
                    Method[] methods = weatherHandlerClientClass.getDeclaredMethods();
                    for (Method method : methods) {
                        if (java.lang.reflect.Modifier.isStatic(method.getModifiers()) && 
                            method.getName().contains("getInstance")) {
                            try {
                                method.setAccessible(true);
                                Object result = method.invoke(null);
                                if (result != null && weatherHandlerClass.isInstance(result)) {
                                    return result;
                                }
                            } catch (Exception e) {
                                // Continue trying
                            }
                        }
                    }
                } catch (Exception e) {
                    // Continue
                }
                
            } catch (ClassNotFoundException e) {
                // Continue to next method
            }
            
            // Method 2: Look for PMWeather main class and check for weather handler instances
            try {
                Class<?> pmweatherMainClass = Class.forName("dev.protomanly.pmweather.PMWeather");
                Field[] pmFields = pmweatherMainClass.getDeclaredFields();
                
                for (Field field : pmFields) {
                    try {
                        field.setAccessible(true);
                        Object fieldValue = field.get(null);
                        if (fieldValue != null && weatherHandlerClass.isInstance(fieldValue)) {
                            return fieldValue;
                        }
                        
                        // Check if it's a map or collection that might contain weather handlers
                        if (fieldValue instanceof java.util.Map) {
                            java.util.Map<?, ?> map = (java.util.Map<?, ?>) fieldValue;
                            for (Object value : map.values()) {
                                if (value != null && weatherHandlerClass.isInstance(value)) {
                                    return value;
                                }
                            }
                        }
                        
                        if (fieldValue instanceof java.util.Collection) {
                            java.util.Collection<?> collection = (java.util.Collection<?>) fieldValue;
                            for (Object value : collection) {
                                if (value != null && weatherHandlerClass.isInstance(value)) {
                                    return value;
                                }
                            }
                        }
                        
                    } catch (Exception e) {
                        // Continue trying other fields
                    }
                }
            } catch (ClassNotFoundException e) {
                // Continue
            }
            
            // Method 3: Look in level fields
            Field[] levelFields = level.getClass().getDeclaredFields();
            for (Field field : levelFields) {
                field.setAccessible(true);
                try {
                    Object fieldValue = field.get(level);
                    if (fieldValue != null && weatherHandlerClass.isInstance(fieldValue)) {
                        return fieldValue;
                    }
                } catch (Exception e) {
                    // Skip fields that can't be accessed
                }
            }
            
            // Method 4: Look in superclass fields
            Class<?> levelSuperClass = level.getClass().getSuperclass();
            if (levelSuperClass != null) {
                Field[] superFields = levelSuperClass.getDeclaredFields();
                for (Field field : superFields) {
                    field.setAccessible(true);
                    try {
                        Object fieldValue = field.get(level);
                        if (fieldValue != null && weatherHandlerClass.isInstance(fieldValue)) {
                            return fieldValue;
                        }
                    } catch (Exception e) {
                        // Skip fields that can't be accessed
                    }
                }
            }
            
            // Method 5: Try alternative search
            Object altResult = findWeatherHandlerAlternative();
            if (altResult != null) {
                return altResult;
            }
            
            return null;
            
        } catch (Exception e) {
            return null;
        }
    }
    
    // Try to find weather handler through alternative means
    private Object findWeatherHandlerAlternative() {
        try {
            // Look for any static weather handler instances across all PMWeather classes
            String[] classesToCheck = {
                "dev.protomanly.pmweather.PMWeather",
                "dev.protomanly.pmweather.weather.WeatherHandlerClient", 
                "dev.protomanly.pmweather.weather.WeatherManager",
                "dev.protomanly.pmweather.ClientSetup",
                "dev.protomanly.pmweather.event.GameBusClientEvents"
            };
            
            for (String className : classesToCheck) {
                try {
                    Class<?> clazz = Class.forName(className);
                    
                    // Check static fields
                    Field[] fields = clazz.getDeclaredFields();
                    for (Field field : fields) {
                        if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                            try {
                                field.setAccessible(true);
                                Object value = field.get(null);
                                if (value != null && weatherHandlerClass.isInstance(value)) {
                                    return value;
                                }
                                
                                // Check maps and collections
                                if (value instanceof java.util.Map) {
                                    for (Object mapValue : ((java.util.Map<?, ?>) value).values()) {
                                        if (mapValue != null && weatherHandlerClass.isInstance(mapValue)) {
                                            return mapValue;
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                // Continue
                            }
                        }
                    }
                    
                } catch (ClassNotFoundException e) {
                    // Class doesn't exist, continue
                }
            }
            
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    
 // ENHANCED: Replace the createAccurateDamageEvidence method in TornadoTracker.java

    /**
     * ENHANCED: Create comprehensive damage evidence including debarking and scouring
     */
    private void createAccurateDamageEvidence(long tornadoId, ChunkPos chunkPos, int windspeed, 
                                            Vec3 tornadoPos, Level level, int windfieldWidth) {
        
        // Sample random positions like PMWeather does
        int sampleCount = Math.min(12, 4 + (windspeed / 40)); // More samples for stronger tornadoes
        
        for (int i = 0; i < sampleCount; i++) {
            // Random position within chunk
            int x = chunkPos.x * 16 + level.random.nextInt(16);
            int z = chunkPos.z * 16 + level.random.nextInt(16);
            
            BlockPos surfacePos = level.getHeightmapPos(
                net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, 
                new BlockPos(x, 0, z));
            
            // Check multiple heights for different types of damage
            checkForBlockDamage(tornadoId, chunkPos, surfacePos, tornadoPos, windspeed, windfieldWidth, level);
            checkForDebarkingEvidence(tornadoId, chunkPos, surfacePos, tornadoPos, windspeed, windfieldWidth, level);
            checkForScouringEvidence(tornadoId, chunkPos, surfacePos, tornadoPos, windspeed, windfieldWidth, level);
        }
        
        // ADDED: Scan for existing evidence that may have been missed
        scanChunkForExistingEvidence(tornadoId, chunkPos, level);
    }

    /**
     * ENHANCED: Check for block damage evidence
     */
    private void checkForBlockDamage(long tornadoId, ChunkPos chunkPos, BlockPos surfacePos, 
                                    Vec3 tornadoPos, int windspeed, int windfieldWidth, Level level) {
        
        // Check blocks at and above surface (where most damage occurs)
        for (int dy = -1; dy <= 2; dy++) {
            BlockPos pos = surfacePos.offset(0, dy, 0);
            
            if (pos.getY() < level.getMinBuildHeight() || pos.getY() > level.getMaxBuildHeight()) {
                continue;
            }
            
            net.minecraft.world.level.block.state.BlockState blockState = level.getBlockState(pos);
            
            if (blockState.isAir() || !blockState.getFluidState().isEmpty()) {
                continue;
            }
            
            // Calculate wind effect
            double windEffect = calculateWindEffectAtPosition(
                pos.getCenter(), tornadoPos, windfieldWidth, windspeed);
            
            // Calculate block strength
            float blockStrength = getBlockStrengthPMWeather(blockState.getBlock(), level);
            
            // Only create damage record if wind would destroy this block
            if (windEffect >= blockStrength) {
                DamageSurveyManager.getInstance().addDamage(
                    tornadoId, 
                    chunkPos, 
                    pos, 
                    blockState, 
                    net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),
                    windspeed, 
                    level
                );
                
        
                
                // Limit evidence per sample point
                return;
            }
        }
    }

    /**
     * ADDED: Check for debarking evidence based on PMWeather's logic
     */
    private void checkForDebarkingEvidence(long tornadoId, ChunkPos chunkPos, BlockPos surfacePos,
                                          Vec3 tornadoPos, int windspeed, int windfieldWidth, Level level) {
        
        // Only check for debarking if tornado has sufficient windspeed
        if (windspeed < 140) return;
        
        // Check area around surface position for logs
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -1; dy <= 3; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos pos = surfacePos.offset(dx, dy, dz);
                    
                    if (pos.getY() < level.getMinBuildHeight() || pos.getY() > level.getMaxBuildHeight()) {
                        continue;
                    }
                    
                    net.minecraft.world.level.block.state.BlockState blockState = level.getBlockState(pos);
                    
                    // Check if this is a log that would be debarked
                    if (blockState.is(net.minecraft.tags.BlockTags.LOGS) && 
                        !blockState.is(net.neoforged.neoforge.common.Tags.Blocks.STRIPPED_LOGS)) {
                        
                        // Calculate wind effect at this position
                        double windEffect = calculateWindEffectAtPosition(
                            pos.getCenter(), tornadoPos, windfieldWidth, windspeed);
                        
                        // PMWeather's debarking threshold is 140 mph
                        if (windEffect >= 140.0) {
                            // Calculate probability like PMWeather does
                            double probability = Math.min((windEffect - 140.0) / 20.0, 1.0);
                            
                            // Use a conservative probability to avoid over-estimating
                            if (level.random.nextDouble() < probability * 0.3) {
                                DamageSurveyManager.getInstance().addDebarkingEvidence(tornadoId, chunkPos, pos);
                                
                                EASAddon.LOGGER.debug("Debarking evidence: Tornado {} would debark log at {} " +
                                    "(wind: {}mph, probability: {}%)", 
                                    tornadoId, pos, Math.round(windEffect), Math.round(probability * 100));
                                
                                return; // Found one debarking instance
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * ADDED: Check for ground scouring evidence based on PMWeather's logic
     */
    private void checkForScouringEvidence(long tornadoId, ChunkPos chunkPos, BlockPos surfacePos,
                                         Vec3 tornadoPos, int windspeed, int windfieldWidth, Level level) {
        
        // Only check for scouring if tornado has sufficient windspeed
        if (windspeed < 140) return;
        
        net.minecraft.world.level.block.state.BlockState surfaceState = level.getBlockState(surfacePos);
        
        // Calculate wind effect at surface
        double windEffect = calculateWindEffectAtPosition(
            surfacePos.getCenter(), tornadoPos, windfieldWidth, windspeed);
        
        // Check for grass -> dirt scouring (140+ mph)
        if (surfaceState.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK) && windEffect >= 140.0) {
            double probability = Math.min((windEffect - 140.0) / 80.0, 1.0);
            
            if (level.random.nextDouble() < probability * 0.1) { // Conservative probability
                DamageSurveyManager.getInstance().addScouringEvidence(
                    tornadoId, chunkPos, surfacePos, ChunkDamageData.ScouringLevel.GRASS_TO_DIRT);
                
                EASAddon.LOGGER.debug("Light scouring evidence: Tornado {} would scour grass at {} " +
                    "(wind: {}mph, probability: {}%)", 
                    tornadoId, surfacePos, Math.round(windEffect), Math.round(probability * 100));
            }
        }
        
        // Check for dirt -> medium scouring (170+ mph)
        else if (surfaceState.is(net.minecraft.world.level.block.Blocks.DIRT) && windEffect >= 170.0) {
            double probability = Math.min((windEffect - 170.0) / 40.0, 1.0);
            
            if (level.random.nextDouble() < probability * 0.1) { // Conservative probability
                DamageSurveyManager.getInstance().addScouringEvidence(
                    tornadoId, chunkPos, surfacePos, ChunkDamageData.ScouringLevel.DIRT_TO_MEDIUM);
                
                EASAddon.LOGGER.debug("Medium scouring evidence: Tornado {} would create medium scouring at {} " +
                    "(wind: {}mph, probability: {}%)", 
                    tornadoId, surfacePos, Math.round(windEffect), Math.round(probability * 100));
            }
        }
        
        // Check for existing medium scouring -> heavy scouring (200+ mph)
        else if (isMediumScouringBlock(surfaceState) && windEffect >= 200.0) {
            double probability = Math.min((windEffect - 200.0) / 30.0, 1.0);
            
            if (level.random.nextDouble() < probability * 0.1) { // Conservative probability
                DamageSurveyManager.getInstance().addScouringEvidence(
                    tornadoId, chunkPos, surfacePos, ChunkDamageData.ScouringLevel.MEDIUM_TO_HEAVY);
                
                EASAddon.LOGGER.debug("Heavy scouring evidence: Tornado {} would create heavy scouring at {} " +
                    "(wind: {}mph, probability: {}%)", 
                    tornadoId, surfacePos, Math.round(windEffect), Math.round(probability * 100));
            }
        }
    }

    /**
     * ADDED: Scan chunk for existing debarking/scouring evidence
     */
    private void scanChunkForExistingEvidence(long tornadoId, ChunkPos chunkPos, Level level) {
        try {
            int evidenceFound = 0;
            
            // Sample positions across the chunk
            for (int x = 2; x < 16; x += 4) {
                for (int z = 2; z < 16; z += 4) {
                    BlockPos worldPos = new BlockPos(chunkPos.x * 16 + x, 0, chunkPos.z * 16 + z);
                    BlockPos surfacePos = level.getHeightmapPos(
                        net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, worldPos);
                    
                    // Check for existing stripped logs in natural areas
                    for (int dy = -2; dy <= 4; dy++) {
                        BlockPos checkPos = surfacePos.offset(0, dy, 0);
                        if (checkPos.getY() < level.getMinBuildHeight() || checkPos.getY() > level.getMaxBuildHeight()) {
                            continue;
                        }
                        
                        net.minecraft.world.level.block.state.BlockState state = level.getBlockState(checkPos);
                        
                        // Check for debarking evidence
                        if (state.is(net.neoforged.neoforge.common.Tags.Blocks.STRIPPED_LOGS) && 
                            isInNaturalForestArea(checkPos, level)) {
                            
                            DamageSurveyManager.getInstance().addDebarkingEvidence(tornadoId, chunkPos, checkPos);
                            evidenceFound++;
                            
                            EASAddon.LOGGER.debug("Found existing debarking evidence at {}", checkPos);
                        }
                    }
                    
                    // Check surface for scouring evidence
                    net.minecraft.world.level.block.state.BlockState surfaceState = level.getBlockState(surfacePos);
                    ChunkDamageData.ScouringLevel scouringLevel = detectScouringLevel(surfaceState, surfacePos, level);
                    
                    if (scouringLevel != null) {
                        DamageSurveyManager.getInstance().addScouringEvidence(tornadoId, chunkPos, surfacePos, scouringLevel);
                        evidenceFound++;
                        
                        EASAddon.LOGGER.debug("Found existing {} scouring evidence at {}", 
                            scouringLevel.name(), surfacePos);
                    }
                    
                    // Limit evidence scanning to prevent performance issues
                    if (evidenceFound >= 3) break;
                }
                if (evidenceFound >= 3) break;
            }
            
        } catch (Exception e) {
            EASAddon.LOGGER.debug("Error scanning for existing evidence: {}", e.getMessage());
        }
    }

    /**
     * ADDED: Check if position is in a natural forest area
     */
    private boolean isInNaturalForestArea(BlockPos pos, Level level) {
        int treeBlocks = 0;
        int totalChecked = 0;
        
        // Check area around position for natural tree features
        for (BlockPos nearby : BlockPos.betweenClosed(pos.offset(-4, -2, -4), pos.offset(4, 4, 4))) {
            if (!level.isLoaded(nearby)) continue;
            
            net.minecraft.world.level.block.state.BlockState state = level.getBlockState(nearby);
            totalChecked++;
            
            if (state.is(net.minecraft.tags.BlockTags.LOGS) || 
                state.is(net.minecraft.tags.BlockTags.LEAVES) ||
                state.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) {
                treeBlocks++;
            }
        }
        
        // If more than 25% of nearby blocks are natural tree/grass, this is likely a natural forest
        return totalChecked > 0 && (double)treeBlocks / totalChecked > 0.25;
    }

    /**
     * ADDED: Detect scouring level from block state
     */
    private ChunkDamageData.ScouringLevel detectScouringLevel(net.minecraft.world.level.block.state.BlockState state, 
                                                             BlockPos pos, Level level) {
        try {
            // Check for PMWeather scouring blocks
            if (isHeavyScouringBlock(state)) {
                return ChunkDamageData.ScouringLevel.MEDIUM_TO_HEAVY;
            } else if (isMediumScouringBlock(state)) {
                return ChunkDamageData.ScouringLevel.DIRT_TO_MEDIUM;
            }
            
            // Check for dirt in areas that should have grass
            if (state.is(net.minecraft.world.level.block.Blocks.DIRT) && isInNaturalGrassArea(pos, level)) {
                return ChunkDamageData.ScouringLevel.GRASS_TO_DIRT;
            }
            
        } catch (Exception e) {
            // Continue without error
        }
        
        return null;
    }

    /**
     * ADDED: Helper methods for block type detection
     */
    private boolean isMediumScouringBlock(net.minecraft.world.level.block.state.BlockState state) {
        try {
            Class<?> modBlocks = Class.forName("dev.protomanly.pmweather.block.ModBlocks");
            Object mediumScouringHolder = modBlocks.getField("MEDIUM_SCOURING").get(null);
            Object mediumScouringBlock = mediumScouringHolder.getClass().getMethod("get").invoke(mediumScouringHolder);
            return state.getBlock() == mediumScouringBlock;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isHeavyScouringBlock(net.minecraft.world.level.block.state.BlockState state) {
        try {
            Class<?> modBlocks = Class.forName("dev.protomanly.pmweather.block.ModBlocks");
            Object heavyScouringHolder = modBlocks.getField("HEAVY_SCOURING").get(null);
            Object heavyScouringBlock = heavyScouringHolder.getClass().getMethod("get").invoke(heavyScouringHolder);
            return state.getBlock() == heavyScouringBlock;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isInNaturalGrassArea(BlockPos pos, Level level) {
        int grassBlocks = 0;
        int totalChecked = 0;
        
        // Check surrounding area for grass
        for (BlockPos nearby : BlockPos.betweenClosed(pos.offset(-3, 0, -3), pos.offset(3, 0, 3))) {
            if (!level.isLoaded(nearby) || nearby.equals(pos)) continue;
            
            net.minecraft.world.level.block.state.BlockState state = level.getBlockState(nearby);
            totalChecked++;
            
            if (state.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) {
                grassBlocks++;
            }
        }
        
        // If more than 40% of nearby blocks are grass, this dirt is likely scoured grass
        return totalChecked > 0 && (double)grassBlocks / totalChecked > 0.4;
    }
    
    public List<TornadoData> getAllTornadoData() {
        return new ArrayList<>(trackedTornadoes.values());
    }
    
    public List<TornadoData> getActiveTornadoData() {
        return trackedTornadoes.values().stream()
                .filter(TornadoData::isActive)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    public TornadoData getTornadoData(long id) {
        return trackedTornadoes.get(id);
    }
    
    // FIXED: Add method to add/update tornado data (for survey results from other clients)
    public void addOrUpdateTornadoData(TornadoData tornadoData) {
        trackedTornadoes.put(tornadoData.getId(), tornadoData);
        EASAddon.LOGGER.info("Added/updated tornado data for ID: {}", tornadoData.getId());
    }
    
    public int getTotalTornadoCount() {
        return trackedTornadoes.size();
    }
    
    public int getActiveTornadoCount() {
        return (int) trackedTornadoes.values().stream().filter(TornadoData::isActive).count();
    }
    

    
    public void forceUpdate() {
        if (Minecraft.getInstance().level != null) {
            updateTornadoTracking(Minecraft.getInstance().level);
        }
    }
    
    public void removeInactiveTornado(long id) {
        TornadoData tornado = trackedTornadoes.get(id);
        if (tornado != null && !tornado.isActive()) {
            trackedTornadoes.remove(id);
            EASAddon.LOGGER.info("Removed inactive tornado with ID: {}", id);
        }
    }
    
    public void forceSave() {
        if (Minecraft.getInstance().level != null) {
            saveDataForLevel(Minecraft.getInstance().level);
        }
    }
    
    public boolean isTrackingEnabled() {
        return reflectionInitialized && EASAddon.isPMWeatherAvailable();
    }
}