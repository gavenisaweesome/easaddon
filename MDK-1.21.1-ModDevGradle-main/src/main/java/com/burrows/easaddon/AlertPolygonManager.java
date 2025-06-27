package com.burrows.easaddon;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AlertPolygonManager {
    
    // Map of BlockPos -> List of AlertPolygons
    private static final Map<BlockPos, List<AlertPolygon>> polygonMap = new ConcurrentHashMap<>();
    
    public static void addPolygon(BlockPos pos, AlertPolygon polygon) {
                List<AlertPolygon> list = polygonMap.computeIfAbsent(pos, k -> new ArrayList<>());
                // ←— REMOVE any old polygon from the same storm
                list.removeIf(existing -> existing.stormId == polygon.stormId);
               // ←— THEN add the new one
                list.add(polygon);

    }
    
    public static Collection<AlertPolygon> getPolygonsAt(BlockPos pos) {
        Collection<AlertPolygon> polys = polygonMap.getOrDefault(pos, Collections.emptyList());
        
        return polys;
    }
    
    public static void clearPolygons(BlockPos pos) {
        polygonMap.remove(pos);
    }
    
    
    /**  
     * Remove any polygons whose stormId is *not* in `keepIds`.  
     */
    public static void retainPolygons(BlockPos pos, Set<Long> keepIds) {
        List<AlertPolygon> list = polygonMap.get(pos);
        if (list == null) return;
        boolean changed = list.removeIf(poly -> !keepIds.contains(poly.stormId));
        if (changed) {
        }
    }
    
    

    public static void writePolygons(BlockPos pos, CompoundTag tag) {
        List<AlertPolygon> polygons = polygonMap.get(pos);
        
        if (polygons == null || polygons.isEmpty()) {
            return;
        }
        
        ListTag list = new ListTag();
        for (AlertPolygon poly : polygons) {
            CompoundTag polyTag = new CompoundTag();
            polyTag.putLong("stormId", poly.stormId);
            polyTag.putDouble("centerX", poly.centerX);
            polyTag.putDouble("centerZ", poly.centerZ);
            polyTag.putFloat("halfWidth", poly.halfWidth);
            polyTag.putFloat("halfHeight", poly.halfHeight);
            polyTag.putFloat("rotation", poly.rotationDeg);
            polyTag.putInt("stormType", poly.stormType);     // Write stormType
            polyTag.putInt("stormStage", poly.stormStage);   // Write stormStage
            polyTag.putInt("level", poly.level);
            list.add(polyTag);
            

        }
        tag.put("polygons", list);
    }
    
    public static void readPolygons(BlockPos pos, CompoundTag tag) {

        
        if (!tag.contains("polygons")) {
            return;
        }
        
        
        ListTag list = tag.getList("polygons", Tag.TAG_COMPOUND);
        
        List<AlertPolygon> polygons = new ArrayList<>();
        
        for (int i = 0; i < list.size(); i++) {
            CompoundTag polyTag = list.getCompound(i);
            // FIXED: Correct parameter order to match constructor in EASBlockEntity
            AlertPolygon poly = new AlertPolygon(
                polyTag.getLong("stormId"),
                polyTag.getDouble("centerX"),
                polyTag.getDouble("centerZ"),
                polyTag.getFloat("halfWidth"),
                polyTag.getFloat("halfHeight"),
                polyTag.getFloat("rotation"),
                polyTag.getInt("level"),
                polyTag.getInt("stormType"),    // Read stormType (position 8)
                polyTag.getInt("stormStage")    // Read stormStage (position 9)
            );
            polygons.add(poly);
            

        }
        
        if (!polygons.isEmpty()) {
            polygonMap.put(pos, polygons);
        } else {
        }
    }
    
    // Debug method to check current state
    public static void debugPrintState() {
        for (Map.Entry<BlockPos, List<AlertPolygon>> entry : polygonMap.entrySet()) {
        }
    }
    
 // Add this method to your AlertPolygonManager class:



}