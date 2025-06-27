package com.burrows.easaddon;

import com.burrows.easaddon.survey.DamageSurveyManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import com.burrows.easaddon.client.DamageSurveyorScreen;
import net.minecraft.client.Minecraft;

public class DamageSurveyorItem extends Item {
    
    public DamageSurveyorItem(Properties properties) {
        super(properties);
    }
    
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        
        if (level.isClientSide()) {
            // Check if player is in an active survey
            DamageSurveyManager.SurveySession session = DamageSurveyManager.getInstance()
                .getActiveSurvey(player.getName().getString());
            
            if (session != null) {
                // Player is surveying - handle survey action at current position
                boolean success = DamageSurveyManager.getInstance().handleSurveyAction(player, player.blockPosition());
                return InteractionResultHolder.sidedSuccess(itemStack, true);
            } else {
                // Open GUI normally
                openDamageSurveyorGUI();
            }
        }
        
        return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide());
    }
    
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos();
        
        if (level.isClientSide() && player != null) {
            // Check if player is in an active survey
            DamageSurveyManager.SurveySession session = DamageSurveyManager.getInstance()
                .getActiveSurvey(player.getName().getString());
            
            if (session != null) {
                // Player is surveying - handle survey action at clicked position
                boolean success = DamageSurveyManager.getInstance().handleSurveyAction(player, pos);
                return InteractionResult.SUCCESS;
            }
        }
        
        return InteractionResult.PASS;
    }
    
    @OnlyIn(Dist.CLIENT)
    private void openDamageSurveyorGUI() {
        if (!EASAddon.isPMWeatherAvailable()) {
            // Show error message if PMWeather is not available
            Player player = Minecraft.getInstance().player;
            if (player != null) {
                player.sendSystemMessage(Component.literal("§cPMWeather mod not detected! Damage Surveyor requires PMWeather to function."));
            }
            return;
        }
        
        Minecraft.getInstance().setScreen(new DamageSurveyorScreen());
    }
}