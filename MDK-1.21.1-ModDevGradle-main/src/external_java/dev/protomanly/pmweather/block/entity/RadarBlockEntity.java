/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Holder
 *  net.minecraft.core.Holder$Reference
 *  net.minecraft.core.Vec3i
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.NbtUtils
 *  net.minecraft.nbt.Tag
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.util.RandomSource
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.biome.Biome
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.levelgen.LegacyRandomSource
 *  net.minecraft.world.level.levelgen.synth.SimplexNoise
 */
package dev.protomanly.pmweather.block.entity;

import dev.protomanly.pmweather.PMWeather;
import dev.protomanly.pmweather.block.entity.ModBlockEntities;
import dev.protomanly.pmweather.event.GameBusClientEvents;
import dev.protomanly.pmweather.networking.ModNetworking;
import dev.protomanly.pmweather.weather.Storm;
import dev.protomanly.pmweather.weather.WeatherHandlerClient;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

public class RadarBlockEntity
extends BlockEntity {
    public List<Storm> storms = new ArrayList<Storm>();
    public int tickCount;
    public int updateCount;
    public SimplexNoise noise;
    public Map<String, Float> reflectivityMap = new HashMap<String, Float>();
    public Map<String, Float> temperatureMap = new HashMap<String, Float>();
    public Map<String, Float> velocityMap = new HashMap<String, Float>();
    public Map<String, Color> debugMap = new HashMap<String, Color>();
    public List<BiomeData> biomeData = new ArrayList<BiomeData>();
    public boolean init = false;
    public int lastUpdate = 0;
    public int ticksNoPacket = 0;
    public Map<BlockPos, Holder<Biome>> biomeCache = new HashMap<BlockPos, Holder<Biome>>();

    public RadarBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.RADAR_BE.get(), pos, blockState);
        this.noise = new SimplexNoise((RandomSource)new LegacyRandomSource(0L));
    }

    @Nullable
    public Holder<Biome> getNearestBiome(BlockPos pos) {
        double nearest = Double.MAX_VALUE;
        Holder<Biome> biome = null;
        if (this.biomeCache.containsKey(pos.atY(0))) {
            return this.biomeCache.get(pos.atY(0));
        }
        for (BiomeData bData : this.biomeData) {
            double dist = pos.distManhattan((Vec3i)bData.pos);
            if (dist < nearest) {
                nearest = dist;
                biome = bData.biome;
            }
            if (!(dist < 128.0)) continue;
            break;
        }
        if (biome != null) {
            this.biomeCache.put(pos.atY(0), biome);
        }
        return biome;
    }

    public void clientInit(Level level, CompoundTag data) {
        if (!this.init) {
            PMWeather.LOGGER.debug("Radar data received");
            this.init = true;
            CompoundTag list = data.getCompound("data");
            for (String key : list.getAllKeys()) {
                CompoundTag element = list.getCompound(key);
                BlockPos blockPos = (BlockPos)NbtUtils.readBlockPos((CompoundTag)element, (String)"blockPos").orElseThrow();
                Holder.Reference biome = level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(ResourceKey.create((ResourceKey)Registries.BIOME, (ResourceLocation)ResourceLocation.parse((String)element.getString("biome"))));
                this.biomeData.add(new BiomeData(blockPos, (Holder<Biome>)biome));
            }
        }
    }

    public void sync(@Nullable Player player, BlockPos blockPos) {
        if (!this.init) {
            return;
        }
        CompoundTag data = new CompoundTag();
        data.putString("packetCommand", "Radar");
        data.putString("command", "syncBiomes");
        CompoundTag map = new CompoundTag();
        int i = 0;
        for (BiomeData bData : this.biomeData) {
            CompoundTag element = new CompoundTag();
            element.put("blockPos", NbtUtils.writeBlockPos((BlockPos)bData.pos()));
            element.putString("biome", bData.biome.getRegisteredName());
            map.put(String.valueOf(i), (Tag)element);
            ++i;
        }
        data.put("data", (Tag)map);
        data.put("blockPos", NbtUtils.writeBlockPos((BlockPos)blockPos));
        if (player == null) {
            ModNetworking.serverSendToClientDimension(data, this.level);
        } else {
            ModNetworking.serverSendToClientPlayer(data, player);
        }
    }

    public static void playerRequestsSync(ServerPlayer player, BlockPos blockPos) {
        Level lvl = player.level();
        BlockEntity blockEntity = lvl.getBlockEntity(blockPos);
        if (blockEntity instanceof RadarBlockEntity) {
            RadarBlockEntity radarBlockEntity = (RadarBlockEntity)blockEntity;
            radarBlockEntity.sync((Player)player, blockPos);
        }
    }

    public void tick(Level level, BlockPos blockPos, BlockState blockState) {
        ++this.tickCount;
        if (level.isClientSide() && (level.getGameTime() % 100L == 0L || this.storms.isEmpty())) {
            WeatherHandlerClient weatherHandler = (WeatherHandlerClient)GameBusClientEvents.weatherHandler;
            if (weatherHandler == null) {
                return;
            }
            ++this.updateCount;
            this.storms = weatherHandler.getStorms();
        }
        if (!this.init) {
            if (!level.isClientSide()) {
                this.init = true;
                for (int x = -2048; x <= 2048; x += 64) {
                    for (int z = -2048; z <= 2048; z += 64) {
                        BlockPos pos = blockPos.offset(new Vec3i(x, 0, z));
                        Holder biome = level.getBiome(pos);
                        this.biomeData.add(new BiomeData(pos, (Holder<Biome>)biome));
                    }
                }
                this.sync(null, blockPos);
            } else {
                ++this.ticksNoPacket;
                if (this.ticksNoPacket > 40) {
                    PMWeather.LOGGER.debug("Requesting data from server for radar at {}", (Object)blockPos);
                    this.ticksNoPacket = 0;
                    CompoundTag data = new CompoundTag();
                    data.putString("packetCommand", "Radar");
                    data.putString("command", "syncBiomes");
                    data.put("blockPos", NbtUtils.writeBlockPos((BlockPos)blockPos));
                    ModNetworking.clientSendToSever(data);
                }
            }
        } else {
            this.ticksNoPacket = 0;
        }
    }

    public record BiomeData(BlockPos pos, Holder<Biome> biome) {
    }
}

