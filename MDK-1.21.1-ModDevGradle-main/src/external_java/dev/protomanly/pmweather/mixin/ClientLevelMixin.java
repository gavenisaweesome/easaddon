/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.multiplayer.ClientLevel
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package dev.protomanly.pmweather.mixin;

import dev.protomanly.pmweather.event.GameBusClientEvents;
import dev.protomanly.pmweather.weather.WeatherHandlerClient;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={ClientLevel.class})
public class ClientLevelMixin {
    @Inject(method={"getSkyDarken"}, at={@At(value="RETURN")}, cancellable=true)
    public void editSkyDarken(float partialTick, CallbackInfoReturnable<Float> callbackInfoReturnable) {
        float darken = 1.0f;
        WeatherHandlerClient weatherHandler = (WeatherHandlerClient)GameBusClientEvents.weatherHandler;
        if (weatherHandler != null) {
            darken = Math.clamp(weatherHandler.getPrecipitation(), 0.0f, 1.0f) * 0.8f;
        }
        callbackInfoReturnable.setReturnValue((Object)Float.valueOf(((Float)callbackInfoReturnable.getReturnValue()).floatValue() * (1.0f - darken)));
    }
}

