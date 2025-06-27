package com.burrows.easaddon;

public class AlertPolygon {
    public final long stormId;
    public final double centerX; // Normalized coordinates (-1 to 1)
    public final double centerZ; // Normalized coordinates (-1 to 1)
    public final float halfWidth;
    public final float halfHeight;
    public final float rotationDeg;
    public final int level; // Alert level (1-3)
    public final int stormType;
    public final int stormStage;
    
    public AlertPolygon(long stormId, double centerX, double centerZ, 
                       float halfWidth, float halfHeight, float rotationDeg, int level, int stormType, int stormStage) {
        this.stormId = stormId;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.halfWidth = halfWidth;
        this.halfHeight = halfHeight;
        this.rotationDeg = rotationDeg;
        this.level = level;
		this.stormType = stormType;
		this.stormStage = stormStage;
    }
}