package com.burrows.easaddon.tornado;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import com.burrows.easaddon.EASAddon;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TornadoSavedData extends SavedData {
    private static final String DATA_NAME = "easaddon_tornado_data";
    
    private final Map<Long, TornadoData> tornadoData = new HashMap<>();
    
    public TornadoSavedData() {}
    
    public TornadoSavedData(CompoundTag tag, HolderLookup.Provider provider) {
        load(tag);
    }
    
    public static TornadoSavedData create() {
        return new TornadoSavedData();
    }
    
    public static Factory<TornadoSavedData> factory() {
        return new Factory<>(TornadoSavedData::create, TornadoSavedData::new);
    }
    
    public void load(CompoundTag tag) {
        tornadoData.clear();
        
        if (tag.contains("tornadoes", Tag.TAG_LIST)) {
            ListTag tornadoList = tag.getList("tornadoes", Tag.TAG_COMPOUND);
            
            for (int i = 0; i < tornadoList.size(); i++) {
                CompoundTag tornadoTag = tornadoList.getCompound(i);
                
                try {
                    long id = tornadoTag.getLong("id");
                    TornadoData tornado = new TornadoData(id);
                    
                    // Load basic data
                    tornado.setActive(tornadoTag.getBoolean("active"));
                    tornado.setMaxWindspeed(tornadoTag.getInt("maxWindspeed"));
                    tornado.setMaxWidth(tornadoTag.getFloat("maxWidth"));
                    tornado.setRating(tornadoTag.getString("rating"));
                    tornado.setFirstSeenTime(tornadoTag.getLong("firstSeenTime"));
                    tornado.setLastSeenTime(tornadoTag.getLong("lastSeenTime"));
                    tornado.setHasRecordedData(tornadoTag.getBoolean("hasRecordedData"));
                    
                    // Load position history
                    if (tornadoTag.contains("positionHistory", Tag.TAG_LIST)) {
                        ListTag positionList = tornadoTag.getList("positionHistory", Tag.TAG_COMPOUND);
                        for (int j = 0; j < positionList.size(); j++) {
                            CompoundTag posTag = positionList.getCompound(j);
                            Vec3 position = new Vec3(
                                posTag.getDouble("x"),
                                posTag.getDouble("y"),
                                posTag.getDouble("z")
                            );
                            long timestamp = posTag.getLong("timestamp");
                            int windspeed = posTag.getInt("windspeed");
                            float width = posTag.getFloat("width");
                            
                            tornado.addPositionRecord(new TornadoData.PositionRecord(position, timestamp, windspeed, width));
                        }
                    }
                    
                    // Load damaged chunks
                    if (tornadoTag.contains("damagedChunks", Tag.TAG_LIST)) {
                        ListTag chunkList = tornadoTag.getList("damagedChunks", Tag.TAG_COMPOUND);
                        for (int j = 0; j < chunkList.size(); j++) {
                            CompoundTag chunkTag = chunkList.getCompound(j);
                            ChunkPos chunkPos = new ChunkPos(chunkTag.getInt("x"), chunkTag.getInt("z"));
                            tornado.addDamagedChunk(chunkPos);
                        }
                    }
                    
                    tornadoData.put(id, tornado);
                    
                } catch (Exception e) {
                    EASAddon.LOGGER.error("Failed to load tornado data for entry {}: {}", i, e.getMessage());
                }
            }
        }
        
        EASAddon.LOGGER.info("Loaded {} tornado records from world save", tornadoData.size());
    }
    
    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag tornadoList = new ListTag();
        
        for (TornadoData tornado : tornadoData.values()) {
            CompoundTag tornadoTag = new CompoundTag();
            
            // Save basic data
            tornadoTag.putLong("id", tornado.getId());
            tornadoTag.putBoolean("active", tornado.isActive());
            tornadoTag.putInt("maxWindspeed", tornado.getMaxWindspeed());
            tornadoTag.putFloat("maxWidth", tornado.getMaxWidth());
            tornadoTag.putString("rating", tornado.getRating());
            tornadoTag.putLong("firstSeenTime", tornado.getFirstSeenTime());
            tornadoTag.putLong("lastSeenTime", tornado.getLastSeenTime());
            tornadoTag.putBoolean("hasRecordedData", tornado.hasRecordedData());
            
            // Save position history
            ListTag positionList = new ListTag();
            for (TornadoData.PositionRecord record : tornado.getPositionHistory()) {
                CompoundTag posTag = new CompoundTag();
                posTag.putDouble("x", record.position.x);
                posTag.putDouble("y", record.position.y);
                posTag.putDouble("z", record.position.z);
                posTag.putLong("timestamp", record.timestamp);
                posTag.putInt("windspeed", record.windspeed);
                posTag.putFloat("width", record.width);
                positionList.add(posTag);
            }
            tornadoTag.put("positionHistory", positionList);
            
            // Save damaged chunks
            ListTag chunkList = new ListTag();
            for (ChunkPos chunkPos : tornado.getDamagedChunks()) {
                CompoundTag chunkTag = new CompoundTag();
                chunkTag.putInt("x", chunkPos.x);
                chunkTag.putInt("z", chunkPos.z);
                chunkList.add(chunkTag);
            }
            tornadoTag.put("damagedChunks", chunkList);
            
            tornadoList.add(tornadoTag);
        }
        
        tag.put("tornadoes", tornadoList);
        
        EASAddon.LOGGER.info("Saved {} tornado records to world save", tornadoData.size());
        return tag;
    }
    
    public Map<Long, TornadoData> getTornadoData() {
        return tornadoData;
    }
    
    public void putTornadoData(long id, TornadoData data) {
        tornadoData.put(id, data);
        setDirty();
    }
    
    public void removeTornadoData(long id) {
        tornadoData.remove(id);
        setDirty();
    }
    
    public void clearAllData() {
        tornadoData.clear();
        setDirty();
    }
    
    @Override
    public boolean isDirty() {
        return super.isDirty();
    }
}