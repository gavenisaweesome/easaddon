package com.burrows.easaddon;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

public class RadarOverlayBlock extends BaseEntityBlock {
    public RadarOverlayBlock(Properties props) {
        super(props);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RadarOverlayBlockEntity(pos, state);
    }

    /**
     * Pass through right-clicks to the radar block below
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        // Get the block below (should be the radar)
        BlockPos radarPos = pos.below();
        BlockState radarState = level.getBlockState(radarPos);
        Block radarBlock = radarState.getBlock();
        
        try {
            Class<?> radarCls = Class.forName("dev.protomanly.pmweather.block.RadarBlock");
            if (radarCls.isInstance(radarBlock)) {
                // Create a new BlockHitResult with the radar position
                BlockHitResult radarHit = new BlockHitResult(
                    hit.getLocation(), 
                    hit.getDirection(), 
                    radarPos, 
                    hit.isInside()
                );
                
                // Use reflection to call the protected useWithoutItem method
                java.lang.reflect.Method useMethod = radarCls.getDeclaredMethod(
                    "useWithoutItem", 
                    BlockState.class, 
                    Level.class, 
                    BlockPos.class, 
                    Player.class, 
                    BlockHitResult.class
                );
                useMethod.setAccessible(true);
                return (InteractionResult) useMethod.invoke(radarBlock, radarState, level, radarPos, player, radarHit);
            }
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | 
                 java.lang.reflect.InvocationTargetException e) {
            // PMWeather not installed or reflection failed
            org.apache.logging.log4j.LogManager.getLogger("EASAddon")
                .warn("EAS: Failed to pass interaction to radar block: {}", e.getMessage());
        }
        
        return InteractionResult.PASS;
    }

    /**
     * Check if the radar block below still exists, remove overlay if not
     */
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, 
                               net.minecraft.world.level.block.Block neighborBlock, 
                               BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        
        if (!level.isClientSide()) {
            checkRadarBelow(level, pos);
        }
    }

    /**
     * Also check on random tick to catch any missed removals
     */
    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, net.minecraft.util.RandomSource random) {
        super.tick(state, level, pos, random);
        checkRadarBelow(level, pos);
    }

    /**
     * Check if radar block exists below, remove overlay if not
     */
    private void checkRadarBelow(Level level, BlockPos overlayPos) {
        BlockPos radarPos = overlayPos.below();
        BlockState radarState = level.getBlockState(radarPos);
        
        boolean isRadarBlock = false;
        try {
            Class<?> radarCls = Class.forName("dev.protomanly.pmweather.block.RadarBlock");
            isRadarBlock = radarCls.isInstance(radarState.getBlock());
        } catch (ClassNotFoundException e) {
            // PMWeather not installed, assume no radar
        }
        
        if (!isRadarBlock) {
            // No radar block below, remove this overlay
            level.destroyBlock(overlayPos, false);
            
            // Clean up any polygons associated with this position
            AlertPolygonManager.clearPolygons(overlayPos);
            
            // Log for debugging
            org.apache.logging.log4j.LogManager.getLogger("EASAddon")
                .info("EAS: Removed radar overlay at {} - no radar block found below", overlayPos);
        }
    }

    /**
     * Required by BaseEntityBlock
     */
    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return MapCodec.unit(this);
    }
}