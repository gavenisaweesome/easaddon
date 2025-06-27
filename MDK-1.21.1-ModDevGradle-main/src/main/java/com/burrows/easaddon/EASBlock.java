package com.burrows.easaddon;

import com.burrows.easaddon.EASBlockEntity;
import com.burrows.easaddon.RegistryHandler;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
// Add these imports to your EASBlock.java
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.Containers;
import java.util.List;
// NEW IMPORTS FOR DIRECTIONAL SUPPORT
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;

public class EASBlock extends Block implements EntityBlock {
    private static Class<?> tornadoSensorCls;
    private static Class<?> metarCls;
    
    // ADD THIS: Directional property
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    // Use BlockStateProperties.FACING if you want 6-directional (including up/down)

    public EASBlock(BlockBehaviour.Properties props) {
        super(props);
        // ADD THIS: Set default state with facing
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    // ADD THIS: Register the FACING property
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    // ADD THIS: Handle block placement to set facing direction
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        // Use context.getNearestLookingDirection().getOpposite() for 6-directional
    }

    // ADD THIS: Handle rotation
    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    // ADD THIS: Handle mirroring
    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EASBlockEntity(pos, state);
    }

    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type == RegistryHandler.EAS_BLOCK_ENTITY.get()) {

            return (lvl, pos, st, be) -> EASBlockEntity.tick(lvl, pos, st, (EASBlockEntity)be);
        }
        return null;
    }

    private String posToString(Level level, BlockState state) {
        return state.toString();
    }
    
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        List<ItemStack> drops = new java.util.ArrayList<>();
        drops.add(new ItemStack(RegistryHandler.EAS_BLOCK_ITEM.get()));
        return drops;
    }

    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        
        boolean tornadoPowered = false;
        boolean metarDetected = false;
        BooleanProperty POWERED = BlockStateProperties.POWERED;
        for (Direction dir : Direction.values()) {
            BlockPos adj = pos.relative(dir);
            BlockState st = level.getBlockState(adj);
            Block b = st.getBlock();
            if (tornadoSensorCls != null && tornadoSensorCls.isInstance(b)) {
                boolean pw = st.hasProperty(POWERED) && st.getValue(POWERED);
                tornadoPowered |= pw;
            }
            if (metarCls == null || !metarCls.isInstance(b)) continue;
            metarDetected = true;
        }
        
        boolean anyTornadoInRange = false;
        boolean anySupercellInRange = false;
        try {
            Class<?> gbe = Class.forName("dev.protomanly.pmweather.event.GameBusEvents");
            Field managersF = gbe.getField("MANAGERS");
            Map managers = (Map)managersF.get(null);
            Object handler = managers.get(level.dimension());
            if (handler != null) {
                Method getStorms = handler.getClass().getMethod("getStorms");
                List storms = (List)getStorms.invoke(handler);
                
                for (Object storm : storms) {
                    double dz;
                    double dy;
                    int type = storm.getClass().getField("stormType").getInt(storm);
                    int stage = storm.getClass().getField("stage").getInt(storm);
                    Vec3 stormPos = (Vec3)storm.getClass().getField("position").get(storm);
                    int windspeed = storm.getClass().getField("windspeed").getInt(storm);
                    double dx = stormPos.x - (double)pos.getX();
                    if (dx * dx + (dy = stormPos.y - (double)pos.getY()) * dy + (dz = stormPos.z - (double)pos.getZ()) * dz > 10000.0) continue;
                    if (type == 0) {
                        if (stage >= 3) {
                            anyTornadoInRange = true;
                            continue;
                        }
                        anySupercellInRange = true;
                        continue;
                    }
                    if (type != 1) continue;
                }
            }
        }
        catch (Exception e) {
        }
        
        if (!anyTornadoInRange && !tornadoPowered) {
            // Your logic here
        }
        if (!anySupercellInRange && metarDetected) {
            // Your logic here
        }
        if (tornadoPowered || anyTornadoInRange) {
            // Your logic here
        }
        if (metarDetected && anySupercellInRange) {
            // Your logic here
        }
        return InteractionResult.CONSUME;
    }

    static {
        try {
            tornadoSensorCls = Class.forName("dev.protomanly.pmweather.block.TornadoSensorBlock");
        }
        catch (ClassNotFoundException e) {
        }
        try {
            metarCls = Class.forName("dev.protomanly.pmweather.block.MetarBlock");
        }
        catch (ClassNotFoundException e) {
        }
    }
}