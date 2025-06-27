package com.burrows.easaddon.tornado;

import com.burrows.easaddon.EASAddon;
import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ClientTornadoPersistence {
    private static final String TORNADO_DATA_FOLDER = "easaddon";
    private static final String TORNADO_DATA_FILE = "tornado_data.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    public static void saveTornadoData(String dimensionKey, Map<Long, TornadoData> tornadoData) {
        try {
            // Get the world save directory
            Path worldDir = getWorldSaveDirectory();
            if (worldDir == null) {
                EASAddon.LOGGER.warn("Could not get world save directory, skipping tornado data save");
                return;
            }
            
            // Create our mod's data directory
            Path modDir = worldDir.resolve(TORNADO_DATA_FOLDER);
            Files.createDirectories(modDir);
            
            // Create dimension-specific file name
            String fileName = dimensionKey.replace(":", "_").replace("/", "_") + "_" + TORNADO_DATA_FILE;
            Path dataFile = modDir.resolve(fileName);
            
            // FIXED: Create backup of existing file
            if (Files.exists(dataFile)) {
                Path backupFile = modDir.resolve(fileName + ".bak");
                try {
                    Files.copy(dataFile, backupFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception e) {
                    EASAddon.LOGGER.warn("Failed to create backup: {}", e.getMessage());
                }
            }
            
            // Convert to JSON
            JsonObject root = new JsonObject();
            root.addProperty("version", 3); // INCREASED: Version for survey data support
            root.addProperty("dimension", dimensionKey);
            root.addProperty("saveTime", System.currentTimeMillis());
            root.addProperty("totalTornadoes", tornadoData.size());
            
            JsonArray tornadoArray = new JsonArray();
            for (TornadoData tornado : tornadoData.values()) {
                JsonObject tornadoObj = tornadoToJson(tornado);
                tornadoArray.add(tornadoObj);
            }
            root.add("tornadoes", tornadoArray);
            
            // Write to temporary file first, then rename (atomic operation)
            Path tempFile = modDir.resolve(fileName + ".tmp");
            try (FileWriter writer = new FileWriter(tempFile.toFile())) {
                GSON.toJson(root, writer);
                writer.flush();
            }
            
            // Atomic move to final file
            Files.move(tempFile, dataFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            
            EASAddon.LOGGER.info("Saved {} tornado records for dimension {} to {}", 
                               tornadoData.size(), dimensionKey, dataFile);
            
        } catch (Exception e) {
            EASAddon.LOGGER.error("Failed to save tornado data: {}", e.getMessage(), e);
        }
    }
    
    public static Map<Long, TornadoData> loadTornadoData(String dimensionKey) {
        Map<Long, TornadoData> result = new HashMap<>();
        
        try {
            // Get the world save directory
            Path worldDir = getWorldSaveDirectory();
            if (worldDir == null) {
                EASAddon.LOGGER.warn("Could not get world save directory, returning empty tornado data");
                return result;
            }
            
            // Get our mod's data directory
            Path modDir = worldDir.resolve(TORNADO_DATA_FOLDER);
            String fileName = dimensionKey.replace(":", "_").replace("/", "_") + "_" + TORNADO_DATA_FILE;
            Path dataFile = modDir.resolve(fileName);
            
            if (!Files.exists(dataFile)) {
                // FIXED: Try to load from backup if main file doesn't exist
                Path backupFile = modDir.resolve(fileName + ".bak");
                if (Files.exists(backupFile)) {
                    EASAddon.LOGGER.info("Main file not found, attempting to load from backup for dimension {}", dimensionKey);
                    dataFile = backupFile;
                } else {
                    EASAddon.LOGGER.info("No tornado data file found for dimension {}, starting fresh", dimensionKey);
                    return result;
                }
            }
            
            // Read and parse JSON
            String jsonContent = Files.readString(dataFile);
            JsonObject root = JsonParser.parseString(jsonContent).getAsJsonObject();
            
            // Check version compatibility
            int version = root.has("version") ? root.get("version").getAsInt() : 1;
            if (version > 3) {
                EASAddon.LOGGER.warn("Tornado data file has newer version {} than supported (3), attempting to load anyway", version);
            }
            
            // Validate dimension
            String savedDimension = root.get("dimension").getAsString();
            if (!dimensionKey.equals(savedDimension)) {
                EASAddon.LOGGER.warn("Dimension mismatch in tornado data file: expected {}, got {}", 
                                   dimensionKey, savedDimension);
                return result;
            }
            
            // Parse tornadoes
            JsonArray tornadoArray = root.getAsJsonArray("tornadoes");
            int loadedCount = 0;
            int errorCount = 0;
            
            for (JsonElement element : tornadoArray) {
                try {
                    TornadoData tornado = tornadoFromJson(element.getAsJsonObject(), version);
                    result.put(tornado.getId(), tornado);
                    loadedCount++;
                } catch (Exception e) {
                    errorCount++;
                    EASAddon.LOGGER.error("Failed to parse tornado data entry: {}", e.getMessage());
                }
            }
            
            EASAddon.LOGGER.info("Loaded {} tornado records for dimension {} from {} (errors: {})", 
                               loadedCount, dimensionKey, dataFile, errorCount);
            
            // FIXED: Validate loaded data integrity
            if (root.has("totalTornadoes")) {
                int expectedCount = root.get("totalTornadoes").getAsInt();
                if (loadedCount != expectedCount) {
                    EASAddon.LOGGER.warn("Data integrity issue: expected {} tornadoes, loaded {}", expectedCount, loadedCount);
                }
            }
            
        } catch (Exception e) {
            EASAddon.LOGGER.error("Failed to load tornado data: {}", e.getMessage(), e);
            
            // FIXED: Try backup file as last resort
            try {
                Path worldDir = getWorldSaveDirectory();
                if (worldDir != null) {
                    Path modDir = worldDir.resolve(TORNADO_DATA_FOLDER);
                    String fileName = dimensionKey.replace(":", "_").replace("/", "_") + "_" + TORNADO_DATA_FILE;
                    Path backupFile = modDir.resolve(fileName + ".bak");
                    
                    if (Files.exists(backupFile)) {
                        EASAddon.LOGGER.info("Attempting to recover from backup file...");
                        return loadTornadoDataFromFile(backupFile, dimensionKey);
                    }
                }
            } catch (Exception backupError) {
                EASAddon.LOGGER.error("Backup recovery also failed: {}", backupError.getMessage());
            }
        }
        
        return result;
    }
    
    // FIXED: Helper method for loading from specific file
    private static Map<Long, TornadoData> loadTornadoDataFromFile(Path dataFile, String dimensionKey) {
        Map<Long, TornadoData> result = new HashMap<>();
        
        try {
            String jsonContent = Files.readString(dataFile);
            JsonObject root = JsonParser.parseString(jsonContent).getAsJsonObject();
            
            int version = root.has("version") ? root.get("version").getAsInt() : 1;
            JsonArray tornadoArray = root.getAsJsonArray("tornadoes");
            
            for (JsonElement element : tornadoArray) {
                try {
                    TornadoData tornado = tornadoFromJson(element.getAsJsonObject(), version);
                    result.put(tornado.getId(), tornado);
                } catch (Exception e) {
                    EASAddon.LOGGER.error("Failed to parse tornado entry during backup recovery: {}", e.getMessage());
                }
            }
            
            EASAddon.LOGGER.info("Recovered {} tornado records from backup", result.size());
        } catch (Exception e) {
            EASAddon.LOGGER.error("Backup file recovery failed: {}", e.getMessage());
        }
        
        return result;
    }
    
// Replace the getWorldSaveDirectory method in ClientTornadoPersistence.java

private static Path getWorldSaveDirectory() {
    try {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return null;
        }
        
        // Get the save directory from the integrated server (singleplayer)
        if (mc.getSingleplayerServer() != null) {
            Path worldPath = mc.getSingleplayerServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT);
            EASAddon.LOGGER.info("Using singleplayer world path: {}", worldPath);
            return worldPath;
        }
        
        // For multiplayer, create a unique identifier that includes more than just IP
        Path gameDir = mc.gameDirectory.toPath();
        
        // FIXED: Create comprehensive server identifier
        String serverIdentifier = "unknown_server";
        if (mc.getCurrentServer() != null) {
            String serverIP = mc.getCurrentServer().ip;
            String serverName = mc.getCurrentServer().name;
            
            // Create hash of server info to ensure uniqueness while keeping paths reasonable
            String combinedInfo = serverIP + "|" + serverName + "|" + System.currentTimeMillis() / (1000 * 60 * 60); // Hour precision
            int serverHash = combinedInfo.hashCode();
            
            // FIXED: Include both readable info and hash for uniqueness
            String cleanIP = serverIP.replace(":", "_").replace(".", "_");
            String cleanName = serverName.replaceAll("[^a-zA-Z0-9_-]", "_");
            serverIdentifier = cleanName + "_" + cleanIP + "_" + Math.abs(serverHash);
            
            EASAddon.LOGGER.info("Using multiplayer server identifier: {} (from IP: {}, Name: {})", 
                serverIdentifier, serverIP, serverName);
        }
        
        Path serverDir = gameDir.resolve("easaddon_servers").resolve(serverIdentifier);
        
        // ADDITIONAL: Add level/world identifier for servers with multiple worlds
        String levelName = "world";
        if (mc.level != null && mc.level.getLevelData() != null) {
            try {
                // Try to get a unique identifier for this specific world
                String dimensionLocation = mc.level.dimension().location().toString();
                levelName = dimensionLocation.replace(":", "_").replace("/", "_");
            } catch (Exception e) {
                // Fallback to default
                levelName = "world";
            }
        }
        
        Path finalPath = serverDir.resolve(levelName);
        EASAddon.LOGGER.info("Final multiplayer world path: {}", finalPath);
        return finalPath;
        
    } catch (Exception e) {
        EASAddon.LOGGER.error("Failed to get world save directory: {}", e.getMessage());
        return null;
    }
}

// ADDED: Method to get a unique world identifier for current session
public static String getCurrentWorldIdentifier() {
    try {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return "no_world";
        }
        
        // For singleplayer
        if (mc.getSingleplayerServer() != null) {
            try {
                Path worldPath = mc.getSingleplayerServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT);
                return "singleplayer_" + worldPath.getFileName().toString();
            } catch (Exception e) {
                return "singleplayer_unknown";
            }
        }
        
        // For multiplayer
        if (mc.getCurrentServer() != null) {
            String serverIP = mc.getCurrentServer().ip;
            String serverName = mc.getCurrentServer().name;
            String dimensionLocation = mc.level.dimension().location().toString();
            
            // Create a consistent identifier
            String combined = serverName + "|" + serverIP + "|" + dimensionLocation;
            return "multiplayer_" + Math.abs(combined.hashCode());
        }
        
        return "unknown_world";
    } catch (Exception e) {
        return "error_world";
    }
}

// ADDED: Method to detect if we've switched to a completely different world
public static boolean isNewWorldSession(String previousWorldId, String currentWorldId) {
    if (previousWorldId == null || currentWorldId == null) {
        return true;
    }
    return !previousWorldId.equals(currentWorldId);
}
    
    private static JsonObject tornadoToJson(TornadoData tornado) {
        JsonObject obj = new JsonObject();
        
        obj.addProperty("id", tornado.getId());
        obj.addProperty("active", tornado.isActive());
        obj.addProperty("maxWindspeed", tornado.getMaxWindspeed());
        obj.addProperty("maxWidth", tornado.getMaxWidth());
        obj.addProperty("rating", tornado.getRating());
        obj.addProperty("firstSeenTime", tornado.getFirstSeenTime());
        obj.addProperty("lastSeenTime", tornado.getLastSeenTime());
        obj.addProperty("hasRecordedData", tornado.hasRecordedData());
        
        // FIXED: Enhanced survey data storage
        obj.addProperty("surveyed", tornado.isSurveyed());
        if (tornado.getSurveyedBy() != null) {
            obj.addProperty("surveyedBy", tornado.getSurveyedBy());
        }
        obj.addProperty("surveyTime", tornado.getSurveyTime());
        obj.addProperty("surveyedEFRating", tornado.getSurveyedEFRating());
        obj.addProperty("surveyedMaxWindspeed", tornado.getSurveyedMaxWindspeed());
        
        // Position history
        JsonArray positionArray = new JsonArray();
        for (TornadoData.PositionRecord record : tornado.getPositionHistory()) {
            JsonObject posObj = new JsonObject();
            posObj.addProperty("x", record.position.x);
            posObj.addProperty("y", record.position.y);
            posObj.addProperty("z", record.position.z);
            posObj.addProperty("timestamp", record.timestamp);
            posObj.addProperty("windspeed", record.windspeed);
            posObj.addProperty("width", record.width);
            positionArray.add(posObj);
        }
        obj.add("positionHistory", positionArray);
        
        // Damaged chunks
        JsonArray chunkArray = new JsonArray();
        for (ChunkPos chunkPos : tornado.getDamagedChunks()) {
            JsonObject chunkObj = new JsonObject();
            chunkObj.addProperty("x", chunkPos.x);
            chunkObj.addProperty("z", chunkPos.z);
            chunkArray.add(chunkObj);
        }
        obj.add("damagedChunks", chunkArray);
        
        return obj;
    }
    
    private static TornadoData tornadoFromJson(JsonObject obj, int version) {
        long id = obj.get("id").getAsLong();
        TornadoData tornado = new TornadoData(id);
        
        tornado.setActive(obj.get("active").getAsBoolean());
        tornado.setMaxWindspeed(obj.get("maxWindspeed").getAsInt());
        tornado.setMaxWidth(obj.get("maxWidth").getAsFloat());
        
        // FIXED: Handle rating carefully - only set if surveyed
        String savedRating = obj.get("rating").getAsString();
        boolean isSurveyed = obj.has("surveyed") && obj.get("surveyed").getAsBoolean();
        
        tornado.setFirstSeenTime(obj.get("firstSeenTime").getAsLong());
        tornado.setLastSeenTime(obj.get("lastSeenTime").getAsLong());
        tornado.setHasRecordedData(obj.get("hasRecordedData").getAsBoolean());
        
        // FIXED: Load survey data first, then rating
        if (version >= 3 && obj.has("surveyed")) {
            // Load full survey data
            tornado.setSurveyed(isSurveyed);
            if (obj.has("surveyedBy") && !obj.get("surveyedBy").isJsonNull()) {
                tornado.setSurveyedBy(obj.get("surveyedBy").getAsString());
            }
            if (obj.has("surveyTime")) {
                tornado.setSurveyTime(obj.get("surveyTime").getAsLong());
            }
            if (obj.has("surveyedEFRating")) {
                tornado.setSurveyedEFRating(obj.get("surveyedEFRating").getAsInt());
            }
            if (obj.has("surveyedMaxWindspeed")) {
                tornado.setSurveyedMaxWindspeed(obj.get("surveyedMaxWindspeed").getAsFloat());
            }
        }
        
        // FIXED: Only set rating after survey data is loaded
        if (isSurveyed) {
            tornado.setRating(savedRating); // Will be allowed since tornado is marked as surveyed
        } else {
            // Force EFU for non-surveyed tornadoes regardless of saved rating
            tornado.setRating("EFU");
        }
        
        // Position history
        if (obj.has("positionHistory")) {
            JsonArray positionArray = obj.getAsJsonArray("positionHistory");
            for (JsonElement element : positionArray) {
                JsonObject posObj = element.getAsJsonObject();
                Vec3 position = new Vec3(
                    posObj.get("x").getAsDouble(),
                    posObj.get("y").getAsDouble(),
                    posObj.get("z").getAsDouble()
                );
                long timestamp = posObj.get("timestamp").getAsLong();
                int windspeed = posObj.get("windspeed").getAsInt();
                float width = posObj.get("width").getAsFloat();
                
                tornado.addPositionRecord(new TornadoData.PositionRecord(position, timestamp, windspeed, width));
            }
        }
        
        // Damaged chunks
        if (obj.has("damagedChunks")) {
            JsonArray chunkArray = obj.getAsJsonArray("damagedChunks");
            for (JsonElement element : chunkArray) {
                JsonObject chunkObj = element.getAsJsonObject();
                ChunkPos chunkPos = new ChunkPos(
                    chunkObj.get("x").getAsInt(),
                    chunkObj.get("z").getAsInt()
                );
                tornado.addDamagedChunk(chunkPos);
            }
        }
        
        return tornado;
    }
}