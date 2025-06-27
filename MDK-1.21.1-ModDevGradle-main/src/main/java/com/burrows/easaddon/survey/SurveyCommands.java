package com.burrows.easaddon.survey;

import com.burrows.easaddon.EASAddon;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.burrows.easaddon.tornado.TornadoTracker;
import com.burrows.easaddon.tornado.TornadoData;
import java.util.Collection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

/**
 * Commands for managing damage surveys
 */
@OnlyIn(Dist.CLIENT)
public class SurveyCommands {
    
    @SubscribeEvent
    public static void registerCommands(RegisterClientCommandsEvent event) {
        EASAddon.LOGGER.info("Registering survey commands...");
        register(event.getDispatcher());
    }
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("survey")
        		
            .then(Commands.literal("quit")
                .executes(SurveyCommands::quitSurvey)
            )
            .then(Commands.literal("finish")
                .executes(SurveyCommands::finishSurvey)
            )
            .then(Commands.literal("status")
                .executes(SurveyCommands::surveyStatus)
            )
            .executes(SurveyCommands::surveyHelp)
        );
        EASAddon.LOGGER.info("Survey commands registered");
    }
    
    private static int quitSurvey(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (source.getEntity() instanceof Player player) {
            boolean success = DamageSurveyManager.getInstance().quitSurvey(player);
            if (!success) {
                player.sendSystemMessage(Component.literal("§cYou are not currently surveying a tornado"));
            }
        }
        return 1;
    }
    
    private static int finishSurvey(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (source.getEntity() instanceof Player player) {
            boolean success = DamageSurveyManager.getInstance().finishSurvey(player);
            if (!success) {
                // Error message already sent by the manager
            }
        }
        return 1;
    }
    
    private static int surveyStatus(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (source.getEntity() instanceof Player player) {
            DamageSurveyManager.SurveySession session = DamageSurveyManager.getInstance()
                .getActiveSurvey(player.getName().getString());
            
            if (session == null) {
                player.sendSystemMessage(Component.literal("§cNo active survey"));
                return 1;
            }
            
            player.sendSystemMessage(Component.literal("§6=== SURVEY STATUS ==="));
            player.sendSystemMessage(Component.literal("§eTornado ID: " + session.tornadoId));
            player.sendSystemMessage(Component.literal("§eProgress: " + session.surveyedChunks.size() + "/" + session.requiredSurveys + " required"));
            player.sendSystemMessage(Component.literal("§eTotal chunks: " + session.targetChunks.size()));
            player.sendSystemMessage(Component.literal("§eProgress: " + Math.round(session.getProgress() * 100) + "%"));
            
            if (session.canFinish) {
                player.sendSystemMessage(Component.literal("§a✓ Can finish survey"));
            } else {
                int remaining = session.requiredSurveys - session.surveyedChunks.size();
                player.sendSystemMessage(Component.literal("§7Need " + remaining + " more surveys"));
            }
            
            if (session.currentTargetChunk != null) {
                player.sendSystemMessage(Component.literal("§bNext target: Chunk (" + 
                    session.currentTargetChunk.x + ", " + session.currentTargetChunk.z + ")"));
            }
        }
        return 1;
    }
    
    private static int surveyHelp(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (source.getEntity() instanceof Player player) {
            player.sendSystemMessage(Component.literal("§6=== SURVEY COMMANDS ==="));
            player.sendSystemMessage(Component.literal("§e/survey status §7- Show current survey progress"));
            player.sendSystemMessage(Component.literal("§e/survey finish §7- Complete survey (if minimum met)"));
            player.sendSystemMessage(Component.literal("§e/survey quit §7- Cancel current survey"));
            player.sendSystemMessage(Component.literal("§b"));
            player.sendSystemMessage(Component.literal("§bTo survey: Right-click with surveyor tool in damaged chunks"));
        }
        return 1;
    }
}