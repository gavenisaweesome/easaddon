/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.resources.sounds.SoundInstance
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Holder
 *  net.minecraft.core.Registry
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.Vec3
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.api.distmarker.OnlyIn
 *  net.neoforged.neoforge.common.util.DeferredSoundType
 *  net.neoforged.neoforge.registries.DeferredRegister
 */
package dev.protomanly.pmweather.sound;

import dev.protomanly.pmweather.sound.MovingSoundStreamingSource;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.util.DeferredSoundType;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create((Registry)BuiltInRegistries.SOUND_EVENT, (String)"pmweather");
    public static final Holder<SoundEvent> SIREN = SOUND_EVENTS.register("siren", SoundEvent::createVariableRangeEvent);
    public static final Holder<SoundEvent> HAIL = SOUND_EVENTS.register("hail", SoundEvent::createVariableRangeEvent);
    public static final Holder<SoundEvent> CALM_AMBIENCE = SOUND_EVENTS.register("calm_ambience", SoundEvent::createVariableRangeEvent);
    public static final Holder<SoundEvent> WIND_STRONG = SOUND_EVENTS.register("wind_strong", SoundEvent::createVariableRangeEvent);
    public static final Holder<SoundEvent> WIND_MED = SOUND_EVENTS.register("wind_med", SoundEvent::createVariableRangeEvent);
    public static final Holder<SoundEvent> WIND_CALM = SOUND_EVENTS.register("wind_calm", SoundEvent::createVariableRangeEvent);
    public static final Holder<SoundEvent> TORNADIC_WIND = SOUND_EVENTS.register("tornadic_wind", SoundEvent::createVariableRangeEvent);
    public static final Holder<SoundEvent> SUPERCELL_WIND = SOUND_EVENTS.register("supercell_wind", SoundEvent::createVariableRangeEvent);
    public static final Holder<SoundEvent> WSR88D_COMPLETED = SOUND_EVENTS.register("wsr88d_completed", SoundEvent::createVariableRangeEvent);
    public static final Holder<SoundEvent> WSR88D_DISMANTLED = SOUND_EVENTS.register("wsr88d_dismantled", SoundEvent::createVariableRangeEvent);
    public static final Holder<SoundEvent> RAIN = SOUND_EVENTS.register("rain", SoundEvent::createVariableRangeEvent);
    public static final Holder<SoundEvent> SLEET = SOUND_EVENTS.register("sleet", SoundEvent::createVariableRangeEvent);
    public static final Holder<SoundEvent> THUNDER_NEAR = SOUND_EVENTS.register("thunder_near", SoundEvent::createVariableRangeEvent);
    public static final Holder<SoundEvent> THUNDER_FAR = SOUND_EVENTS.register("thunder_far", SoundEvent::createVariableRangeEvent);
    public static final Supplier<SoundEvent> SLEET_BREAK = SOUND_EVENTS.register("sleet_break", SoundEvent::createVariableRangeEvent);
    public static final Supplier<SoundEvent> SLEET_STEP = SOUND_EVENTS.register("sleet_step", SoundEvent::createVariableRangeEvent);
    public static final Supplier<SoundEvent> SLEET_PLACE = SOUND_EVENTS.register("sleet_place", SoundEvent::createVariableRangeEvent);
    public static final Supplier<SoundEvent> SLEET_HIT = SOUND_EVENTS.register("sleet_hit", SoundEvent::createVariableRangeEvent);
    public static final Supplier<SoundEvent> SLEET_FALL = SOUND_EVENTS.register("sleet_fall", SoundEvent::createVariableRangeEvent);
    public static final DeferredSoundType SLEET_BLOCK = new DeferredSoundType(1.0f, 1.0f, SLEET_BREAK, SLEET_STEP, SLEET_PLACE, SLEET_HIT, SLEET_FALL);

    @OnlyIn(value=Dist.CLIENT)
    public static void playBlockSound(Level level, BlockState block, BlockPos blockPos, SoundEvent soundEvent, float volume, float pitch, float cutOffRange) {
        MovingSoundStreamingSource sound = new MovingSoundStreamingSource(level, block, blockPos, soundEvent, SoundSource.WEATHER, volume, pitch, cutOffRange);
        Minecraft.getInstance().getSoundManager().play((SoundInstance)sound);
    }

    @OnlyIn(value=Dist.CLIENT)
    public static void playPlayerLockedSound(Vec3 pos, SoundEvent soundEvent, float volume, float pitch) {
        MovingSoundStreamingSource sound = new MovingSoundStreamingSource(pos, soundEvent, SoundSource.WEATHER, volume, pitch, true);
        Minecraft.getInstance().getSoundManager().play((SoundInstance)sound);
    }
}

