/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.Registry
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.EntityType$Builder
 *  net.minecraft.world.entity.MobCategory
 *  net.neoforged.neoforge.registries.DeferredRegister
 */
package dev.protomanly.pmweather.entity;

import dev.protomanly.pmweather.entity.MovingBlock;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create((Registry)BuiltInRegistries.ENTITY_TYPE, (String)"pmweather");
    public static final Supplier<EntityType<MovingBlock>> MOVING_BLOCK = ENTITY_TYPES.register("moving_block", () -> EntityType.Builder.of(MovingBlock::new, (MobCategory)MobCategory.MISC).sized(1.0f, 1.0f).build("moving_block"));
}

