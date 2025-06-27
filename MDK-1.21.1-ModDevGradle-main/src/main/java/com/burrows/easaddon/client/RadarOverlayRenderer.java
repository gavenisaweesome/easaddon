package com.burrows.easaddon.client;

import com.burrows.easaddon.AlertPolygon;
import com.burrows.easaddon.AlertPolygonManager;
import com.burrows.easaddon.RadarOverlayBlockEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.slf4j.Logger;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.stream.Collectors;

public class RadarOverlayRenderer implements BlockEntityRenderer<RadarOverlayBlockEntity> {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // Cache for computed polygon vertices to avoid recalculation every frame
    private static final Map<Long, CachedPolygonData> polygonCache = new ConcurrentHashMap<>();
    
    // Performance optimization: Only log once per polygon creation/update
    private static final Map<Long, Boolean> loggedPolygons = new ConcurrentHashMap<>();
    
    // FIXED CONSTANTS - Proper scaling for radar display
    private static final float RADAR_DISPLAY_SIZE = 1.0f; // The overlay block covers 1x1 block area
    private static final float RENDER_HEIGHT = 0.05f; // Reduced height - just slightly above block surface
    private static final float ROTATION_CORRECTION = 180.0f;
    private static final float MIN_POLYGON_SIZE = 0.2f; // Minimum visible size
    private static final float LINE_WIDTH = 0.015f; // Width of the outline lines

    public RadarOverlayRenderer(BlockEntityRendererProvider.Context ctx) {}

    private static class CachedPolygonData {
        final Vector3f[] corners;
        final float r, g, b, a;
        final long lastUpdateTime;
        
        CachedPolygonData(Vector3f[] corners, float r, float g, float b, float a) {
            this.corners = corners;
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
            this.lastUpdateTime = System.currentTimeMillis();
        }
    }

 // Add this import to your existing imports at the top of the file:


 @Override
 public void render(RadarOverlayBlockEntity be,
                    float partialTicks,
                    PoseStack poseStack,
                    MultiBufferSource buffers,
                    int packedLight,
                    int packedOverlay) {
     
     BlockPos pos = be.getBlockPos();
     
     // FIXED: Get the original polygons collection
     Collection<AlertPolygon> originalPolys = AlertPolygonManager.getPolygonsAt(pos);
     
     // FIXED: Clear cache immediately when no polygons are present
     if (originalPolys.isEmpty()) {
         clearAllCaches(); // Clear all cached data for this position
         return; // Early exit - no polygons to render
     }
     
     // Create a snapshot copy to avoid concurrent modification during iteration
     List<AlertPolygon> polys;
     try {
         polys = new ArrayList<>(originalPolys);
     } catch (Exception e) {
         // If we can't safely copy the collection, skip this render frame
         LOGGER.warn("RadarOverlayRenderer: Failed to copy polygon collection, skipping render: {}", e.getMessage());
         return;
     }
     
     if (polys.isEmpty()) {
         clearAllCaches(); // Clear cache if copy ended up empty too
         return; // Early exit after copying - collection was cleared
     }

     // FIXED: Track which polygons are currently active for this render
     Set<Long> activeStormIds = polys.stream()
         .map(p -> p.stormId)
         .collect(Collectors.toSet());
     
  // FIXED: Remove cached data for storms that are no longer active
     int removedCacheEntries = 0;
     for (Long stormId : new ArrayList<>(polygonCache.keySet())) {
         if (!activeStormIds.contains(stormId)) {
             polygonCache.remove(stormId);
             polygonDataHash.remove(stormId);
             loggedPolygons.remove(stormId);
             removedCacheEntries++;
         }
     }
     if (removedCacheEntries > 0) {
         LOGGER.info("RadarOverlayRenderer: Removed {} cached storm entries that are no longer active", removedCacheEntries);
     }

     // Debug logging for first polygon
     if (!polys.isEmpty()) {
         AlertPolygon firstPoly = polys.get(0);
     }

     // ─── BATCH SETUP - Only once per render call ─────────────────────
     Matrix4fStack mvStack = RenderSystem.getModelViewStack();
     mvStack.pushMatrix();
     mvStack.mul(poseStack.last().pose());
     
     // FIXED: Position the rendering origin at the bottom corner of the block
     // This makes (0,0) correspond to the bottom-left of the radar display
     // and (1,1) correspond to the top-right
     mvStack.translate(0.0f, 0.05f, 0.0f);
     RenderSystem.applyModelViewMatrix();
     
     RenderSystem.enableBlend();
     RenderSystem.defaultBlendFunc();
     RenderSystem.setShader(GameRenderer::getPositionColorShader);
     RenderSystem.enableDepthTest();
  // In the render method, after RenderSystem.enableDepthTest();
     RenderSystem.depthFunc(GL11.GL_LEQUAL); // Allow rendering at same depth
     RenderSystem.depthMask(false); // FIXED: Disable depth writing for better visibility
     RenderSystem.disableCull(); // FIXED: Disable face culling so lines are visible from both sides

     
     // ─── BATCH RENDER ALL POLYGON OUTLINES ───────────────────────────
     Tesselator tess = Tesselator.getInstance();
     BufferBuilder buf = tess.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
     
     // FIXED: Iterate over the safe copy instead of the original collection
     for (AlertPolygon poly : polys) {
         try {
             CachedPolygonData cached = getCachedPolygonData(poly);
             renderPolygonOutline(buf, cached.corners, cached.r, cached.g, cached.b, cached.a);
         } catch (Exception e) {
             // Log but don't crash if individual polygon processing fails
             LOGGER.warn("RadarOverlayRenderer: Failed to render polygon {}: {}", poly.stormId, e.getMessage());
         }
     }
     
     // Render all outlines in one draw call
     MeshData mesh = buf.build();
     if (mesh != null) {
         BufferUploader.drawWithShader(mesh);
     }
     
     // ─── CLEANUP ─────────────────────────────────────────────────────
     RenderSystem.enableCull(); // Re-enable face culling
     RenderSystem.disableBlend();
     RenderSystem.depthMask(true);
     mvStack.popMatrix();
     RenderSystem.applyModelViewMatrix();
     
     // Clean up old cache entries periodically (every ~5 seconds)
     if (System.currentTimeMillis() % 5000 < 50) {
         cleanupCache();
     }
 }

 // FIXED: Add method to clear all caches
//FIXED: Add method to clear all caches
private void clearAllCaches() {
  polygonCache.clear();
  polygonDataHash.clear();
  loggedPolygons.clear();
}
    /**
     * Renders a complete polygon outline as a rectangle with proper corners
     * @param buf BufferBuilder to add vertices to
     * @param corners Polygon corner vertices (4 points)
     * @param r Red color component
     * @param g Green color component
     * @param b Blue color component
     * @param a Alpha component
     */
private void renderPolygonOutline(BufferBuilder buf, Vector3f[] corners, float r, float g, float b, float a) {
    // Render each edge as a thick line, extending slightly past corners for complete rectangle
    for (int i = 0; i < 4; i++) {
        Vector3f start = corners[i];
        Vector3f end = corners[(i + 1) % 4];
        
        // Calculate direction vector
        float dx = end.x - start.x;
        float dz = end.z - start.z;
        float length = (float) Math.sqrt(dx * dx + dz * dz);
        
        if (length < 1e-5) continue; // Skip degenerate edges
        
        // Calculate normalized direction and perpendicular vector
        float invLength = 1.0f / length;
        float dirX = dx * invLength;
        float dirZ = dz * invLength;
        float perpX = -dirZ * LINE_WIDTH;
        float perpZ = dirX * LINE_WIDTH;
        
        // Extend the line slightly past the corners to ensure complete rectangle
        float extension = LINE_WIDTH * 0.5f;
        Vector3f extendedStart = new Vector3f(
            start.x - dirX * extension,
            start.y,
            start.z - dirZ * extension
        );
        Vector3f extendedEnd = new Vector3f(
            end.x + dirX * extension,
            end.y,
            end.z + dirZ * extension
        );
        
        // Calculate quad vertices for thick line (using extended points)
        Vector3f topLeft = new Vector3f(
            extendedStart.x + perpX,
            extendedStart.y,
            extendedStart.z + perpZ
        );
        
        Vector3f bottomLeft = new Vector3f(
            extendedStart.x - perpX,
            extendedStart.y,
            extendedStart.z - perpZ
        );
        
        Vector3f bottomRight = new Vector3f(
            extendedEnd.x - perpX,
            extendedEnd.y,
            extendedEnd.z - perpZ
        );
        
        Vector3f topRight = new Vector3f(
            extendedEnd.x + perpX,
            extendedEnd.y,
            extendedEnd.z + perpZ
        );
        
        // FIXED: Render the quad twice with opposite winding orders for true double-sided rendering
        
        // First quad - counter-clockwise when viewed from above (original order)
        buf.addVertex(topLeft.x, topLeft.y, topLeft.z).setColor(r, g, b, a);
        buf.addVertex(topRight.x, topRight.y, topRight.z).setColor(r, g, b, a);
        buf.addVertex(bottomRight.x, bottomRight.y, bottomRight.z).setColor(r, g, b, a);
        buf.addVertex(bottomLeft.x, bottomLeft.y, bottomLeft.z).setColor(r, g, b, a);
        
        // Second quad - clockwise when viewed from above (reverse order for viewing from below)
        buf.addVertex(topLeft.x, topLeft.y, topLeft.z).setColor(r, g, b, a);
        buf.addVertex(bottomLeft.x, bottomLeft.y, bottomLeft.z).setColor(r, g, b, a);
        buf.addVertex(bottomRight.x, bottomRight.y, bottomRight.z).setColor(r, g, b, a);
        buf.addVertex(topRight.x, topRight.y, topRight.z).setColor(r, g, b, a);
    }
}
    
    private CachedPolygonData getCachedPolygonData(AlertPolygon poly) {
        // Create a cache key that includes all relevant polygon properties
        long cacheKey = poly.stormId;
        
        CachedPolygonData cached = polygonCache.get(cacheKey);
        
        // Check if we need to recalculate (cache miss or data changed)
        if (cached == null || needsRecalculation(poly, cached)) {
            cached = calculatePolygonData(poly);
            polygonCache.put(cacheKey, cached);
            
            // Log only once per polygon for debugging
            if (!loggedPolygons.getOrDefault(cacheKey, false)) {
                LOGGER.info("RadarOverlayRenderer: Cached new polygon data for storm {} at display coords ({}, {})", 
                           poly.stormId, poly.centerX, poly.centerZ);
                loggedPolygons.put(cacheKey, true);
            }
        }
        
        return cached;
    }
    
    // Replace the needsRecalculation method and add a simple cache key:

    private static final Map<Long, String> polygonDataHash = new ConcurrentHashMap<>();

    private boolean needsRecalculation(AlertPolygon poly, CachedPolygonData cached) {
        // Include stormStage in the hash calculation
        String currentHash = String.format("%.3f,%.3f,%.3f,%.3f,%.1f,%d,%d,%d", 
            poly.centerX, poly.centerZ, poly.halfWidth, poly.halfHeight, 
            poly.rotationDeg, poly.level, poly.stormType, poly.stormStage); // Added stormStage
        
        String lastHash = polygonDataHash.get(poly.stormId);
        
        if (!currentHash.equals(lastHash)) {
            polygonDataHash.put(poly.stormId, currentHash);
            return true;
        }
        
        // Also recalc if too much time has passed (fallback)
        return System.currentTimeMillis() - cached.lastUpdateTime > 5000;
    }
    
private CachedPolygonData calculatePolygonData(AlertPolygon poly) {
    // FIXED: Direct mapping from normalized coordinates (0.0-1.0) to block space (0.0-1.0)
    // The polygon coordinates are already in the correct range for the radar display
    float worldX = (float)(poly.centerX * RADAR_DISPLAY_SIZE);
    float worldZ = (float)(poly.centerZ * RADAR_DISPLAY_SIZE);
    
    // FIXED: Scale the polygon size with different ratios for width and height to create rectangles
    // Make storms wider than they are tall for more realistic storm representation
    float baseWidth = Math.max((float)(poly.halfWidth * RADAR_DISPLAY_SIZE), MIN_POLYGON_SIZE);
    float baseHeight = Math.max((float)(poly.halfHeight * RADAR_DISPLAY_SIZE), MIN_POLYGON_SIZE);
    
    // Apply aspect ratio scaling - make storms 1.5x wider than tall
    float baseHw = baseWidth * 1.5f;  // Base width multiplier for rectangular shape
    float hh = baseHeight * 0.75f; // Height multiplier for rectangular shape
    
    LOGGER.info("RadarOverlayRenderer: Storm {} - Input coords: ({}, {}), World coords: ({}, {}), Base Size: {}x{}, Final Size: {}x{}", 
               poly.stormId, poly.centerX, poly.centerZ, worldX, worldZ, 
               baseWidth * 2, baseHeight * 2, baseHw * 2, hh * 2);
    
    // Calculate rotation based on tornado direction
    // Assuming poly.rotationDeg represents the direction the tornado is moving
    float correctedRotation = poly.rotationDeg + ROTATION_CORRECTION;
    double radians = Math.toRadians(correctedRotation);
    float cos = (float)Math.cos(radians);
    float sin = (float)Math.sin(radians);
    
    // Calculate the four corners of the polygon
    Vector3f[] corners = new Vector3f[4];
    
    // Check storm type to determine shape
    if (poly.stormType == 0) {
        // Trapezoid shape for supercell/tornado storms (stormType = 0)
    	
        float hh2 = baseHeight * 1.5f; 
        
      
        float trapezoidal_factor = 0.36f; 
 
        float front_width_multiplier = 1.0f - trapezoidal_factor; // Front (direction side) width
        float back_width_multiplier = 1.0f + trapezoidal_factor;  // Back (opposite direction) width
        
        // Calculate trapezoid corners - varying the width for front vs back
        float front_hw = baseWidth * 0.75f * front_width_multiplier;
        float back_hw = baseWidth * 0.75f * back_width_multiplier;
        
        // Front edge (direction of movement) - narrower width
        corners[0] = new Vector3f(-front_hw * cos - (-hh2) * sin + worldX, RENDER_HEIGHT, -front_hw * sin + (-hh2) * cos + worldZ); // Front-left
        corners[1] = new Vector3f(front_hw * cos - (-hh2) * sin + worldX, RENDER_HEIGHT, front_hw * sin + (-hh2) * cos + worldZ);   // Front-right
        
        // Back edge (opposite to direction) - wider width
        corners[2] = new Vector3f(back_hw * cos - hh2 * sin + worldX, RENDER_HEIGHT, back_hw * sin + hh2 * cos + worldZ);         // Back-right
        corners[3] = new Vector3f(-back_hw * cos - hh2 * sin + worldX, RENDER_HEIGHT, -back_hw * sin + hh2 * cos + worldZ);       // Back-left
        
        LOGGER.info("RadarOverlayRenderer: Storm {} - Trapezoid shape: Front width: {}, Back width: {}, Length: {}", 
                   poly.stormId, front_hw * 2, back_hw * 2, hh2 * 2);
    } else {
        // Regular rectangle for other storm types (stormType = 1, etc.)
    	float correctedRotation1 = poly.rotationDeg + 90.0f;
        double radians1 = Math.toRadians(correctedRotation1);
        float cos1 = (float)Math.cos(radians1);
        float sin1 = (float)Math.sin(radians1);
        float hw = baseHw; // Use base width for regular rectangle
        float hh2 = baseHeight * 1.5f; 
        corners[0] = new Vector3f(-hw * cos1 - (-hh2) * sin1 + worldX, RENDER_HEIGHT, -hw * sin1 + (-hh2) * cos1 + worldZ); // Top-left
        corners[1] = new Vector3f(hw * cos1 - (-hh2) * sin1 + worldX, RENDER_HEIGHT, hw * sin1 + (-hh2) * cos1 + worldZ);   // Top-right
        corners[2] = new Vector3f(hw * cos1 - hh2 * sin1 + worldX, RENDER_HEIGHT, hw * sin1 + hh2 * cos1 + worldZ);         // Bottom-right
        corners[3] = new Vector3f(-hw * cos1 - hh2 * sin1 + worldX, RENDER_HEIGHT, -hw * sin1 + hh2 * cos1 + worldZ);       // Bottom-left
        
        LOGGER.info("RadarOverlayRenderer: Storm {} - Rectangle shape: Width: {}, Height: {}", 
                   poly.stormId, hw * 2, hh2 * 2);
    }
    
    LOGGER.info("RadarOverlayRenderer: Storm {} - Final corners: FL({}, {}, {}), FR({}, {}, {}), BR({}, {}, {}), BL({}, {}, {})", 
               poly.stormId,
               corners[0].x, corners[0].y, corners[0].z,
               corners[1].x, corners[1].y, corners[1].z,
               corners[2].x, corners[2].y, corners[2].z,
               corners[3].x, corners[3].y, corners[3].z);
    
    // Calculate color (cached)
    float[] color = calculatePolygonColor(poly);
    
    return new CachedPolygonData(corners, color[0], color[1], color[2], color[3]);
}


    private float[] calculatePolygonColor(AlertPolygon poly) {
        // Validate polygon data
        int validatedStormType = Math.max(0, Math.min(1, poly.stormType));
        int validatedStormStage = Math.max(1, Math.min(3, poly.stormStage));
        int validatedLevel = Math.max(0, Math.min(3, poly.level));
        
        float r, g, b, a = 0.6f;
        
        if (validatedStormType == 0) { // Supercell/Tornado storms
            if (validatedStormStage == 3) {
                // Tornado stages - Red/Purple based on level
                if (validatedLevel >= 3) {
                    r = 0.5f; g = 0.0f; b = 0.5f; // Purple
                } else if (validatedLevel >= 2) {
                    r = 0.8f; g = 0.0f; b = 0.0f; // Dark Red
                } else {
                    r = 1.0f; g = 0.0f; b = 0.0f; // Red
                }
            } else if (validatedStormStage <= 2) {
                // Supercell stages - Yellow
                r = 1.0f; g = 1.0f; b = 0.0f;
            } else {
                // Fallback to yellow for unexpected stages
                r = 1.0f; g = 1.0f; b = 0.0f; // Changed from white to yellow
            }
        } else if (validatedStormType == 1)  {

                // Fallback to yellow for unexpected stages
                r = 1.0f; g = 1.0f; b = 0.0f; // Changed from white to yellow
        
        } else {
        	
        	r = 1.0f; g = 1.0f; b = 1.0f;
        }
        
        LOGGER.info("RadarOverlayRenderer: Storm type is validated as {}, stormType returns {}", 
                validatedStormType, poly.stormType);
        
        return new float[]{r, g, b, a};
    }
    
    // Replace the cleanupCache method:

    private void cleanupCache() {
        long currentTime = System.currentTimeMillis();
        polygonCache.entrySet().removeIf(entry -> 
            currentTime - entry.getValue().lastUpdateTime > 30000); // Remove after 30 seconds
        
        // Clean up the hash cache to match
        polygonDataHash.keySet().retainAll(polygonCache.keySet());
        
        // Also clean up the logging cache
        if (polygonCache.isEmpty()) {
            loggedPolygons.clear();
            polygonDataHash.clear();
        }
    }
}