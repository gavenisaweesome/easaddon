package com.burrows.easaddon;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import com.burrows.easaddon.client.RadarOverlayRenderer;
import com.burrows.easaddon.survey.SurveyCommands;
import com.burrows.easaddon.tornado.TornadoTracker;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent; // ADDED
import net.neoforged.neoforge.network.registration.PayloadRegistrar; // ADDED
import com.burrows.easaddon.network.SurveyNetworkPackets; // ADDED
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;


@Mod(EASAddon.MODID)
public class EASAddon {
    public static final String MODID = "easaddon";
    public static final Logger LOGGER = LogUtils.getLogger();
    private static boolean pmweatherAvailable = false;

    public EASAddon(IEventBus modEventBus, ModContainer modContainer) {
        verifyPMWeatherPresence();
        
        RegistryHandler.register(modEventBus);
        modEventBus.addListener(EntityRenderersEvent.RegisterRenderers.class, this::onRegisterRenderers);

        // ADDED: Register network packets
        modEventBus.addListener(this::registerNetworking);
        
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);

        // NeoForge version-aware logging
        LOGGER.info("Initialized for Minecraft {} with NeoForge");
        
        LOGGER.info("PMWeather integration: {}", 
            pmweatherAvailable ? "§aACTIVE" : "§cUNAVAILABLE");
    }

    // ADDED: Register network packets for survey system
    private void registerNetworking(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(MODID);
        
        // Register survey packets
        registrar.playToServer(
            SurveyNetworkPackets.StartSurveyPacket.TYPE,
            SurveyNetworkPackets.StartSurveyPacket.STREAM_CODEC,
            SurveyNetworkPackets.StartSurveyPacket::handle
        );
        
        registrar.playToServer(
            SurveyNetworkPackets.QuitSurveyPacket.TYPE,
            SurveyNetworkPackets.QuitSurveyPacket.STREAM_CODEC,
            SurveyNetworkPackets.QuitSurveyPacket::handle
        );
        
        registrar.playToServer(
            SurveyNetworkPackets.FinishSurveyPacket.TYPE,
            SurveyNetworkPackets.FinishSurveyPacket.STREAM_CODEC,
            SurveyNetworkPackets.FinishSurveyPacket::handle
        );
        
        registrar.playToServer(
            SurveyNetworkPackets.SurveyActionPacket.TYPE,
            SurveyNetworkPackets.SurveyActionPacket.STREAM_CODEC,
            SurveyNetworkPackets.SurveyActionPacket::handle
        );
        
        registrar.playToClient(
            SurveyNetworkPackets.SurveyUpdatePacket.TYPE,
            SurveyNetworkPackets.SurveyUpdatePacket.STREAM_CODEC,
            SurveyNetworkPackets.SurveyUpdatePacket::handle
        );
        
        LOGGER.info("Survey network packets registered successfully");
    }
    
    private void verifyPMWeatherPresence() {
        // Check both mod ID and class existence
        pmweatherAvailable = ModList.get().isLoaded("pmweather") && 
            classExists("dev.protomanly.pmweather.block.TornadoSensorBlock");
        
        if(!pmweatherAvailable) {
            LOGGER.warn("PMWeather not found - tornado detection disabled");
        }
    }

    private boolean classExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
       /** NeoForge will fire this on the client side. */
       private void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers evt) {
    	   LOGGER.info("[EASAddon] → Registering RadarOverlayRenderer");
           evt.registerBlockEntityRenderer(
               RegistryHandler.RADAR_OVERLAY_BE.get(),
               RadarOverlayRenderer::new
           );
       }


    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Common setup complete");
        LOGGER.debug("NeoForge API version: {}");
    }

    
    private void clientSetup(final FMLClientSetupEvent event) {
        LOGGER.info("Client setup complete");
        
        // Register tornado tracker only on client side
        if (FMLEnvironment.dist == Dist.CLIENT && pmweatherAvailable) {
            NeoForge.EVENT_BUS.register(TornadoTracker.getInstance());
            LOGGER.info("Tornado tracker registered for client-side events");
            
            // Register PMWeather damage hook for real damage tracking
            NeoForge.EVENT_BUS.register(com.burrows.easaddon.survey.PMWeatherDamageHook.getInstance());
            LOGGER.info("PMWeather damage hook registered for real damage tracking");
            
            // Register client commands using the correct NeoForge method
            NeoForge.EVENT_BUS.addListener(this::onRegisterClientCommands);
        }
    }

// Update this method in EASAddon.java:
private void onRegisterClientCommands(net.neoforged.neoforge.client.event.RegisterClientCommandsEvent event) {
    SurveyCommands.register(event.getDispatcher());
    LOGGER.info("Survey commands registered successfully");
}

    public static boolean isPMWeatherAvailable() {
        return pmweatherAvailable;
    }
}