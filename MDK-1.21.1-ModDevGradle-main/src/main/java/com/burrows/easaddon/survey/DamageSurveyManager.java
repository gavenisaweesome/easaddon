package com.burrows.easaddon.survey;

import com.burrows.easaddon.EASAddon;
import com.burrows.easaddon.tornado.TornadoData;
import com.burrows.easaddon.tornado.TornadoTracker;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.world.level.chunk.LevelChunk;
import java.util.concurrent.CompletableFuture;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the damage surveying process for tornadoes
 */
@OnlyIn(Dist.CLIENT)
public class DamageSurveyManager {
    private static DamageSurveyManager instance;
    
    // Active survey sessions
    private final Map<Long, SurveySession> activeSurveys = new ConcurrentHashMap<>();
    
    // Tornado damage data storage
    private final Map<Long, Map<ChunkPos, ChunkDamageData>> tornadoDamageData = new ConcurrentHashMap<>();
    private final Map<Long, String> activeSurveyIds = new HashMap<>();
    private final Set<ChunkPos> forceLoadedChunks = new HashSet<>();
    private static final int CHUNK_LOAD_TIMEOUT_MS = 3000;
    
    // ADDED: Reflection for custom block strengths
    private Class<?> serverConfigClass;
    private Field blockStrengthsField;
    private boolean reflectionInitialized = false;
    
    public static class SurveySession {
        public final long tornadoId;
        public final String playerName;
        public final long startTime;
        public final List<ChunkPos> targetChunks;
        public final Set<ChunkPos> surveyedChunks;
        public ChunkPos currentTargetChunk;
        public int requiredSurveys;
        public boolean canFinish;
        
        public SurveySession(long tornadoId, String playerName, List<ChunkPos> chunks) {
            this.tornadoId = tornadoId;
            this.playerName = playerName;
            this.startTime = System.currentTimeMillis();
            this.targetChunks = new ArrayList<>(chunks);
            this.surveyedChunks = new HashSet<>();
            this.requiredSurveys = Math.max(1, chunks.size() / 4); // 1/4 requirement
            this.canFinish = false;
            
            // Set initial target to nearest chunk
            this.currentTargetChunk = chunks.isEmpty() ? null : chunks.get(0);
        }
        
        public float getProgress() {
            return targetChunks.isEmpty() ? 1.0f : (float) surveyedChunks.size() / requiredSurveys;
        }
        
        public boolean meetsMinimumRequirement() {
            return surveyedChunks.size() >= requiredSurveys;
        }
    }
    
    private DamageSurveyManager() {
        initializeReflection();
    }
    
    public static DamageSurveyManager getInstance() {
        if (instance == null) {
            instance = new DamageSurveyManager();
        }
        return instance;
    }
    
    // ADDED: Initialize reflection for custom block strengths
    private void initializeReflection() {
        if (!EASAddon.isPMWeatherAvailable()) {
            return;
        }
        
        try {
            serverConfigClass = Class.forName("dev.protomanly.pmweather.config.ServerConfig");
            blockStrengthsField = serverConfigClass.getDeclaredField("blockStrengths");
            blockStrengthsField.setAccessible(true);
            
            reflectionInitialized = true;
            EASAddon.LOGGER.info("DamageSurveyManager initialized with custom block strength support");
            
        } catch (Exception e) {
            EASAddon.LOGGER.error("Failed to initialize DamageSurveyManager reflection for custom block strengths", e);
            reflectionInitialized = false;
        }
    }
    
    /**
     * NEW: Check if a survey would result in a rating downgrade
     */
    private boolean wouldDowngradeRating(TornadoData tornadoData, int newRating) {
        // Check if tornado has already been surveyed
        if (!tornadoData.isSurveyed()) {
            return false; // No existing rating to downgrade from
        }
        
        int existingRating = tornadoData.getSurveyedEFRating();
        
        // Allow re-survey if new rating is equal or higher
        if (newRating >= existingRating) {
            return false;
        }
        
        // This would be a downgrade
        EASAddon.LOGGER.warn("Survey attempt would downgrade tornado {} from EF{} to EF{}", 
            tornadoData.getId(), existingRating, newRating);
        return true;
    }
    
    /**
     * Start a damage survey for a tornado
     * FIXED: Ensure client and server use the same filtered chunk list with correct damage calculations
     */
public boolean startSurvey(TornadoData tornadoData, Player player) {
    ClientSurveyManager clientSurveyManager;
    long tornadoId = tornadoData.getId();
    String playerName = player.getName().getString();
    
    // CRITICAL FIX: Prevent surveys on active tornadoes - ADD THIS CHECK ONLY
    if (tornadoData.isActive()) {
        player.sendSystemMessage((Component)Component.literal((String)"§c§l=== SURVEY BLOCKED ==="));
        player.sendSystemMessage((Component)Component.literal((String)"§cCannot start damage survey on an active tornado!"));
        player.sendSystemMessage((Component)Component.literal((String)"§cTornado ID " + tornadoId + " is still ongoing."));
        player.sendSystemMessage((Component)Component.literal((String)"§c"));
        player.sendSystemMessage((Component)Component.literal((String)"§eWait for the tornado to dissipate completely"));
        player.sendSystemMessage((Component)Component.literal((String)"§ebefore conducting a damage survey."));
        player.sendSystemMessage((Component)Component.literal((String)"§c"));
        player.sendSystemMessage((Component)Component.literal((String)"§7Current status: §cACTIVE"));
        player.sendSystemMessage((Component)Component.literal((String)"§7Max windspeed: §a" + tornadoData.getMaxWindspeed() + " mph"));
        
        // Play warning sound
        player.level().playSound(null, player.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0f, 0.8f);
        
        EASAddon.LOGGER.warn("Player {} attempted to survey active tornado {}", playerName, tornadoId);
        return false;
    }
    // END OF ADDED CHECK - rest of method stays exactly the same
    
    if (tornadoData.isSurveyed()) {
        int existingRating = tornadoData.getSurveyedEFRating();
        String existingSurveyor = tornadoData.getSurveyedBy();
        if (!playerName.equals(existingSurveyor)) {
            player.sendSystemMessage((Component)Component.literal((String)"\u00a7e=== RE-SURVEY WARNING ==="));
            player.sendSystemMessage((Component)Component.literal((String)("\u00a7eThis tornado was already surveyed by: \u00a7a" + existingSurveyor)));
            player.sendSystemMessage((Component)Component.literal((String)("\u00a7eCurrent rating: \u00a7a" + this.formatEFRating(existingRating))));
            player.sendSystemMessage((Component)Component.literal((String)"\u00a7c"));
            player.sendSystemMessage((Component)Component.literal((String)("\u00a7cNote: If your survey results in a lower rating than EF" + existingRating + ",")));
            player.sendSystemMessage((Component)Component.literal((String)"\u00a7cthe results will be rejected to prevent data corruption."));
            player.sendSystemMessage((Component)Component.literal((String)"\u00a7c"));
        }
    }
    if ((clientSurveyManager = ClientSurveyManager.getInstance()).isTornadoBeingSurveyed(tornadoId)) {
        String surveyorName = clientSurveyManager.getSurveyorName(tornadoId);
        player.sendSystemMessage((Component)Component.literal((String)("\u00a7cTornado is already being surveyed by " + surveyorName)));
        return false;
    }
    if (clientSurveyManager.getActiveSurvey(playerName) != null) {
        player.sendSystemMessage((Component)Component.literal((String)"\u00a7cYou are already surveying another tornado. Use /survey quit to cancel."));
        return false;
    }
    if (this.activeSurveys.containsKey(tornadoId)) {
        SurveySession existing = this.activeSurveys.get(tornadoId);
        player.sendSystemMessage((Component)Component.literal((String)("\u00a7cTornado is already being surveyed by " + existing.playerName)));
        return false;
    }
    Set<ChunkPos> damagedChunks = tornadoData.getDamagedChunks();
    if (damagedChunks.isEmpty()) {
        player.sendSystemMessage((Component)Component.literal((String)"\u00a7cNo damaged chunks found for this tornado"));
        player.sendSystemMessage((Component)Component.literal((String)"\u00a77This tornado may not have caused any trackable damage,"));
        player.sendSystemMessage((Component)Component.literal((String)"\u00a77or it was too weak to destroy blocks."));
        return false;
    }
    List<ChunkPos> validChunks = this.filterChunksWithActualDamageEvidence(tornadoId, damagedChunks, player);
    if (validChunks.isEmpty()) {
        player.sendSystemMessage((Component)Component.literal((String)"\u00a7cNo surveyable damage evidence found for this tornado"));
        player.sendSystemMessage((Component)Component.literal((String)"\u00a77The tornado damaged chunks but no survey data was captured."));
        player.sendSystemMessage((Component)Component.literal((String)"\u00a77This may indicate the damage occurred before the mod was active."));
        return false;
    }
    Vec3 playerPos = player.position();
    validChunks.sort((a, b) -> {
        double distA = this.getDistanceToChunk(playerPos, (ChunkPos)a);
        double distB = this.getDistanceToChunk(playerPos, (ChunkPos)b);
        return Double.compare(distA, distB);
    });
    boolean networkStartSuccess = clientSurveyManager.startSurvey(tornadoId, validChunks, player);
    if (!networkStartSuccess) {
        player.sendSystemMessage((Component)Component.literal((String)"\u00a7cFailed to start networked survey"));
        return false;
    }
    SurveySession session = new SurveySession(tornadoId, playerName, validChunks);
    this.activeSurveys.put(tornadoId, session);
    player.sendSystemMessage((Component)Component.literal((String)"\u00a76=== DAMAGE SURVEY STARTED ==="));
    player.sendSystemMessage((Component)Component.literal((String)("\u00a7eTornado ID: " + tornadoId)));
    player.sendSystemMessage((Component)Component.literal((String)("\u00a7eChunks with actual damage: " + validChunks.size())));
    player.sendSystemMessage((Component)Component.literal((String)("\u00a7eRequired surveys: " + session.requiredSurveys + " (25%)")));
    this.showActualDamageSummary(tornadoId, validChunks, player);
    player.sendSystemMessage((Component)Component.literal((String)"\u00a7b"));
    player.sendSystemMessage((Component)Component.literal((String)"\u00a7bInstructions:"));
    player.sendSystemMessage((Component)Component.literal((String)"\u00a77\u2022 Navigate to highlighted chunks"));
    player.sendSystemMessage((Component)Component.literal((String)"\u00a77\u2022 Right-click with surveyor tool in chunk"));
    player.sendSystemMessage((Component)Component.literal((String)"\u00a77\u2022 Rate damage based on strongest evidence"));
    player.sendSystemMessage((Component)Component.literal((String)"\u00a77\u2022 Use /survey quit to stop early"));
    this.guideToNextChunk(player, session);
    EASAddon.LOGGER.info("Started survey for tornado {} with {} chunks containing REAL damage (required: {})", new Object[]{tornadoId, validChunks.size(), session.requiredSurveys});
    return true;
}

    /**
     * SIMPLIFIED: Filter chunks to only include those with actual damage evidence
     * Much simpler now since tornadoData.getDamagedChunks() should only contain real damage
     */
    private List<ChunkPos> filterChunksWithActualDamageEvidence(long tornadoId, Set<ChunkPos> candidateChunks, Player player) {
    List<ChunkPos> validChunks = new ArrayList<>();
    Map<ChunkPos, ChunkDamageData> tornadoChunks = tornadoDamageData.get(tornadoId);
    Level level = player.level();
    
    int totalCandidates = candidateChunks.size();
    int chunksWithEvidence = 0;
    int chunksForceLoaded = 0;
    
    EASAddon.LOGGER.info("Survey: Starting chunk damage evidence validation for {} candidate chunks", totalCandidates);
    
    // STEP 1: Force load all candidate chunks to ensure damage calculations can run
    Set<ChunkPos> chunksToLoad = new HashSet<>();
    for (ChunkPos chunk : candidateChunks) {
        if (!level.hasChunk(chunk.x, chunk.z)) {
            chunksToLoad.add(chunk);
        }
    }
    
    if (!chunksToLoad.isEmpty()) {
        EASAddon.LOGGER.info("Survey: Force loading {} unloaded chunks for damage analysis", chunksToLoad.size());
        
        // Force load chunks both client and server-side
        forceLoadChunksForSurvey(chunksToLoad, level);
        chunksForceLoaded = chunksToLoad.size();
        
        // Wait a moment for chunks to fully load and damage calculations to process
        try {
            Thread.sleep(500); // Give time for chunks to load and calculations to run
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            EASAddon.LOGGER.warn("Survey: Interrupted while waiting for chunk loading");
        }
    }
    
    // STEP 2: Run damage recalculation for newly loaded chunks
    recalculateDamageForLoadedChunks(tornadoId, chunksToLoad, level);
    
    // STEP 3: Check for damage evidence (now that chunks are loaded)
    if (tornadoChunks != null) {
        for (ChunkPos chunk : candidateChunks) {
            ChunkDamageData data = tornadoChunks.get(chunk);
            
            if (data != null && data.hasDamage()) {
                validChunks.add(chunk);
                chunksWithEvidence++;
                
                EASAddon.LOGGER.debug("Survey: Chunk ({}, {}) has {} real damage records, max intensity: {}", 
                    chunk.x, chunk.z, data.getDamageCount(), data.getMaxDamageIntensity());
            } else {
                // Try to analyze the chunk directly if no stored damage data
                if (analyzeChunkForDamageEvidence(tornadoId, chunk, level)) {
                    validChunks.add(chunk);
                    chunksWithEvidence++;
                    EASAddon.LOGGER.debug("Survey: Chunk ({}, {}) found damage evidence via direct analysis", 
                        chunk.x, chunk.z);
                }
            }
        }
    } else {
        // No stored tornado data - perform direct analysis on all chunks
        EASAddon.LOGGER.info("Survey: No stored tornado data found, performing direct chunk analysis");
        for (ChunkPos chunk : candidateChunks) {
            if (analyzeChunkForDamageEvidence(tornadoId, chunk, level)) {
                validChunks.add(chunk);
                chunksWithEvidence++;
                EASAddon.LOGGER.debug("Survey: Chunk ({}, {}) found damage evidence via direct analysis", 
                    chunk.x, chunk.z);
            }
        }
    }
    
    // STEP 4: Log results and cleanup
    EASAddon.LOGGER.info("Survey: Damage evidence validation complete:");
    EASAddon.LOGGER.info("  Total candidates: {}", totalCandidates);
    EASAddon.LOGGER.info("  Chunks force loaded: {}", chunksForceLoaded);
    EASAddon.LOGGER.info("  Chunks with evidence: {}", chunksWithEvidence);
    EASAddon.LOGGER.info("  Valid chunks found: {}", validChunks.size());
    
    // Clean up force loaded chunks after a delay (let survey process use them first)
    scheduleChunkCleanup(chunksToLoad, level, 30000); // Clean up after 30 seconds
    
    return validChunks;
}

/**
 * Force load chunks for survey analysis on both client and server side
 */
private void forceLoadChunksForSurvey(Set<ChunkPos> chunks, Level level) {
    for (ChunkPos chunk : chunks) {
        try {
            if (level.isClientSide()) {
                // CLIENT SIDE: Request chunk loading
                EASAddon.LOGGER.debug("Survey: Force loading chunk ({}, {}) on client", chunk.x, chunk.z);
                LevelChunk loadedChunk = level.getChunk(chunk.x, chunk.z);
                if (loadedChunk != null) {
                    forceLoadedChunks.add(chunk);
                    EASAddon.LOGGER.debug("Survey: Successfully loaded chunk ({}, {}) on client", chunk.x, chunk.z);
                }
            } else {
                // SERVER SIDE: Force load chunk
                if (level instanceof ServerLevel serverLevel) {
                    EASAddon.LOGGER.debug("Survey: Force loading chunk ({}, {}) on server", chunk.x, chunk.z);
                    serverLevel.setChunkForced(chunk.x, chunk.z, true);
                    forceLoadedChunks.add(chunk);
                    EASAddon.LOGGER.debug("Survey: Successfully force loaded chunk ({}, {}) on server", chunk.x, chunk.z);
                }
            }
        } catch (Exception e) {
            EASAddon.LOGGER.error("Survey: Failed to force load chunk ({}, {}): {}", chunk.x, chunk.z, e.getMessage());
        }
    }
}

/**
 * Recalculate damage for newly loaded chunks by simulating tornado interaction
 */
private void recalculateDamageForLoadedChunks(long tornadoId, Set<ChunkPos> loadedChunks, Level level) {
    if (loadedChunks.isEmpty()) return;
    
    EASAddon.LOGGER.info("Survey: Recalculating damage for {} newly loaded chunks", loadedChunks.size());
    
    // Get tornado data to understand the tornado's path and intensity
    TornadoData tornadoData = TornadoTracker.getInstance().getTornadoData(tornadoId);
    if (tornadoData == null) {
        EASAddon.LOGGER.warn("Survey: No tornado data found for ID {}, cannot recalculate damage", tornadoId);
        return;
    }
    
    try {
        // For each newly loaded chunk, scan for blocks that should have been damaged
        for (ChunkPos chunkPos : loadedChunks) {
            // OPTIMIZATION: Skip if we already have damage data for this chunk
            if (hasDamageDataForChunk(tornadoId, chunkPos)) {
                EASAddon.LOGGER.debug("Survey: Chunk ({}, {}) already has damage data, skipping recalculation", 
                    chunkPos.x, chunkPos.z);
                continue;
            }
            
            analyzeChunkForRetroactiveDamage(tornadoId, chunkPos, level, tornadoData);
        }
    } catch (Exception e) {
        EASAddon.LOGGER.error("Survey: Error during damage recalculation: {}", e.getMessage());
    }
}

/**
 * Analyze a chunk for retroactive damage evidence
 */
private void analyzeChunkForRetroactiveDamage(long tornadoId, ChunkPos chunkPos, Level level, TornadoData tornadoData) {
    try {
        LevelChunk chunk = level.getChunk(chunkPos.x, chunkPos.z);
        int chunkStartX = chunkPos.x * 16;
        int chunkStartZ = chunkPos.z * 16;
        
        // Get tornado's closest approach to this chunk
        Vec3 chunkCenter = new Vec3(chunkStartX + 8, 0, chunkStartZ + 8);
        double minDistanceToTornado = Double.MAX_VALUE;
        Vec3 closestTornadoPos = null;
        int maxWindspeedAtChunk = 0;
        
        // Find closest tornado position to this chunk
        for (TornadoData.PositionRecord record : tornadoData.getPositionHistory()) {
            Vec3 tornadoPos = record.position;
            double distance = tornadoPos.distanceTo(chunkCenter);
            if (distance < minDistanceToTornado) {
                minDistanceToTornado = distance;
                closestTornadoPos = tornadoPos;
                maxWindspeedAtChunk = record.windspeed;
            }
        }
        
        if (closestTornadoPos == null) return;
        
        // Only analyze if tornado was close enough to cause damage
        double maxDamageRange = Math.max(tornadoData.getMaxWidth() * 2.0, 80.0); // Similar to PMWeather logic
        if (minDistanceToTornado > maxDamageRange) {
            EASAddon.LOGGER.debug("Survey: Chunk ({}, {}) too far from tornado path ({}m > {}m)", 
                chunkPos.x, chunkPos.z, Math.round(minDistanceToTornado), Math.round(maxDamageRange));
            return;
        }
        
        // Sample blocks in the chunk for evidence of damage
        int evidenceFound = 0;
        for (int x = 0; x < 16; x += 2) { // Sample every other block for performance
            for (int z = 0; z < 16; z += 2) {
                BlockPos surfacePos = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, 
                    new BlockPos(chunkStartX + x, 0, chunkStartZ + z));
                
                if (checkBlockForDamageEvidence(surfacePos, level, closestTornadoPos, maxWindspeedAtChunk)) {
                    evidenceFound++;
                    
                    // Record this as damage evidence
                    recordRetroactiveDamage(tornadoId, chunkPos, surfacePos, level);
                }
            }
        }
        
        if (evidenceFound > 0) {
            EASAddon.LOGGER.info("Survey: Found {} damage evidence points in chunk ({}, {}) via retroactive analysis", 
                evidenceFound, chunkPos.x, chunkPos.z);
        }
        
    } catch (Exception e) {
        EASAddon.LOGGER.error("Survey: Error analyzing chunk ({}, {}) for retroactive damage: {}", 
            chunkPos.x, chunkPos.z, e.getMessage());
    }
}

/**
 * Check if a specific block shows evidence of tornado damage
 */
private boolean checkBlockForDamageEvidence(BlockPos pos, Level level, Vec3 tornadoPos, int tornadoWindspeed) {
    try {
        BlockState currentState = level.getBlockState(pos);
        
        // Look for common tornado damage indicators:
        // 1. Missing vegetation where there should be some
        // 2. Exposed dirt/ground at surface level
        // 3. Scattered debris patterns
        // 4. Unnatural air blocks at surface level
        
        if (currentState.isAir()) {
            // Air block at surface level could indicate removed vegetation/structures
            BlockPos below = pos.below();
            BlockState belowState = level.getBlockState(below);
            
            // If there's dirt/grass below an air block at surface, likely vegetation was removed
            if (belowState.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK) || 
                belowState.is(net.minecraft.world.level.block.Blocks.DIRT)) {
                
                double distanceToTornado = tornadoPos.distanceTo(pos.getCenter());
                // Simple wind effect calculation
                double windEffect = tornadoWindspeed * (1.0 - (distanceToTornado / 100.0));
                
                if (windEffect > 40.0) { // Minimum wind to remove vegetation
                    return true;
                }
            }
        }
        
        // Check for debarking evidence on logs
        if (currentState.is(net.minecraft.world.level.block.Blocks.STRIPPED_OAK_LOG) ||
            currentState.is(net.minecraft.world.level.block.Blocks.STRIPPED_BIRCH_LOG) ||
            currentState.is(net.minecraft.world.level.block.Blocks.STRIPPED_SPRUCE_LOG) ||
            currentState.is(net.minecraft.world.level.block.Blocks.STRIPPED_DARK_OAK_LOG) ||
            currentState.is(net.minecraft.world.level.block.Blocks.STRIPPED_ACACIA_LOG) ||
            currentState.is(net.minecraft.world.level.block.Blocks.STRIPPED_JUNGLE_LOG)) {
            
            double distanceToTornado = tornadoPos.distanceTo(pos.getCenter());
            double windEffect = tornadoWindspeed * (1.0 - (distanceToTornado / 100.0));
            
            if (windEffect > 140.0) { // Minimum wind for debarking
                return true;
            }
        }
        
        return false;
        
    } catch (Exception e) {
        return false;
    }
}

/**
 * Record retroactive damage evidence
 */
private void recordRetroactiveDamage(long tornadoId, ChunkPos chunkPos, BlockPos pos, Level level) {
    Map<ChunkPos, ChunkDamageData> tornadoChunks = tornadoDamageData.computeIfAbsent(tornadoId, k -> new ConcurrentHashMap<>());
    ChunkDamageData chunkData = tornadoChunks.computeIfAbsent(chunkPos, ChunkDamageData::new);
    
    BlockState currentState = level.getBlockState(pos);
    BlockState presumedOriginal = currentState.isAir() ? 
        net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState() : 
        currentState; // For stripped logs, we assume they were normal logs
    
    // Add damage record
    chunkData.addDamage(pos, presumedOriginal, currentState, 60.0f, 100); // Estimated values
    
    EASAddon.LOGGER.debug("Survey: Recorded retroactive damage at {} in chunk ({}, {})", 
        pos, chunkPos.x, chunkPos.z);
}

/**
 * Direct analysis of chunk for damage evidence (when no stored data exists)
 */
private boolean analyzeChunkForDamageEvidence(long tornadoId, ChunkPos chunk, Level level) {
    try {
        TornadoData tornadoData = TornadoTracker.getInstance().getTornadoData(tornadoId);
        if (tornadoData == null) return false;
        
        // Quick check: was tornado close enough to this chunk to cause damage?
        Vec3 chunkCenter = new Vec3(chunk.x * 16 + 8, 0, chunk.z * 16 + 8);
        double minDistance = Double.MAX_VALUE;
        
        for (TornadoData.PositionRecord record : tornadoData.getPositionHistory()) {
            Vec3 tornadoPos = record.position;
            double distance = tornadoPos.distanceTo(chunkCenter);
            minDistance = Math.min(minDistance, distance);
        }
        
        double maxDamageRange = Math.max(tornadoData.getMaxWidth() * 2.0, 80.0);
        if (minDistance <= maxDamageRange) {
            // Tornado was close enough - likely damaged this chunk
            EASAddon.LOGGER.debug("Survey: Chunk ({}, {}) within tornado damage range ({}m <= {}m)", 
                chunk.x, chunk.z, Math.round(minDistance), Math.round(maxDamageRange));
            
            // Perform retroactive analysis
            analyzeChunkForRetroactiveDamage(tornadoId, chunk, level, tornadoData);
            
            // Check if we found any evidence
            Map<ChunkPos, ChunkDamageData> tornadoChunks = tornadoDamageData.get(tornadoId);
            if (tornadoChunks != null) {
                ChunkDamageData data = tornadoChunks.get(chunk);
                return data != null && data.hasDamage();
            }
        }
        
        return false;
        
    } catch (Exception e) {
        EASAddon.LOGGER.error("Survey: Error analyzing chunk ({}, {}) for damage evidence: {}", 
            chunk.x, chunk.z, e.getMessage());
        return false;
    }
}

/**
 * Schedule cleanup of force-loaded chunks after survey completion
 */
private void scheduleChunkCleanup(Set<ChunkPos> chunksToClean, Level level, long delayMs) {
    if (chunksToClean.isEmpty()) return;
    
    // Use a separate thread to clean up after delay
    CompletableFuture.delayedExecutor(delayMs, TimeUnit.MILLISECONDS).execute(() -> {
        cleanupForceLoadedChunks(chunksToClean, level);
    });
}

/**
 * Clean up force-loaded chunks
 */
private void cleanupForceLoadedChunks(Set<ChunkPos> chunksToClean, Level level) {
    EASAddon.LOGGER.info("Survey: Cleaning up {} force-loaded chunks", chunksToClean.size());
    
    for (ChunkPos chunk : chunksToClean) {
        try {
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.setChunkForced(chunk.x, chunk.z, false);
                forceLoadedChunks.remove(chunk);
                EASAddon.LOGGER.debug("Survey: Released force-loaded chunk ({}, {}) on server", chunk.x, chunk.z);
            } else {
                // On client side, just remove from tracking
                forceLoadedChunks.remove(chunk);
                EASAddon.LOGGER.debug("Survey: Removed chunk ({}, {}) from client tracking", chunk.x, chunk.z);
            }
        } catch (Exception e) {
            EASAddon.LOGGER.error("Survey: Error cleaning up chunk ({}, {}): {}", chunk.x, chunk.z, e.getMessage());
        }
    }
}

/**
 * ENHANCED: Add cleanup method to be called when mod shuts down
 */
public void shutdown() {
    EASAddon.LOGGER.info("Survey: Shutting down damage survey manager, cleaning up {} force-loaded chunks", 
        forceLoadedChunks.size());
    
    // Clean up any remaining force-loaded chunks
    Level level = Minecraft.getInstance().level;
    if (level != null) {
        cleanupForceLoadedChunks(new HashSet<>(forceLoadedChunks), level);
    }
    
    forceLoadedChunks.clear();
}

    /**
     * Estimate EF rating from actual damage intensity
     */
    private int estimateEFRatingFromIntensity(float maxIntensity) {
        if (maxIntensity > 200) return 5; // EF5
        if (maxIntensity >= 166) return 4; // EF4
        if (maxIntensity >= 136) return 3; // EF3
        if (maxIntensity >= 111) return 2; // EF2
        if (maxIntensity >= 86) return 1;  // EF1
        if (maxIntensity >= 65) return 0;  // EF0
        return 0; // Default to EF0 for any damage
    }
    
    /**
     * Handle a survey action at a specific position
     */
    public boolean handleSurveyAction(Player player, BlockPos pos) {
        String playerName = player.getName().getString();
        
        // FIXED: Check both local and networked survey state
        ClientSurveyManager clientSurveyManager = ClientSurveyManager.getInstance();
        
        // Check networked survey state first
        ClientSurveyManager.ClientSurveyInfo networkSurvey = clientSurveyManager.getActiveSurvey(playerName);
        if (networkSurvey == null) {
            player.sendSystemMessage(Component.literal("§cYou are not currently surveying a tornado"));
            return false;
        }
        
        // Find corresponding local survey session
        SurveySession session = activeSurveys.get(networkSurvey.tornadoId);
        if (session == null) {
            player.sendSystemMessage(Component.literal("§cLocal survey session not found"));
            return false;
        }
        
        ChunkPos currentChunk = new ChunkPos(pos);
        
        // Check if player is in a valid chunk for this survey
        if (!session.targetChunks.contains(currentChunk)) {
            player.sendSystemMessage(Component.literal("§cThis chunk was not damaged by the tornado"));
            return false;
        }
        
        // Check if chunk already surveyed
        if (session.surveyedChunks.contains(currentChunk)) {
            player.sendSystemMessage(Component.literal("§cThis chunk has already been surveyed"));
            return false;
        }
        
        // Get damage data for this chunk
        Map<ChunkPos, ChunkDamageData> tornadoChunks = tornadoDamageData.get(session.tornadoId);
        if (tornadoChunks == null) {
            player.sendSystemMessage(Component.literal("§cNo damage data found for this chunk"));
            return false;
        }
        
        ChunkDamageData chunkData = tornadoChunks.get(currentChunk);
        if (chunkData == null || !chunkData.hasDamage()) {
            player.sendSystemMessage(Component.literal("§cNo damage evidence found in this chunk"));
            return false;
        }
        
        // FIXED: Calculate EF rating based on evidence with proper minimum enforcement
        int efRating = calculateEFRating(chunkData);
        float maxWindspeed = calculateMaxWindspeed(chunkData);
        
        // Mark chunk as surveyed
        chunkData.markSurveyed(playerName, efRating, maxWindspeed);
        session.surveyedChunks.add(currentChunk);
        
        // Update session state
        session.canFinish = session.meetsMinimumRequirement();
        
        // Send feedback to player
        player.sendSystemMessage(Component.literal("§a✓ Chunk surveyed successfully!"));

        // Show detailed evidence breakdown
        String evidenceSummary = chunkData.getEvidenceSummary();
        player.sendSystemMessage(Component.literal("§7Evidence: " + evidenceSummary));

        player.sendSystemMessage(Component.literal("§7Enhanced rating: §e" + formatEFRating(efRating)));
        player.sendSystemMessage(Component.literal("§7Enhanced windspeed: §e" + Math.round(maxWindspeed) + " mph"));

        // Show specific evidence contributions
        if (!chunkData.getDebarkedLogs().isEmpty()) {
            player.sendSystemMessage(Component.literal("§c  + Debarking evidence (≥140mph → EF3+)"));
        }

        Map<ChunkDamageData.ScouringLevel, Integer> scouringCounts = new HashMap<>();
        for (ChunkDamageData.ScouringLevel level : chunkData.getScouringEvidence().values()) {
            scouringCounts.merge(level, 1, Integer::sum);
        }

        for (Map.Entry<ChunkDamageData.ScouringLevel, Integer> entry : scouringCounts.entrySet()) {
            String levelName = switch (entry.getKey()) {
                case GRASS_TO_DIRT -> "Light scouring (≥140mph → EF3+)";
                case DIRT_TO_MEDIUM -> "Medium scouring (≥170mph → EF4+)";  
                case MEDIUM_TO_HEAVY -> "Heavy scouring (≥200mph → EF5)";
            };
            player.sendSystemMessage(Component.literal("§c  + " + levelName + " x" + entry.getValue()));
        }

        player.sendSystemMessage(Component.literal("§b"));
        player.sendSystemMessage(Component.literal("§bProgress: " + session.surveyedChunks.size() + "/" + session.requiredSurveys + " required"));
        
        // Play sound
        player.level().playSound(null, pos, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 1.0f, 1.2f);
        
        // FIXED: Send the client-calculated rating and windspeed to the networked survey system
        if (networkSurvey != null) {
            ChunkPos chunkPos = new ChunkPos(pos);
            // Send survey action with client-calculated values
            PacketDistributor.sendToServer(new com.burrows.easaddon.network.SurveyNetworkPackets.SurveyActionPacket(
                networkSurvey.tornadoId, chunkPos.x, chunkPos.z, efRating, maxWindspeed));
            
            EASAddon.LOGGER.info("Sent survey action with client-calculated EF{}/{}mph for tornado {} at chunk ({}, {})", 
                efRating, maxWindspeed, networkSurvey.tornadoId, chunkPos.x, chunkPos.z);
        }
        
        if (session.canFinish) {
            player.sendSystemMessage(Component.literal("§a§lMinimum survey requirement met!"));
            player.sendSystemMessage(Component.literal("§aYou can now /survey finish or continue surveying"));
        } else {
            // Guide to next chunk
            guideToNextChunk(player, session);
        }
        
        return true;
    }
    
    /**
     * ADDED: Clear all world-specific survey data (called when switching worlds)
     */
    public void clearWorldData() {
        EASAddon.LOGGER.info("Clearing all survey data for world session");
        
        // Clear active surveys
        activeSurveys.clear();
        
        // Clear tornado damage data
        tornadoDamageData.clear();
        
        EASAddon.LOGGER.info("Survey data cleared for world switch");
    }

    /**
     * ADDED: Get current world info for debugging
     */
    public String getCurrentWorldInfo() {
        return String.format("Active surveys: %d, Tornado damage data: %d entries", 
            activeSurveys.size(), tornadoDamageData.size());
    }

    /**
     * ADDED: Force clear specific tornado data (for admin/cleanup)
     */
    public void clearTornadoData(long tornadoId) {
        activeSurveys.remove(tornadoId);
        tornadoDamageData.remove(tornadoId);
        EASAddon.LOGGER.info("Cleared survey data for tornado: {}", tornadoId);
    }
    
    private void guideToNextChunk(Player player, SurveySession session) {
        // Find nearest unsurveyed chunk
        Vec3 playerPos = player.position();
        ChunkPos nearestChunk = null;
        double nearestDistance = Double.MAX_VALUE;
        
        for (ChunkPos chunk : session.targetChunks) {
            if (!session.surveyedChunks.contains(chunk)) {
                double distance = getDistanceToChunk(playerPos, chunk);
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestChunk = chunk;
                }
            }
        }
        
        if (nearestChunk != null) {
            session.currentTargetChunk = nearestChunk;
            
            // Calculate chunk center coordinates
            int chunkCenterX = nearestChunk.x * 16 + 8;
            int chunkCenterZ = nearestChunk.z * 16 + 8;
            
            player.sendSystemMessage(Component.literal("§b→ Next target: Chunk (" + nearestChunk.x + ", " + nearestChunk.z + ")"));
            player.sendSystemMessage(Component.literal("§7Coordinates: " + chunkCenterX + ", ~, " + chunkCenterZ + " (≈" + Math.round(nearestDistance) + "m away)"));
        }
    }
    
    private double getDistanceToChunk(Vec3 playerPos, ChunkPos chunk) {
        double chunkCenterX = chunk.x * 16 + 8;
        double chunkCenterZ = chunk.z * 16 + 8;
        return Math.sqrt(Math.pow(playerPos.x - chunkCenterX, 2) + Math.pow(playerPos.z - chunkCenterZ, 2));
    }
    
    /**
     * Finish a survey session
     */
    public boolean finishSurvey(Player player) {
        String playerName = player.getName().getString();
        
        // FIXED: Use ClientSurveyManager for networked finish
        ClientSurveyManager clientSurveyManager = ClientSurveyManager.getInstance();
        
        // Check networked survey state
        ClientSurveyManager.ClientSurveyInfo networkSurvey = clientSurveyManager.getActiveSurvey(playerName);
        if (networkSurvey == null) {
            player.sendSystemMessage(Component.literal("§cYou are not currently surveying a tornado"));
            return false;
        }
        
        // Find corresponding local session
        SurveySession session = activeSurveys.get(networkSurvey.tornadoId);
        if (session == null) {
            player.sendSystemMessage(Component.literal("§cLocal survey session not found"));
            return false;
        }
        
        if (!session.canFinish) {
            player.sendSystemMessage(Component.literal("§cYou must survey at least " + session.requiredSurveys + " chunks before finishing"));
            return false;
        }
        
        // Calculate final tornado rating
        Map<ChunkPos, ChunkDamageData> tornadoChunks = tornadoDamageData.get(session.tornadoId);
        int finalRating = -1;
        float finalWindspeed = 0;
        
        if (tornadoChunks != null) {
            // Find highest rating among surveyed chunks
            for (ChunkPos chunk : session.surveyedChunks) {
                ChunkDamageData data = tornadoChunks.get(chunk);
                if (data != null && data.isSurveyed()) {
                    finalRating = Math.max(finalRating, data.getDeterminedEFRating());
                    finalWindspeed = Math.max(finalWindspeed, data.getMaxWindspeedFound());
                }
            }
        }
        
        // NEW: Check for rating downgrade before proceeding
        com.burrows.easaddon.tornado.TornadoTracker tracker = 
            com.burrows.easaddon.tornado.TornadoTracker.getInstance();
        com.burrows.easaddon.tornado.TornadoData tornado = tracker.getTornadoData(session.tornadoId);
        
        if (tornado != null && wouldDowngradeRating(tornado, finalRating)) {
            int existingRating = tornado.getSurveyedEFRating();
            String existingSurveyor = tornado.getSurveyedBy();
            
            player.sendSystemMessage(Component.literal("§c§l=== SURVEY REJECTED ==="));
            player.sendSystemMessage(Component.literal("§cYour survey rating of §e" + formatEFRating(finalRating) + " §cis lower than"));
            player.sendSystemMessage(Component.literal("§cthe existing rating of §a" + formatEFRating(existingRating) + " §cby §a" + existingSurveyor));
            player.sendSystemMessage(Component.literal("§c"));
            player.sendSystemMessage(Component.literal("§cSurvey results have been discarded to prevent"));
            player.sendSystemMessage(Component.literal("§caccidental data corruption or conflicts."));
            player.sendSystemMessage(Component.literal("§c"));
            player.sendSystemMessage(Component.literal("§eIf you believe the existing rating is incorrect,"));
            player.sendSystemMessage(Component.literal("§econtact a server administrator."));
            
            // Cancel the survey without saving results
            quitSurvey(player);
            
            // Play error sound
            player.level().playSound(null, player.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0f, 0.8f);
            
            EASAddon.LOGGER.warn("Survey rejected for tornado {}: Player {} attempted to downgrade from EF{} to EF{}", 
                session.tornadoId, playerName, existingRating, finalRating);
            return false;
        }
        
        // FIXED: Use ClientSurveyManager for networked finish with calculated values
        // Send the actual calculated values instead of placeholder values

        if (networkSurvey != null) {
            // Send the finish request with our calculated values
            PacketDistributor.sendToServer(new com.burrows.easaddon.network.SurveyNetworkPackets.FinishSurveyPacket(
                networkSurvey.tornadoId, finalRating, finalWindspeed));
            
            EASAddon.LOGGER.info("Sent finish survey request with calculated EF{}/{}mph for tornado {}", 
                finalRating, finalWindspeed, networkSurvey.tornadoId);
        } else {
            player.sendSystemMessage(Component.literal("§cFailed to finish networked survey"));
            return false;
        }
        
        // Update local tornado data with final rating
        if (tornado != null) {
            tornado.setSurveyResults(playerName, finalRating, finalWindspeed);
            // Force save the updated data
            tracker.forceSave();
        }
        
        // Remove local active survey
        activeSurveys.remove(session.tornadoId);
        
        // Send completion message
        player.sendSystemMessage(Component.literal("§a§l=== SURVEY COMPLETED ==="));
        player.sendSystemMessage(Component.literal("§eTornado ID: " + session.tornadoId));
        player.sendSystemMessage(Component.literal("§eChunks surveyed: " + session.surveyedChunks.size() + "/" + session.targetChunks.size()));
        player.sendSystemMessage(Component.literal("§eFinal rating: §a" + formatEFRating(finalRating)));
        player.sendSystemMessage(Component.literal("§eEstimated max windspeed: §a" + Math.round(finalWindspeed) + " mph"));
        player.sendSystemMessage(Component.literal("§b"));
        player.sendSystemMessage(Component.literal("§bSurvey data has been recorded and will be visible to all players."));
        
        // Play completion sound
        player.level().playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);
        
        return true;
    }
    
    /**
     * Quit a survey early
     */
    public boolean quitSurvey(Player player) {
        String playerName = player.getName().getString();
        
        // FIXED: Use ClientSurveyManager for networked quit
        ClientSurveyManager clientSurveyManager = ClientSurveyManager.getInstance();
        
        // Find and remove networked survey
        
        boolean networkQuitSuccess = clientSurveyManager.quitSurvey(player);
        
        // Find and remove local survey
        SurveySession session = null;
        for (Map.Entry<Long, SurveySession> entry : activeSurveys.entrySet()) {
            if (entry.getValue().playerName.equals(playerName)) {
                session = entry.getValue();
                activeSurveys.remove(entry.getKey());
                break;
            }
        }
        
        if (session == null && !networkQuitSuccess) {
            player.sendSystemMessage(Component.literal("§cYou are not currently surveying a tornado"));
            return false;
        }
        
        player.sendSystemMessage(Component.literal("§6Survey cancelled. Progress was not saved."));
        return true;
    }
    
    /**
     * Add damage data for a tornado
     */
    public void addDamage(long tornadoId, ChunkPos chunk, BlockPos pos, BlockState original, BlockState resulting, int tornadoWindspeed, Level level) {
        Map<ChunkPos, ChunkDamageData> tornadoChunks = tornadoDamageData.computeIfAbsent(tornadoId, k -> new ConcurrentHashMap<>());
        ChunkDamageData chunkData = tornadoChunks.computeIfAbsent(chunk, ChunkDamageData::new);
        
        // FIXED: Calculate block strength using custom values
        float blockStrength = getBlockStrengthWithCustom(original.getBlock(), level);
        
        chunkData.addDamage(pos, original, resulting, blockStrength, tornadoWindspeed);
    }
    
    // FIXED: Enhanced EF rating calculation using evidence-based minimums
    private int calculateEFRating(ChunkDamageData chunkData) {
        // Get enhanced windspeed estimate (now properly using minimums)
        float enhancedWindspeed = chunkData.getEnhancedWindspeedEstimate();
        
        // Convert windspeed to EF rating
        int efRatingFromWindspeed = windspeedToEFRating(enhancedWindspeed);
        
        // FIXED: Get minimum rating from evidence types
        int minimumRatingFromEvidence = chunkData.getMinimumEFRatingFromEvidence();
        
        // Use the higher of the two (evidence minimum takes precedence)
        int finalRating = Math.max(efRatingFromWindspeed, minimumRatingFromEvidence);
        
        EASAddon.LOGGER.info("EF calculation for chunk {}: windspeed={}mph → EF{}, evidence_minimum=EF{}, final=EF{}", 
            chunkData.getChunkPos(), Math.round(enhancedWindspeed), efRatingFromWindspeed, 
            minimumRatingFromEvidence, finalRating);
        
        return finalRating;
    }
    
    // FIXED: Enhanced windspeed calculation without logical fallacies
    private float calculateMaxWindspeed(ChunkDamageData chunkData) {
        // Get the evidence-based windspeed estimate (now properly using minimums)
        float enhancedWindspeed = chunkData.getEnhancedWindspeedEstimate();
        
        EASAddon.LOGGER.info("Enhanced windspeed calculation for chunk {}: {}mph", 
            chunkData.getChunkPos(), Math.round(enhancedWindspeed));
        
        return enhancedWindspeed;
    }

    /**
     * Convert windspeed to EF rating using enhanced thresholds
     */
    private int windspeedToEFRating(float windspeed) {
        // Enhanced EF scale thresholds (based on damage observed)
        if (windspeed > 200) return 5; // EF5: 200+ mph (heavy scouring threshold)
        if (windspeed >= 166) return 4; // EF4: 166-199 mph  
        if (windspeed >= 140) return 3; // EF3: 140-165 mph (debarking/light scouring threshold)
        if (windspeed >= 111) return 2; // EF2: 111-139 mph
        if (windspeed >= 86) return 1;  // EF1: 86-110 mph
        if (windspeed >= 65) return 0;  // EF0: 65-85 mph
        return 0; // Default to EF0 for any tornado damage
    }

    /**
     * ENHANCED: Show detailed damage summary including all evidence types
     */
    private void showActualDamageSummary(long tornadoId, List<ChunkPos> validChunks, Player player) {
        Map<ChunkPos, ChunkDamageData> tornadoChunks = tornadoDamageData.get(tornadoId);
        if (tornadoChunks == null || validChunks.isEmpty()) return;
        
        int totalDamageBlocks = 0;
        int totalDebarking = 0;
        int totalScouring = 0;
        float maxIntensityFound = 0;
        float avgWindspeed = 0;
        int windspeedSamples = 0;
        
        // ADDED: Count evidence by type
        Map<ChunkDamageData.ScouringLevel, Integer> scouringByLevel = new HashMap<>();
        int chunksWithMultipleEvidenceTypes = 0;
        
        for (ChunkPos chunk : validChunks) {
            ChunkDamageData data = tornadoChunks.get(chunk);
            if (data != null) {
                totalDamageBlocks += data.getDamageRecords().size();
                totalDebarking += data.getDebarkedLogs().size();
                totalScouring += data.getScouringEvidence().size();
                
                // Count high-confidence chunks
                if (data.hasHighConfidenceEvidence()) {
                    chunksWithMultipleEvidenceTypes++;
                }
                
                // Count scouring by level
                for (ChunkDamageData.ScouringLevel level : data.getScouringEvidence().values()) {
                    scouringByLevel.merge(level, 1, Integer::sum);
                }
                
                maxIntensityFound = Math.max(maxIntensityFound, data.getEnhancedWindspeedEstimate());
                
                float chunkAvgWind = data.getAverageTornadoWindspeed();
                if (chunkAvgWind > 0) {
                    avgWindspeed += chunkAvgWind;
                    windspeedSamples++;
                }
            }
        }
        
        if (windspeedSamples > 0) {
            avgWindspeed /= windspeedSamples;
        }
        
        // Convert max intensity to estimated EF rating
        int estimatedMaxRating = windspeedToEFRating(maxIntensityFound);
        
        player.sendSystemMessage(Component.literal("§7=== ENHANCED Damage Analysis ==="));
        player.sendSystemMessage(Component.literal("§7Blocks destroyed: " + totalDamageBlocks));
        
        if (totalDebarking > 0) {
            player.sendSystemMessage(Component.literal("§7Debarked trees: " + totalDebarking + " §c(≥140mph → EF3+)"));
        }
        
        if (totalScouring > 0) {
            player.sendSystemMessage(Component.literal("§7Ground scouring: " + totalScouring + " locations"));
            
            for (Map.Entry<ChunkDamageData.ScouringLevel, Integer> entry : scouringByLevel.entrySet()) {
                String levelName = switch (entry.getKey()) {
                    case GRASS_TO_DIRT -> "Light (≥140mph → EF3+)";
                    case DIRT_TO_MEDIUM -> "Medium (≥170mph → EF4+)";
                    case MEDIUM_TO_HEAVY -> "Heavy (≥200mph → EF5)";
                };
                player.sendSystemMessage(Component.literal("§7  " + levelName + ": " + entry.getValue()));
            }
        }
        
        if (chunksWithMultipleEvidenceTypes > 0) {
            player.sendSystemMessage(Component.literal("§7High-confidence chunks: " + chunksWithMultipleEvidenceTypes + 
                " §a(multiple evidence types)"));
        }
        
        player.sendSystemMessage(Component.literal("§7Enhanced max windspeed: " + Math.round(maxIntensityFound) + " mph"));
        player.sendSystemMessage(Component.literal("§7Estimated rating: §e" + formatEFRating(estimatedMaxRating)));
        player.sendSystemMessage(Component.literal("§a§lAll evidence verified by tornado damage patterns"));
    }
    
    private String formatEFRating(int rating) {
        if (rating < 0) return "EFU";
        return "EF" + rating;
    }
    
    /**
     * FIXED: Block strength calculation that checks PMWeather's custom values first
     */
    private float getBlockStrengthWithCustom(Block block, Level level) {
        try {
            // First check if PMWeather has a custom strength for this block
            if (reflectionInitialized && blockStrengthsField != null) {
                Map<Block, Float> customStrengths = (Map<Block, Float>) blockStrengthsField.get(null);
                
                if (customStrengths != null && customStrengths.containsKey(block)) {
                    float customStrength = customStrengths.get(block);
                    EASAddon.LOGGER.debug("DamageSurveyManager: Using custom block strength for {}: {} mph", 
                                         block.getDescriptionId(), customStrength);
                    return customStrength;
                }
            }
        } catch (Exception e) {
            EASAddon.LOGGER.debug("DamageSurveyManager: Failed to get custom block strength for {}: {}", 
                                 block.getDescriptionId(), e.getMessage());
        }
        
        // Fall back to default calculation if no custom strength found
        return getBlockStrengthDefault(block, level);
    }
    
    
    
 // Add these methods to DamageSurveyManager.java class

 // Add this field to the class
 private final Map<Long, Map<ChunkPos, RetroactiveDamageInfo>> pendingRetroactiveAnalysis = new ConcurrentHashMap<>();

 /**
  * ADDED: Mark a chunk for retroactive damage analysis when it loads
  */
 public void markChunkForRetroactiveAnalysis(long tornadoId, ChunkPos chunkPos, 
                                            int windspeed, Vec3 tornadoPos, int windfieldWidth) {
     try {
         RetroactiveDamageInfo damageInfo = new RetroactiveDamageInfo(
             tornadoPos, windspeed, windfieldWidth, System.currentTimeMillis()
         );
         
         pendingRetroactiveAnalysis
             .computeIfAbsent(tornadoId, k -> new ConcurrentHashMap<>())
             .put(chunkPos, damageInfo);
         
         EASAddon.LOGGER.debug("Marked chunk ({}, {}) for retroactive analysis - tornado {} at {}mph", 
             chunkPos.x, chunkPos.z, tornadoId, windspeed);
             
     } catch (Exception e) {
         EASAddon.LOGGER.error("Error marking chunk for retroactive analysis: {}", e.getMessage());
     }
 }

 /**
  * ADDED: Process retroactive damage for a chunk that just loaded
  */
 public boolean processRetroactiveDamageForChunk(long tornadoId, ChunkPos chunkPos, Level level) {
     try {
         Map<ChunkPos, RetroactiveDamageInfo> tornadoPendingChunks = pendingRetroactiveAnalysis.get(tornadoId);
         if (tornadoPendingChunks == null) {
             return false;
         }
         
         RetroactiveDamageInfo damageInfo = tornadoPendingChunks.remove(chunkPos);
         if (damageInfo == null) {
             return false;
         }
         
         // Check if this retroactive analysis is still relevant (not too old)
         long age = System.currentTimeMillis() - damageInfo.timestamp;
         if (age > 600000) { // 10 minutes
             EASAddon.LOGGER.debug("Skipping retroactive analysis for chunk ({}, {}) - too old ({}ms)", 
                 chunkPos.x, chunkPos.z, age);
             return false;
         }
         
         // Perform the retroactive damage calculation
         EASAddon.LOGGER.info("Processing retroactive damage for chunk ({}, {}) - tornado {} at {}mph", 
             chunkPos.x, chunkPos.z, tornadoId, damageInfo.windspeed);
         
         // Use TornadoTracker's damage calculation methods
         boolean damageFound = performRetroactiveDamageCalculation(
             tornadoId, chunkPos, level, damageInfo
         );
         
         if (damageFound) {
             EASAddon.LOGGER.info("Successfully calculated retroactive damage for chunk ({}, {}) - tornado {}", 
                 chunkPos.x, chunkPos.z, tornadoId);
         }
         
         return damageFound;
         
     } catch (Exception e) {
         EASAddon.LOGGER.error("Error processing retroactive damage for chunk ({}, {}): {}", 
             chunkPos.x, chunkPos.z, e.getMessage());
         return false;
     }
 }

 /**
  * ADDED: Perform the actual retroactive damage calculation
  */
 private boolean performRetroactiveDamageCalculation(long tornadoId, ChunkPos chunkPos, 
                                                    Level level, RetroactiveDamageInfo damageInfo) {
     try {
         int sampleCount = Math.min(8, 3 + (damageInfo.windspeed / 50));
         boolean damageFound = false;
         
         for (int i = 0; i < sampleCount; i++) {
             // Random position within chunk
             int x = chunkPos.x * 16 + level.random.nextInt(16);
             int z = chunkPos.z * 16 + level.random.nextInt(16);
             
             BlockPos surfacePos = level.getHeightmapPos(
                 net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, 
                 new BlockPos(x, 0, z));
             
             // Check multiple heights for different types of damage
             if (checkForRetroactiveBlockDamage(tornadoId, chunkPos, surfacePos, 
                     damageInfo.tornadoPos, damageInfo.windspeed, damageInfo.windfieldWidth, level)) {
                 damageFound = true;
             }
             
             if (checkForRetroactiveDebarkingEvidence(tornadoId, chunkPos, surfacePos, 
                     damageInfo.tornadoPos, damageInfo.windspeed, damageInfo.windfieldWidth, level)) {
                 damageFound = true;
             }
             
             if (checkForRetroactiveScouringEvidence(tornadoId, chunkPos, surfacePos, 
                     damageInfo.tornadoPos, damageInfo.windspeed, damageInfo.windfieldWidth, level)) {
                 damageFound = true;
             }
         }
         
         return damageFound;
         
     } catch (Exception e) {
         EASAddon.LOGGER.error("Error in retroactive damage calculation: {}", e.getMessage());
         return false;
     }
 }
 
 
//Add these helper methods to DamageSurveyManager.java class

/**
* ADDED: Check for retroactive debarking evidence
*/
private boolean checkForRetroactiveDebarkingEvidence(long tornadoId, ChunkPos chunkPos, BlockPos surfacePos,
                                                  Vec3 tornadoPos, int windspeed, int windfieldWidth, Level level) {
  try {
      // Only check for debarking if tornado has sufficient windspeed
      if (windspeed < 140) return false;
      
      // Check area around surface position for logs
      for (int dx = -2; dx <= 2; dx++) {
          for (int dy = -1; dy <= 3; dy++) {
              for (int dz = -2; dz <= 2; dz++) {
                  BlockPos pos = surfacePos.offset(dx, dy, dz);
                  
                  if (pos.getY() < level.getMinBuildHeight() || pos.getY() > level.getMaxBuildHeight()) {
                      continue;
                  }
                  
                  net.minecraft.world.level.block.state.BlockState blockState = level.getBlockState(pos);
                  
                  // Check if this is a stripped log (evidence of debarking)
                  if (blockState.is(net.neoforged.neoforge.common.Tags.Blocks.STRIPPED_LOGS) && 
                      isInNaturalForestArea(pos, level)) {
                      
                      // Calculate wind effect at this position
                      double windEffect = calculateWindEffectAtPosition(
                          pos.getCenter(), tornadoPos, windfieldWidth, windspeed);
                      
                      // PMWeather's debarking threshold is 140 mph
                      if (windEffect >= 140.0) {
                          addDebarkingEvidence(tornadoId, chunkPos, pos);
                          
                          EASAddon.LOGGER.debug("Retroactive debarking evidence found at {} in chunk ({}, {}) - wind: {}mph", 
                              pos, chunkPos.x, chunkPos.z, Math.round(windEffect));
                          return true;
                      }
                  }
              }
          }
      }
      
      return false;
      
  } catch (Exception e) {
      EASAddon.LOGGER.error("Error checking retroactive debarking evidence: {}", e.getMessage());
      return false;
  }
}

/**
* ADDED: Check for retroactive scouring evidence
*/
private boolean checkForRetroactiveScouringEvidence(long tornadoId, ChunkPos chunkPos, BlockPos surfacePos,
                                                 Vec3 tornadoPos, int windspeed, int windfieldWidth, Level level) {
  try {
      // Only check for scouring if tornado has sufficient windspeed
      if (windspeed < 140) return false;
      
      net.minecraft.world.level.block.state.BlockState surfaceState = level.getBlockState(surfacePos);
      
      // Calculate wind effect at surface
      double windEffect = calculateWindEffectAtPosition(
          surfacePos.getCenter(), tornadoPos, windfieldWidth, windspeed);
      
      // Check for evidence of grass -> dirt scouring
      if (surfaceState.is(net.minecraft.world.level.block.Blocks.DIRT) && 
          isInNaturalGrassArea(surfacePos, level) && windEffect >= 140.0) {
          
          addScouringEvidence(tornadoId, chunkPos, surfacePos, ChunkDamageData.ScouringLevel.GRASS_TO_DIRT);
          
          EASAddon.LOGGER.debug("Retroactive grass scouring evidence found at {} in chunk ({}, {}) - wind: {}mph", 
              surfacePos, chunkPos.x, chunkPos.z, Math.round(windEffect));
          return true;
      }
      
      // Check for evidence of dirt -> medium scouring
      if (isMediumScouringBlock(surfaceState) && windEffect >= 170.0) {
          addScouringEvidence(tornadoId, chunkPos, surfacePos, ChunkDamageData.ScouringLevel.DIRT_TO_MEDIUM);
          
          EASAddon.LOGGER.debug("Retroactive medium scouring evidence found at {} in chunk ({}, {}) - wind: {}mph", 
              surfacePos, chunkPos.x, chunkPos.z, Math.round(windEffect));
          return true;
      }
      
      // Check for evidence of medium -> heavy scouring
      if (isHeavyScouringBlock(surfaceState) && windEffect >= 200.0) {
          addScouringEvidence(tornadoId, chunkPos, surfacePos, ChunkDamageData.ScouringLevel.MEDIUM_TO_HEAVY);
          
          EASAddon.LOGGER.debug("Retroactive heavy scouring evidence found at {} in chunk ({}, {}) - wind: {}mph", 
              surfacePos, chunkPos.x, chunkPos.z, Math.round(windEffect));
          return true;
      }
      
      return false;
      
  } catch (Exception e) {
      EASAddon.LOGGER.error("Error checking retroactive scouring evidence: {}", e.getMessage());
      return false;
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
* ADDED: Check if position was likely in a natural grass area
*/
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
 

 /**
  * ADDED: Check for retroactive block damage
  */
 private boolean checkForRetroactiveBlockDamage(long tornadoId, ChunkPos chunkPos, BlockPos surfacePos, 
                                               Vec3 tornadoPos, int windspeed, int windfieldWidth, Level level) {
     try {
         // Check blocks at and above surface
         for (int dy = -1; dy <= 2; dy++) {
             BlockPos pos = surfacePos.offset(0, dy, 0);
             
             if (pos.getY() < level.getMinBuildHeight() || pos.getY() > level.getMaxBuildHeight()) {
                 continue;
             }
             
             net.minecraft.world.level.block.state.BlockState blockState = level.getBlockState(pos);
             
             if (blockState.isAir() || !blockState.getFluidState().isEmpty()) {
                 continue;
             }
             
             // Calculate wind effect using the same logic as TornadoTracker
             double windEffect = calculateWindEffectAtPosition(
                 pos.getCenter(), tornadoPos, windfieldWidth, windspeed);
             
             // Calculate block strength
             float blockStrength = getBlockStrengthPMWeather(blockState.getBlock(), level);
             
             // If wind would destroy this block, record the damage
             if (windEffect >= blockStrength) {
                 addDamage(tornadoId, chunkPos, pos, blockState, 
                     net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), windspeed, level);
                 
                 EASAddon.LOGGER.debug("Retroactive block damage recorded at {} in chunk ({}, {}) - wind: {}mph, strength: {}", 
                     pos, chunkPos.x, chunkPos.z, Math.round(windEffect), blockStrength);
                 return true;
             }
         }
         
         return false;
         
     } catch (Exception e) {
         EASAddon.LOGGER.error("Error checking retroactive block damage: {}", e.getMessage());
         return false;
     }
 }

 /**
  * ADDED: Data structure for retroactive damage info
  */
 private static class RetroactiveDamageInfo {
     public final Vec3 tornadoPos;
     public final int windspeed;
     public final int windfieldWidth;
     public final long timestamp;
     
     public RetroactiveDamageInfo(Vec3 tornadoPos, int windspeed, int windfieldWidth, long timestamp) {
         this.tornadoPos = tornadoPos;
         this.windspeed = windspeed;
         this.windfieldWidth = windfieldWidth;
         this.timestamp = timestamp;
     }
 }

 /**
  * ADDED: Clean up old retroactive analysis requests
  */
 public void cleanupOldRetroactiveAnalysis() {
     try {
         long cutoffTime = System.currentTimeMillis() - 600000; // 10 minutes ago
         
         pendingRetroactiveAnalysis.entrySet().removeIf(tornadoEntry -> {
             Map<ChunkPos, RetroactiveDamageInfo> chunkMap = tornadoEntry.getValue();
             chunkMap.entrySet().removeIf(chunkEntry -> 
                 chunkEntry.getValue().timestamp < cutoffTime);
             return chunkMap.isEmpty();
         });
         
     } catch (Exception e) {
         EASAddon.LOGGER.error("Error cleaning up retroactive analysis: {}", e.getMessage());
     }
 }

 // Add these helper methods (copied from TornadoTracker for consistency)

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
    
    
    
    
    /**
     * Default block strength calculation (copied from Storm.java)
     */
    private static float getBlockStrengthDefault(Block block, Level level) {
        net.minecraft.world.item.ItemStack item = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_AXE);
        float destroySpeed = block.defaultBlockState().getDestroySpeed(level, BlockPos.ZERO);
        return 60.0f + Mth.sqrt((destroySpeed /= item.getDestroySpeed(block.defaultBlockState()))) * 60.0f;
    }
    
    /**
     * ADDED: Add debarking evidence to tornado damage data
     */
    public void addDebarkingEvidence(long tornadoId, ChunkPos chunk, BlockPos pos) {
        Map<ChunkPos, ChunkDamageData> tornadoChunks = tornadoDamageData.computeIfAbsent(tornadoId, k -> new ConcurrentHashMap<>());
        ChunkDamageData chunkData = tornadoChunks.computeIfAbsent(chunk, ChunkDamageData::new);
        
        chunkData.addDebarkingEvidence(pos);
        
        EASAddon.LOGGER.debug("Added debarking evidence for tornado {} at chunk ({}, {}) position {}", 
            tornadoId, chunk.x, chunk.z, pos);
    }

    /**
     * ADDED: Add scouring evidence to tornado damage data
     */
    public void addScouringEvidence(long tornadoId, ChunkPos chunk, BlockPos pos, ChunkDamageData.ScouringLevel level) {
        Map<ChunkPos, ChunkDamageData> tornadoChunks = tornadoDamageData.computeIfAbsent(tornadoId, k -> new ConcurrentHashMap<>());
        ChunkDamageData chunkData = tornadoChunks.computeIfAbsent(chunk, ChunkDamageData::new);
        
        chunkData.addScouringEvidence(pos, level);
        
        EASAddon.LOGGER.debug("Added {} scouring evidence for tornado {} at chunk ({}, {}) position {}", 
            level.name(), tornadoId, chunk.x, chunk.z, pos);
    }
    
    // FIXED: Updated getters to check both local and networked state
    public SurveySession getActiveSurvey(String playerName) {
        // First check networked state
        ClientSurveyManager.ClientSurveyInfo networkSurvey = ClientSurveyManager.getInstance().getActiveSurvey(playerName);
        if (networkSurvey != null) {
            // Return corresponding local session if it exists
            return activeSurveys.get(networkSurvey.tornadoId);
        }
        
        // Fallback to local state
        return activeSurveys.values().stream()
                .filter(session -> session.playerName.equals(playerName))
                .findFirst()
                .orElse(null);
    }
    
    public Map<ChunkPos, ChunkDamageData> getTornadoDamageData(long tornadoId) {
        return tornadoDamageData.get(tornadoId);
    }
    
    public boolean isTornadoBeingSurveyed(long tornadoId) {
        // Check networked state first (authoritative for multiplayer)
        if (ClientSurveyManager.getInstance().isTornadoBeingSurveyed(tornadoId)) {
            return true;
        }
        
        // Fallback to local state
        return activeSurveys.containsKey(tornadoId);
    }
    
    public String getSurveyorName(long tornadoId) {
        // Check networked state first
        String networkSurveyor = ClientSurveyManager.getInstance().getSurveyorName(tornadoId);
        if (networkSurveyor != null) {
            return networkSurveyor;
        }
        
        // Fallback to local state
        SurveySession session = activeSurveys.get(tornadoId);
        return session != null ? session.playerName : null;
    }
    
    
 // Add this method to DamageSurveyManager.java to prevent duplicate calculations

    /**
     * Check if damage data already exists for a chunk to avoid recalculation
     */
    public boolean hasDamageDataForChunk(long tornadoId, ChunkPos chunkPos) {
        Map<ChunkPos, ChunkDamageData> tornadoChunks = tornadoDamageData.get(tornadoId);
        if (tornadoChunks != null) {
            ChunkDamageData data = tornadoChunks.get(chunkPos);
            return data != null && data.hasDamage();
        }
        return false;
    }


    
}