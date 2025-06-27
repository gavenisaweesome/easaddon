package com.burrows.easaddon.client;

import com.burrows.easaddon.tornado.TornadoData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
public class TornadoDetailScreen extends Screen {
    // FIXED: Make these non-final so they can be adjusted based on screen size
    private static final int BASE_GUI_WIDTH = 320;
    private static final int BASE_GUI_HEIGHT = 320;
    private static final int BASE_MAP_SIZE = 160;
    private static final int MIN_GUI_WIDTH = 280;  // Minimum usable width
    private static final int MIN_GUI_HEIGHT = 240; // Minimum usable height
    
    // ADDED: Actual dimensions after scaling adjustments
    private int actualGuiWidth;
    private int actualGuiHeight;
    private int actualMapSize;
    
    private final DamageSurveyorScreen parentScreen;
    private final TornadoData tornadoData;
    private int leftPos;
    private int topPos;
    private int scrollOffset = 0;
    private boolean showMap = true;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
    
    // Map bounds
    private double minX, maxX, minZ, maxZ;
    
    public TornadoDetailScreen(DamageSurveyorScreen parentScreen, TornadoData tornadoData) {
        super(Component.literal("Tornado " + tornadoData.getId() + " - " + tornadoData.getRating()));
        this.parentScreen = parentScreen;
        this.tornadoData = tornadoData;
        calculateMapBounds();
    }
    
    /**
     * ADDED: Calculate responsive GUI dimensions based on screen size
     */
    private void calculateResponsiveDimensions() {
        // Calculate available screen space with padding for safety
        int availableWidth = this.width - 40; // 20px padding on each side
        int availableHeight = this.height - 40; // 20px padding on top/bottom
        
        // Calculate scale factor to fit GUI on screen
        double widthScale = (double) availableWidth / BASE_GUI_WIDTH;
        double heightScale = (double) availableHeight / BASE_GUI_HEIGHT;
        double scale = Math.min(widthScale, heightScale);
        
        // Don't scale up, only scale down if needed
        scale = Math.min(scale, 1.0);
        
        // Apply scaling but ensure minimum dimensions
        actualGuiWidth = Math.max(MIN_GUI_WIDTH, (int)(BASE_GUI_WIDTH * scale));
        actualGuiHeight = Math.max(MIN_GUI_HEIGHT, (int)(BASE_GUI_HEIGHT * scale));
        
        // Scale map proportionally
        double mapScale = (double) actualGuiWidth / BASE_GUI_WIDTH;
        actualMapSize = Math.max(120, (int)(BASE_MAP_SIZE * mapScale)); // Minimum 120px map
        
        // Ensure map doesn't exceed GUI bounds
        int maxMapSize = Math.min(actualGuiWidth - 40, actualGuiHeight - 120); // Leave space for stats and buttons
        actualMapSize = Math.min(actualMapSize, maxMapSize);
        
        // Calculate centered position
        this.leftPos = (this.width - actualGuiWidth) / 2;
        this.topPos = (this.height - actualGuiHeight) / 2;
        
        // Debug logging
        com.burrows.easaddon.EASAddon.LOGGER.info("TornadoDetailScreen: Screen {}x{}, Scale {:.2f}, GUI {}x{}, Map {}", 
            this.width, this.height, scale, actualGuiWidth, actualGuiHeight, actualMapSize);
    }
    
    private void calculateMapBounds() {
        List<TornadoData.PositionRecord> history = tornadoData.getPositionHistory();
        if (history.isEmpty()) {
            // Default bounds if no history
            Player player = Minecraft.getInstance().player;
            if (player != null) {
                Vec3 playerPos = player.position();
                minX = playerPos.x - 100;
                maxX = playerPos.x + 100;
                minZ = playerPos.z - 100;
                maxZ = playerPos.z + 100;
            } else {
                minX = maxX = minZ = maxZ = 0;
            }
            return;
        }
        
        // Find the maximum tornado width for padding calculation
        float maxWidth = 0;
        for (TornadoData.PositionRecord record : history) {
            maxWidth = Math.max(maxWidth, record.width);
        }
        
        minX = maxX = history.get(0).position.x;
        minZ = maxZ = history.get(0).position.z;
        
        for (TornadoData.PositionRecord record : history) {
            minX = Math.min(minX, record.position.x);
            maxX = Math.max(maxX, record.position.x);
            minZ = Math.min(minZ, record.position.z);
            maxZ = Math.max(maxZ, record.position.z);
        }
        
        // Include player position in bounds calculation
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            Vec3 playerPos = player.position();
            minX = Math.min(minX, playerPos.x);
            maxX = Math.max(maxX, playerPos.x);
            minZ = Math.min(minZ, playerPos.z);
            maxZ = Math.max(maxZ, playerPos.z);
        }
        
        // FIXED: Dynamic scaling - ensure minimum useful range
        double currentRangeX = maxX - minX;
        double currentRangeZ = maxZ - minZ;
        double minRange = Math.max(200, maxWidth * 3); // Minimum 200 blocks or 3x tornado width
        
        if (currentRangeX < minRange) {
            double expand = (minRange - currentRangeX) / 2;
            minX -= expand;
            maxX += expand;
        }
        
        if (currentRangeZ < minRange) {
            double expand = (minRange - currentRangeZ) / 2;
            minZ -= expand;
            maxZ += expand;
        }
        
        // Add padding based on maximum tornado width plus extra for visibility
        double padding = Math.max(50, maxWidth + 30);
        minX -= padding;
        maxX += padding;
        minZ -= padding;
        maxZ += padding;
        
        // Ensure square aspect ratio
        double width = maxX - minX;
        double height = maxZ - minZ;
        if (width > height) {
            double diff = (width - height) / 2;
            minZ -= diff;
            maxZ += diff;
        } else {
            double diff = (height - width) / 2;
            minX -= diff;
            maxX += diff;
        }
    }
    
    // FIXED: Add method to recalculate bounds for active tornadoes
    private void recalculateBoundsIfNeeded() {
        // Only recalculate if tornado is still active
        if (tornadoData.isActive()) {
            // Get current tornado position if it exists in tracking
            TornadoData currentData = com.burrows.easaddon.tornado.TornadoTracker.getInstance().getTornadoData(tornadoData.getId());
            if (currentData != null && !currentData.getPositionHistory().isEmpty()) {
                // Check if the latest position is outside current bounds
                List<TornadoData.PositionRecord> history = currentData.getPositionHistory();
                TornadoData.PositionRecord latest = history.get(history.size() - 1);
                
                boolean needsRecalc = false;
                double margin = 50; // Margin before recalculating
                
                if (latest.position.x < minX + margin || latest.position.x > maxX - margin ||
                    latest.position.z < minZ + margin || latest.position.z > maxZ - margin) {
                    needsRecalc = true;
                }
                
                if (needsRecalc) {
                    // Update our tornado data with latest information
                    this.tornadoData.getPositionHistory().clear();
                    for (TornadoData.PositionRecord record : history) {
                        this.tornadoData.addPositionRecord(record);
                    }
                    
                    // Recalculate bounds
                    calculateMapBounds();
                }
            }
        }
    }
    
@Override
protected void init() {
    super.init();
    
    // FIXED: Calculate responsive dimensions first
    calculateResponsiveDimensions();
    
    // FIXED: Use responsive button positioning - ensure buttons are always visible
    int buttonY = Math.max(this.topPos + actualGuiHeight - 25, this.height - 30); // At least 30px from bottom
    int buttonSpacing = 85; // Space between buttons
    
    // FIXED: Adjust button width based on available space
    int buttonWidth = Math.max(40, Math.min(100, actualGuiWidth / 5)); // Responsive button width
    int smallButtonWidth = Math.max(30, buttonWidth - 20);
    
    // Back button (left side)
    this.addRenderableWidget(Button.builder(Component.literal("Back"), 
        button -> this.minecraft.setScreen(parentScreen))
        .bounds(this.leftPos + 10, buttonY, smallButtonWidth + 20, 20)
        .build());
    
    // Survey Damage button (left-center) - adjust text based on button size
    String surveyText = buttonWidth >= 80 ? "Survey Damage" : "Survey";
    this.addRenderableWidget(Button.builder(Component.literal(surveyText), 
        button -> this.startDamageSurvey())
        .bounds(this.leftPos + 80, buttonY, buttonWidth, 20)
        .build());
        
    // Toggle Map/List button (right-center)
    String toggleText = buttonWidth >= 70 ? (showMap ? "Show List" : "Show Map") : (showMap ? "List" : "Map");
    this.addRenderableWidget(Button.builder(Component.literal(toggleText), 
        button -> {
            this.showMap = !this.showMap;
            String newText = buttonWidth >= 70 ? (showMap ? "Show List" : "Show Map") : (showMap ? "List" : "Map");
            button.setMessage(Component.literal(newText));
            this.scrollOffset = 0;
        })
        .bounds(this.leftPos + 190, buttonY, buttonWidth, 20)
        .build());
    
    // Close button (right side)
    this.addRenderableWidget(Button.builder(Component.literal("Close"), 
        button -> this.onClose())
        .bounds(this.leftPos + actualGuiWidth - smallButtonWidth - 10, buttonY, smallButtonWidth, 20)
        .build());
}
    
    private void startDamageSurvey() {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        
        // Import the survey manager
        com.burrows.easaddon.survey.DamageSurveyManager surveyManager = 
            com.burrows.easaddon.survey.DamageSurveyManager.getInstance();
        
        // Check if this tornado is already being surveyed
        if (surveyManager.isTornadoBeingSurveyed(tornadoData.getId())) {
            String surveyorName = surveyManager.getSurveyorName(tornadoData.getId());
            player.sendSystemMessage(Component.literal("§cThis tornado is already being surveyed by " + surveyorName));
            return;
        }
        
        // Check if player is already surveying another tornado
        if (surveyManager.getActiveSurvey(player.getName().getString()) != null) {
            player.sendSystemMessage(Component.literal("§cYou are already surveying another tornado. Use /survey quit to cancel."));
            return;
        }
        
        // Start the survey
        boolean success = surveyManager.startSurvey(tornadoData, player);
        if (success) {
            // Close the GUI and let the survey begin
            this.onClose();
        }
    }
    
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // FIXED: Override to prevent default background blur
        this.renderDirtBackground(guiGraphics);
    }
    
    private void renderDirtBackground(GuiGraphics guiGraphics) {
        // FIXED: Render solid opaque background that completely overrides blur
        guiGraphics.fill(0, 0, this.width, this.height, 0xFF101010);
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // FIXED: Don't call super.renderBackground to avoid blur
        this.renderDirtBackground(guiGraphics);
        
        // FIXED: Use actual dimensions instead of constants
        // Main background
        guiGraphics.fill(leftPos, topPos, leftPos + actualGuiWidth, topPos + actualGuiHeight, 0xC0101010);
        guiGraphics.fill(leftPos + 2, topPos + 2, leftPos + actualGuiWidth - 2, topPos + actualGuiHeight - 2, 0xC0C0C0C0);
        
        // Title
        guiGraphics.drawCenteredString(font, this.title, leftPos + actualGuiWidth / 2, topPos + 8, 0xFFFFFF);
        
        // Statistics summary - FIXED: Adjust font size and layout for smaller screens
        int statsY = topPos + 25;
        int statsTextSize = actualGuiWidth < 300 ? 6 : 8; // Smaller text for smaller GUIs
        
        guiGraphics.drawString(font, "Max Wind: " + tornadoData.getMaxWindspeed() + " mph", leftPos + 10, statsY, 0xFFFFFF, true);
        guiGraphics.drawString(font, "Max Width: " + String.format("%.1f", tornadoData.getMaxWidth()), leftPos + 120, statsY, 0xFFFFFF, true);
        
        // FIXED: Only show path length if there's enough space
        if (actualGuiWidth >= 300) {
            guiGraphics.drawString(font, "Path Length: " + String.format("%.1f blocks", tornadoData.getTotalPathLength()), leftPos + 210, statsY, 0xFFFFFF, true);
        }
        
        statsY += 15;
        guiGraphics.drawString(font, "Duration: " + formatDuration(tornadoData.getLastSeenTime() - tornadoData.getFirstSeenTime()), 
            leftPos + 10, statsY, 0xFFFFFF, true);
        guiGraphics.drawString(font, "Chunks Affected: " + tornadoData.getDamagedChunks().size(), 
            leftPos + 120, statsY, 0xFFFFFF, true);
        
        // Draw separator
        guiGraphics.hLine(leftPos + 5, leftPos + actualGuiWidth - 5, statsY + 12, 0xFF808080);
        
        if (showMap) {
            renderSatelliteMap(guiGraphics);
        } else {
            renderCoordinatesList(guiGraphics);
        }
        
        // Render widgets (buttons) on top
        for (var widget : this.renderables) {
            widget.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }
    
// NEW: Satellite-style map rendering - FIXED: Use actual dimensions
private void renderSatelliteMap(GuiGraphics guiGraphics) {
    // FIXED: Recalculate bounds if tornado is still active and moving
    recalculateBoundsIfNeeded();
    
    // FIXED: Use actual map size and center it properly
    int mapX = leftPos + (actualGuiWidth - actualMapSize) / 2;
    int mapY = topPos + 65;
    
    // Map background (dark for unloaded areas)
    guiGraphics.fill(mapX, mapY, mapX + actualMapSize, mapY + actualMapSize, 0xFF202020);
    guiGraphics.fill(mapX + 1, mapY + 1, mapX + actualMapSize - 1, mapY + actualMapSize - 1, 0xFF000000);
    
    Level level = Minecraft.getInstance().level;
    if (level != null) {
        // Render satellite view of the terrain
        renderTerrainBlocks(guiGraphics, mapX, mapY, level);
    }
    
    // Draw grid overlay (subtle)
    for (int i = 0; i <= 4; i++) {
        int gridPos = mapX + (actualMapSize * i / 4);
        guiGraphics.vLine(gridPos, mapY, mapY + actualMapSize, 0x40808080);
        gridPos = mapY + (actualMapSize * i / 4);
        guiGraphics.hLine(mapX, mapX + actualMapSize, gridPos, 0x40808080);
    }
    
    List<TornadoData.PositionRecord> history = tornadoData.getPositionHistory();
    if (history.isEmpty()) {
        guiGraphics.drawCenteredString(font, "No path data available", 
            mapX + actualMapSize / 2, mapY + actualMapSize / 2 - 4, 0x808080);
        return;
    }
    
    // ADDED: Get survey damage data for chunk ratings
    Map<ChunkPos, com.burrows.easaddon.survey.ChunkDamageData> chunkDamageData = null;
    boolean hasSurveyData = tornadoData.isSurveyed();
    
    if (hasSurveyData) {
        try {
            chunkDamageData = com.burrows.easaddon.survey.DamageSurveyManager.getInstance()
                .getTornadoDamageData(tornadoData.getId());
        } catch (Exception e) {
            // Fallback if survey data not available
            hasSurveyData = false;
        }
    }
    
    // Handle single-point tornadoes
    if (history.size() == 1) {
        TornadoData.PositionRecord record = history.get(0);
        Vec3 pos = record.position;
        
        int centerX = mapX + actualMapSize / 2; // Center single point
        int centerZ = mapY + actualMapSize / 2;
        
        // Calculate tornado width for single point
        double worldRange = Math.max(maxX - minX, maxZ - minZ);
        double pixelsPerBlock = (actualMapSize - 4) / worldRange;
        int widthRadius = Math.max(3, (int)(record.width * pixelsPerBlock / 2));
        widthRadius = Math.min(widthRadius, actualMapSize / 6);
        
        int color = getWindspeedColor(record.windspeed);
        float alpha = getTornadoAlpha(record.windspeed);
        
        drawTornadoCircle(guiGraphics, centerX, centerZ, widthRadius, color, alpha);
        guiGraphics.fill(centerX - 1, centerZ - 1, centerX + 2, centerZ + 2, color);
        
        // Label for single point
        guiGraphics.drawCenteredString(font, "Single Position", 
            mapX + actualMapSize / 2, mapY + actualMapSize - 40, 0x808080);
        return;
    }
    
    // ADDED: Draw surveyed chunks with EF ratings (if available)
    if (hasSurveyData && chunkDamageData != null) {
        renderSurveyedChunks(guiGraphics, mapX, mapY, chunkDamageData);
    }
    
    // Draw tornado path with width visualization
    for (int i = 0; i < history.size(); i++) {
        TornadoData.PositionRecord record = history.get(i);
        Vec3 pos = record.position;
        
        int centerX = mapX + (int)((pos.x - minX) / (maxX - minX) * (actualMapSize - 4)) + 2;
        int centerZ = mapY + (int)((pos.z - minZ) / (maxZ - minZ) * (actualMapSize - 4)) + 2;
        
        // Calculate tornado width in map pixels
        double worldRange = Math.max(maxX - minX, maxZ - minZ);
        double pixelsPerBlock = (actualMapSize - 4) / worldRange;
        int widthRadius = Math.max(1, (int)(record.width * pixelsPerBlock / 2));
        
        // Cap the radius to prevent oversized circles
        widthRadius = Math.min(widthRadius, actualMapSize / 8);
        
        // Get color based on windspeed
        int color = getWindspeedColor(record.windspeed);
        
        // Variable transparency based on tornado intensity
        float alpha = getTornadoAlpha(record.windspeed);
        
        // Draw tornado width as a filled circle with transparency
        drawTornadoCircle(guiGraphics, centerX, centerZ, widthRadius, color, alpha);
        
        // Draw connecting line to next position (if exists)
        if (i < history.size() - 1) {
            Vec3 nextPos = history.get(i + 1).position;
            int nextX = mapX + (int)((nextPos.x - minX) / (maxX - minX) * (actualMapSize - 4)) + 2;
            int nextZ = mapY + (int)((nextPos.z - minZ) / (maxZ - minZ) * (actualMapSize - 4)) + 2;
            
            // Draw thin connecting line
            drawLine(guiGraphics, centerX, centerZ, nextX, nextZ, 0xFF808080);
        }
        
        // Draw center point
        guiGraphics.fill(centerX - 1, centerZ - 1, centerX + 2, centerZ + 2, color);
    }
    
    // Draw start and end markers
    if (!history.isEmpty()) {
        Vec3 start = history.get(0).position;
        Vec3 end = history.get(history.size() - 1).position;
        
        int startX = mapX + (int)((start.x - minX) / (maxX - minX) * (actualMapSize - 4)) + 2;
        int startZ = mapY + (int)((start.z - minZ) / (maxZ - minZ) * (actualMapSize - 4)) + 2;
        int endX = mapX + (int)((end.x - minX) / (maxX - minX) * (actualMapSize - 4)) + 2;
        int endZ = mapY + (int)((end.z - minZ) / (maxZ - minZ) * (actualMapSize - 4)) + 2;
        
        // Start marker (green)
        guiGraphics.fill(startX - 2, startZ - 2, startX + 3, startZ + 3, 0xFF00FF00);
        
        // End marker (red)
        guiGraphics.fill(endX - 2, endZ - 2, endX + 3, endZ + 3, 0xFFFF0000);
    }
    
    // FIXED: Draw player position marker
    Player player = Minecraft.getInstance().player;
    if (player != null) {
        Vec3 playerPos = player.position();
        int playerX = mapX + (int)((playerPos.x - minX) / (maxX - minX) * (actualMapSize - 4)) + 2;
        int playerZ = mapY + (int)((playerPos.z - minZ) / (maxZ - minZ) * (actualMapSize - 4)) + 2;
        
        // Check if player is within map bounds
        if (playerX >= mapX && playerX <= mapX + actualMapSize && 
            playerZ >= mapY && playerZ <= mapY + actualMapSize) {
            
            // Draw player marker (blue diamond shape)
            guiGraphics.fill(playerX, playerZ - 2, playerX + 1, playerZ + 3, 0xFF0080FF); // Vertical line
            guiGraphics.fill(playerX - 2, playerZ, playerX + 3, playerZ + 1, 0xFF0080FF); // Horizontal line
            guiGraphics.fill(playerX - 1, playerZ - 1, playerX + 2, playerZ + 2, 0xFF0080FF); // Center
        }
    }
    
    // FIXED: Enhanced map legend with survey data - adjust for smaller screens
    int legendY = mapY + actualMapSize + 10;
    int legendSpace = actualGuiHeight - (legendY - topPos) - 30; // Space available for legend
    
    if (legendSpace >= 40) { // Only show full legend if there's space
        guiGraphics.drawString(font, "Start", mapX, legendY, 0x00FF00, true);
        guiGraphics.drawString(font, "End", mapX + 40, legendY, 0xFF0000, true);
        guiGraphics.drawString(font, "Player", mapX + 70, legendY, 0x0080FF, true);
        
        // Second line of legend
        if (legendSpace >= 25) {
            legendY += 10;
            if (hasSurveyData && chunkDamageData != null) {
                guiGraphics.drawString(font, "Survey: Chunks rated by damage", mapX, legendY, 0xFFFF80, true);
            } else {
                guiGraphics.drawString(font, "Satellite view with tornado path", mapX, legendY, 0x808080, true);
            }
            
            // FIXED: Only show scale if there's enough width
            if (actualGuiWidth >= 280) {
                guiGraphics.drawString(font, String.format("Scale: %.0f blocks", (maxX - minX)), 
                    mapX + 140, legendY, 0x808080, true);
            }
        }
        
        // Third line - survey status
        if (legendSpace >= 35) {
            legendY += 10;
            if (hasSurveyData) {
                guiGraphics.drawString(font, String.format("Surveyed by: %s (EF%d)", 
                    tornadoData.getSurveyedBy(), tornadoData.getSurveyedEFRating()), 
                    mapX, legendY, 0x80FF80, true);
            } else {
                guiGraphics.drawString(font, "Not surveyed - Use Damage Surveyor", 
                    mapX, legendY, 0xFF8080, true);
            }
        }

        // FIXED: Fourth line - player coordinates (only if there's space)  
        if (legendSpace >= 45 && player != null) {
            legendY += 10;
            Vec3 playerPos = player.position();
            guiGraphics.drawString(font, String.format("Player: %.0f, %.0f, %.0f", 
                playerPos.x, playerPos.y, playerPos.z), mapX, legendY, 0x0080FF, true);
        }
    }
}

// NEW: Render terrain blocks as satellite view - FIXED: Use actual map size
private void renderTerrainBlocks(GuiGraphics guiGraphics, int mapX, int mapY, Level level) {
    // Calculate world coordinates to map pixel ratio
    double worldWidth = maxX - minX;
    double worldHeight = maxZ - minZ;
    double pixelsPerBlock = (actualMapSize - 4) / Math.max(worldWidth, worldHeight);
    
    // Sample resolution - how many world blocks per map pixel
    int sampleRate = Math.max(1, (int)(1.0 / pixelsPerBlock));
    
    // Render the terrain
    for (int mapPixelX = 0; mapPixelX < actualMapSize - 4; mapPixelX += Math.max(1, (int)(1.0 / Math.max(pixelsPerBlock, 0.1)))) {
        for (int mapPixelZ = 0; mapPixelZ < actualMapSize - 4; mapPixelZ += Math.max(1, (int)(1.0 / Math.max(pixelsPerBlock, 0.1)))) {
            // Convert map pixel to world coordinates
            double worldX = minX + (mapPixelX / (double)(actualMapSize - 4)) * worldWidth;
            double worldZ = minZ + (mapPixelZ / (double)(actualMapSize - 4)) * worldHeight;
            
            BlockPos pos = new BlockPos((int)worldX, 0, (int)worldZ);
            
            // Check if chunk is loaded
            if (!level.hasChunkAt(pos)) {
                continue; // Skip unloaded chunks (will remain black)
            }
            
            // Get surface block
            BlockPos surfacePos = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, pos);
            BlockState surfaceState = level.getBlockState(surfacePos);
            
            // Get block color based on type
            int blockColor = getBlockColor(surfaceState, level, surfacePos);
            
            // Calculate the size of each pixel based on sampling rate
            int pixelSize = Math.max(1, (int)(1.0 / Math.max(pixelsPerBlock, 0.1)));
            
            // Draw the pixel(s)
            guiGraphics.fill(
                mapX + 2 + mapPixelX, 
                mapY + 2 + mapPixelZ, 
                mapX + 2 + mapPixelX + pixelSize, 
                mapY + 2 + mapPixelZ + pixelSize, 
                blockColor
            );
        }
    }
}

// NEW: Get realistic block colors for satellite view
private int getBlockColor(BlockState state, Level level, BlockPos pos) {
    // Check for water first
    FluidState fluidState = state.getFluidState();
    if (!fluidState.isEmpty()) {
        if (fluidState.is(Fluids.WATER)) {
            return 0xFF4A90E2; // Water blue
        }
        if (fluidState.is(Fluids.LAVA)) {
            return 0xFFFF4500; // Lava orange
        }
    }
    
    // Get block type colors (similar to minimap mods)
    if (state.is(Blocks.GRASS_BLOCK)) {
        return 0xFF7CB342; // Grass green
    } else if (state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT)) {
        return 0xFF8D6E63; // Dirt brown
    } else if (state.is(Blocks.STONE)) {
        return 0xFF757575; // Stone gray
    } else if (state.is(Blocks.SAND)) {
        return 0xFFF4E4BC; // Sand tan
    } else if (state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK)) {
        return 0xFFFFFFFF; // Snow white
    } else if (state.is(Blocks.ICE) || state.is(Blocks.PACKED_ICE)) {
        return 0xFFB3E5FC; // Ice light blue
    } else if (state.getBlock().getName().getString().toLowerCase().contains("leaves")) {
        return 0xFF4CAF50; // Leaves green
    } else if (state.getBlock().getName().getString().toLowerCase().contains("log") ||
               state.getBlock().getName().getString().toLowerCase().contains("wood")) {
        return 0xFF5D4037; // Wood brown
    } else if (state.getBlock().getName().getString().toLowerCase().contains("ore")) {
        return 0xFF424242; // Ore dark gray
    } else if (state.is(Blocks.NETHERRACK)) {
        return 0xFF8D4E47; // Netherrack dark red
    } else if (state.is(Blocks.END_STONE)) {
        return 0xFFFFF8DC; // End stone pale yellow
    } else if (state.is(Blocks.OBSIDIAN)) {
        return 0xFF2E1A47; // Obsidian dark purple
    } else if (state.getBlock().getName().getString().toLowerCase().contains("concrete")) {
        return 0xFFBDBDBD; // Concrete light gray
    } else if (state.getBlock().getName().getString().toLowerCase().contains("terracotta")) {
        return 0xFFD7974C; // Terracotta orange-brown
    }
    
    // Default colors based on material properties
    if (state.blocksMotion()) {
        return 0xFF616161; // Solid blocks - gray
    } else {
        return 0xFF000000; // Transparent/air - black
    }
}

// ADDED: New method to render surveyed chunks with EF ratings - FIXED: Use actual map size
private void renderSurveyedChunks(GuiGraphics guiGraphics, int mapX, int mapY, 
                                 Map<ChunkPos, com.burrows.easaddon.survey.ChunkDamageData> chunkDamageData) {
    
    // Calculate map scaling
    double worldRange = Math.max(maxX - minX, maxZ - minZ);
    double pixelsPerBlock = (actualMapSize - 4) / worldRange;
    
    // Draw each surveyed chunk as a colored square
    for (Map.Entry<ChunkPos, com.burrows.easaddon.survey.ChunkDamageData> entry : chunkDamageData.entrySet()) {
        ChunkPos chunkPos = entry.getKey();
        com.burrows.easaddon.survey.ChunkDamageData damageData = entry.getValue();
        
        // Only show surveyed chunks
        if (!damageData.isSurveyed()) {
            continue;
        }
        
        // Calculate chunk position on map
        double chunkCenterX = (chunkPos.x * 16) + 8; // Chunk center in world coordinates
        double chunkCenterZ = (chunkPos.z * 16) + 8;
        
        // Convert to map coordinates
        int screenX = mapX + (int)((chunkCenterX - minX) / (maxX - minX) * (actualMapSize - 4)) + 2;
        int screenZ = mapY + (int)((chunkCenterZ - minZ) / (maxZ - minZ) * (actualMapSize - 4)) + 2;
        
        // Check if chunk is within map bounds
        if (screenX < mapX || screenX > mapX + actualMapSize || screenZ < mapY || screenZ > mapY + actualMapSize) {
            continue;
        }
        
        // Calculate chunk size on map (minimum 3 pixels)
        int chunkPixelSize = Math.max(3, (int)(16 * pixelsPerBlock));
        chunkPixelSize = Math.min(chunkPixelSize, 12); // Cap at 12 pixels
        
        // Get EF rating and corresponding color
        int efRating = damageData.getDeterminedEFRating();
        int chunkColor = getEFRatingColor(efRating);
        
        // Draw chunk square with transparency overlay
        int halfSize = chunkPixelSize / 2;
        int alphaColor = (chunkColor & 0x00FFFFFF) | 0x80000000; // 50% transparency
        guiGraphics.fill(screenX - halfSize, screenZ - halfSize, 
                        screenX + halfSize, screenZ + halfSize, alphaColor);
        
        // Draw chunk border for visibility
        guiGraphics.fill(screenX - halfSize, screenZ - halfSize, screenX + halfSize, screenZ - halfSize + 1, 0xFF000000); // Top
        guiGraphics.fill(screenX - halfSize, screenZ + halfSize - 1, screenX + halfSize, screenZ + halfSize, 0xFF000000); // Bottom
        guiGraphics.fill(screenX - halfSize, screenZ - halfSize, screenX - halfSize + 1, screenZ + halfSize, 0xFF000000); // Left
        guiGraphics.fill(screenX + halfSize - 1, screenZ - halfSize, screenX + halfSize, screenZ + halfSize, 0xFF000000); // Right
        
        // Draw EF rating text if chunk is large enough
        if (chunkPixelSize >= 8) {
            String ratingText = efRating >= 0 ? String.valueOf(efRating) : "U";
            int textColor = (efRating >= 3) ? 0xFFFFFFFF : 0xFF000000; // White text for dark backgrounds
            
            // Center the text in the chunk
            int textWidth = font.width(ratingText);
            int textX = screenX - textWidth / 2;
            int textY = screenZ - 4; // Center vertically
            
            guiGraphics.drawString(font, ratingText, textX, textY, textColor, false);
        }
    }
    
    // ADDED: Draw EF rating legend on the side (only if there's space)
    if (actualGuiWidth >= 300) {
        drawEFRatingLegend(guiGraphics, mapX + actualMapSize + 5, mapY);
    }
}

// ADDED: Method to get color for EF rating
private int getEFRatingColor(int efRating) {
    return switch (efRating) {
        case 0 -> 0xFF00FFFF; // EF0 - Blue
        case 1 -> 0xFF00FF80; // EF1 - Green
        case 2 -> 0xFFFFFF00; // EF2 - Yellow
        case 3 -> 0xFFFF8000; // EF3 - Orange
        case 4 -> 0xFFFF0000; // EF4 - Dark Red
        case 5 -> 0xFF8B00FF; // EF5 - Purple
        default -> 0xFF808080; // EFU - Gray
    };
}

// ADDED: Method to draw EF rating legend - FIXED: Check available space
private void drawEFRatingLegend(GuiGraphics guiGraphics, int startX, int startY) {
    // Only draw if there's space (check if legend would fit)
    if (startX + 60 > this.width) {
        return; // Not enough space
    }
    
    guiGraphics.drawString(font, "EF Scale:", startX, startY, 0xFFFFFF, true);
    
    int[] ratings = {0, 1, 2, 3, 4, 5, -1}; // -1 represents EFU
    String[] labels = {"EF0", "EF1", "EF2", "EF3", "EF4", "EF5", "EFU"};
    
    for (int i = 0; i < ratings.length; i++) {
        int y = startY + 15 + (i * 12);
        int color = getEFRatingColor(ratings[i]);
        
        // Draw color square
        guiGraphics.fill(startX, y, startX + 8, y + 8, color);
        guiGraphics.fill(startX, y, startX + 8, y + 1, 0xFF000000); // Top border
        guiGraphics.fill(startX, y + 7, startX + 8, y + 8, 0xFF000000); // Bottom border
        guiGraphics.fill(startX, y, startX + 1, y + 8, 0xFF000000); // Left border
        guiGraphics.fill(startX + 7, y, startX + 8, y + 8, 0xFF000000); // Right border
        
        // Draw label
        guiGraphics.drawString(font, labels[i], startX + 12, y, 0xFFFFFF, true);
    }
}
    
    private void renderCoordinatesList(GuiGraphics guiGraphics) {
        int listX = leftPos + 10;
        int listY = topPos + 65;
        int listHeight = actualGuiHeight - 150; // FIXED: Use actual GUI height
        
        List<TornadoData.PositionRecord> history = tornadoData.getPositionHistory();
        Set<ChunkPos> damagedChunks = tornadoData.getDamagedChunks();
        
        // Headers
        guiGraphics.drawString(font, "Time", listX, listY, 0x808080, true);
        guiGraphics.drawString(font, "Position", listX + 60, listY, 0x808080, true);
        guiGraphics.drawString(font, "Wind", listX + 200, listY, 0x808080, true);
        guiGraphics.drawString(font, "Width", listX + 250, listY, 0x808080, true);
        
        listY += 15;
        guiGraphics.hLine(leftPos + 8, leftPos + actualGuiWidth - 8, listY - 3, 0x404040);
        
        // Position records - FIXED: Calculate max display based on available space
        int maxDisplay = Math.max(5, listHeight / 15); // At least 5, based on available height
        int endIndex = Math.min(history.size(), scrollOffset + maxDisplay);
        
        for (int i = scrollOffset; i < endIndex; i++) {
            TornadoData.PositionRecord record = history.get(i);
            int y = listY + (i - scrollOffset) * 15;
            
            String time = timeFormat.format(new Date(record.timestamp));
            String pos = String.format("%.0f, %.0f, %.0f", 
                record.position.x, record.position.y, record.position.z);
            
            guiGraphics.drawString(font, time, listX, y, 0xFFFFFF, true);
            guiGraphics.drawString(font, pos, listX + 60, y, 0xFFFFFF, true);
            guiGraphics.drawString(font, record.windspeed + " mph", listX + 200, y, 
                getWindspeedColor(record.windspeed), true);
            guiGraphics.drawString(font, String.format("%.1f", record.width), listX + 250, y, 0xFFFFFF, true);
        }
        
        // Scroll indicator
        if (history.size() > maxDisplay) {
            guiGraphics.drawString(font, 
                String.format("Showing %d-%d of %d records", 
                    scrollOffset + 1, endIndex, history.size()), 
                listX, topPos + actualGuiHeight - 85, 0x808080, true);
        }
        
        // Damaged chunks summary
        if (!damagedChunks.isEmpty()) {
            int chunksY = topPos + actualGuiHeight - 70;
            guiGraphics.drawString(font, "Damaged Chunks: " + damagedChunks.size() + " total", 
                listX, chunksY, 0xFF8080, true);
        }
        
        // FIXED: Show player coordinates in list view too
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            Vec3 playerPos = player.position();
            int playerY = topPos + actualGuiHeight - 55;
            guiGraphics.drawString(font, String.format("Player Position: %.0f, %.0f, %.0f", 
                playerPos.x, playerPos.y, playerPos.z), listX, playerY, 0x0080FF, true);
        }
    }
    
    private void drawTornadoCircle(GuiGraphics guiGraphics, int centerX, int centerZ, int radius, int baseColor, float alpha) {
        // Extract RGB components from the base color
        int r = (baseColor >> 16) & 0xFF;
        int g = (baseColor >> 8) & 0xFF;
        int b = baseColor & 0xFF;
        
        // Create color with alpha
        int alphaInt = (int)(alpha * 255);
        int colorWithAlpha = (alphaInt << 24) | (r << 16) | (g << 8) | b;
        
        // Draw filled circle using multiple filled rectangles
        // This creates a rough circle approximation
        for (int y = -radius; y <= radius; y++) {
            for (int x = -radius; x <= radius; x++) {
                double distance = Math.sqrt(x * x + y * y);
                if (distance <= radius) {
                    // Inside the circle - draw pixel
                    guiGraphics.fill(centerX + x, centerZ + y, centerX + x + 1, centerZ + y + 1, colorWithAlpha);
                }
            }
        }
        
        // Draw circle outline for better visibility
        drawCircleOutline(guiGraphics, centerX, centerZ, radius, baseColor);
    }
    
    private void drawCircleOutline(GuiGraphics guiGraphics, int centerX, int centerZ, int radius, int color) {
        // Draw circle outline using Bresenham's circle algorithm
        int x = 0;
        int y = radius;
        int d = 3 - 2 * radius;
        
        while (y >= x) {
            // Draw 8 points of the circle
            plotCirclePoints(guiGraphics, centerX, centerZ, x, y, color);
            x++;
            
            if (d > 0) {
                y--;
                d = d + 4 * (x - y) + 10;
            } else {
                d = d + 4 * x + 6;
            }
        }
    }
    
    private void plotCirclePoints(GuiGraphics guiGraphics, int centerX, int centerZ, int x, int y, int color) {
        // Plot 8 symmetric points
        guiGraphics.fill(centerX + x, centerZ + y, centerX + x + 1, centerZ + y + 1, color);
        guiGraphics.fill(centerX - x, centerZ + y, centerX - x + 1, centerZ + y + 1, color);
        guiGraphics.fill(centerX + x, centerZ - y, centerX + x + 1, centerZ - y + 1, color);
        guiGraphics.fill(centerX - x, centerZ - y, centerX - x + 1, centerZ - y + 1, color);
        guiGraphics.fill(centerX + y, centerZ + x, centerX + y + 1, centerZ + x + 1, color);
        guiGraphics.fill(centerX - y, centerZ + x, centerX - y + 1, centerZ + x + 1, color);
        guiGraphics.fill(centerX + y, centerZ - x, centerX + y + 1, centerZ - x + 1, color);
        guiGraphics.fill(centerX - y, centerZ - x, centerX - y + 1, centerZ - x + 1, color);
    }
    
    private float getTornadoAlpha(int windspeed) {
        // Variable transparency based on tornado intensity
        // Stronger tornadoes are more opaque (more visible impact)
        if (windspeed > 200) return 0.7f; // EF5 - very opaque
        if (windspeed >= 166) return 0.6f; // EF4 - quite opaque
        if (windspeed >= 136) return 0.5f; // EF3 - moderately opaque
        if (windspeed >= 111) return 0.4f; // EF2 - somewhat transparent
        if (windspeed >= 86) return 0.3f;  // EF1 - more transparent
        if (windspeed >= 65) return 0.2f;  // EF0 - quite transparent
        return 0.1f; // Very weak - barely visible
    }
    
    private void drawLine(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int color) {
        // Bresenham's line algorithm
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;
        
        while (true) {
            guiGraphics.fill(x1, y1, x1 + 1, y1 + 1, color);
            
            if (x1 == x2 && y1 == y2) break;
            
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x1 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y1 += sy;
            }
        }
    }
    
    private int getWindspeedColor(int windspeed) {
        if (windspeed > 200) return 0xFF8B00FF; // Purple (EF5)
        if (windspeed >= 166) return 0xFFFF0000; // Red (EF4)
        if (windspeed >= 136) return 0xFFFF8000; // Orange-Red (EF3)
        if (windspeed >= 111) return 0xFFFFFF00; // Dark Orange (EF2)
        if (windspeed >= 86) return 0xFF00FF80; // Orange (EF1)
        if (windspeed >= 65) return 0xFF00FFFF; // Yellow (EF0)
        return 0xFF808080; // Gray (EFU)
    }
    
    private String formatDuration(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!showMap) {
            List<TornadoData.PositionRecord> history = tornadoData.getPositionHistory();
            int listHeight = actualGuiHeight - 150;
            int maxDisplay = Math.max(5, listHeight / 15); // Use calculated max display
            if (history.size() > maxDisplay) {
                if (scrollY > 0 && scrollOffset > 0) {
                    scrollOffset--;
                    return true;
                } else if (scrollY < 0 && scrollOffset < history.size() - maxDisplay) {
                    scrollOffset++;
                    return true;
                }
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false; // FIXED: Ensure this doesn't pause the game
    }
}