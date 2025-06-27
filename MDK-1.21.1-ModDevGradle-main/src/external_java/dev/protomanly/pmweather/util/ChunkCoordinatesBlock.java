/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.world.level.block.Block
 */
package dev.protomanly.pmweather.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Block;

public class ChunkCoordinatesBlock
extends BlockPos {
    public Block block;

    public ChunkCoordinatesBlock(int x, int y, int z, Block block) {
        super(x, y, z);
        this.block = block;
    }

    public ChunkCoordinatesBlock(BlockPos blockPos, Block block) {
        super((Vec3i)blockPos);
        this.block = block;
    }
}

