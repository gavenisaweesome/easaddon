package com.burrows.easaddon.survey;

import com.burrows.easaddon.EASAddon;
import com.burrows.easaddon.tornado.TornadoTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * Hooks into actual block break events to track tornado damage
 */
@OnlyIn(Dist.CLIENT)
public class PMWeatherDamageHook {
    private static PMWeatherDamageHook instance;
    
    // Reflection cache for PMWeather classes
    private Class<?> stormClass;
    private Class<?> serverConfigClass;
    private Field stormPositionField;
    private Field stormWindspeedField;
    private Field stormIDField;
    private Field stormStageField;
    private Field blockStrengthsField;
    private Method getStormsMethod;
    private boolean reflectionInitialized = false;
    
    // ADDED: Track recently changed blocks to detect scouring
    private Set<BlockPos> recentlyChangedBlocks = new HashSet<>();
    private long lastCleanupTime = 0;
    
    private PMWeatherDamageHook() {
        initializeReflection();
    }
    
    public static PMWeatherDamageHook getInstance() {
        if (instance == null) {
            instance = new PMWeatherDamageHook();
        }
        return instance;
    }
    
    private void initializeReflection() {
        if (!EASAddon.isPMWeatherAvailable()) {
            return;
        }
        
        try {
            // Get PMWeather classes
            Class<?> weatherHandlerClass = Class.forName("dev.protomanly.pmweather.weather.WeatherHandler");
            stormClass = Class.forName("dev.protomanly.pmweather.weather.Storm");
            serverConfigClass = Class.forName("dev.protomanly.pmweather.config.ServerConfig");
            
            // Get fields from Storm class
            stormPositionField = stormClass.getDeclaredField("position");
            stormPositionField.setAccessible(true);
            
            stormWindspeedField = stormClass.getDeclaredField("windspeed");
            stormWindspeedField.setAccessible(true);
            
            stormIDField = stormClass.getDeclaredField("ID");
            stormIDField.setAccessible(true);
            
            stormStageField = stormClass.getDeclaredField("stage");
            stormStageField.setAccessible(true);
            
            // Get custom block strengths field
            blockStrengthsField = serverConfigClass.getDeclaredField("blockStrengths");
            blockStrengthsField.setAccessible(true);
            
            // Get the getStorms() method
            getStormsMethod = weatherHandlerClass.getDeclaredMethod("getStorms");
            getStormsMethod.setAccessible(true);
            
            reflectionInitialized = true;
            EASAddon.LOGGER.info("PMWeather damage hook initialized successfully with enhanced scouring detection");
            
        } catch (Exception e) {
            EASAddon.LOGGER.error("Failed to initialize PMWeather damage hook reflection", e);
            reflectionInitialized = false;
        }
    }
    
    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!reflectionInitialized) return;
        
        Level level = (Level) event.getLevel();
        if (!level.isClientSide()) return; // Only track on client side
        
        BlockPos pos = event.getPos();
        BlockState state = event.getState();
        
        // CRITICAL: Only track blocks that were broken by natural game mechanics
        // If a player broke this block, don't count it as tornado damage
        if (event.getPlayer() != null) {
            return; // Player-caused break, not tornado damage
        }
        
        // Track this position for potential scouring detection
        recentlyChangedBlocks.add(pos);
        
        // Find nearby active tornadoes that could have caused this damage
        try {
            Object weatherHandler = getWeatherHandler(level);
            if (weatherHandler == null) return;
            
            List<?> storms = (List<?>) getStormsMethod.invoke(weatherHandler);
            if (storms == null) return;
            
            // Check each storm to see if it could have caused this damage
            for (Object storm : storms) {
                try {
                    int stormType = (Integer) stormClass.getField("stormType").get(storm);
                    if (stormType != 0) continue; // Only track tornadoes
                    
                    long stormId = (Long) stormIDField.get(storm);
                    net.minecraft.world.phys.Vec3 stormPos = (net.minecraft.world.phys.Vec3) stormPositionField.get(storm);
                    int windspeed = (Integer) stormWindspeedField.get(storm);
                    int stage = (Integer) stormStageField.get(storm);
                    
                    // Only track damage from active stage 3+ tornadoes
                    if (stage < 3 || windspeed < 40) continue;
                    
                    // Use PMWeather's actual damage range calculation
                    float width = (Float) stormClass.getField("width").get(storm);
                    int windfieldWidth = Math.max((int)width, 40);
                    double maxDamageRange = windfieldWidth * 2.0; // PMWeather's actual damage range
                    
                    // Check if block is within tornado's damage range
                    double distance = stormPos.distanceTo(pos.getCenter());
                    
                    if (distance <= maxDamageRange) {
                        // Calculate actual wind effect at block position
                        double windEffectAtBlock = calculateWindEffectUsingPMWeatherLogic(
                            pos.getCenter(), stormPos, width, windspeed, windfieldWidth);
                        
                        // Get block strength using PMWeather's method
                        float blockStrength = getBlockStrengthWithCustom(state.getBlock(), level);
                        
                        // Only record as tornado damage if wind was strong enough to break this block
                        if (windEffectAtBlock >= blockStrength) {
                            ChunkPos chunkPos = new ChunkPos(pos);
                            
                            // CRITICAL: This is REAL damage - a block was actually broken by the tornado
                            // Record the damage in the survey system
                            DamageSurveyManager.getInstance().addDamage(
                                stormId, 
                                chunkPos, 
                                pos, 
                                state, 
                                net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),
                                windspeed, 
                                level
                            );
                            
                            // CRITICAL: Add this chunk to the tornado's damaged chunk list
                            // This ensures the tornado knows this chunk has real damage
                            com.burrows.easaddon.tornado.TornadoTracker tracker = 
                                com.burrows.easaddon.tornado.TornadoTracker.getInstance();
                            com.burrows.easaddon.tornado.TornadoData tornadoData = tracker.getTornadoData(stormId);
                            if (tornadoData != null) {
                                tornadoData.addDamagedChunk(chunkPos);
                                EASAddon.LOGGER.debug("REAL DAMAGE: Added chunk ({}, {}) to tornado {} - block {} destroyed by {}mph wind", 
                                    chunkPos.x, chunkPos.z, stormId, state.getBlock().getDescriptionId(), Math.round(windEffectAtBlock));
                            }
                            
                            EASAddon.LOGGER.info("PMWeatherHook: Recorded REAL tornado damage - Storm {} destroyed {} at {} " +
                                "(distance: {}m, wind: {}mph, strength: {}mph)", 
                                stormId, state.getBlock().getDescriptionId(), pos, 
                                Math.round(distance), Math.round(windEffectAtBlock), blockStrength);
                            
                            return; // Found the tornado that caused this damage, stop checking others
                            
                        } else {
                            EASAddon.LOGGER.debug("PMWeatherHook: Block break not tornado-caused - insufficient wind " +
                                "(wind: {}mph < strength: {}mph)", 
                                Math.round(windEffectAtBlock), blockStrength);
                        }
                    } else {
                        EASAddon.LOGGER.debug("PMWeatherHook: Block break not tornado-caused - outside damage range " +
                            "(distance: {}m > range: {}m)", Math.round(distance), Math.round(maxDamageRange));
                    }
                    
                } catch (Exception e) {
                    EASAddon.LOGGER.debug("PMWeatherHook: Error processing storm: {}", e.getMessage());
                }
            }
            
        } catch (Exception e) {
            EASAddon.LOGGER.debug("PMWeatherHook: Error in damage tracking: {}", e.getMessage());
        }
    }

    /**
     * FIXED: Enhanced detection using multiple event types and periodic scanning
     */
    @SubscribeEvent
    public void onLevelTick(LevelTickEvent.Post event) {
        if (!reflectionInitialized || !event.getLevel().isClientSide()) return;
        
        // Clean up old tracked blocks every 5 seconds and scan for evidence
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCleanupTime > 5000) {
            scanForScouringAndDebarkingEvidence(event.getLevel());
            
            // Clean up old positions (remove blocks older than 30 seconds)
            recentlyChangedBlocks.removeIf(pos -> {
                // Simple cleanup - in a real implementation you'd track timestamps
                return recentlyChangedBlocks.size() > 100; // Keep only recent 100 positions
            });
            
            lastCleanupTime = currentTime;
        }
    }

    /**
     * ADDED: Comprehensive scanning for debarking and scouring evidence
     */
    private void scanForScouringAndDebarkingEvidence(Level level) {
        try {
            Object weatherHandler = getWeatherHandler(level);
            if (weatherHandler == null) return;
            
            List<?> storms = (List<?>) getStormsMethod.invoke(weatherHandler);
            if (storms == null) return;
            
            // Check each active tornado
            for (Object storm : storms) {
                try {
                    int stormType = (Integer) stormClass.getField("stormType").get(storm);
                    if (stormType != 0) continue; // Only tornadoes
                    
                    long stormId = (Long) stormIDField.get(storm);
                    net.minecraft.world.phys.Vec3 stormPos = (net.minecraft.world.phys.Vec3) stormPositionField.get(storm);
                    int windspeed = (Integer) stormWindspeedField.get(storm);
                    int stage = (Integer) stormStageField.get(storm);
                    
                    // Only check active stage 3+ tornadoes with sufficient wind
                    if (stage < 3 || windspeed < 140) continue;
                    
                    float width = (Float) stormClass.getField("width").get(storm);
                    int windfieldWidth = Math.max((int)width, 40);
                    double maxDamageRange = windfieldWidth * 2.0;
                    
                    // Get tornado tracker
                    com.burrows.easaddon.tornado.TornadoTracker tracker = 
                        com.burrows.easaddon.tornado.TornadoTracker.getInstance();
                    com.burrows.easaddon.tornado.TornadoData tornadoData = tracker.getTornadoData(stormId);
                    if (tornadoData == null) continue;
                    
                    // Scan damaged chunks for evidence
                    Set<ChunkPos> damagedChunks = tornadoData.getDamagedChunks();
                    for (ChunkPos chunkPos : damagedChunks) {
                        scanChunkForEvidence(stormId, chunkPos, stormPos, windspeed, windfieldWidth, level);
                    }
                    
                } catch (Exception e) {
                    EASAddon.LOGGER.debug("Error scanning for evidence in storm: {}", e.getMessage());
                }
            }
            
        } catch (Exception e) {
            EASAddon.LOGGER.debug("Error in evidence scanning: {}", e.getMessage());
        }
    }

    /**
     * ADDED: Scan a specific chunk for debarking and scouring evidence
     */
    private void scanChunkForEvidence(long stormId, ChunkPos chunkPos, Vec3 stormPos, 
                                     int windspeed, int windfieldWidth, Level level) {
        
        // Sample positions across the chunk
        for (int x = 2; x < 16; x += 4) {
            for (int z = 2; z < 16; z += 4) {
                BlockPos worldPos = new BlockPos(chunkPos.x * 16 + x, 0, chunkPos.z * 16 + z);
                BlockPos surfacePos = level.getHeightmapPos(
                    net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, worldPos);
                
                double distance = stormPos.distanceTo(surfacePos.getCenter());
                if (distance > windfieldWidth * 2.0) continue; // Skip if too far
                
                // Check for debarking evidence
                scanForDebarkingAt(stormId, chunkPos, surfacePos, stormPos, windspeed, windfieldWidth, level);
                
                // Check for scouring evidence
                scanForScouringAt(stormId, chunkPos, surfacePos, stormPos, windspeed, windfieldWidth, level);
            }
        }
    }

    /**
     * ADDED: Scan for debarking evidence at a specific position
     */
    private void scanForDebarkingAt(long stormId, ChunkPos chunkPos, BlockPos pos, 
                                   Vec3 stormPos, int windspeed, int windfieldWidth, Level level) {
        
        // Check area around position for stripped logs
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 3; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos checkPos = pos.offset(dx, dy, dz);
                    
                    if (checkPos.getY() < level.getMinBuildHeight() || 
                        checkPos.getY() > level.getMaxBuildHeight()) {
                        continue;
                    }
                    
                    BlockState state = level.getBlockState(checkPos);
                    
                    // Check if this is a stripped log in a natural area
                    if (state.is(net.neoforged.neoforge.common.Tags.Blocks.STRIPPED_LOGS) && 
                        isInNaturalForestArea(checkPos, level)) {
                        
                        // Calculate wind effect to verify this tornado could cause debarking
                        double windEffect = calculateWindEffectUsingPMWeatherLogic(
                            checkPos.getCenter(), stormPos, (float)windfieldWidth / 2.0f, windspeed, windfieldWidth);
                        
                        if (windEffect >= 140.0) { // PMWeather's debarking threshold
                            DamageSurveyManager.getInstance().addDebarkingEvidence(stormId, chunkPos, checkPos);
                            
                            EASAddon.LOGGER.debug("DEBARKING DETECTED: Storm {} evidence at {} " +
                                "(wind: {}mph, distance: {}m)", 
                                stormId, checkPos, Math.round(windEffect), 
                                Math.round(stormPos.distanceTo(checkPos.getCenter())));
                            
                            return; // Found evidence, move to next position
                        }
                    }
                }
            }
        }
    }

    /**
     * ADDED: Scan for scouring evidence at a specific position
     */
    private void scanForScouringAt(long stormId, ChunkPos chunkPos, BlockPos pos, 
                                  Vec3 stormPos, int windspeed, int windfieldWidth, Level level) {
        
        BlockState surfaceState = level.getBlockState(pos);
        
        // Calculate wind effect at this position
        double windEffect = calculateWindEffectUsingPMWeatherLogic(
            pos.getCenter(), stormPos, (float)windfieldWidth / 2.0f, windspeed, windfieldWidth);
        
        ChunkDamageData.ScouringLevel scouringLevel = null;
        
        // Detect scouring progression
        if (isHeavyScouringBlock(surfaceState) && windEffect >= 200.0) {
            scouringLevel = ChunkDamageData.ScouringLevel.MEDIUM_TO_HEAVY;
        } else if (isMediumScouringBlock(surfaceState) && windEffect >= 170.0) {
            scouringLevel = ChunkDamageData.ScouringLevel.DIRT_TO_MEDIUM;
        } else if (surfaceState.is(net.minecraft.world.level.block.Blocks.DIRT) && 
                   isInNaturalGrassArea(pos, level) && windEffect >= 140.0) {
            scouringLevel = ChunkDamageData.ScouringLevel.GRASS_TO_DIRT;
        }
        
        if (scouringLevel != null) {
            DamageSurveyManager.getInstance().addScouringEvidence(stormId, chunkPos, pos, scouringLevel);
            
            EASAddon.LOGGER.debug("SCOURING DETECTED: Storm {} {} evidence at {} " +
                "(wind: {}mph, required: {}mph)", 
                stormId, scouringLevel.name(), pos, Math.round(windEffect), 
                Math.round(scouringLevel.minimumWindspeed));
        }
    }

    /**
     * Calculate wind effect using PMWeather's Rankine vortex model
     */
    private double calculateWindEffectUsingPMWeatherLogic(Vec3 blockPos, Vec3 tornadoPos, 
                                                         float tornadoWidth, int windspeed, int windfieldWidth) {
        try {
            // 1. Distance calculation (2D, ignoring Y)
            double dist = tornadoPos.multiply(1.0, 0.0, 1.0).distanceTo(blockPos.multiply(1.0, 0.0, 1.0));
            
            // 2. Rankine vortex calculation
            float rankineFactor = 4.5f; // Default from Storm.java
            float perc = calculateRankinePercentage(dist, windfieldWidth, rankineFactor);
            
            // 3. Rotational wind calculation
            Vec3 relativePos = blockPos.subtract(tornadoPos);
            Vec3 rotational = new Vec3(relativePos.z, 0.0, -relativePos.x).normalize();
            
            // 4. Basic wind calculation (simplified but accurate for damage detection)
            double realWind = (double)windspeed;
            Vec3 motion = rotational.multiply(realWind * (double)perc, 0.0, realWind * (double)perc);
            
            // 5. Add translational velocity effect
            float affectPerc = (float)Math.sqrt(Math.max(0, 1.0 - dist / (double)((float)windfieldWidth * 2.0f)));
            double estimatedVelocityMagnitude = 15.0; // Typical tornado speed
            motion = motion.add(new Vec3(estimatedVelocityMagnitude * affectPerc, 0.0, estimatedVelocityMagnitude * affectPerc));
            
            return motion.length();
            
        } catch (Exception e) {
            EASAddon.LOGGER.debug("Error calculating wind effect: {}", e.getMessage());
            return 0.0;
        }
    }

    /**
     * Replicate PMWeather's Rankine vortex calculation from Storm.getRankine()
     */
    private float calculateRankinePercentage(double dist, int windfieldWidth, float rankineFactor) {
        float rankineWidth = (float)windfieldWidth / rankineFactor;
        float perc = 0.0f;
        
        if (dist <= (double)(rankineWidth / 2.0f)) {
            // Inner core - linear increase
            perc = (float)dist / (rankineWidth / 2.0f);
        } else if (dist <= (double)((float)windfieldWidth * 2.0f)) {
            // Outer region - power decay
            double denominator = (((float)windfieldWidth * 2.0f - rankineWidth) / 2.0f);
            if (denominator > 0) {
                double falloff = (dist - (double)(rankineWidth / 2.0f)) / denominator;
                perc = (float)Math.pow(Math.max(0, 1.0 - falloff), 1.5);
                perc = Math.max(0.0f, Math.min(1.0f, perc));
            }
        }
        
        if (Float.isNaN(perc)) {
            perc = 0.0f;
        }
        
        return perc;
    }

    /**
     * Block strength calculation with PMWeather custom values support
     */
    private float getBlockStrengthWithCustom(net.minecraft.world.level.block.Block block, Level level) {
        try {
            // Check if PMWeather has a custom strength for this block
            if (reflectionInitialized && blockStrengthsField != null) {
                Map<net.minecraft.world.level.block.Block, Float> customStrengths = 
                    (Map<net.minecraft.world.level.block.Block, Float>) blockStrengthsField.get(null);
                
                if (customStrengths != null && customStrengths.containsKey(block)) {
                    return customStrengths.get(block);
                }
            }
        } catch (Exception e) {
            // Fall through to default calculation
        }
        
        // Use PMWeather's default calculation
        return getBlockStrengthDefaultPMWeather(block, level);
    }

    /**
     * PMWeather's exact default block strength calculation
     */
    private static float getBlockStrengthDefaultPMWeather(net.minecraft.world.level.block.Block block, Level level) {
        net.minecraft.world.item.ItemStack item = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_AXE);
        float destroySpeed = block.defaultBlockState().getDestroySpeed(level, BlockPos.ZERO);
        
        try {
            destroySpeed /= item.getDestroySpeed(block.defaultBlockState());
        } catch (Exception e) {
            destroySpeed = 1.0f; // Safe fallback
        }
        
        // PMWeather's exact formula
        return 60.0f + net.minecraft.util.Mth.sqrt(destroySpeed) * 60.0f;
    }
    
    private Object getWeatherHandler(Level level) {
        // Reuse the same weather handler finding logic from TornadoTracker
        try {
            // Method 1: Look for WeatherHandlerClient class specifically
            try {
                Class<?> weatherHandlerClientClass = Class.forName("dev.protomanly.pmweather.weather.WeatherHandlerClient");
                
                // Look for static instances or fields
                Field[] staticFields = weatherHandlerClientClass.getDeclaredFields();
                for (Field field : staticFields) {
                    if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                        try {
                            field.setAccessible(true);
                            Object fieldValue = field.get(null);
                            if (fieldValue != null) {
                                return fieldValue;
                            }
                        } catch (Exception e) {
                            // Continue trying
                        }
                    }
                }
                
            } catch (ClassNotFoundException e) {
                // Continue to next method
            }
            
            // Method 2: Look in GameBusEvents for managers
            try {
                Class<?> gbe = Class.forName("dev.protomanly.pmweather.event.GameBusEvents");
                Field managersF = gbe.getField("MANAGERS");
                Map managers = (Map)managersF.get(null);
                Object handler = managers.get(level.dimension());
                return handler;
            } catch (Exception e) {
                // Continue
            }
            
            return null;
            
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Helper methods for block type detection
     */
    private boolean isInNaturalForestArea(BlockPos pos, Level level) {
        int treeBlocks = 0;
        int totalChecked = 0;
        
        // Check area around position for natural tree features
        for (BlockPos nearby : BlockPos.betweenClosed(pos.offset(-3, -2, -3), pos.offset(3, 2, 3))) {
            if (!level.isLoaded(nearby)) continue;
            
            BlockState state = level.getBlockState(nearby);
            totalChecked++;
            
            if (state.is(net.minecraft.tags.BlockTags.LOGS) || 
                state.is(net.minecraft.tags.BlockTags.LEAVES) ||
                state.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) {
                treeBlocks++;
            }
        }
        
        // If more than 20% of nearby blocks are natural tree/grass, this is likely a natural forest
        return totalChecked > 0 && (double)treeBlocks / totalChecked > 0.2;
    }

    private boolean isInNaturalGrassArea(BlockPos pos, Level level) {
        int grassBlocks = 0;
        int totalChecked = 0;
        
        // Check surrounding area for grass
        for (BlockPos nearby : BlockPos.betweenClosed(pos.offset(-3, 0, -3), pos.offset(3, 0, 3))) {
            if (!level.isLoaded(nearby) || nearby.equals(pos)) continue;
            
            BlockState state = level.getBlockState(nearby);
            totalChecked++;
            
            if (state.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) {
                grassBlocks++;
            }
        }
        
        // If more than 40% of nearby blocks are grass, this dirt is likely scoured grass
        return totalChecked > 0 && (double)grassBlocks / totalChecked > 0.4;
    }

    private boolean isMediumScouringBlock(BlockState state) {
        try {
            Class<?> modBlocks = Class.forName("dev.protomanly.pmweather.block.ModBlocks");
            Object mediumScouringHolder = modBlocks.getField("MEDIUM_SCOURING").get(null);
            Object mediumScouringBlock = mediumScouringHolder.getClass().getMethod("get").invoke(mediumScouringHolder);
            return state.getBlock() == mediumScouringBlock;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isHeavyScouringBlock(BlockState state) {
        try {
            Class<?> modBlocks = Class.forName("dev.protomanly.pmweather.block.ModBlocks");
            Object heavyScouringHolder = modBlocks.getField("HEAVY_SCOURING").get(null);
            Object heavyScouringBlock = heavyScouringHolder.getClass().getMethod("get").invoke(heavyScouringHolder);
            return state.getBlock() == heavyScouringBlock;
        } catch (Exception e) {
            return false;
        }
    }
}