/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.levelgen.Heightmap$Types
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package dev.protomanly.pmweather.mixin;

import dev.protomanly.pmweather.event.GameBusClientEvents;
import dev.protomanly.pmweather.event.GameBusEvents;
import dev.protomanly.pmweather.weather.ThermodynamicEngine;
import dev.protomanly.pmweather.weather.WeatherHandler;
import dev.protomanly.pmweather.weather.WeatherHandlerClient;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={Level.class})
public class LevelMixin {
    @Inject(method={"isRainingAt"}, at={@At(value="RETURN")}, cancellable=true)
    public void editRainingAt(BlockPos pos, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        ThermodynamicEngine.AtmosphericDataPoint dataPoint;
        float rain;
        Level level = (Level)this;
        if (level.isClientSide()) {
            GameBusClientEvents.getClientWeather();
            WeatherHandlerClient weatherHandler = (WeatherHandlerClient)GameBusClientEvents.weatherHandler;
            rain = weatherHandler.getPrecipitation();
            dataPoint = ThermodynamicEngine.samplePoint(weatherHandler, pos.getCenter(), level, null, 0);
        } else {
            WeatherHandler weatherHandler = GameBusEvents.MANAGERS.get(((Level)this).dimension());
            rain = weatherHandler.getPrecipitation(pos.getCenter());
            dataPoint = ThermodynamicEngine.samplePoint(weatherHandler, pos.getCenter(), level, null, 0);
        }
        if (rain <= 0.2f || dataPoint.temperature() <= 0.0f) {
            callbackInfoReturnable.setReturnValue((Object)false);
        } else if (!level.canSeeSky(pos)) {
            callbackInfoReturnable.setReturnValue((Object)false);
        } else if (level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos).getY() > pos.getY()) {
            callbackInfoReturnable.setReturnValue((Object)false);
        } else {
            callbackInfoReturnable.setReturnValue((Object)true);
        }
    }

    @Inject(method={"getRainLevel"}, at={@At(value="RETURN")}, cancellable=true)
    public void editRain(float delta, CallbackInfoReturnable<Float> callbackInfoReturnable) {
        Level level = (Level)this;
        if (level.isClientSide() && GameBusClientEvents.weatherHandler != null) {
            GameBusClientEvents.getClientWeather();
            callbackInfoReturnable.setReturnValue((Object)Float.valueOf(((WeatherHandlerClient)GameBusClientEvents.weatherHandler).getPrecipitation()));
        } else {
            callbackInfoReturnable.setReturnValue((Object)Float.valueOf(0.0f));
        }
    }

    @Inject(method={"getThunderLevel"}, at={@At(value="RETURN")}, cancellable=true)
    public void editThunder(float delta, CallbackInfoReturnable<Float> callbackInfoReturnable) {
        Level level = (Level)this;
        if (level.isClientSide() && GameBusClientEvents.weatherHandler != null) {
            GameBusClientEvents.getClientWeather();
            float rainAmount = ((WeatherHandlerClient)GameBusClientEvents.weatherHandler).getPrecipitation();
            callbackInfoReturnable.setReturnValue((Object)Float.valueOf(rainAmount));
        } else {
            callbackInfoReturnable.setReturnValue((Object)Float.valueOf(0.0f));
        }
    }
}

