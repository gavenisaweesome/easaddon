
package com.burrows.easaddon;


import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.extensions.IBlockEntityExtension;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.HolderLookup;



public class EASBlockEntity
extends BlockEntity {
    private int tickCounter = 0;
    private final Map<Long, Integer> trackedStormsById = new HashMap<Long, Integer>();
    

    public EASBlockEntity(BlockPos pos, BlockState state) {
    	
        super((BlockEntityType)RegistryHandler.EAS_BLOCK_ENTITY.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, EASBlockEntity be) {
        if (!(level instanceof ServerLevel)) {
            return;
        }
        ServerLevel server = (ServerLevel)level;
        if (++be.tickCounter >= 200) {
            be.tickCounter = 0;
            be.checkStorms(server, pos);
        }
        if (be.tickCounter % 200 == 0) {
            be.cleanupOrphanedOverlays(server, pos);
        }
    }
    
    private void ensureRadarOverlays(ServerLevel level, BlockPos easPos) {

    
    for (Direction dir : Direction.values()) {
        BlockPos radarPos = easPos.relative(dir);
        BlockState radarState = level.getBlockState(radarPos);
        
        try {
            Class<?> radarCls = Class.forName("dev.protomanly.pmweather.block.RadarBlock");
            if (radarCls.isInstance(radarState.getBlock())) {
                BlockPos overlayPos = radarPos.above();
                BlockState overlayState = level.getBlockState(overlayPos);
                

                
                boolean isNewRadar = false;
                
                // Place overlay block if it doesn't exist or is air
                if (overlayState.isAir() || !overlayState.is(RegistryHandler.RADAR_OVERLAY_BLOCK.get())) {
                    level.setBlock(overlayPos, RegistryHandler.RADAR_OVERLAY_BLOCK.get().defaultBlockState(), 3);
                    isNewRadar = true;
                } else {
                }
                
                // NEW: Check if this radar needs polygons for existing tracked storms
                createMissingPolygonsForRadar(level, easPos, radarPos, overlayPos, isNewRadar);
            }
        } catch (ClassNotFoundException e) {
        }
    }
    }
    
 // Add this method to your EASBlockEntity class, and call it from your tick method

    private void cleanupOrphanedOverlays(ServerLevel level, BlockPos easPos) {
        // Check all 6 directions for orphaned overlay blocks
        for (Direction dir : Direction.values()) {
            BlockPos radarPos = easPos.relative(dir);
            BlockPos overlayPos = radarPos.above();
            
            BlockState radarState = level.getBlockState(radarPos);
            BlockState overlayState = level.getBlockState(overlayPos);
            
            // Check if we have an overlay block but no radar block
            if (overlayState.is(RegistryHandler.RADAR_OVERLAY_BLOCK.get())) {
                boolean hasRadarBelow = false;
                try {
                    Class<?> radarCls = Class.forName("dev.protomanly.pmweather.block.RadarBlock");
                    hasRadarBelow = radarCls.isInstance(radarState.getBlock());
                } catch (ClassNotFoundException e) {
                    // PMWeather not installed
                }
                
                if (!hasRadarBelow) {
                    // Remove orphaned overlay
                    level.destroyBlock(overlayPos, false);
                    AlertPolygonManager.clearPolygons(overlayPos);
                }
            }
        }
    }
    
    
    
private void createMissingPolygonsForRadar(ServerLevel level, BlockPos easPos, BlockPos radarPos, BlockPos overlayPos, boolean isNewRadar) {
    if (trackedStormsById.isEmpty()) {
        return; // No storms to check
    }
    

    
    try {
        // Get current storms from PMWeather
        Class<?> gbe = Class.forName("dev.protomanly.pmweather.event.GameBusEvents");
        Field managersF = gbe.getField("MANAGERS");
        Map managers = (Map)managersF.get(null);
        Object handler = managers.get(level.dimension());
        if (handler == null) {
            return;
        }
        
        Method getStorms = handler.getClass().getMethod("getStorms", new Class[0]);
        List storms = (List)getStorms.invoke(handler, new Object[0]);
        
        // Get existing polygons at this overlay position
        Collection<AlertPolygon> existingPolygons = AlertPolygonManager.getPolygonsAt(overlayPos);
        Set<Long> existingStormIds = new HashSet<>();
        for (AlertPolygon poly : existingPolygons) {
            existingStormIds.add(poly.stormId);
        }
        
        
        // Check each currently active storm
        for (Object storm : storms) {
            long stormId = storm.getClass().getField("ID").getLong(storm);
            int type = storm.getClass().getField("stormType").getInt(storm);
            int stage = storm.getClass().getField("stage").getInt(storm);
            Vec3 stormPos = (Vec3)storm.getClass().getField("position").get(storm);
            int windspeed = storm.getClass().getField("windspeed").getInt(storm);
            Vec3 velocity = (Vec3)storm.getClass().getField("velocity").get(storm);
            
            // Skip storms outside of range (512-block radius squared = 262144)
            if (stormPos.distanceToSqr(easPos.getX(), easPos.getY(), easPos.getZ()) > 262144) {
            	
            	
                continue;
               
                	
            } else {
            	
            }
            
            // Skip if we're not tracking this storm (means it hasn't reached alert threshold)
            if (!trackedStormsById.containsKey(stormId)) {
                continue;
            }
            
            // Skip if polygon already exists for this storm at this radar
            if (existingStormIds.contains(stormId)) {
                continue;
            }
            
            // Determine if this storm should have a polygon
            boolean shouldCreatePolygon = false;
            int polygonLevel = 0;
            
            if (type == 0) { // Supercell/Tornado
                if (stage >= 3) {
                    shouldCreatePolygon = true;
                    polygonLevel = Math.max(1, computeAlertLevel(type, stage, windspeed));
                } else if (stage >= 1) {
                    shouldCreatePolygon = true;
                    polygonLevel = stage;
                }
            } else if (type == 1) { // Squall
                if (stage >= 1) {
                    shouldCreatePolygon = true;
                    polygonLevel = stage;
                }
            }
            
            if (shouldCreatePolygon) {
                
                // FIXED: Calculate radar display coordinates
                // The radar display shows a fixed range (typically 1024 blocks in each direction from radar center)
                double radarRange = 1024.0; // Half the total radar display range
                
                // Calculate world distance from radar center to storm
                double worldDx = stormPos.x - (radarPos.getX() + 0.5);
                double worldDz = stormPos.z - (radarPos.getZ() + 0.5);
                
                // Convert to radar display coordinates (0.0 to 1.0 range where 0.5 is center)
                double cx = 0.5 + (worldDx / (radarRange * 2.0));
                double cz = 0.5 + (worldDz / (radarRange * 2.0));
                
                // Clamp to valid radar display bounds
                cx = Math.max(0.0, Math.min(1.0, cx));
                cz = Math.max(0.0, Math.min(1.0, cz));
                
                
                // Size calculation
                float halfW, halfH;
                if (type == 0 && stage >= 3) {
                    halfW = 0.15f;
                    halfH = 0.10f;
                } else if (type == 0) {
                    halfW = 0.25f;
                    halfH = 0.20f;
                } else {
                    halfW = 0.40f;
                    halfH = 0.15f;
                }
                
                // Rotation calculation
                float movementAngle = (float)Math.toDegrees(Math.atan2(velocity.x, -velocity.z));
                movementAngle = (movementAngle + 360) % 360;
                
                float rotation;
                if (type == 1) {
                    rotation = (movementAngle + 90) % 360;
                } else {
                    rotation = movementAngle % 360;
                }
                
                // Create the polygon
                AlertPolygon poly = new AlertPolygon(
                    stormId,
                    cx,           // Radar display X coordinate (0.0-1.0)
                    cz,           // Radar display Z coordinate (0.0-1.0)
                    halfW,
                    halfH,
                    rotation,
                    polygonLevel,
                    type,
                    stage
                );
                
                
                // Add polygon and sync
                AlertPolygonManager.addPolygon(overlayPos, poly);
                
                BlockEntity overlayBE = level.getBlockEntity(overlayPos);
                if (overlayBE instanceof RadarOverlayBlockEntity radarOverlay) {
                    radarOverlay.setChanged();
                    radarOverlay.requestClientUpdate();
                }
            }
        }
        
    } catch (Exception e) {
    }
}


//Add this new method to your EASBlockEntity class
private void updatePolygonAlertLevels(ServerLevel level, BlockPos easPos, long stormId, int type, int stage, int newAlertLevel) {
    
    // FIXED: Get storm occlusion value for consistent rotation
    float stormOcclusion = 0.0f;
    Vec3 velocity = new Vec3(0, 0, 0);
    try {
        Class<?> gbe = Class.forName("dev.protomanly.pmweather.event.GameBusEvents");
        Field managersF = gbe.getField("MANAGERS");
        Map managers = (Map)managersF.get(null);
        Object handler = managers.get(level.dimension());
        if (handler != null) {
            Method getStorms = handler.getClass().getMethod("getStorms", new Class[0]);
            List storms = (List)getStorms.invoke(handler, new Object[0]);
            
            // Find the specific storm by ID
            for (Object storm : storms) {
                long currentStormId = storm.getClass().getField("ID").getLong(storm);
                if (currentStormId == stormId) {
                    stormOcclusion = storm.getClass().getField("occlusion").getFloat(storm);
                    velocity = (Vec3)storm.getClass().getField("velocity").get(storm);
                    break;
                }
            }
        }
    } catch (Exception e) {
    }
    
    boolean polygonUpdated = false;
    
    // Check all radar positions around the EAS
    for (Direction dir : Direction.values()) {
        BlockPos radarPos = easPos.relative(dir);
        BlockState radarState = level.getBlockState(radarPos);
        
        try {
            Class<?> radarCls = Class.forName("dev.protomanly.pmweather.block.RadarBlock");
            if (radarCls.isInstance(radarState.getBlock())) {
                BlockPos overlayPos = radarPos.above();
                
                // Get existing polygons at this radar
                Collection<AlertPolygon> existingPolygons = AlertPolygonManager.getPolygonsAt(overlayPos);
                
                // Find the polygon for this specific storm
                for (AlertPolygon existingPoly : existingPolygons) {
                    if (existingPoly.stormId == stormId) {
                        
                        // Determine new polygon level based on storm type/stage
                        int polygonLevel = newAlertLevel;
                        if (type == 0) { // Supercell/Tornado
                            if (stage >= 3) {
                                polygonLevel = Math.max(1, newAlertLevel);
                            } else if (stage >= 1) {
                                polygonLevel = stage;
                            }
                        } else if (type == 1) { // Squall
                            if (stage >= 1) {
                                polygonLevel = stage;
                            }
                        }
                        
                        // FIXED: Recalculate rotation with current occlusion value
                        float movementAngle = (float)Math.toDegrees(Math.atan2(velocity.x, -velocity.z));
                        movementAngle = (movementAngle + 360) % 360;
                        
                        // Apply occlusion-based rotation
                        float occlusionRotation = stormOcclusion * 45.0f;
                        float newRotation;
                        if (type == 1) {
                            // Squall line
                            newRotation = (movementAngle + 90 + occlusionRotation) % 360;
                        } else {
                            // Tornado
                            newRotation = (movementAngle + occlusionRotation) % 360;
                        }
                        
                        // Create updated polygon with new alert level and current rotation
                        AlertPolygon updatedPoly = new AlertPolygon(
                            stormId,
                            existingPoly.centerX,    // Keep same position
                            existingPoly.centerZ,    // Keep same position
                            existingPoly.halfWidth,  // Keep same size
                            existingPoly.halfHeight, // Keep same size
                            newRotation,             // FIXED: Updated rotation with occlusion
                            polygonLevel,            // NEW ALERT LEVEL
                            type,
                            stage
                        );
                        
                        // Replace the polygon
                        AlertPolygonManager.addPolygon(overlayPos, updatedPoly);
                        polygonUpdated = true;
                        
                        
                        // Trigger client update to show new colors and rotation
                        BlockEntity overlayBE = level.getBlockEntity(overlayPos);
                        if (overlayBE instanceof RadarOverlayBlockEntity radarOverlay) {
                            radarOverlay.setChanged();
                            radarOverlay.requestClientUpdate();
                        }
                        
                        break; // Found and updated the polygon for this storm
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            // PMWeather not installed
        }
    }
    
    if (!polygonUpdated) {
    }
}


// Replace your existing checkStorms method with this enhanced version
private void checkStorms(ServerLevel level, BlockPos easPos) {
    Class<?> metarCls;
    try {
        metarCls = Class.forName("dev.protomanly.pmweather.block.MetarBlock");
    } catch (ClassNotFoundException e) {
        return;
    }
    boolean metarAdjacent = false;
    for (Direction dir : Direction.values()) {
        BlockPos adj = easPos.relative(dir);
        BlockState st = level.getBlockState(adj);
        if (metarCls != null && metarCls.isInstance(st.getBlock())) {
            metarAdjacent = true;
            break;
        }
    }
    
    if (!metarAdjacent) {
        return;
    }
    
    ensureRadarOverlays(level, easPos);

    try {
        Class<?> gbe = Class.forName("dev.protomanly.pmweather.event.GameBusEvents");
        Field managersF = gbe.getField("MANAGERS");
        Map managers = (Map)managersF.get(null);
        Object handler = managers.get(level.dimension());
        if (handler == null) {
            return;
        }
        Method getStorms = handler.getClass().getMethod("getStorms", new Class[0]);
        List storms = (List)getStorms.invoke(handler, new Object[0]);
        
        // 1) Gather current storm IDs and process storms
        Set<Long> allCurrentStormIds = new HashSet<>();
        Set<Long> inRangeStormIds = new HashSet<>();
        
        for (Object storm : storms) {
            long stormId = storm.getClass().getField("ID").getLong(storm);
            int type = storm.getClass().getField("stormType").getInt(storm);
            int stage = storm.getClass().getField("stage").getInt(storm);
            Vec3 stormPos = (Vec3)storm.getClass().getField("position").get(storm);
            int windspeed = storm.getClass().getField("windspeed").getInt(storm);
            Vec3 velocity = (Vec3)storm.getClass().getField("velocity").get(storm);
            double horizontalSpeed = Math.sqrt(velocity.x*velocity.x + velocity.z*velocity.z);
            int movementSpeedMPH = (int)((horizontalSpeed * 20 * 2.23694) / 6);
            
            allCurrentStormIds.add(stormId);
            
            // Check if storm is in EAS range
            if (stormPos.distanceToSqr(easPos.getX(), easPos.getY(), easPos.getZ()) <= 262144) {
            	EASAddon.LOGGER.warn("within range 1231323321321213312321");
                inRangeStormIds.add(stormId);
                
                int newLevel = computeAlertLevel(type, stage, windspeed);
                Integer oldLevel = this.trackedStormsById.get(stormId);
                
                // *** CRITICAL FIX: Handle both new storms AND alert level changes ***
                if (oldLevel == null && newLevel > 0) {
                    // New storm detection
                    this.handleStormDetection(level, easPos, stormId, stormPos, type, stage, windspeed, movementSpeedMPH, velocity);
                    this.trackedStormsById.put(stormId, newLevel);
                } else if (oldLevel != null && newLevel != oldLevel && newLevel > 0) {
                    // *** NEW: Alert level changed for existing storm ***

                    
                    // Send new warning message
                    this.handleStormDetection(level, easPos, stormId, stormPos, type, stage, windspeed, movementSpeedMPH, velocity);
                    
                    // Update polygons with new alert level
                    this.updatePolygonAlertLevels(level, easPos, stormId, type, stage, newLevel);
                    
                    // Update tracking
                    this.trackedStormsById.put(stormId, newLevel);
                } else if (oldLevel != null && newLevel == 0) {
                    // Storm no longer meets alert criteria
                    this.trackedStormsById.remove(stormId);
                }
            }
        }

        // 2) Clean up polygons for ALL radars, handling empty storms list
        boolean hasAnyPolygonsRemaining = false;
        
        for (Direction dir : Direction.values()) {
            BlockPos radarPos = easPos.relative(dir);
            BlockState radarState = level.getBlockState(radarPos);
            
            try {
                Class<?> radarCls = Class.forName("dev.protomanly.pmweather.block.RadarBlock");
                if (radarCls.isInstance(radarState.getBlock())) {
                    BlockPos overlayPos = radarPos.above();
                    
                    // Get current polygons at this radar
                    Collection<AlertPolygon> currentPolygons = AlertPolygonManager.getPolygonsAt(overlayPos);
                    int polygonCountBefore = currentPolygons.size();
                    
                    // Calculate which storms are visible to THIS specific radar
                    Set<Long> visibleStormIds = new HashSet<>();
                    double radarRange = 1024.0;
                    
                    // Only add storm IDs if storms list is not empty
                    if (!storms.isEmpty()) {
                        for (Object storm : storms) {
                            long stormId = storm.getClass().getField("ID").getLong(storm);
                            Vec3 stormPos = (Vec3)storm.getClass().getField("position").get(storm);
                            
                            // Check if storm is within this radar's display range
                            double worldDx = stormPos.x - (radarPos.getX() + 0.5);
                            double worldDz = stormPos.z - (radarPos.getZ() + 0.5);
                            double distanceFromRadar = Math.sqrt(worldDx * worldDx + worldDz * worldDz);
                            
                            // Only include storms visible on this radar's display AND that we're tracking
                            if (distanceFromRadar <= radarRange && trackedStormsById.containsKey(stormId)) {
                                visibleStormIds.add(stormId);
                            }
                        }
                    }
                    
                    
                    // Clean up polygons - only keep those for storms visible to this radar
                    AlertPolygonManager.retainPolygons(overlayPos, visibleStormIds);
                    
                    // Check if any polygons remain after cleanup
                    Collection<AlertPolygon> remainingPolygons = AlertPolygonManager.getPolygonsAt(overlayPos);
                    int polygonCountAfter = remainingPolygons.size();
                    
                    if (polygonCountAfter > 0) {
                        hasAnyPolygonsRemaining = true;
                    }
                    
                    // Always trigger client update when polygons are removed
                    if (polygonCountBefore != polygonCountAfter) {
                        BlockEntity be = level.getBlockEntity(overlayPos);
                        if (be instanceof RadarOverlayBlockEntity roe) {
                            roe.setChanged();
                            roe.requestClientUpdate();

                        }
                    }
                }
            } catch (ClassNotFoundException e) {
                // PMWeather not installed
            }
        }

        // 3) Keep our own trackedStormsById in sync with only in-range storms
        Set<Long> previouslyTracked = new HashSet<>(this.trackedStormsById.keySet());
        this.trackedStormsById.keySet().retainAll(inRangeStormIds);
        
        // Log when storms are removed from tracking
        Set<Long> removedStorms = new HashSet<>(previouslyTracked);
        removedStorms.removeAll(this.trackedStormsById.keySet());
        if (!removedStorms.isEmpty()) {
        }

        // 4) Update existing polygons for position changes (only if storms exist)
        if (!storms.isEmpty()) {
            for (Object storm : storms) {
                long stormId = storm.getClass().getField("ID").getLong(storm);
                int type = storm.getClass().getField("stormType").getInt(storm);
                int stage = storm.getClass().getField("stage").getInt(storm);
                Vec3 stormPos = (Vec3)storm.getClass().getField("position").get(storm);
                Vec3 velocity = (Vec3)storm.getClass().getField("velocity").get(storm);
                
                // FIXED: Get occlusion value for rotation calculation
                float stormOcclusion = 0.0f;
                try {
                    stormOcclusion = storm.getClass().getField("occlusion").getFloat(storm);
                } catch (Exception e) {
                    // Default to 0 if can't get occlusion
                    stormOcclusion = 0.0f;
                }
                
                // Only update polygons for storms that we're tracking
                if (!trackedStormsById.containsKey(stormId)) {
                    continue;
                }
                
                // Update polygon positions for existing storms
                for (Direction dir : Direction.values()) {
                    BlockPos radarPos = easPos.relative(dir);
                    BlockState radarState = level.getBlockState(radarPos);
                    
                    try {
                        Class<?> radarCls = Class.forName("dev.protomanly.pmweather.block.RadarBlock");
                        if (radarCls.isInstance(radarState.getBlock())) {
                            BlockPos overlayPos = radarPos.above();
                            
                            // Check if storm is still visible to this radar
                            double radarRange = 1024.0;
                            double worldDx = stormPos.x - (radarPos.getX() + 0.5);
                            double worldDz = stormPos.z - (radarPos.getZ() + 0.5);
                            double distanceFromRadar = Math.sqrt(worldDx * worldDx + worldDz * worldDz);
                            
                            if (distanceFromRadar > radarRange) {
                                // Storm moved out of this radar's range, remove its polygon
                                Collection<AlertPolygon> existingPolygons = AlertPolygonManager.getPolygonsAt(overlayPos);
                                boolean removed = false;
                                for (AlertPolygon existingPoly : existingPolygons) {
                                    if (existingPoly.stormId == stormId) {
                                        Set<Long> keepIds = new HashSet<>();
                                        for (AlertPolygon poly : existingPolygons) {
                                            if (poly.stormId != stormId) {
                                                keepIds.add(poly.stormId);
                                            }
                                        }
                                        AlertPolygonManager.retainPolygons(overlayPos, keepIds);
                                        removed = true;
                                        break;
                                    }
                                }
                                
                                if (removed) {
                                    BlockEntity overlayBE = level.getBlockEntity(overlayPos);
                                    if (overlayBE instanceof RadarOverlayBlockEntity radarOverlay) {
                                        radarOverlay.setChanged();
                                        radarOverlay.requestClientUpdate();
                                    }
                                }
                                continue;
                            }
                            
                            // Storm is still visible, update its position and rotation
                            Collection<AlertPolygon> existingPolygons = AlertPolygonManager.getPolygonsAt(overlayPos);
                            
                            for (AlertPolygon existingPoly : existingPolygons) {
                                if (existingPoly.stormId == stormId) {
                                    // Calculate updated position
                                    double newCx = 0.5 + (worldDx / (radarRange * 2.0));
                                    double newCz = 0.5 + (worldDz / (radarRange * 2.0));
                                    newCx = Math.max(0.0, Math.min(1.0, newCx));
                                    newCz = Math.max(0.0, Math.min(1.0, newCz));
                                    
                                    // FIXED: Calculate updated rotation with occlusion
                                    float movementAngle = (float)Math.toDegrees(Math.atan2(velocity.x, -velocity.z));
                                    movementAngle = (movementAngle + 360) % 360;
                                    
                                    // Apply occlusion-based rotation (same logic as in handleStormDetection)
                                    float occlusionRotation = stormOcclusion * 45.0f;
                                    float newRotation;
                                    if (type == 1) {
                                        // Squall line
                                        newRotation = (movementAngle + 90 + occlusionRotation) % 360;
                                    } else {
                                        // Tornado
                                        newRotation = (movementAngle + occlusionRotation) % 360;
                                    }
                                    
                                    // Check if position or rotation actually changed
                                    boolean positionChanged = Math.abs(existingPoly.centerX - newCx) > 0.001 || 
                                                            Math.abs(existingPoly.centerZ - newCz) > 0.001;
                                    boolean rotationChanged = Math.abs(existingPoly.rotationDeg - newRotation) > 0.5; // 0.5 degree threshold
                                    
                                    if (positionChanged || rotationChanged) {
                                        // Create updated polygon
                                        AlertPolygon updatedPoly = new AlertPolygon(
                                            stormId,
                                            newCx,
                                            newCz,
                                            existingPoly.halfWidth,
                                            existingPoly.halfHeight,
                                            newRotation, // FIXED: Now includes occlusion-based rotation
                                            existingPoly.level,
                                            type,
                                            stage
                                        );
                                        
                                        // Update the polygon
                                        AlertPolygonManager.addPolygon(overlayPos, updatedPoly);
                                        
                                        // Trigger client update
                                        BlockEntity overlayBE = level.getBlockEntity(overlayPos);
                                        if (overlayBE instanceof RadarOverlayBlockEntity radarOverlay) {
                                            radarOverlay.setChanged();
                                            radarOverlay.requestClientUpdate();
                                        }
                                        
                                    }
                                    break;
                                }
                            }
                        }
                    } catch (ClassNotFoundException e) {
                        // PMWeather not installed
                    }
                }
            }
        }
        
        // Log final state for debugging

        
    }
    catch (Exception e) {
    }
}

    private int computeAlertLevel(int type, int stage, int windspeed) {
        if (type == 0) {
            if (stage >= 3) {
                if (windspeed > 190) {
                    return 3;
                } else if (windspeed >= 137) {
                    return 2;
                } else {
                    // windspeed 0–136
                    return 1;
                }
            } else if (stage == 2) {
                return 1;
            }
        } else if (type == 1) {
            if (stage == 3) {
                return 3;
            }
            if (stage == 2) {
                return 2;
            }
            if (stage == 1) {
                return 1;
            }
        }
        return 0;
    }
    
    




    
    private String getWindDirection(Vec3 velocity) {
        double dx = velocity.x;
        double dz = velocity.z;
        double angle = Math.toDegrees(Math.atan2(dx, -dz));
        angle = (angle + 360) % 360; // Normalize to 0-360 degrees

        String[] directions = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        int index = (int) ((angle + 22.5) / 45) % 8;
        return directions[index];
    }
    
    private float getWindDirectionAngle(Vec3 v) {
        // reuse your atan2 logic but return a float angle
        double angle = Math.toDegrees(Math.atan2(v.x, -v.z));
        return (float)((angle + 360) % 360);
    }
    
    
 // Replace the handleStormDetection method in EASBlockEntity.java with this fixed version:

 // Replace the handleStormDetection method in EASBlockEntity.java

private void handleStormDetection(
    ServerLevel level,
    BlockPos easPos,
    long stormId,
    Vec3 stormPos,
    int type,
    int stage,
    int windspeed,
    int movementSpeedMPH,
    Vec3 velocity
) {
    String dimensionId = level.dimension().location().toString();
    String windDirection = this.getWindDirection(velocity);
    String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
    String windInfo = String.format("%s @ %d MPH", windDirection, movementSpeedMPH);
    SoundEvent alertSound = (SoundEvent)RegistryHandler.EAS_ALERT.get();
    
    int alertLevel = computeAlertLevel(type, stage, windspeed);
    
    // === DETERMINE IF WE SHOULD CREATE A POLYGON ===
    boolean shouldCreatePolygon = false;
    int polygonLevel = 0;
    
    if (type == 0) { // Supercell/Tornado
        if (stage >= 3) {
            shouldCreatePolygon = true;
            polygonLevel = Math.max(1, alertLevel);
        } else if (stage >= 1) {
            shouldCreatePolygon = true;
            polygonLevel = stage;
        }
    } else if (type == 1) { // Squall
        if (stage >= 1) {
            shouldCreatePolygon = true;
            polygonLevel = stage;
        }
    }
    
    // === CREATE AND SEND POLYGON (IF NEEDED) ===
    if (shouldCreatePolygon) {
        
        // Check if polygon already exists to prevent duplicates
        boolean polygonExists = false;
        for (Direction dir : Direction.values()) {
            BlockPos overlayPos = easPos.relative(dir).above();
            Collection<AlertPolygon> existingPolygons = AlertPolygonManager.getPolygonsAt(overlayPos);
            for (AlertPolygon existing : existingPolygons) {
                if (existing.stormId == stormId) {
                    polygonExists = true;
                    break;
                }
            }
            if (polygonExists) break;
        }
        
        if (!polygonExists) {
            // FIXED: Get occlusion value from storm for rotation calculation
            float stormOcclusion = 0.0f;
            try {
                // Get current storms from PMWeather to find occlusion value
                Class<?> gbe = Class.forName("dev.protomanly.pmweather.event.GameBusEvents");
                Field managersF = gbe.getField("MANAGERS");
                Map managers = (Map)managersF.get(null);
                Object handler = managers.get(level.dimension());
                if (handler != null) {
                    Method getStorms = handler.getClass().getMethod("getStorms", new Class[0]);
                    List storms = (List)getStorms.invoke(handler, new Object[0]);
                    
                    // Find the specific storm by ID
                    for (Object storm : storms) {
                        long currentStormId = storm.getClass().getField("ID").getLong(storm);
                        if (currentStormId == stormId) {
                            stormOcclusion = storm.getClass().getField("occlusion").getFloat(storm);
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                stormOcclusion = 0.0f;
            }
            
            // FIXED: Calculate coordinates relative to each radar position
            for (Direction dir : Direction.values()) {
                BlockPos radarPos = easPos.relative(dir);
                BlockState radarState = level.getBlockState(radarPos);
                
                try {
                    Class<?> radarCls = Class.forName("dev.protomanly.pmweather.block.RadarBlock");
                    if (radarCls.isInstance(radarState.getBlock())) {
                        BlockPos overlayPos = radarPos.above();
                        
                        // CRITICAL FIX: Calculate radar display coordinates
                        // The radar display shows a fixed range (typically 1024 blocks in each direction from radar center)
                        double radarRange = 1024.0; // Half the total radar display range
                        
                        // Calculate world distance from radar center to storm
                        double worldDx = stormPos.x - (radarPos.getX() + 0.5);
                        double worldDz = stormPos.z - (radarPos.getZ() + 0.5);
                        
                        // Convert to radar display coordinates (0.0 to 1.0 range where 0.5 is center)
                        // This maps the radar's display area to 0-1 coordinates
                        double cx = 0.5 + (worldDx / (radarRange * 2.0));
                        double cz = 0.5 + (worldDz / (radarRange * 2.0));
                        
                        // Clamp to valid radar display bounds
                        cx = Math.max(0.0, Math.min(1.0, cx));
                        cz = Math.max(0.0, Math.min(1.0, cz));
                        

                        // Size calculation (unchanged)
                        float halfW, halfH;
                        if (type == 0 && stage >= 3) {
                            halfW = 0.15f;
                            halfH = 0.10f;
                        } else if (type == 0) {
                            halfW = 0.25f;
                            halfH = 0.20f;
                        } else {
                            halfW = 0.40f;
                            halfH = 0.15f;
                        }
                        
                        // FIXED: Rotation calculation with occlusion modification
                        float movementAngle = (float)Math.toDegrees(Math.atan2(velocity.x, -velocity.z));
                        movementAngle = (movementAngle + 360) % 360;
                        
                        // CRITICAL FIX: Apply occlusion to rotation
                        // Occlusion represents directional bias - higher occlusion = more directional deviation
                        // Apply up to 45 degrees of rotation based on occlusion (0.0-1.0 maps to 0-45 degrees)
                        float occlusionRotation = stormOcclusion * 45.0f;
                        
                        // For tornadoes, apply the occlusion rotation clockwise
                        // For squall lines, apply it to the perpendicular direction
                        float rotation;
                        if (type == 1) {
                            // Squall line - rotate base angle + 90 degrees + occlusion effect
                            rotation = (movementAngle + 90 + occlusionRotation) % 360;
                        } else {
                            // Tornado - rotate base angle + occlusion effect
                            rotation = (movementAngle + occlusionRotation) % 360;
                        }
                        

                        // Create polygon with occlusion-modified rotation
                        AlertPolygon poly = new AlertPolygon(
                            stormId,      
                            cx,           // Radar display X coordinate (0.0-1.0)
                            cz,           // Radar display Z coordinate (0.0-1.0)
                            halfW,        
                            halfH,        
                            rotation,     // FIXED: Now includes occlusion-based rotation
                            polygonLevel, 
                            type,         
                            stage         
                        );
                        

                        
                        // Add polygon and sync
                        AlertPolygonManager.addPolygon(overlayPos, poly);
                        
                        BlockEntity overlayBE = level.getBlockEntity(overlayPos);
                        if (overlayBE instanceof RadarOverlayBlockEntity radarOverlay) {
                            radarOverlay.setChanged();
                            radarOverlay.requestClientUpdate();
                        }
                    }
                } catch (ClassNotFoundException e) {
                }
            }
        }
    }
    
    // === PLAYER NOTIFICATIONS === (unchanged)
    for (ServerPlayer player : level.players()) {
        Component msg;
        if (!this.isPlayerBound(player, easPos, dimensionId)) continue;
        
        if ((msg = (switch (type) {
            case 0 -> {
                if (stage >= 3) {
                    yield this.createTornadoMessage(alertLevel, time, windInfo);
                }
                yield this.createSupercellMessage(stage, time, windInfo);
            }
            case 1 -> this.createSquallMessage(stage, time, windInfo);
            default -> null;
        })) == null) continue;
        
        player.sendSystemMessage(msg);
        player.playNotifySound(alertSound, SoundSource.MASTER, 1.0f, 1.0f);
    }
}

    private Component createSquallMessage(int stage, String time, String windInfo) {
        String template;
        switch (stage) {
            case 1:
            	template = "\u00a73--EAS BULLETIN--\u00a7r\nThe National Weather Service has issued a SEVERE THUNDERSTORM WARNING for your local area.\nAt %s, severe thunderstorms were located along a line, moving %s\n\u00a74HAZARD:\u00a7r 60 mile per hour wind gusts and quarter size hail.\n\u00a74SOURCE:\u00a7r Radar indicated\n\u00a74IMPACT:\u00a7r Hail may damage entities and players.\n";
            	break;
            case 2:
            	template = "\u00a73--EAS BULLETIN--\u00a7r\nThe National Weather Service has issued a SEVERE THUNDERSTORM WARNING for your local area.\nAt %s, severe thunderstorms were located along a line, moving %s\n\u00a74HAZARD:\u00a7r 70 mile per hour wind gusts and golf ball size hail.\n\u00a74SOURCE:\u00a7r Radar indicated\n\u00a74IMPACT:\u00a7r Hail may damage entities and players.\n";
            	break;
            case 3:
            	template = "\u00a73--EAS BULLETIN--\u00a7r\nThe National Weather Service has issued a SEVERE THUNDERSTORM WARNING for your local area.\nAt %s, severe thunderstorms were located along a line, moving %s\n\u00a74These are DESTRUCTIVE STORMS for your local area.\u00a7r\n\u00a74HAZARD:\u00a7r 80 mile per hour or greater wind gusts and baseball size hail.\n\u00a74SOURCE:\u00a7r Radar indicated\n\u00a74IMPACT:\u00a7r Hail may damage entities and players.\n";
            	break;
            default:
            	template = null;
            	break;
        }
        return template != null ? Component.literal(String.format(template, time, windInfo)) : null;
    }

    private Component createSupercellMessage(int stage, String time, String windInfo) {
        String template;
        switch (stage) {
            case 1:
                template = "\u00a73--EAS BULLETIN--\u00a7r\nThe National Weather Service has issued a SEVERE THUNDERSTORM WARNING for your local area.\nAt %s, a severe thunderstorm was located near your area, moving %s\n\u00a74HAZARD:\u00a7r 60 mile per hour wind gusts and quarter size hail.\n\u00a74SOURCE:\u00a7r Radar indicated\n\u00a74IMPACT:\u00a7r Hail may damage entities and players.\n";
                break;
            case 2:
                template = "\u00a73--EAS BULLETIN--\u00a7r\nThe National Weather Service has issued a SEVERE THUNDERSTORM WARNING for your local area.\nAt %s, a severe thunderstorm was located near your area, moving %s\n\u00a74HAZARD:\u00a7r 70 mile per hour wind gusts and golf ball size hail.\n\u00a74SOURCE:\u00a7r Radar indicated\n\u00a74IMPACT:\u00a7r Hail may damage entities and players. \u00a7cA tornado may form at any moment.\nFor your protection, move into a sturdy building.\u00a7r\n";
                break;
            default:
                template = null;
                break;
        }
        return template != null ? Component.literal(String.format(template, time, windInfo)) : null;
    }

    private Component createTornadoMessage(int alertLevel, String time, String windInfo) {
        switch (alertLevel) {
            case 1:
                // 0–136 → level 1
                return Component.literal(String.format(
                    "\u00a7c--EAS BULLETIN--\u00a7r\n" +
                    "The National Weather Service has issued a TORNADO WARNING for your local area.\n" +
                    "At %s, a severe thunderstorm capable of producing a tornado was located near your area,\n" +
                    "moving %s\n" +
                    "\u00a74HAZARD:\u00a7r Tornado\n" +
                    "\u00a74SOURCE:\u00a7r Radar confirmed Tornado\n" +
                    "\u00a74IMPACT:\u00a7r Flying debris will be dangerous and may suffocate people. " +
                    "Homes may be damaged. If you are caught by the tornado, you may not be let go of " +
                    "for some time and may sustain fall damage.\n",
                    time, windInfo
                ));
            case 2:
                // 137–190 → level 2
                return Component.literal(String.format(
                    "\u00a7c--EAS BULLETIN--\u00a7r\n" +
                    "The National Weather Service has issued a TORNADO WARNING for your local area.\n" +
                    "\u00a74This is a PARTICULARLY DANGEROUS SITUATION.\u00a7r\n" +
                    "At %s, a confirmed large tornado was located near your area, moving %s\n" +
                    "\u00a74HAZARD:\u00a7r Damaging tornado\n" +
                    "\u00a74SOURCE:\u00a7r Radar confirmed Tornado\n" +
                    "\u00a74IMPACT:\u00a7r Flying debris will be deadly and may suffocate people. " +
                    "Homes may be destroyed. If you are caught by the tornado, you may not be " +
                    "let go of for some time and may sustain fall damage once it dissipates.\n",
                    time, windInfo
                ));
            case 3:
                // 191+ → level 3
                return Component.literal(String.format(
                    "\u00a74--TORNADO EMERGENCY FOR YOUR LOCAL AREA--\u00a7r\n" +
                    "The National Weather Service has issued a TORNADO WARNING for your local area.\n" +
                    "At %s, a confirmed large and extremely dangerous tornado was located near your area,\n" +
                    "moving %s\n" +
                    "\u00a74This is a TORNADO EMERGENCY for your local area. Take shelter now!\u00a7r\n" +
                    "\u00a74HAZARD:\u00a7r Deadly tornado\n" +
                    "\u00a74SOURCE:\u00a7r Radar confirmed tornado\n" +
                    "\u00a74IMPACT:\u00a7r Flying debris will be dangerous and will suffocate people. " +
                    "Homes and towns will be damaged or destroyed, making it completely unrecognizable to survivors. If you are caught by the tornado, you may not be let go of " +
                    "for some time and WILL sustain deadly fall damage.\n",
                    time, windInfo
                ));
            default:
                // alertLevel == 0 or unknown
                return Component.literal("\u00a7eNo current tornado threat detected.");
        }
    }


    private boolean isPlayerBound(ServerPlayer player, BlockPos easPos, String dimensionId) {
        for (ItemStack stack : player.getInventory().items) {
            CompoundTag tag;
            CustomData data;
            if (!(stack.getItem() instanceof EasTransmitterItem) || (data = (CustomData)stack.get(DataComponents.CUSTOM_DATA)) == null || (tag = data.copyTag()).getLong("boundPos") != easPos.asLong() || !tag.getString("boundDim").equals(dimensionId)) continue;
            return true;
        }
        return false;
    }
    
    

    

    private record StormKey(BlockPos pos, int type) {
    }
    
    // 1) Write NBT for both world‐save & client‐sync
 // Replace the writeNbt and readNbt methods with the following:

    
    private void sendPolygonToRadarOverlays(ServerLevel level, BlockPos easPos, AlertPolygon polygon) {

    
    // Check all 6 adjacent positions for radar blocks
    for (Direction dir : Direction.values()) {
        BlockPos radarPos = easPos.relative(dir);
        BlockState radarState = level.getBlockState(radarPos);

        
        try {
            Class<?> radarCls = Class.forName("dev.protomanly.pmweather.block.RadarBlock");
            if (radarCls.isInstance(radarState.getBlock())) {

                
                // Found a radar block, now check for overlay above it
                BlockPos overlayPos = radarPos.above();

                
                BlockEntity overlayBE = level.getBlockEntity(overlayPos);

                
                if (overlayBE instanceof RadarOverlayBlockEntity radarOverlay) {

                    
                    // CRITICAL FIX: Store the polygon in the manager BEFORE requesting client update
                    AlertPolygonManager.addPolygon(overlayPos, polygon);

                    
                    // Mark the block entity as changed so it saves to NBT
                    radarOverlay.setChanged();
                    
                    // Trigger client sync - this will call getUpdateTag() which includes our polygon data
                    radarOverlay.requestClientUpdate();
                    
                    // ADDITIONAL DEBUG: Verify the polygon was stored
                    int polyCount = AlertPolygonManager.getPolygonsAt(overlayPos).size();

                    
                } else {

                    
                    // Try to place the overlay block if it's missing
                    BlockState overlayState = level.getBlockState(overlayPos);
                    if (overlayState.isAir()) {
                        level.setBlock(overlayPos, RegistryHandler.RADAR_OVERLAY_BLOCK.get().defaultBlockState(), 3);
                        
                        // Try again after placing
                        BlockEntity newOverlayBE = level.getBlockEntity(overlayPos);
                        if (newOverlayBE instanceof RadarOverlayBlockEntity newRadarOverlay) {
                            AlertPolygonManager.addPolygon(overlayPos, polygon);
                            newRadarOverlay.setChanged();
                            newRadarOverlay.requestClientUpdate();
                        }
                    }
                }
            } else {

            }
        } catch (ClassNotFoundException e) {
        }
    }
    

}
}