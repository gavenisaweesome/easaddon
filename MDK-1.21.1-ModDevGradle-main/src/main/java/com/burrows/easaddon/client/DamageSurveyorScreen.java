package com.burrows.easaddon.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import com.burrows.easaddon.tornado.TornadoTracker;
import com.burrows.easaddon.EASAddon;
import com.burrows.easaddon.tornado.TornadoData;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class DamageSurveyorScreen extends Screen {
    private static final int GUI_WIDTH = 256;
    private static final int GUI_HEIGHT = 200;
    private static final int ENTRY_HEIGHT = 15;
    private static final int BUTTON_AREA_HEIGHT = 35; // Reserve space for buttons
    private static final int LIST_START_Y = 50; // Start position for tornado list (relative to topPos)
    
    private int leftPos;
    private int topPos;
    private int scrollOffset = 0;
    private List<TornadoData> tornadoList;
    private int hoveredIndex = -1;
    
    // Calculate available space for tornado entries
    private int getAvailableListHeight() {
        return GUI_HEIGHT - LIST_START_Y - BUTTON_AREA_HEIGHT;
    }
    
    // Calculate maximum entries that can fit in available space
    private int getMaxDisplayCount() {
        return getAvailableListHeight() / ENTRY_HEIGHT;
    }
    
    public DamageSurveyorScreen() {
        super(Component.literal("Damage Surveyor"));
    }
    
    @Override
    protected void init() {
        super.init();
        
        this.leftPos = (this.width - GUI_WIDTH) / 2;
        this.topPos = (this.height - GUI_HEIGHT) / 2;
        
        // Refresh tornado data
        this.tornadoList = TornadoTracker.getInstance().getAllTornadoData();
        
        // Clear any existing widgets to prevent duplicates
        this.clearWidgets();
        
        // Add close button
        this.addRenderableWidget(Button.builder(Component.literal("Close"), 
            button -> this.onClose())
            .bounds(this.leftPos + GUI_WIDTH - 50, this.topPos + GUI_HEIGHT - 25, 40, 20)
            .build());
            
        // Add refresh button
        this.addRenderableWidget(Button.builder(Component.literal("Refresh"), 
            button -> this.refreshData())
            .bounds(this.leftPos + 10, this.topPos + GUI_HEIGHT - 25, 60, 20)
            .build());
            
        // Add clear all button
        this.addRenderableWidget(Button.builder(Component.literal("Clear All"), 
            button -> this.clearAllData())
            .bounds(this.leftPos + 80, this.topPos + GUI_HEIGHT - 25, 70, 20)
            .build());
    }
    
    private void clearAllData() {
        // Get current world info for logging
        String worldInfo = getCurrentWorldInfo();
        
        // FIXED: Only clear inactive tornadoes, not all data
        TornadoTracker tracker = TornadoTracker.getInstance();
        List<TornadoData> allTornadoes = tracker.getAllTornadoData();
        
        // Count how many will be removed
        int removedCount = 0;
        List<Long> toRemove = new ArrayList<>();
        
        for (TornadoData tornado : allTornadoes) {
            if (!tornado.isActive()) {
                toRemove.add(tornado.getId());
                removedCount++;
            }
        }
        
        // Remove inactive tornadoes
        for (Long id : toRemove) {
            tracker.removeInactiveTornado(id);
        }
        
        // Force save after cleanup
        tracker.forceSave();
        
        // Also clear survey data
        try {
            com.burrows.easaddon.survey.DamageSurveyManager.getInstance().clearWorldData();
        } catch (Exception e) {
            EASAddon.LOGGER.warn("Failed to clear survey data: {}", e.getMessage());
        }
        
        // Refresh the display
        this.refreshData();
        
        EASAddon.LOGGER.info("Cleared {} inactive tornado records from {}", removedCount, worldInfo);
    }
    
    private void refreshData() {
        // Force save current data first
        TornadoTracker.getInstance().forceSave();
        
        // Force an immediate update of tornado tracking data
        TornadoTracker.getInstance().forceUpdate();
        
        // Then refresh the GUI list
        this.tornadoList = TornadoTracker.getInstance().getAllTornadoData();
        this.scrollOffset = 0;
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Completely override background with solid color to prevent blur
        this.renderDirtBackground(guiGraphics);
        
        // Draw a simple GUI background using built-in method
        guiGraphics.fill(leftPos, topPos, leftPos + GUI_WIDTH, topPos + GUI_HEIGHT, 0xC0101010);
        guiGraphics.fill(leftPos + 2, topPos + 2, leftPos + GUI_WIDTH - 2, topPos + GUI_HEIGHT - 2, 0xC0C0C0C0);
        
        // Draw title with better positioning and shadow
        guiGraphics.drawCenteredString(font, this.title, leftPos + GUI_WIDTH / 2, topPos + 8, 0xFFFFFF);
        
        // Draw current world information
        String worldInfo = getCurrentWorldInfo();
        guiGraphics.drawCenteredString(font, "§7" + worldInfo, leftPos + GUI_WIDTH / 2, topPos + 20, 0x808080);
        
        // Draw tornado list
        renderTornadoList(guiGraphics);
        
        // Calculate hover detection for tornado entries
        int listStartY = topPos + LIST_START_Y;
        int listEndY = listStartY + getAvailableListHeight();
        int relativeY = mouseY - listStartY;
        
        if (mouseX >= leftPos + 10 && mouseX <= leftPos + GUI_WIDTH - 10 && 
            mouseY >= listStartY && mouseY < listEndY &&
            relativeY >= 0) {
            
            int entryIndex = relativeY / ENTRY_HEIGHT;
            hoveredIndex = scrollOffset + entryIndex;
            
            // Make sure we don't exceed the available entries or display count
            if (hoveredIndex >= tornadoList.size() || entryIndex >= getMaxDisplayCount()) {
                hoveredIndex = -1;
            }
        } else {
            hoveredIndex = -1;
        }
        
        // Render widgets last (buttons) - this ensures they appear on top
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private String getCurrentWorldInfo() {
        try {
            String worldId = com.burrows.easaddon.tornado.TornadoTracker.getInstance().getCurrentWorldId();
            if (worldId != null) {
                if (worldId.startsWith("singleplayer_")) {
                    return "World: " + worldId.substring(13); // Remove "singleplayer_" prefix
                } else if (worldId.startsWith("multiplayer_")) {
                    return "Server: " + worldId.substring(12); // Remove "multiplayer_" prefix  
                } else {
                    return "World: " + worldId;
                }
            }
        } catch (Exception e) {
            // Fallback
        }
        return "Current World";
    }
    
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Override to prevent any default background rendering that might cause blur
        this.renderDirtBackground(guiGraphics);
    }
    
    private void renderDirtBackground(GuiGraphics guiGraphics) {
        // Render solid dark background that completely overrides blur
        guiGraphics.fillGradient(0, 0, this.width, this.height, 0xC0101010, 0xD0101010);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false; // Ensure this doesn't trigger pause screen blur
    }
    
    private void renderTornadoList(GuiGraphics guiGraphics) {
        int startY = topPos + 35; // Header start position
        
        // Show tracking status
        if (!TornadoTracker.getInstance().isTrackingEnabled()) {
            guiGraphics.drawString(font, "§cTornado tracking disabled", 
                leftPos + 10, startY, 0xFF4444, true);
            guiGraphics.drawString(font, "§7PMWeather integration failed", 
                leftPos + 10, startY + 12, 0x808080, true);
            return;
        }
        
        if (tornadoList.isEmpty()) {
            guiGraphics.drawString(font, "No tornado data available", 
                leftPos + 10, startY, 0x808080, true);
            guiGraphics.drawString(font, "§7Spawn a tornado to see data here", 
                leftPos + 10, startY + 12, 0x808080, true);
            return;
        }
        
        // Draw headers with shadows
        guiGraphics.drawString(font, "ID", leftPos + 10, startY, 0x404040, true);
        guiGraphics.drawString(font, "Status", leftPos + 35, startY, 0x404040, true);
        guiGraphics.drawString(font, "Rating", leftPos + 80, startY, 0x404040, true);
        guiGraphics.drawString(font, "Max Wind", leftPos + 115, startY, 0x404040, true);
        guiGraphics.drawString(font, "Max Width", leftPos + 165, startY, 0x404040, true);
        
        startY += 15;
        
        // Draw separator line
        guiGraphics.hLine(leftPos + 8, leftPos + GUI_WIDTH - 8, startY - 3, 0x404040);
        
        // Calculate how many entries we can actually display
        int maxDisplayCount = getMaxDisplayCount();
        int displayCount = Math.min(maxDisplayCount, tornadoList.size() - scrollOffset);
        
        // Draw tornado data entries
        for (int i = 0; i < displayCount; i++) {
            int index = i + scrollOffset;
            if (index >= tornadoList.size()) break;
            
            TornadoData tornado = tornadoList.get(index);
            int yPos = startY + (i * ENTRY_HEIGHT);
            
            // Highlight hovered entry
            if (index == hoveredIndex) {
                guiGraphics.fill(leftPos + 8, yPos - 2, leftPos + GUI_WIDTH - 8, yPos + 13, 0x40FFFFFF);
            }
            
            // Color code based on active status
            int color = tornado.isActive() ? 0xFF4444 : 0x808080;
            
            // Draw tornado data with shadows for better visibility
            guiGraphics.drawString(font, String.valueOf(tornado.getId()), 
                leftPos + 10, yPos, color, true);
                
            guiGraphics.drawString(font, tornado.isActive() ? "ACTIVE" : "ENDED", 
                leftPos + 35, yPos, color, true);
                
            guiGraphics.drawString(font, tornado.getRating(), 
                leftPos + 80, yPos, color, true);
                
            guiGraphics.drawString(font, tornado.getMaxWindspeed() + " mph", 
                leftPos + 115, yPos, color, true);
                
            guiGraphics.drawString(font, String.format("%.1f", tornado.getMaxWidth()), 
                leftPos + 165, yPos, color, true);
        }
        
        // Draw a separator line between list and buttons
        int separatorY = topPos + GUI_HEIGHT - BUTTON_AREA_HEIGHT;
        guiGraphics.hLine(leftPos + 8, leftPos + GUI_WIDTH - 8, separatorY, 0x404040);
        
        // Draw scroll indicator if needed
        if (tornadoList.size() > maxDisplayCount) {
            guiGraphics.drawString(font, 
                String.format("§7Showing %d-%d of %d", 
                    scrollOffset + 1, 
                    Math.min(scrollOffset + maxDisplayCount, tornadoList.size()), 
                    tornadoList.size()), 
                leftPos + 10, separatorY - 12, 0x808080, true);
        }
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int maxDisplayCount = getMaxDisplayCount();
        if (tornadoList.size() > maxDisplayCount) {
            if (scrollY > 0 && scrollOffset > 0) {
                scrollOffset--;
                return true;
            } else if (scrollY < 0 && scrollOffset < tornadoList.size() - maxDisplayCount) {
                scrollOffset++;
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // First check if we're clicking on any widgets (buttons)
        for (var widget : this.children()) {
            if (widget.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        
        // Only handle tornado entry clicks if clicking in the list area
        if (button == 0 && hoveredIndex >= 0 && hoveredIndex < tornadoList.size()) {
            int listStartY = topPos + LIST_START_Y;
            int listEndY = listStartY + getAvailableListHeight();
            
            // Check if click is within the tornado list area (not in button area)
            if (mouseX >= leftPos + 10 && mouseX <= leftPos + GUI_WIDTH - 10 && 
                mouseY >= listStartY && mouseY < listEndY) {
                
                // Open detail screen for clicked tornado
                TornadoData clickedTornado = tornadoList.get(hoveredIndex);
                this.minecraft.setScreen(new TornadoDetailScreen(this, clickedTornado));
                return true;
            }
        }
        
        return false;
    }
}