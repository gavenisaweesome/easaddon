package com.burrows.easaddon.survey;

import com.burrows.easaddon.EASAddon;
import com.burrows.easaddon.tornado.TornadoData;
import com.burrows.easaddon.tornado.TornadoTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.minecraft.tags.BlockTags;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Analyzes chunks for tornado damage evidence when chunks are loaded late
 */
@OnlyIn(Dist.CLIENT)
public class ChunkDamageAnalyzer {
    private static ChunkDamageAnalyzer instance;
    
    // Cache for block strength calculations
    private final Map<Block, Float> blockStrengthCache = new HashMap<>();
    
    // Reflection for PMWeather integration
    private Class<?> serverConfigClass;
    private Field blockStrengthsField;
    private boolean reflectionInitialized = false;
    
    private ChunkDamageAnalyzer() {
        initializeReflection();
    }
    
    public static ChunkDamageAnalyzer getInstance() {
        if (instance == null) {
            instance = new ChunkDamageAnalyzer();
        }
        return instance;
    }
    
    private void initializeReflection() {
        if (!EASAddon.isPMWeatherAvailable()) {
            return;
        }
        
        try {
            serverConfigClass = Class.forName("dev.protomanly.pmweather.config.ServerConfig");
            blockStrengthsField = serverConfigClass.getDeclaredField("blockStrengths");
            blockStrengthsField.setAccessible(true);
            reflectionInitialized = true;
            
            EASAddon.LOGGER.info("ChunkDamageAnalyzer: PMWeather reflection initialized successfully");
        } catch (Exception e) {
            EASAddon.LOGGER.error("ChunkDamageAnalyzer: Failed to initialize PMWeather reflection", e);
            reflectionInitialized = false;
        }
    }
    
    /**
     * Analyze a chunk for tornado damage evidence
     */
    public ChunkAnalysisResult analyzeChunkForTornadoDamage(ChunkPos chunkPos, Level level, long tornadoId) {
        ChunkAnalysisResult result = new ChunkAnalysisResult(chunkPos);
        
        try {
            TornadoData tornadoData = TornadoTracker.getInstance().getTornadoData(tornadoId);
            if (tornadoData == null) {
                EASAddon.LOGGER.warn("ChunkDamageAnalyzer: No tornado data found for ID {}", tornadoId);
                return result;
            }
            
            // Get tornado's closest approach to this chunk
            TornadoApproach closestApproach = findClosestTornadoApproach(chunkPos, tornadoData);
            if (closestApproach == null || closestApproach.distance > getMaxDamageRange(tornadoData)) {
                EASAddon.LOGGER.debug("ChunkDamageAnalyzer: Chunk ({}, {}) outside tornado damage range", 
                    chunkPos.x, chunkPos.z);
                return result;
            }
            
            result.withinDamageRange = true;
            result.minDistanceToTornado = closestApproach.distance;
            result.maxWindspeedAtChunk = closestApproach.windspeed;
            
            // Analyze the chunk
            LevelChunk chunk = level.getChunk(chunkPos.x, chunkPos.z);
            analyzeFogwoodZone(chunk, result, closestApproach);
            analyzeDebarkingEvidence(chunk, result, closestApproach);
            analyzeScouringEvidence(chunk, result, closestApproach);
            analyzeMissingVegetation(chunk, result, closestApproach);
            analyzeStructuralDamage(chunk, result, closestApproach);
            
            EASAddon.LOGGER.info("ChunkDamageAnalyzer: Analysis complete for chunk ({}, {}) - {} evidence types found", 
                chunkPos.x, chunkPos.z, result.getEvidenceTypeCount());
            
        } catch (Exception e) {
            EASAddon.LOGGER.error("ChunkDamageAnalyzer: Error analyzing chunk ({}, {}): {}", 
                chunkPos.x, chunkPos.z, e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Find the tornado's closest approach to a chunk
     */
    private TornadoApproach findClosestTornadoApproach(ChunkPos chunkPos, TornadoData tornadoData) {
        Vec3 chunkCenter = new Vec3(chunkPos.x * 16 + 8, 0, chunkPos.z * 16 + 8);
        
        double minDistance = Double.MAX_VALUE;
        int maxWindspeed = 0;
        Vec3 closestPosition = null;
        
        for (TornadoData.PositionRecord record : tornadoData.getPositionHistory()) {
            Vec3 tornadoPos = record.position;
            double distance = tornadoPos.distanceTo(chunkCenter);
            
            if (distance < minDistance) {
                minDistance = distance;
                maxWindspeed = record.windspeed;
                closestPosition = tornadoPos;
            }
        }
        
        if (closestPosition == null) return null;
        
        return new TornadoApproach(closestPosition, minDistance, maxWindspeed);
    }
    
    /**
     * Calculate maximum damage range for tornado
     */
    private double getMaxDamageRange(TornadoData tornadoData) {
        // Use similar logic to PMWeather
        float width = tornadoData.getMaxWidth();
        int windfieldWidth = Math.max((int)width, 40);
        return windfieldWidth * 2.0; // PMWeather's damage range calculation
    }
    
    /**
     * Analyze for fogwood zone evidence (very close to tornado path)
     */
    private void analyzeFogwoodZone(LevelChunk chunk, ChunkAnalysisResult result, TornadoApproach approach) {
        if (approach.distance > 50) return; // Fogwood only occurs very close to path
        
        int chunkStartX = chunk.getPos().x * 16;
        int chunkStartZ = chunk.getPos().z * 16;
        
        // Sample blocks in the chunk
        for (int x = 0; x < 16; x += 4) {
            for (int z = 0; z < 16; z += 4) {
                BlockPos surfacePos = chunk.getLevel().getHeightmapPos(
                    net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, 
                    new BlockPos(chunkStartX + x, 0, chunkStartZ + z));
                
                // Look for scattered blocks or debris patterns
                if (isScatteredDebris(chunk, surfacePos)) {
                    result.fogwoodZoneEvidence.add(surfacePos);
                }
            }
        }
        
        if (!result.fogwoodZoneEvidence.isEmpty()) {
            EASAddon.LOGGER.debug("ChunkDamageAnalyzer: Found {} fogwood zone evidence points in chunk ({}, {})", 
                result.fogwoodZoneEvidence.size(), chunk.getPos().x, chunk.getPos().z);
        }
    }
    
    /**
     * Analyze for debarking evidence on logs
     */
    private void analyzeDebarkingEvidence(LevelChunk chunk, ChunkAnalysisResult result, TornadoApproach approach) {
        if (approach.windspeed < 140) return; // Debarking requires 140+ mph winds
        
        int chunkStartX = chunk.getPos().x * 16;
        int chunkStartZ = chunk.getPos().z * 16;
        
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 100; y += 5) { // Sample every 5 blocks vertically
                    BlockPos pos = new BlockPos(chunkStartX + x, y, chunkStartZ + z);
                    BlockState state = chunk.getBlockState(pos);
                    
                    if (isStrippedLog(state)) {
                        double windEffectAtBlock = calculateWindEffectAtPosition(pos, approach);
                        if (windEffectAtBlock >= 140.0) {
                            result.debarkingEvidence.add(pos);
                        }
                    }
                }
            }
        }
        
        if (!result.debarkingEvidence.isEmpty()) {
            EASAddon.LOGGER.debug("ChunkDamageAnalyzer: Found {} debarking evidence points in chunk ({}, {})", 
                result.debarkingEvidence.size(), chunk.getPos().x, chunk.getPos().z);
        }
    }
    
    /**
     * Analyze for ground scouring evidence
     */
    private void analyzeScouringEvidence(LevelChunk chunk, ChunkAnalysisResult result, TornadoApproach approach) {
        if (approach.windspeed < 140) return; // Scouring requires high winds
        
        int chunkStartX = chunk.getPos().x * 16;
        int chunkStartZ = chunk.getPos().z * 16;
        
        for (int x = 0; x < 16; x += 2) {
            for (int z = 0; z < 16; z += 2) {
                BlockPos surfacePos = chunk.getLevel().getHeightmapPos(
                    net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, 
                    new BlockPos(chunkStartX + x, 0, chunkStartZ + z));
                
                ChunkDamageData.ScouringLevel scouringLevel = detectScouringLevel(chunk, surfacePos, approach);
                if (scouringLevel != null) {
                    result.scouringEvidence.put(surfacePos, scouringLevel);
                }
            }
        }
        
        if (!result.scouringEvidence.isEmpty()) {
            EASAddon.LOGGER.debug("ChunkDamageAnalyzer: Found {} scouring evidence points in chunk ({}, {})", 
                result.scouringEvidence.size(), chunk.getPos().x, chunk.getPos().z);
        }
    }
    
    /**
     * Analyze for missing vegetation patterns
     */
    private void analyzeMissingVegetation(LevelChunk chunk, ChunkAnalysisResult result, TornadoApproach approach) {
        int chunkStartX = chunk.getPos().x * 16;
        int chunkStartZ = chunk.getPos().z * 16;
        
        for (int x = 0; x < 16; x += 2) {
            for (int z = 0; z < 16; z += 2) {
                BlockPos surfacePos = chunk.getLevel().getHeightmapPos(
                    net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, 
                    new BlockPos(chunkStartX + x, 0, chunkStartZ + z));
                
                if (isMissingVegetation(chunk, surfacePos, approach)) {
                    result.missingVegetation.add(surfacePos);
                }
            }
        }
        
        if (!result.missingVegetation.isEmpty()) {
            EASAddon.LOGGER.debug("ChunkDamageAnalyzer: Found {} missing vegetation points in chunk ({}, {})", 
                result.missingVegetation.size(), chunk.getPos().x, chunk.getPos().z);
        }
    }
    
    /**
     * Analyze for structural damage evidence
     */
    private void analyzeStructuralDamage(LevelChunk chunk, ChunkAnalysisResult result, TornadoApproach approach) {
        int chunkStartX = chunk.getPos().x * 16;
        int chunkStartZ = chunk.getPos().z * 16;
        
        // Look for patterns that suggest structures were damaged
        for (int x = 0; x < 16; x += 3) {
            for (int z = 0; z < 16; z += 3) {
                for (int y = 60; y < 120; y += 10) {
                    BlockPos pos = new BlockPos(chunkStartX + x, y, chunkStartZ + z);
                    
                    if (isStructuralDamageEvidence(chunk, pos, approach)) {
                        result.structuralDamage.add(pos);
                    }
                }
            }
        }
        
        if (!result.structuralDamage.isEmpty()) {
            EASAddon.LOGGER.debug("ChunkDamageAnalyzer: Found {} structural damage points in chunk ({}, {})", 
                result.structuralDamage.size(), chunk.getPos().x, chunk.getPos().z);
        }
    }
    
    /**
     * Check if a position shows scattered debris patterns
     */
    private boolean isScatteredDebris(LevelChunk chunk, BlockPos pos) {
        BlockState state = chunk.getBlockState(pos);
        
        // Look for unusual block placements that might indicate scattered debris
        if (state.isAir()) {
            // Check surrounding area for debris patterns
            int debrisCount = 0;
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    
                    BlockPos checkPos = pos.offset(dx, 0, dz);
                    BlockState checkState = chunk.getBlockState(checkPos);
                    
                    // Count non-natural blocks at surface level as potential debris
                    if (!checkState.isAir() && !isNaturalSurfaceBlock(checkState)) {
                        debrisCount++;
                    }
                }
            }
            
            return debrisCount >= 3; // Threshold for debris pattern
        }
        
        return false;
    }
    
    /**
     * Check if a block state represents a stripped log
     */
    private boolean isStrippedLog(BlockState state) {
        Block block = state.getBlock();
        return block == Blocks.STRIPPED_OAK_LOG || 
               block == Blocks.STRIPPED_BIRCH_LOG ||
               block == Blocks.STRIPPED_SPRUCE_LOG ||
               block == Blocks.STRIPPED_DARK_OAK_LOG ||
               block == Blocks.STRIPPED_ACACIA_LOG ||
               block == Blocks.STRIPPED_JUNGLE_LOG ||
               block == Blocks.STRIPPED_MANGROVE_LOG ||
               block == Blocks.STRIPPED_CHERRY_LOG;
    }
    
    /**
     * Detect scouring level at a position
     */
    private ChunkDamageData.ScouringLevel detectScouringLevel(LevelChunk chunk, BlockPos pos, TornadoApproach approach) {
        BlockState surfaceState = chunk.getBlockState(pos);
        BlockState belowState = chunk.getBlockState(pos.below());
        
        double windEffect = calculateWindEffectAtPosition(pos, approach);
        
        // Check for different levels of scouring
        if (windEffect >= 200.0) {
            // Heavy scouring - exposed bedrock or deep removal
            if (belowState.is(Blocks.BEDROCK) || surfaceState.is(Blocks.STONE)) {
                return ChunkDamageData.ScouringLevel.MEDIUM_TO_HEAVY;
            }
        }
        
        if (windEffect >= 170.0) {
            // Medium scouring - subsoil exposed
            if (surfaceState.is(Blocks.DIRT) && !surfaceState.is(Blocks.GRASS_BLOCK)) {
                return ChunkDamageData.ScouringLevel.DIRT_TO_MEDIUM;
            }
        }
        
        if (windEffect >= 140.0) {
            // Light scouring - grass removed, dirt exposed
            if (surfaceState.is(Blocks.DIRT) && chunk.getBlockState(pos.above()).isAir()) {
                return ChunkDamageData.ScouringLevel.GRASS_TO_DIRT;
            }
        }
        
        return null;
    }
    
    /**
     * Check for missing vegetation patterns
     */
    private boolean isMissingVegetation(LevelChunk chunk, BlockPos pos, TornadoApproach approach) {
        BlockState state = chunk.getBlockState(pos);
        BlockState below = chunk.getBlockState(pos.below());
        
        // Air block above grass/dirt suggests removed vegetation
        if (state.isAir() && (below.is(Blocks.GRASS_BLOCK) || below.is(Blocks.DIRT))) {
            double windEffect = calculateWindEffectAtPosition(pos, approach);
            return windEffect >= 40.0; // Minimum wind to remove vegetation
        }
        
        return false;
    }
    
    /**
     * Check for structural damage evidence
     */
    private boolean isStructuralDamageEvidence(LevelChunk chunk, BlockPos pos, TornadoApproach approach) {
        BlockState state = chunk.getBlockState(pos);
        
        // Look for building material blocks that might be structural debris
        if (state.is(BlockTags.PLANKS) || 
            state.is(Blocks.COBBLESTONE) ||
            state.is(Blocks.BRICKS) ||
            state.is(BlockTags.DOORS) ||
            state.is(BlockTags.FENCES)) {
            
            double windEffect = calculateWindEffectAtPosition(pos, approach);
            float blockStrength = getBlockStrength(state.getBlock(), chunk.getLevel());
            
            return windEffect >= blockStrength;
        }
        
        return false;
    }
    
    /**
     * Check if a block is a natural surface block
     */
    private boolean isNaturalSurfaceBlock(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK) ||
               state.is(Blocks.DIRT) ||
               state.is(Blocks.STONE) ||
               state.is(Blocks.SAND) ||
               state.is(Blocks.GRAVEL) ||
               state.is(BlockTags.FLOWERS) ||
               state.is(BlockTags.SMALL_FLOWERS);
    }
    
    /**
     * Calculate wind effect at a specific position
     */
    private double calculateWindEffectAtPosition(BlockPos pos, TornadoApproach approach) {
        double distance = approach.position.distanceTo(pos.getCenter());
        // Simple linear decay - could be enhanced with more complex wind field modeling
        double distanceRatio = 1.0 - (distance / 100.0); // Assumes 100 block effective range
        return approach.windspeed * Math.max(0.0, distanceRatio);
    }
    
    /**
     * Get block strength with caching
     */
    private float getBlockStrength(Block block, Level level) {
        return blockStrengthCache.computeIfAbsent(block, b -> calculateBlockStrength(b, level));
    }
    
    /**
     * Calculate block strength using PMWeather's method
     */
    private float calculateBlockStrength(Block block, Level level) {
        try {
            // Check for custom PMWeather block strengths first
            if (reflectionInitialized && blockStrengthsField != null) {
                Map<Block, Float> customStrengths = (Map<Block, Float>) blockStrengthsField.get(null);
                if (customStrengths != null && customStrengths.containsKey(block)) {
                    return customStrengths.get(block);
                }
            }
        } catch (Exception e) {
            // Fall through to default calculation
        }
        
        // Use PMWeather's default calculation
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
     * Data class for tornado approach information
     */
    public static class TornadoApproach {
        public final Vec3 position;
        public final double distance;
        public final int windspeed;
        
        public TornadoApproach(Vec3 position, double distance, int windspeed) {
            this.position = position;
            this.distance = distance;
            this.windspeed = windspeed;
        }
    }
    
    /**
     * Data class for chunk analysis results
     */
    public static class ChunkAnalysisResult {
        public final ChunkPos chunkPos;
        public boolean withinDamageRange = false;
        public double minDistanceToTornado = Double.MAX_VALUE;
        public int maxWindspeedAtChunk = 0;
        
        // Evidence collections
        public final Set<BlockPos> fogwoodZoneEvidence = new HashSet<>();
        public final Set<BlockPos> debarkingEvidence = new HashSet<>();
        public final Map<BlockPos, ChunkDamageData.ScouringLevel> scouringEvidence = new HashMap<>();
        public final Set<BlockPos> missingVegetation = new HashSet<>();
        public final Set<BlockPos> structuralDamage = new HashSet<>();
        
        public ChunkAnalysisResult(ChunkPos chunkPos) {
            this.chunkPos = chunkPos;
        }
        
        public boolean hasAnyEvidence() {
            return !fogwoodZoneEvidence.isEmpty() ||
                   !debarkingEvidence.isEmpty() ||
                   !scouringEvidence.isEmpty() ||
                   !missingVegetation.isEmpty() ||
                   !structuralDamage.isEmpty();
        }
        
        public int getEvidenceTypeCount() {
            int count = 0;
            if (!fogwoodZoneEvidence.isEmpty()) count++;
            if (!debarkingEvidence.isEmpty()) count++;
            if (!scouringEvidence.isEmpty()) count++;
            if (!missingVegetation.isEmpty()) count++;
            if (!structuralDamage.isEmpty()) count++;
            return count;
        }
        
        public int getTotalEvidencePoints() {
            return fogwoodZoneEvidence.size() + 
                   debarkingEvidence.size() +
                   scouringEvidence.size() +
                   missingVegetation.size() +
                   structuralDamage.size();
        }
    }
}