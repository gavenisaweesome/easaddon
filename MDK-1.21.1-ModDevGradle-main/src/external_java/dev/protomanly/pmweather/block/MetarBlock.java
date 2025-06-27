/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.chat.Component
 *  net.minecraft.world.InteractionResult
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.context.BlockPlaceContext
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.state.BlockBehaviour$Properties
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.StateDefinition$Builder
 *  net.minecraft.world.level.block.state.properties.BlockStateProperties
 *  net.minecraft.world.level.block.state.properties.DirectionProperty
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraft.world.phys.Vec3
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.api.distmarker.OnlyIn
 *  org.jetbrains.annotations.Nullable
 */
package dev.protomanly.pmweather.block;

import dev.protomanly.pmweather.config.ClientConfig;
import dev.protomanly.pmweather.event.GameBusEvents;
import dev.protomanly.pmweather.networking.ModNetworking;
import dev.protomanly.pmweather.util.Util;
import dev.protomanly.pmweather.weather.Sounding;
import dev.protomanly.pmweather.weather.ThermodynamicEngine;
import dev.protomanly.pmweather.weather.WeatherHandler;
import dev.protomanly.pmweather.weather.WindEngine;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

public class MetarBlock
extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private Map<UUID, Long> lastInteractions = new HashMap<UUID, Long>();

    protected MetarBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState((BlockState)this.defaultBlockState().setValue((Property)FACING, (Comparable)Direction.NORTH));
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(new Property[]{FACING});
    }

    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState blockstate = super.getStateForPlacement(context);
        if (blockstate == null) {
            blockstate = this.defaultBlockState();
        }
        return (BlockState)blockstate.setValue((Property)FACING, (Comparable)context.getHorizontalDirection().getOpposite());
    }

    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            UUID uuid = player.getUUID();
            long lastInteration = this.lastInteractions.getOrDefault(uuid, -10000L);
            long curTime = level.getGameTime();
            if (curTime - lastInteration > 40L) {
                float r;
                Sounding sounding;
                int i;
                this.lastInteractions.put(uuid, curTime);
                WeatherHandler weatherHandler = GameBusEvents.MANAGERS.get(level.dimension());
                Vec3 wind = WindEngine.getWind(pos, level);
                int windAngle = Math.floorMod((int)Math.toDegrees(Math.atan2(wind.x, -wind.z)), 360);
                double windspeed = wind.length();
                ThermodynamicEngine.AtmosphericDataPoint sfc = ThermodynamicEngine.samplePoint(weatherHandler, pos.getCenter(), level, null, 0);
                float riskV = 0.0f;
                float peakRiskOffset = 0.0f;
                float risk2 = 0.0f;
                float risk3 = 0.0f;
                for (i = 0; i < 24000; i += 200) {
                    sounding = new Sounding(weatherHandler, pos.getCenter(), level, 250, 16000, i);
                    r = sounding.getRisk(i);
                    if (!(r > riskV)) continue;
                    riskV = r;
                    peakRiskOffset = i;
                }
                for (i = 24000; i < 48000; i += 400) {
                    sounding = new Sounding(weatherHandler, pos.getCenter(), level, 250, 16000, i);
                    r = sounding.getRisk(i);
                    if (!(r > risk2)) continue;
                    risk2 = r;
                }
                for (i = 48000; i < 72000; i += 800) {
                    sounding = new Sounding(weatherHandler, pos.getCenter(), level, 250, 16000, i);
                    r = sounding.getRisk(i);
                    if (!(r > risk3)) continue;
                    risk3 = r;
                }
                float temperature = sfc.temperature();
                float dew = sfc.dewpoint();
                CompoundTag data = new CompoundTag();
                data.putString("packetCommand", "Metar");
                data.putString("command", "sendData");
                data.putFloat("temp", temperature);
                data.putFloat("dew", dew);
                data.putFloat("day1", riskV);
                data.putFloat("day2", risk2);
                data.putFloat("day3", risk3);
                data.putFloat("peakOffset", peakRiskOffset);
                data.putFloat("windAngle", (float)windAngle);
                data.putDouble("windspeed", windspeed);
                ModNetworking.serverSendToClientPlayer(data, player);
            }
        }
        return InteractionResult.SUCCESS_NO_ITEM_USED;
    }

    @OnlyIn(value=Dist.CLIENT)
    public static void sendMessage(CompoundTag data) {
        if (Minecraft.getInstance().player == null) {
            return;
        }
        String strForFormat = "Wind: %s\u00b0 @ %s MPH\nTemp: %s\u00b0F\nDew: %s\u00b0F\n0-24hr Risk: %s\n24-48hr Risk: %s\n48-72hr Risk: %s\n0-24hr Risk Peak: %s";
        double windspeed = data.getDouble("windspeed");
        float windAngle = data.getFloat("windAngle");
        float temperature = data.getFloat("temp");
        float dew = data.getFloat("dew");
        if (ClientConfig.metric) {
            strForFormat = "Wind: %s\u00b0 @ %s km/h\nTemp: %s\u00b0C\nDew: %s\u00b0C\n0-24hr Risk: %s\n24-48hr Risk: %s\n48-72hr Risk: %s\n0-24hr Risk Peak: %s";
            windspeed *= 1.609;
        } else {
            temperature = Util.celsiusToFahrenheit(temperature);
            dew = Util.celsiusToFahrenheit(dew);
        }
        float riskV = data.getFloat("day1");
        float risk2 = data.getFloat("day2");
        float risk3 = data.getFloat("day3");
        float peakRiskOffset = data.getFloat("peakOffset");
        String str = String.format(strForFormat, Float.valueOf(windAngle), (int)windspeed, (int)temperature, (int)dew, Util.riskToString(riskV), Util.riskToString(risk2), Util.riskToString(risk3), peakRiskOffset < 1200.0f ? "Now" : String.format("In %s minutes", (int)Math.floor(peakRiskOffset / 20.0f / 60.0f)));
        Minecraft.getInstance().player.sendSystemMessage((Component)Component.literal((String)str));
    }
}

