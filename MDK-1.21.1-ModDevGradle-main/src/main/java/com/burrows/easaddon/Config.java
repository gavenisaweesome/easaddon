package com.burrows.easaddon;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;



  @EventBusSubscriber(modid = EASAddon.MODID, bus = EventBusSubscriber.Bus.MOD)
  public class Config {
  /*\*

  * Empty ModConfigSpec: no definitions or comments, satisfying builder constraints.
    \*/
    public static final ModConfigSpec SPEC = new ModConfigSpec.Builder().build();

    @SubscribeEvent
    public static void onLoad(ModConfigEvent event) {
    // Placeholder: no values to read yet
    }
    }
