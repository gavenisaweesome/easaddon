/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.util.Mth
 *  net.minecraft.world.level.ClipContext
 *  net.minecraft.world.level.ClipContext$Block
 *  net.minecraft.world.level.ClipContext$Fluid
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraft.world.phys.HitResult$Type
 *  net.minecraft.world.phys.Vec2
 *  net.minecraft.world.phys.Vec3
 *  net.minecraft.world.phys.shapes.CollisionContext
 */
package dev.protomanly.pmweather.util;

import dev.protomanly.pmweather.PMWeather;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

public class Util {
    public static Vec3[] RAIN_POSITIONS;
    public static int MAX_RAIN_DROPS;
    public static Map<Block, Block> STRIPPED_VARIANTS;
    public static float ROCP;

    public static void checkLogs(BlockState state, ServerLevel level, BlockPos pos) {
    }

    public static void checkLogs(BlockState state, ServerLevel level, BlockPos pos, int y) {
    }

    public static boolean canLogSurvive(BlockState state, ServerLevel level, BlockPos pos, List<BlockPos> checked) {
        return true;
    }

    public static boolean canWindAffect(Vec3 pos, Level level) {
        BlockHitResult upRay = level.clip(new ClipContext(pos.add(0.0, 0.55, 0.0), pos.add(0.0, 128.0, 0.0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
        BlockHitResult pxRay = level.clip(new ClipContext(pos.add(1.0, 0.55, 0.0), pos.add(64.0, 128.0, 0.0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
        BlockHitResult nxRay = level.clip(new ClipContext(pos.add(-1.0, 0.55, 0.0), pos.add(-64.0, 128.0, 0.0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
        BlockHitResult pzRay = level.clip(new ClipContext(pos.add(0.0, 0.55, 1.0), pos.add(0.0, 128.0, 64.0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
        BlockHitResult nzRay = level.clip(new ClipContext(pos.add(0.0, 0.55, -1.0), pos.add(0.0, 128.0, -64.0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
        return upRay.getType() == HitResult.Type.MISS || pxRay.getType() == HitResult.Type.MISS || nxRay.getType() == HitResult.Type.MISS || pzRay.getType() == HitResult.Type.MISS || nzRay.getType() == HitResult.Type.MISS;
    }

    public static Vec2 mulVec2(Vec2 a, Vec2 b) {
        return new Vec2(a.x * b.x, a.y * b.y);
    }

    public static Vec2 mulVec2(Vec2 a, float b) {
        return new Vec2(a.x * b, a.y * b);
    }

    public static Vec2 nearestPoint(Vec2 v, Vec2 w, Vec2 p) {
        float l2 = v.distanceToSqr(w);
        float t = Mth.clamp((float)(p.add(v.negated()).dot(w.add(v.negated())) / l2), (float)0.0f, (float)1.0f);
        return v.add(Util.mulVec2(w.add(v.negated()), t));
    }

    public static float minimumDistance(Vec2 v, Vec2 w, Vec2 p) {
        float l2 = v.distanceToSqr(w);
        if (l2 == 0.0f) {
            return Mth.sqrt((float)p.distanceToSqr(v));
        }
        Vec2 proj = Util.nearestPoint(v, w, p);
        return Mth.sqrt((float)p.distanceToSqr(proj));
    }

    public static boolean isInteger(String string) {
        try {
            Integer.parseInt(string);
            return true;
        }
        catch (NumberFormatException e) {
            return false;
        }
    }

    public static float celsiusToFahrenheit(float t) {
        return t * 1.8f + 32.0f;
    }

    public static float fahrenheitToCelsius(float t) {
        return (t - 32.0f) * 0.5555556f;
    }

    public static float celsiusToKelvin(float t) {
        return t + 273.15f;
    }

    public static float kelvinToCelsius(float t) {
        return t - 273.15f;
    }

    public static float MixingRatio(float vapprs, float prs, @Nullable Float molWeight) {
        if (molWeight == null) {
            molWeight = Float.valueOf(0.62197f);
        }
        return molWeight.floatValue() * (vapprs / (prs - vapprs));
    }

    public static float SaturationVaporPressure(float t) {
        return 6.112f * (float)Math.exp(17.67f * t / (t + 243.5f));
    }

    public static String riskToString(float riskV) {
        String risk = "NONE (0/6)";
        if (riskV > 1.5f) {
            risk = "HIGH (6/6)";
        } else if (riskV > 1.2f) {
            risk = "MDT (5/6)";
        } else if (riskV > 0.8f) {
            risk = "ENH (4/6)";
        } else if (riskV > 0.6f) {
            risk = "SLGT (3/6)";
        } else if (riskV > 0.3f) {
            risk = "MRGL (2/6)";
        } else if (riskV > 0.15f) {
            risk = "TSTM (1/6)";
        }
        return risk;
    }

    public static float SaturationMixingRatio(float tp, float t) {
        return Util.MixingRatio(Util.SaturationVaporPressure(t), tp, null);
    }

    static {
        MAX_RAIN_DROPS = 2000;
        STRIPPED_VARIANTS = new HashMap<Block, Block>(){
            {
                this.put(Blocks.OAK_LOG, Blocks.STRIPPED_OAK_LOG);
                this.put(Blocks.ACACIA_LOG, Blocks.STRIPPED_ACACIA_LOG);
                this.put(Blocks.BIRCH_LOG, Blocks.STRIPPED_BIRCH_LOG);
                this.put(Blocks.CHERRY_LOG, Blocks.STRIPPED_CHERRY_LOG);
                this.put(Blocks.DARK_OAK_LOG, Blocks.STRIPPED_DARK_OAK_LOG);
                this.put(Blocks.JUNGLE_LOG, Blocks.STRIPPED_JUNGLE_LOG);
                this.put(Blocks.MANGROVE_LOG, Blocks.STRIPPED_MANGROVE_LOG);
                this.put(Blocks.SPRUCE_LOG, Blocks.STRIPPED_SPRUCE_LOG);
            }
        };
        RAIN_POSITIONS = new Vec3[MAX_RAIN_DROPS];
        float range = 10.0f;
        for (int i = 0; i < MAX_RAIN_DROPS; ++i) {
            Util.RAIN_POSITIONS[i] = new Vec3((double)(PMWeather.RANDOM.nextFloat() * range - range / 2.0f), (double)(PMWeather.RANDOM.nextFloat() * range - range / 2.0f), (double)(PMWeather.RANDOM.nextFloat() * range - range / 2.0f));
        }
        ROCP = 0.28571427f;
    }
}

