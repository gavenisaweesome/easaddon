/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.arguments.ArgumentType
 *  com.mojang.brigadier.arguments.IntegerArgumentType
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  net.minecraft.commands.CommandBuildContext
 *  net.minecraft.commands.CommandSourceStack
 *  net.minecraft.commands.Commands
 *  net.minecraft.commands.arguments.item.ItemArgument
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.chat.Component
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.item.BlockItem
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.levelgen.Heightmap$Types
 *  net.minecraft.world.phys.Vec3
 */
package dev.protomanly.pmweather.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.protomanly.pmweather.PMWeather;
import dev.protomanly.pmweather.config.ServerConfig;
import dev.protomanly.pmweather.event.GameBusEvents;
import dev.protomanly.pmweather.weather.Sounding;
import dev.protomanly.pmweather.weather.Storm;
import dev.protomanly.pmweather.weather.ThermodynamicEngine;
import dev.protomanly.pmweather.weather.WeatherHandlerServer;
import dev.protomanly.pmweather.weather.WindEngine;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

public class WeatherCommands {
    public WeatherCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        dispatcher.register((LiteralArgumentBuilder)Commands.literal((String)"pmweather").then(((LiteralArgumentBuilder)Commands.literal((String)"atmosphere").requires(plr -> plr.hasPermission(2))).then(Commands.literal((String)"sample").then(Commands.literal((String)"column").executes(this::sampleColumn)))));
        dispatcher.register((LiteralArgumentBuilder)Commands.literal((String)"pmweather").then(((LiteralArgumentBuilder)Commands.literal((String)"spawn").requires(plr -> plr.hasPermission(2))).then(Commands.literal((String)"tornado").then(Commands.argument((String)"windspeed", (ArgumentType)IntegerArgumentType.integer((int)0, (int)400)).then(Commands.argument((String)"width", (ArgumentType)IntegerArgumentType.integer((int)5, (int)1000)).executes(this::newTornado))))));
        dispatcher.register((LiteralArgumentBuilder)Commands.literal((String)"pmweather").then(((LiteralArgumentBuilder)Commands.literal((String)"spawn").requires(plr -> plr.hasPermission(2))).then(Commands.literal((String)"tornado").then(Commands.literal((String)"buildto").then(Commands.argument((String)"fromStage", (ArgumentType)IntegerArgumentType.integer((int)0, (int)2)).then(Commands.argument((String)"fromEnergy", (ArgumentType)IntegerArgumentType.integer((int)0, (int)99)).then(Commands.argument((String)"windspeed", (ArgumentType)IntegerArgumentType.integer((int)0, (int)400)).then(Commands.argument((String)"width", (ArgumentType)IntegerArgumentType.integer((int)5, (int)1000)).executes(this::buildTornado)))))))));
        dispatcher.register((LiteralArgumentBuilder)Commands.literal((String)"pmweather").then(((LiteralArgumentBuilder)Commands.literal((String)"strike").requires(plr -> plr.hasPermission(2))).executes(this::strike)));
        dispatcher.register((LiteralArgumentBuilder)Commands.literal((String)"pmweather").then(((LiteralArgumentBuilder)Commands.literal((String)"spawn").requires(plr -> plr.hasPermission(2))).then(Commands.literal((String)"supercell").then(Commands.argument((String)"stage", (ArgumentType)IntegerArgumentType.integer((int)0, (int)2)).then(Commands.argument((String)"energy", (ArgumentType)IntegerArgumentType.integer((int)1, (int)99)).executes(this::newStorm))))));
        dispatcher.register((LiteralArgumentBuilder)Commands.literal((String)"pmweather").then(((LiteralArgumentBuilder)Commands.literal((String)"spawn").requires(plr -> plr.hasPermission(2))).then(Commands.literal((String)"supercell").then(Commands.literal((String)"buildto").then(Commands.argument((String)"stage", (ArgumentType)IntegerArgumentType.integer((int)0, (int)2)).then(Commands.argument((String)"energy", (ArgumentType)IntegerArgumentType.integer((int)1, (int)99)).executes(this::buildStorm)))))));
        dispatcher.register((LiteralArgumentBuilder)Commands.literal((String)"pmweather").then(((LiteralArgumentBuilder)Commands.literal((String)"spawn").requires(plr -> plr.hasPermission(2))).then(Commands.literal((String)"squall").then(Commands.argument((String)"stage", (ArgumentType)IntegerArgumentType.integer((int)0, (int)3)).then(Commands.argument((String)"energy", (ArgumentType)IntegerArgumentType.integer((int)1, (int)99)).executes(this::newSquall))))));
        dispatcher.register((LiteralArgumentBuilder)Commands.literal((String)"pmweather").then(((LiteralArgumentBuilder)Commands.literal((String)"spawn").requires(plr -> plr.hasPermission(2))).then(Commands.literal((String)"squall").then(Commands.literal((String)"buildto").then(Commands.argument((String)"stage", (ArgumentType)IntegerArgumentType.integer((int)0, (int)3)).then(Commands.argument((String)"energy", (ArgumentType)IntegerArgumentType.integer((int)1, (int)99)).executes(this::buildSquall)))))));
        dispatcher.register((LiteralArgumentBuilder)Commands.literal((String)"pmweather").then(((LiteralArgumentBuilder)Commands.literal((String)"spawn").requires(plr -> plr.hasPermission(2))).then(Commands.literal((String)"supercell").then(Commands.literal((String)"natural").executes(this::naturalStorm)))));
        dispatcher.register((LiteralArgumentBuilder)Commands.literal((String)"pmweather").then(((LiteralArgumentBuilder)Commands.literal((String)"spawn").requires(plr -> plr.hasPermission(2))).then(Commands.literal((String)"squall").then(Commands.literal((String)"natural").executes(this::naturalSquall)))));
        dispatcher.register((LiteralArgumentBuilder)Commands.literal((String)"pmweather").then(((LiteralArgumentBuilder)Commands.literal((String)"clear").requires(plr -> plr.hasPermission(2))).then(Commands.literal((String)"all").executes(this::clearAll))));
        dispatcher.register((LiteralArgumentBuilder)Commands.literal((String)"pmweather").then(((LiteralArgumentBuilder)Commands.literal((String)"aimtoplayer").requires(plr -> plr.hasPermission(2))).then(Commands.literal((String)"all").executes(this::aimToPlayer))));
        dispatcher.register((LiteralArgumentBuilder)Commands.literal((String)"pmweather").then(Commands.literal((String)"blockstrength").then(Commands.argument((String)"block", (ArgumentType)ItemArgument.item((CommandBuildContext)context)).executes(this::blockStrength))));
    }

    private int sampleColumn(CommandContext<CommandSourceStack> context) {
        ServerLevel level = ((CommandSourceStack)context.getSource()).getLevel();
        WeatherHandlerServer weatherHandlerServer = (WeatherHandlerServer)GameBusEvents.MANAGERS.get(level.dimension());
        ServerPlayer player = ((CommandSourceStack)context.getSource()).getPlayer();
        if (player != null) {
            Sounding sounding = new Sounding(weatherHandlerServer, player.position(), (Level)level, 1000, 16000);
            player.sendSystemMessage((Component)Component.literal((String)sounding.toString()));
            return 1;
        }
        return -1;
    }

    private int blockStrength(CommandContext<CommandSourceStack> context) {
        Item item = ItemArgument.getItem(context, (String)"block").getItem();
        if (item instanceof BlockItem) {
            BlockItem blockItem = (BlockItem)item;
            Block block = blockItem.getBlock();
            float strength = ServerConfig.blockStrengths.containsKey(block) ? ServerConfig.blockStrengths.get(block).floatValue() : Storm.getBlockStrength(block, (Level)((CommandSourceStack)context.getSource()).getLevel(), null);
            if (((CommandSourceStack)context.getSource()).isPlayer()) {
                ((CommandSourceStack)context.getSource()).getPlayer().sendSystemMessage((Component)Component.literal((String)String.format("%s Strength: Damaged at %s MPH", block.getName().getString(), Math.floor(strength))));
            }
            return 1;
        }
        ((CommandSourceStack)context.getSource()).sendFailure((Component)Component.literal((String)"Failed to get block from item, is item not a block?"));
        return -1;
    }

    private int aimToPlayer(CommandContext<CommandSourceStack> context) {
        ServerLevel level = ((CommandSourceStack)context.getSource()).getLevel();
        WeatherHandlerServer weatherHandlerServer = (WeatherHandlerServer)GameBusEvents.MANAGERS.get(level.dimension());
        for (Storm storm : weatherHandlerServer.getStorms()) {
            storm.aimAtPlayer();
        }
        ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal((String)"Successfully aimed all storms and clouds at players"), true);
        return 1;
    }

    private int clearAll(CommandContext<CommandSourceStack> context) {
        ServerLevel level = ((CommandSourceStack)context.getSource()).getLevel();
        WeatherHandlerServer weatherHandlerServer = (WeatherHandlerServer)GameBusEvents.MANAGERS.get(level.dimension());
        weatherHandlerServer.clearAllStorms();
        ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal((String)"Successfully cleared all storms"), true);
        return 1;
    }

    private int buildTornado(CommandContext<CommandSourceStack> context) {
        ServerLevel level = ((CommandSourceStack)context.getSource()).getLevel();
        ServerPlayer player = ((CommandSourceStack)context.getSource()).getPlayer();
        int windspeed = IntegerArgumentType.getInteger(context, (String)"windspeed");
        int width = IntegerArgumentType.getInteger(context, (String)"width");
        int fromStage = IntegerArgumentType.getInteger(context, (String)"fromStage");
        int fromEnergy = IntegerArgumentType.getInteger(context, (String)"fromEnergy");
        WeatherHandlerServer weatherHandlerServer = (WeatherHandlerServer)GameBusEvents.MANAGERS.get(level.dimension());
        Storm storm = new Storm(weatherHandlerServer, (Level)level, null, 0);
        storm.width = 15.0f;
        storm.windspeed = 0;
        storm.stormType = 0;
        storm.stage = fromStage;
        storm.position = player.position();
        storm.velocity = Vec3.ZERO;
        storm.energy = fromEnergy;
        storm.initFirstTime();
        storm.maxStage = 3;
        storm.maxProgress = 100;
        storm.maxWindspeed = windspeed;
        storm.maxWidth = width;
        weatherHandlerServer.addStorm(storm);
        weatherHandlerServer.syncStormNew(storm);
        ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal((String)"Successfully spawned storm"), true);
        return 1;
    }

    private int buildStorm(CommandContext<CommandSourceStack> context) {
        ServerLevel level = ((CommandSourceStack)context.getSource()).getLevel();
        ServerPlayer player = ((CommandSourceStack)context.getSource()).getPlayer();
        int fromStage = IntegerArgumentType.getInteger(context, (String)"stage");
        int fromEnergy = IntegerArgumentType.getInteger(context, (String)"energy");
        WeatherHandlerServer weatherHandlerServer = (WeatherHandlerServer)GameBusEvents.MANAGERS.get(level.dimension());
        Vec3 pos = new Vec3(player.getX(), (double)level.getMaxBuildHeight(), player.getZ()).add((double)PMWeather.RANDOM.nextInt(-ServerConfig.spawnRange, ServerConfig.spawnRange + 1), 0.0, (double)PMWeather.RANDOM.nextInt(-ServerConfig.spawnRange, ServerConfig.spawnRange + 1));
        Storm storm = new Storm(weatherHandlerServer, (Level)level, null, 0);
        storm.width = 0.0f;
        storm.windspeed = 0;
        storm.stormType = 0;
        storm.stage = 0;
        Vec3 wind = WindEngine.getWind(new Vec3(player.getX(), (double)(level.getMaxBuildHeight() + 1), player.getZ()), (Level)level, true, true, false);
        float dist = PMWeather.RANDOM.nextFloat(256.0f, 512.0f) * 4.0f;
        storm.position = pos = pos.add(wind.normalize().multiply((double)(-dist), 0.0, (double)(-dist)));
        storm.velocity = Vec3.ZERO;
        storm.energy = 0;
        storm.maxStage = fromStage;
        storm.maxProgress = fromEnergy;
        storm.initFirstTime();
        weatherHandlerServer.addStorm(storm);
        weatherHandlerServer.syncStormNew(storm);
        ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal((String)("Successfully spawned storm:\nMax Stage: " + storm.maxStage + " Max Energy: " + storm.maxProgress + " Max Windspeed: " + storm.maxWindspeed + " Max Width: " + storm.maxWidth)), true);
        return 1;
    }

    private int buildSquall(CommandContext<CommandSourceStack> context) {
        ServerLevel level = ((CommandSourceStack)context.getSource()).getLevel();
        ServerPlayer player = ((CommandSourceStack)context.getSource()).getPlayer();
        int fromStage = IntegerArgumentType.getInteger(context, (String)"stage");
        int fromEnergy = IntegerArgumentType.getInteger(context, (String)"energy");
        WeatherHandlerServer weatherHandlerServer = (WeatherHandlerServer)GameBusEvents.MANAGERS.get(level.dimension());
        Vec3 pos = new Vec3(player.getX(), (double)level.getMaxBuildHeight(), player.getZ()).add((double)PMWeather.RANDOM.nextInt(-ServerConfig.spawnRange, ServerConfig.spawnRange + 1), 0.0, (double)PMWeather.RANDOM.nextInt(-ServerConfig.spawnRange, ServerConfig.spawnRange + 1));
        Storm storm = new Storm(weatherHandlerServer, (Level)level, null, 1);
        storm.width = 0.0f;
        storm.windspeed = 0;
        storm.stage = 0;
        Vec3 wind = WindEngine.getWind(new Vec3(player.getX(), (double)(level.getMaxBuildHeight() + 1), player.getZ()), (Level)level, true, true, false);
        if (wind.length() < 10.0) {
            wind = wind.normalize().multiply(10.0, 0.0, 10.0);
        }
        float dist = PMWeather.RANDOM.nextFloat(256.0f, 512.0f) * 6.0f;
        storm.position = pos = pos.add(wind.normalize().multiply((double)(-dist), 0.0, (double)(-dist)));
        storm.velocity = wind.multiply(0.1, 0.0, 0.1);
        storm.energy = 0;
        storm.stormType = 1;
        storm.maxStage = fromStage;
        storm.maxProgress = fromEnergy;
        storm.initFirstTime();
        weatherHandlerServer.addStorm(storm);
        weatherHandlerServer.syncStormNew(storm);
        ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal((String)("Successfully spawned storm:\nMax Stage: " + storm.maxStage + " Max Energy: " + storm.maxProgress)), true);
        return 1;
    }

    private int newTornado(CommandContext<CommandSourceStack> context) {
        ServerLevel level = ((CommandSourceStack)context.getSource()).getLevel();
        ServerPlayer player = ((CommandSourceStack)context.getSource()).getPlayer();
        int windspeed = IntegerArgumentType.getInteger(context, (String)"windspeed");
        int width = IntegerArgumentType.getInteger(context, (String)"width");
        WeatherHandlerServer weatherHandlerServer = (WeatherHandlerServer)GameBusEvents.MANAGERS.get(level.dimension());
        Storm storm = new Storm(weatherHandlerServer, (Level)level, null, 0);
        storm.width = width;
        storm.windspeed = windspeed;
        storm.stormType = 0;
        storm.stage = 3;
        storm.position = player.position();
        storm.velocity = Vec3.ZERO;
        storm.energy = 0;
        storm.initFirstTime();
        storm.maxStage = Math.max(storm.maxStage, 3);
        storm.maxProgress = Math.max(storm.maxProgress, 100);
        storm.maxWindspeed = windspeed;
        storm.maxWidth = width;
        weatherHandlerServer.addStorm(storm);
        weatherHandlerServer.syncStormNew(storm);
        ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal((String)"Successfully spawned storm"), true);
        return 1;
    }

    private int newStorm(CommandContext<CommandSourceStack> context) {
        ServerLevel level = ((CommandSourceStack)context.getSource()).getLevel();
        ServerPlayer player = ((CommandSourceStack)context.getSource()).getPlayer();
        int stage = IntegerArgumentType.getInteger(context, (String)"stage");
        int energy = IntegerArgumentType.getInteger(context, (String)"energy");
        if (stage >= 0 && stage < 3) {
            if (energy >= 0 && energy <= 100) {
                WeatherHandlerServer weatherHandlerServer = (WeatherHandlerServer)GameBusEvents.MANAGERS.get(level.dimension());
                Storm storm = new Storm(weatherHandlerServer, (Level)level, null, 0);
                storm.width = 0.0f;
                storm.windspeed = 0;
                storm.stormType = 0;
                storm.stage = stage;
                storm.position = player.position();
                storm.velocity = Vec3.ZERO;
                storm.energy = energy;
                storm.initFirstTime();
                storm.maxStage = Math.max(storm.maxStage, stage);
                storm.maxProgress = Math.max(storm.maxProgress, energy);
                weatherHandlerServer.addStorm(storm);
                weatherHandlerServer.syncStormNew(storm);
                ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal((String)"Successfully spawned storm"), true);
                return 1;
            }
            ((CommandSourceStack)context.getSource()).sendFailure((Component)Component.literal((String)"energy must be within range 0-100"));
            return -1;
        }
        ((CommandSourceStack)context.getSource()).sendFailure((Component)Component.literal((String)"stage must be within range 0-2"));
        return -1;
    }

    private int newSquall(CommandContext<CommandSourceStack> context) {
        ServerLevel level = ((CommandSourceStack)context.getSource()).getLevel();
        ServerPlayer player = ((CommandSourceStack)context.getSource()).getPlayer();
        int stage = IntegerArgumentType.getInteger(context, (String)"stage");
        int energy = IntegerArgumentType.getInteger(context, (String)"energy");
        if (stage >= 0 && stage <= 3) {
            if (energy >= 0 && energy <= 100) {
                WeatherHandlerServer weatherHandlerServer = (WeatherHandlerServer)GameBusEvents.MANAGERS.get(level.dimension());
                Storm storm = new Storm(weatherHandlerServer, (Level)level, null, 1);
                Vec3 wind = WindEngine.getWind(new Vec3(player.getX(), (double)(level.getMaxBuildHeight() + 1), player.getZ()), (Level)level, true, true, false);
                if (wind.length() < 10.0) {
                    wind = wind.normalize().multiply(10.0, 0.0, 10.0);
                }
                storm.width = 0.0f;
                storm.windspeed = 0;
                storm.stormType = 1;
                storm.stage = stage;
                storm.position = player.position();
                storm.velocity = wind.multiply(0.1, 0.0, 0.1);
                storm.energy = energy;
                storm.initFirstTime();
                storm.maxStage = Math.max(storm.maxStage, stage);
                storm.maxProgress = Math.max(storm.maxProgress, energy);
                storm.coldEnergy = stage * 100 + energy;
                weatherHandlerServer.addStorm(storm);
                weatherHandlerServer.syncStormNew(storm);
                ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal((String)"Successfully spawned storm"), true);
                return 1;
            }
            ((CommandSourceStack)context.getSource()).sendFailure((Component)Component.literal((String)"energy must be within range 0-100"));
            return -1;
        }
        ((CommandSourceStack)context.getSource()).sendFailure((Component)Component.literal((String)"stage must be within range 0-3"));
        return -1;
    }

    private int naturalStorm(CommandContext<CommandSourceStack> context) {
        ServerLevel level = ((CommandSourceStack)context.getSource()).getLevel();
        ServerPlayer player = ((CommandSourceStack)context.getSource()).getPlayer();
        WeatherHandlerServer weatherHandlerServer = (WeatherHandlerServer)GameBusEvents.MANAGERS.get(level.dimension());
        Vec3 pos = new Vec3(player.getX(), (double)level.getMaxBuildHeight(), player.getZ()).add((double)PMWeather.RANDOM.nextInt(-ServerConfig.spawnRange, ServerConfig.spawnRange + 1), 0.0, (double)PMWeather.RANDOM.nextInt(-ServerConfig.spawnRange, ServerConfig.spawnRange + 1));
        Vec3 sfcPos = weatherHandlerServer.getWorld().getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, player.blockPosition()).getCenter();
        Sounding sounding = new Sounding(weatherHandlerServer, sfcPos, (Level)level, 250, 16000);
        ThermodynamicEngine.AtmosphericDataPoint sfc = ThermodynamicEngine.samplePoint(weatherHandlerServer, sfcPos, (Level)level, null, 0);
        float riskV = sounding.getRisk(0);
        Storm storm = new Storm(weatherHandlerServer, (Level)level, Float.valueOf(riskV), 0);
        storm.width = 0.0f;
        storm.windspeed = 0;
        storm.stormType = 0;
        storm.stage = 0;
        Vec3 wind = WindEngine.getWind(new Vec3(player.getX(), (double)(level.getMaxBuildHeight() + 1), player.getZ()), (Level)level, true, true, false);
        float dist = PMWeather.RANDOM.nextFloat(256.0f, 512.0f) * 4.0f;
        pos = pos.add(wind.normalize().multiply((double)(-dist), 0.0, (double)(-dist)));
        if (ServerConfig.environmentSystem) {
            if (PMWeather.RANDOM.nextFloat() <= riskV * 2.5f) {
                storm.maxStage = Math.max(storm.maxStage, 1);
            }
            if (PMWeather.RANDOM.nextFloat() <= riskV * 2.0f) {
                storm.maxStage = Math.max(storm.maxStage, 2);
            }
            if (PMWeather.RANDOM.nextFloat() <= riskV * 1.5f) {
                storm.maxStage = Math.max(storm.maxStage, 3);
            }
            storm.recalc(Float.valueOf(riskV));
        }
        storm.position = pos;
        storm.velocity = Vec3.ZERO;
        storm.energy = 0;
        storm.initFirstTime();
        weatherHandlerServer.addStorm(storm);
        weatherHandlerServer.syncStormNew(storm);
        ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal((String)("Successfully spawned storm:\nMax Stage: " + storm.maxStage + " Max Energy: " + storm.maxProgress + " Max Windspeed: " + storm.maxWindspeed + " Max Width: " + storm.maxWidth)), true);
        return 1;
    }

    private int naturalSquall(CommandContext<CommandSourceStack> context) {
        ServerLevel level = ((CommandSourceStack)context.getSource()).getLevel();
        ServerPlayer player = ((CommandSourceStack)context.getSource()).getPlayer();
        WeatherHandlerServer weatherHandlerServer = (WeatherHandlerServer)GameBusEvents.MANAGERS.get(level.dimension());
        Vec3 pos = new Vec3(player.getX(), (double)level.getMaxBuildHeight(), player.getZ()).add((double)PMWeather.RANDOM.nextInt(-ServerConfig.spawnRange, ServerConfig.spawnRange + 1), 0.0, (double)PMWeather.RANDOM.nextInt(-ServerConfig.spawnRange, ServerConfig.spawnRange + 1));
        Vec3 sfcPos = weatherHandlerServer.getWorld().getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, player.blockPosition()).getCenter();
        Sounding sounding = new Sounding(weatherHandlerServer, sfcPos, (Level)level, 250, 16000);
        ThermodynamicEngine.AtmosphericDataPoint sfc = ThermodynamicEngine.samplePoint(weatherHandlerServer, sfcPos, (Level)level, null, 0);
        float riskV = sounding.getRisk(0);
        if (sfc.temperature() < 3.0f) {
            riskV += Math.clamp((sfc.temperature() - 3.0f) / -6.0f, 0.0f, 1.0f) * 0.25f;
        }
        Storm storm = new Storm(weatherHandlerServer, (Level)level, Float.valueOf(riskV), 1);
        storm.width = 0.0f;
        storm.windspeed = 0;
        storm.stage = 0;
        Vec3 wind = WindEngine.getWind(new Vec3(player.getX(), (double)(level.getMaxBuildHeight() + 1), player.getZ()), (Level)level, true, true, false);
        if (wind.length() < 10.0) {
            wind = wind.normalize().multiply(10.0, 0.0, 10.0);
        }
        float dist = PMWeather.RANDOM.nextFloat(256.0f, 512.0f) * 6.0f;
        pos = pos.add(wind.normalize().multiply((double)(-dist), 0.0, (double)(-dist)));
        if (ServerConfig.environmentSystem) {
            if (PMWeather.RANDOM.nextFloat() <= riskV * 2.5f) {
                storm.maxStage = Math.max(storm.maxStage, 1);
            }
            if (PMWeather.RANDOM.nextFloat() <= riskV * 2.0f) {
                storm.maxStage = Math.max(storm.maxStage, 2);
            }
            if (PMWeather.RANDOM.nextFloat() <= riskV * 1.5f) {
                storm.maxStage = Math.max(storm.maxStage, 3);
            }
            storm.recalc(Float.valueOf(riskV));
        }
        storm.position = pos;
        storm.velocity = wind.multiply(0.1, 0.0, 0.1);
        storm.energy = 0;
        storm.stormType = 1;
        storm.initFirstTime();
        weatherHandlerServer.addStorm(storm);
        weatherHandlerServer.syncStormNew(storm);
        ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal((String)("Successfully spawned storm:\nMax Stage: " + storm.maxStage + " Max Energy: " + storm.maxProgress)), true);
        return 1;
    }

    private int strike(CommandContext<CommandSourceStack> context) {
        ServerLevel level = ((CommandSourceStack)context.getSource()).getLevel();
        if (((CommandSourceStack)context.getSource()).isPlayer()) {
            ServerPlayer player = ((CommandSourceStack)context.getSource()).getPlayer();
            Vec3 lPos = player.position().add((double)(PMWeather.RANDOM.nextFloat((float)(-ServerConfig.stormSize), (float)ServerConfig.stormSize) / 2.0f), 0.0, (double)(PMWeather.RANDOM.nextFloat((float)(-ServerConfig.stormSize), (float)ServerConfig.stormSize) / 2.0f));
            int height = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos((int)lPos.x, (int)lPos.y, (int)lPos.z)).getY();
            ((WeatherHandlerServer)GameBusEvents.MANAGERS.get(level.dimension())).syncLightningNew(new Vec3(lPos.x, (double)height, lPos.z));
            return 1;
        }
        return 0;
    }
}

