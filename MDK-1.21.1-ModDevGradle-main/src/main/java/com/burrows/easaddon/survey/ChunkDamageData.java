package com.burrows.easaddon.survey;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;
import java.util.stream.Collectors;

import com.burrows.easaddon.EASAddon;

/**
 * Enhanced chunk damage data that tracks debarking and ground scouring evidence
 */
public class ChunkDamageData {
    private final ChunkPos chunkPos;
    private final Map<BlockPos, DamageRecord> damageRecords = new HashMap<>();
    
    // ENHANCED: Track debarking and scouring evidence
    private final Set<BlockPos> debarkedLogs = new HashSet<>();
    private final Map<BlockPos, ScouringLevel> scouringEvidence = new HashMap<>();
    
    private boolean surveyed = false;
    private String surveyedBy = null;
    private long surveyTime = 0;
    private int determinedEFRating = -1;
    private float maxWindspeedFound = 0.0f;
    
    // ADDED: Scouring evidence levels
    public enum ScouringLevel {
        GRASS_TO_DIRT(140.0f),      // Minimum 140 mph
        DIRT_TO_MEDIUM(170.0f),     // Minimum 170 mph  
        MEDIUM_TO_HEAVY(200.0f);    // Minimum 200 mph
        
        public final float minimumWindspeed;
        
        ScouringLevel(float minimumWindspeed) {
            this.minimumWindspeed = minimumWindspeed;
        }
    }
    
    public static class DamageRecord {
        public final BlockState originalBlock;
        public final BlockState resultingBlock;
        public final float blockStrength;
        public final long timestamp;
        public final int tornadoWindspeed;
        
        public DamageRecord(BlockState original, BlockState resulting, float strength, long time, int windspeed) {
            this.originalBlock = original;
            this.resultingBlock = resulting;
            this.blockStrength = strength;
            this.timestamp = time;
            this.tornadoWindspeed = windspeed;
        }
    }
    
    public ChunkDamageData(ChunkPos chunkPos) {
        this.chunkPos = chunkPos;
    }
    
    public void addDamage(BlockPos pos, BlockState original, BlockState resulting, float strength, int tornadoWindspeed) {
        damageRecords.put(pos, new DamageRecord(original, resulting, strength, System.currentTimeMillis(), tornadoWindspeed));
    }
    
    // ADDED: Record debarking evidence
    public void addDebarkingEvidence(BlockPos pos) {
        debarkedLogs.add(pos);
    }
    
    // ADDED: Record scouring evidence  
    public void addScouringEvidence(BlockPos pos, ScouringLevel level) {
        scouringEvidence.put(pos, level);
    }
    
    // ENHANCED: Calculate maximum damage intensity including debarking/scouring
    public float getMaxDamageIntensity() {
        float maxFromBlocks = damageRecords.values().stream()
                .map(record -> record.blockStrength)
                .max(Float::compareTo)
                .orElse(0.0f);
        
        // ADDED: Check debarking evidence (minimum 140 mph)
        float maxFromDebarking = debarkedLogs.isEmpty() ? 0.0f : 140.0f;
        
        // ADDED: Check scouring evidence
        float maxFromScouring = scouringEvidence.values().stream()
                .map(level -> level.minimumWindspeed)
                .max(Float::compareTo)
                .orElse(0.0f);
        
        return Math.max(maxFromBlocks, Math.max(maxFromDebarking, maxFromScouring));
    }
    
    // FIXED: Get enhanced windspeed estimate using evidence-based minimums
/**
 * FIXED: Get enhanced windspeed estimate using evidence-based minimums properly
 */
public float getEnhancedWindspeedEstimate() {
    // Start with the highest block strength found
    float maxBlockStrength = 0.0f;
    float maxTornadoWindspeed = 0.0f;
    
    for (DamageRecord record : damageRecords.values()) {
        maxBlockStrength = Math.max(maxBlockStrength, record.blockStrength);
        maxTornadoWindspeed = Math.max(maxTornadoWindspeed, record.tornadoWindspeed);
    }
    
    // FIXED: Start with the actual recorded tornado windspeed as baseline
    float estimatedWindspeed = Math.max(maxBlockStrength, maxTornadoWindspeed);
    
    // FIXED: Evidence establishes minimum thresholds, not additive bonuses
    float minimumFromEvidence = estimatedWindspeed;
    
    // FIXED: Debarking evidence establishes absolute minimum of 140 mph
    if (!debarkedLogs.isEmpty()) {
        minimumFromEvidence = Math.max(minimumFromEvidence, 140.0f);
        EASAddon.LOGGER.debug("Chunk {}: Debarking evidence found - minimum windspeed now 140mph", chunkPos);
    }
    
    // FIXED: Scouring evidence establishes minimum based on highest level found
    for (ScouringLevel level : scouringEvidence.values()) {
        minimumFromEvidence = Math.max(minimumFromEvidence, level.minimumWindspeed);
        EASAddon.LOGGER.debug("Chunk {}: {} scouring evidence - minimum windspeed now {}mph", 
            chunkPos, level.name(), level.minimumWindspeed);
    }
    
    // FIXED: Multiple evidence types increase confidence, not windspeed
    int evidenceTypeCount = 0;
    if (!damageRecords.isEmpty()) evidenceTypeCount++;
    if (!debarkedLogs.isEmpty()) evidenceTypeCount++;
    if (!scouringEvidence.isEmpty()) evidenceTypeCount++;
    
    // Small confidence boost (max 10%) for multiple evidence types
    if (evidenceTypeCount >= 2) {
        float confidenceBoost = 1.0f + (evidenceTypeCount - 1) * 0.05f; // 5% per additional evidence type
        minimumFromEvidence *= confidenceBoost;
        EASAddon.LOGGER.debug("Chunk {}: Multiple evidence types ({}) - confidence boost: {:.1f}%", 
            chunkPos, evidenceTypeCount, (confidenceBoost - 1.0f) * 100);
    }
    
    EASAddon.LOGGER.info("Chunk {}: Enhanced windspeed calculation: baseline={}mph, evidence_minimum={}mph, final={}mph", 
        chunkPos, Math.round(estimatedWindspeed), Math.round(minimumFromEvidence), Math.round(minimumFromEvidence));
    
    return minimumFromEvidence;
}

/**
 * FIXED: Get minimum EF rating from evidence with proper thresholds
 */
public int getMinimumEFRatingFromEvidence() {
    int minimumRating = -1;
    
    // Debarking evidence = minimum EF3 (140+ mph)
    if (!debarkedLogs.isEmpty()) {
        minimumRating = Math.max(minimumRating, 3);
        EASAddon.LOGGER.debug("Chunk {}: Debarking evidence establishes minimum EF3", chunkPos);
    }
    
    // Scouring evidence establishes minimums
    for (ScouringLevel level : scouringEvidence.values()) {
        int ratingFromScouring = switch (level) {
            case GRASS_TO_DIRT -> 3;      // 140+ mph = EF3+
            case DIRT_TO_MEDIUM -> 4;     // 170+ mph = EF4+  
            case MEDIUM_TO_HEAVY -> 5;    // 200+ mph = EF5
        };
        minimumRating = Math.max(minimumRating, ratingFromScouring);
        EASAddon.LOGGER.debug("Chunk {}: {} scouring establishes minimum EF{}", 
            chunkPos, level.name(), ratingFromScouring);
    }
    
    return minimumRating;
}

/**
 * FIXED: Check if chunk has high confidence evidence (multiple types)
 */
public boolean hasHighConfidenceEvidence() {
    int evidenceTypes = 0;
    if (!damageRecords.isEmpty()) evidenceTypes++;
    if (!debarkedLogs.isEmpty()) evidenceTypes++;
    if (!scouringEvidence.isEmpty()) evidenceTypes++;
    
    return evidenceTypes >= 2;
}

/**
 * FIXED: Get evidence summary with proper details
 */
public String getEvidenceSummary() {
    StringBuilder summary = new StringBuilder();
    
    if (!damageRecords.isEmpty()) {
        float maxStrength = (float) damageRecords.values().stream()
            .mapToDouble(r -> r.blockStrength)
            .max().orElse(0.0);
        summary.append(String.format("Block damage (%.0f mph)", maxStrength));
    }
    
    if (!debarkedLogs.isEmpty()) {
        if (summary.length() > 0) summary.append(", ");
        summary.append(String.format("Debarking (%d trees)", debarkedLogs.size()));
    }
    
    if (!scouringEvidence.isEmpty()) {
        if (summary.length() > 0) summary.append(", ");
        
        Map<ScouringLevel, Long> counts = scouringEvidence.values().stream()
            .collect(Collectors.groupingBy(level -> level, Collectors.counting()));
        
        for (Map.Entry<ScouringLevel, Long> entry : counts.entrySet()) {
            String levelName = switch (entry.getKey()) {
                case GRASS_TO_DIRT -> "Light scouring";
                case DIRT_TO_MEDIUM -> "Medium scouring";
                case MEDIUM_TO_HEAVY -> "Heavy scouring";
            };
            summary.append(String.format("%s (%d)", levelName, entry.getValue()));
        }
    }
    
    if (summary.length() == 0) {
        summary.append("No evidence found");
    }
    
    return summary.toString();
}

/**
 * FIXED: Get average tornado windspeed from damage records
 */
public float getAverageTornadoWindspeed() {
    if (damageRecords.isEmpty()) return 0.0f;
    
    return (float) damageRecords.values().stream()
        .mapToInt(record -> record.tornadoWindspeed)
        .average()
        .orElse(0.0);
}
    
    // ADDED: Get evidence summary for detailed reporting
 
    
    // ADDED: Get minimum EF rating based on evidence types

    
    // ADDED: Check if chunk has high-confidence evidence

    
    public void markSurveyed(String playerName, int efRating, float maxWindspeed) {
        this.surveyed = true;
        this.surveyedBy = playerName;
        this.surveyTime = System.currentTimeMillis();
        this.determinedEFRating = efRating;
        this.maxWindspeedFound = maxWindspeed;
    }
    

    
    public int getDamageCount() {
        return damageRecords.size() + debarkedLogs.size() + scouringEvidence.size();
    }
    
    public boolean hasDamage() {
        return !damageRecords.isEmpty() || !debarkedLogs.isEmpty() || !scouringEvidence.isEmpty();
    }
    
    // Getters for new fields
    public Set<BlockPos> getDebarkedLogs() { return new HashSet<>(debarkedLogs); }
    public Map<BlockPos, ScouringLevel> getScouringEvidence() { return new HashMap<>(scouringEvidence); }
    
    // Existing getters
    public ChunkPos getChunkPos() { return chunkPos; }
    public boolean isSurveyed() { return surveyed; }
    public String getSurveyedBy() { return surveyedBy; }
    public long getSurveyTime() { return surveyTime; }
    public int getDeterminedEFRating() { return determinedEFRating; }
    public float getMaxWindspeedFound() { return maxWindspeedFound; }
    public Map<BlockPos, DamageRecord> getDamageRecords() { return new HashMap<>(damageRecords); }
    
    // ENHANCED: NBT serialization with new evidence types
    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("chunkX", chunkPos.x);
        tag.putInt("chunkZ", chunkPos.z);
        tag.putBoolean("surveyed", surveyed);
        if (surveyedBy != null) {
            tag.putString("surveyedBy", surveyedBy);
        }
        tag.putLong("surveyTime", surveyTime);
        tag.putInt("efRating", determinedEFRating);
        tag.putFloat("maxWindspeed", maxWindspeedFound);
        
        // Serialize damage records
        ListTag damageList = new ListTag();
        for (Map.Entry<BlockPos, DamageRecord> entry : damageRecords.entrySet()) {
            CompoundTag damageTag = new CompoundTag();
            BlockPos pos = entry.getKey();
            DamageRecord record = entry.getValue();
            
            damageTag.putInt("x", pos.getX());
            damageTag.putInt("y", pos.getY());
            damageTag.putInt("z", pos.getZ());
            damageTag.putString("originalBlock", record.originalBlock.getBlock().getDescriptionId());
            if (record.resultingBlock != null) {
                damageTag.putString("resultingBlock", record.resultingBlock.getBlock().getDescriptionId());
            }
            damageTag.putFloat("blockStrength", record.blockStrength);
            damageTag.putLong("timestamp", record.timestamp);
            damageTag.putInt("tornadoWindspeed", record.tornadoWindspeed);
            
            damageList.add(damageTag);
        }
        tag.put("damageRecords", damageList);
        
        // ADDED: Serialize debarking evidence
        ListTag debarkList = new ListTag();
        for (BlockPos pos : debarkedLogs) {
            CompoundTag debarkTag = new CompoundTag();
            debarkTag.putInt("x", pos.getX());
            debarkTag.putInt("y", pos.getY());
            debarkTag.putInt("z", pos.getZ());
            debarkList.add(debarkTag);
        }
        tag.put("debarkedLogs", debarkList);
        
        // ADDED: Serialize scouring evidence
        ListTag scourList = new ListTag();
        for (Map.Entry<BlockPos, ScouringLevel> entry : scouringEvidence.entrySet()) {
            CompoundTag scourTag = new CompoundTag();
            BlockPos pos = entry.getKey();
            scourTag.putInt("x", pos.getX());
            scourTag.putInt("y", pos.getY());
            scourTag.putInt("z", pos.getZ());
            scourTag.putString("level", entry.getValue().name());
            scourList.add(scourTag);
        }
        tag.put("scouringEvidence", scourList);
        
        return tag;
    }
    
    public static ChunkDamageData fromNBT(CompoundTag tag) {
        ChunkPos chunkPos = new ChunkPos(tag.getInt("chunkX"), tag.getInt("chunkZ"));
        ChunkDamageData data = new ChunkDamageData(chunkPos);
        
        data.surveyed = tag.getBoolean("surveyed");
        if (tag.contains("surveyedBy")) {
            data.surveyedBy = tag.getString("surveyedBy");
        }
        data.surveyTime = tag.getLong("surveyTime");
        data.determinedEFRating = tag.getInt("efRating");
        data.maxWindspeedFound = tag.getFloat("maxWindspeed");
        
        // Load damage records
        if (tag.contains("damageRecords", Tag.TAG_LIST)) {
            ListTag damageList = tag.getList("damageRecords", Tag.TAG_COMPOUND);
            for (int i = 0; i < damageList.size(); i++) {
                CompoundTag damageTag = damageList.getCompound(i);
                // Simplified loading - could be enhanced to restore full BlockStates
            }
        }
        
        // ADDED: Load debarking evidence
        if (tag.contains("debarkedLogs", Tag.TAG_LIST)) {
            ListTag debarkList = tag.getList("debarkedLogs", Tag.TAG_COMPOUND);
            for (int i = 0; i < debarkList.size(); i++) {
                CompoundTag debarkTag = debarkList.getCompound(i);
                BlockPos pos = new BlockPos(
                    debarkTag.getInt("x"),
                    debarkTag.getInt("y"),
                    debarkTag.getInt("z")
                );
                data.debarkedLogs.add(pos);
            }
        }
        
        // ADDED: Load scouring evidence
        if (tag.contains("scouringEvidence", Tag.TAG_LIST)) {
            ListTag scourList = tag.getList("scouringEvidence", Tag.TAG_COMPOUND);
            for (int i = 0; i < scourList.size(); i++) {
                CompoundTag scourTag = scourList.getCompound(i);
                BlockPos pos = new BlockPos(
                    scourTag.getInt("x"),
                    scourTag.getInt("y"),
                    scourTag.getInt("z")
                );
                try {
                    ScouringLevel level = ScouringLevel.valueOf(scourTag.getString("level"));
                    data.scouringEvidence.put(pos, level);
                } catch (IllegalArgumentException e) {
                    // Skip invalid scouring levels
                }
            }
        }
        
        return data;
    }
}