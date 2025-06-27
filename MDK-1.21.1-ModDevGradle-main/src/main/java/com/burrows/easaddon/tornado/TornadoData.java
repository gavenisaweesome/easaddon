package com.burrows.easaddon.tornado;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import java.util.*;

import com.burrows.easaddon.EASAddon;

public class TornadoData {
    private final long id;
    private boolean active;
    private int maxWindspeed;
    private float maxWidth;
    private String rating;
    private long firstSeenTime;
    private long lastSeenTime;
    private boolean hasRecordedData;
    
    // Position and damage tracking
    private final List<PositionRecord> positionHistory = new ArrayList<>();
    private final Set<ChunkPos> damagedChunks = new HashSet<>();
    private long lastPositionRecordTime = 0;
    private Vec3 lastKnownPosition;
    
    // Survey results
    private boolean surveyed = false;
    private String surveyedBy = null;
    private long surveyTime = 0;
    private int surveyedEFRating = -1; // -1 = not surveyed, 0-5 = EF0-EF5
    private float surveyedMaxWindspeed = 0.0f;
    
    public static class PositionRecord {
        public final Vec3 position;
        public final long timestamp;
        public final int windspeed;
        public final float width;
        
        public PositionRecord(Vec3 position, long timestamp, int windspeed, float width) {
            this.position = position;
            this.timestamp = timestamp;
            this.windspeed = windspeed;
            this.width = width;
        }
    }
    
    public TornadoData(long id) {
        this.id = id;
        this.active = true;
        this.maxWindspeed = 0;
        this.maxWidth = 0.0f;
        this.rating = "EFU"; // FIXED: Always start as EFU
        this.firstSeenTime = System.currentTimeMillis();
        this.lastSeenTime = this.firstSeenTime;
        this.hasRecordedData = false;
    }
    
    public void updateData(int currentWindspeed, float currentWidth, int stage, Vec3 position) {
        this.lastSeenTime = System.currentTimeMillis();
        this.lastKnownPosition = position;
        
        // FIXED: Handle roping out properly - don't record bogus width values when windspeed is 0
        boolean isRopingOut = (currentWindspeed <= 0 && currentWidth > 0);
        
        // Track position every 10 seconds
        if (System.currentTimeMillis() - lastPositionRecordTime >= 10000) {
            if (isRopingOut) {
                // When roping out, use the historical max width instead of current inflated width
                // This prevents the 1/10th max size display issue
                float historicalWidth = Math.min(this.maxWidth, currentWidth);
                positionHistory.add(new PositionRecord(position, System.currentTimeMillis(), 0, historicalWidth));
                EASAddon.LOGGER.debug("Tornado {} roping out - using historical width {:.1f} instead of current {:.1f}", 
                                    id, historicalWidth, currentWidth);
            } else {
                // Normal tracking with actual values
                positionHistory.add(new PositionRecord(position, System.currentTimeMillis(), currentWindspeed, currentWidth));
            }
            lastPositionRecordTime = System.currentTimeMillis();
        }
        
        // FIXED: Only update max values if we have meaningful data (not roping out)
        if (!isRopingOut) {
            // Track the maximum windspeed ever recorded for this tornado
            if (!hasRecordedData || currentWindspeed > this.maxWindspeed) {
                this.maxWindspeed = currentWindspeed;
            }
            
            // Track the maximum width ever recorded for this tornado
            if (!hasRecordedData || currentWidth > this.maxWidth) {
                this.maxWidth = currentWidth;
            }
            
            // Mark that we've recorded at least one data point
            this.hasRecordedData = true;
        } else {
            // When roping out, we still mark as having recorded data but don't update maximums
            this.hasRecordedData = true;
            
            // Log the roping out process
            EASAddon.LOGGER.debug("Tornado {} roping out: windspeed={}mph, reported_width={:.1f}, max_width={:.1f}", 
                                id, currentWindspeed, currentWidth, this.maxWidth);
        }
        
        // FIXED: Do NOT auto-update rating - keep as EFU until surveyed
        // Rating should only be changed through setSurveyResults()
    }

    /**
     * FIXED: Enhanced inactive marking that handles roping out properly
     */
    public void markInactive() {
        this.active = false;
        
        // Add final position if we have one and it's been a while since last record
        if (lastKnownPosition != null && (positionHistory.isEmpty() || 
            System.currentTimeMillis() - positionHistory.get(positionHistory.size() - 1).timestamp > 5000)) {
            
            // Use historical values for the final position record, not zeros
            int finalWindspeed = 0; // Tornado has dissipated
            float finalWidth = this.maxWidth * 0.1f; // Small remnant width
            
            positionHistory.add(new PositionRecord(lastKnownPosition, System.currentTimeMillis(), finalWindspeed, finalWidth));
            
            EASAddon.LOGGER.info("Tornado {} marked inactive - final position recorded with width {:.1f}", 
                               id, finalWidth);
        }
    }
    
    public void addDamagedChunk(ChunkPos chunkPos) {
        damagedChunks.add(chunkPos);
    }
    
    // REMOVED: updateRating() method - no more auto-rating
    
   
    
    // ========== SURVEY RESULT METHODS ==========
    
    /**
     * Set survey results when a player completes damage survey
     */
    public void setSurveyResults(String surveyorName, int efRating, float maxWindspeed) {
        this.surveyed = true;
        this.surveyedBy = surveyorName;
        this.surveyTime = System.currentTimeMillis();
        this.surveyedEFRating = efRating;
        this.surveyedMaxWindspeed = maxWindspeed;
        
        // FIXED: Only update the displayed rating when surveyed
        if (efRating >= 0 && efRating <= 5) {
            this.rating = "EF" + efRating;
        } else {
            this.rating = "EFU"; // Invalid rating defaults to EFU
        }
        
        // Update max windspeed if survey found higher value
        if (maxWindspeed > this.maxWindspeed) {
            this.maxWindspeed = Math.round(maxWindspeed);
        }
    }
    
    // ========== BASIC SETTERS (for loading from NBT/JSON) ==========
    
    public void setActive(boolean active) { 
        this.active = active; 
    }

    public void setMaxWindspeed(int maxWindspeed) { 
        this.maxWindspeed = maxWindspeed; 
    }

    public void setMaxWidth(float maxWidth) { 
        this.maxWidth = maxWidth; 
    }

    public void setRating(String rating) { 
        // FIXED: Only allow setting rating if it's a survey result or EFU
        if (rating.equals("EFU") || this.surveyed) {
            this.rating = rating; 
        } else {
            this.rating = "EFU"; // Force EFU if not surveyed
        }
    }

    public void setFirstSeenTime(long firstSeenTime) { 
        this.firstSeenTime = firstSeenTime; 
    }

    public void setLastSeenTime(long lastSeenTime) { 
        this.lastSeenTime = lastSeenTime; 
    }

    public void setHasRecordedData(boolean hasRecordedData) { 
        this.hasRecordedData = hasRecordedData; 
    }
    
    // ========== SURVEY SETTERS (for loading from NBT/JSON) ==========
    
    public void setSurveyed(boolean surveyed) { 
        this.surveyed = surveyed; 
    }

    public void setSurveyedBy(String surveyedBy) { 
        this.surveyedBy = surveyedBy; 
    }

    public void setSurveyTime(long surveyTime) { 
        this.surveyTime = surveyTime; 
    }

    public void setSurveyedEFRating(int surveyedEFRating) { 
        this.surveyedEFRating = surveyedEFRating; 
    }

    public void setSurveyedMaxWindspeed(float surveyedMaxWindspeed) { 
        this.surveyedMaxWindspeed = surveyedMaxWindspeed; 
    }

    // ========== POSITION RECORD METHODS ==========
    
    /**
     * Method to add position records (for loading from NBT/JSON)
     */
    public void addPositionRecord(PositionRecord record) {
        positionHistory.add(record);
    }
    
    /**
     * Clear position history
     */
    public void clearPositionHistory() {
        positionHistory.clear();
    }
    
    // ========== BASIC GETTERS ==========
    
    public long getId() { 
        return id; 
    }
    
    public boolean isActive() { 
        return active; 
    }
    
    /**
     * FIXED: Return surveyed windspeed when available, otherwise return raw windspeed
     */
    public int getMaxWindspeed() { 
        // If tornado has been surveyed, return the surveyed windspeed (more accurate)
        if (surveyed && surveyedMaxWindspeed > 0) {
            return Math.round(surveyedMaxWindspeed);
        }
        // Otherwise return the raw tornado windspeed
        return maxWindspeed; 
    }
    
    /**
     * Get the raw tornado windspeed (before survey correction)
     */
    public int getRawMaxWindspeed() {
        return maxWindspeed;
    }
    
    public float getMaxWidth() { 
        return maxWidth; 
    }
    
    public String getRating() { 
        return rating; 
    }
    
    public long getFirstSeenTime() { 
        return firstSeenTime; 
    }
    
    public long getLastSeenTime() { 
        return lastSeenTime; 
    }
    
    public boolean hasRecordedData() { 
        return hasRecordedData; 
    }
    
    public List<PositionRecord> getPositionHistory() { 
        return new ArrayList<>(positionHistory); 
    }
    
    public Set<ChunkPos> getDamagedChunks() { 
        return new HashSet<>(damagedChunks); 
    }
    
    // ========== SURVEY GETTERS ==========
    
    public boolean isSurveyed() { 
        return surveyed; 
    }

    public String getSurveyedBy() { 
        return surveyedBy; 
    }

    public long getSurveyTime() { 
        return surveyTime; 
    }

    public int getSurveyedEFRating() { 
        return surveyedEFRating; 
    }

    public float getSurveyedMaxWindspeed() { 
        return surveyedMaxWindspeed; 
    }
    
    // ========== UTILITY METHODS ==========
    
    public double getTotalPathLength() {
        double totalLength = 0;
        for (int i = 1; i < positionHistory.size(); i++) {
            Vec3 prev = positionHistory.get(i - 1).position;
            Vec3 curr = positionHistory.get(i).position;
            totalLength += prev.distanceTo(curr);
        }
        return totalLength;
    }
}