/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.Tag
 *  net.minecraft.util.Mth
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.levelgen.Heightmap$Types
 *  net.minecraft.world.phys.Vec2
 *  net.minecraft.world.phys.Vec3
 */
package dev.protomanly.pmweather.weather;

import dev.protomanly.pmweather.block.entity.RadarBlockEntity;
import dev.protomanly.pmweather.config.ServerConfig;
import dev.protomanly.pmweather.util.Util;
import dev.protomanly.pmweather.weather.ThermodynamicEngine;
import dev.protomanly.pmweather.weather.WeatherHandler;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SequencedCollection;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class Sounding {
    public Map<Integer, ThermodynamicEngine.AtmosphericDataPoint> data = new HashMap<Integer, ThermodynamicEngine.AtmosphericDataPoint>();
    Vec3 position;
    public WeatherHandler weatherHandler;

    public Sounding(WeatherHandler weatherHandler, Vec3 pos, Level level, int res, int height) {
        int base;
        this.weatherHandler = weatherHandler;
        this.position = pos;
        for (int y = base = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int)pos.x, (int)pos.z); y <= height + base; y += res) {
            Vec3 p = new Vec3(pos.x, (double)y, pos.z);
            ThermodynamicEngine.AtmosphericDataPoint dataPoint = ThermodynamicEngine.samplePoint(weatherHandler, p, level, null, 0);
            this.data.put(y, dataPoint);
        }
    }

    public Sounding(WeatherHandler weatherHandler, Vec3 pos, Level level, int res, int height, RadarBlockEntity radarBlockEntity) {
        int base;
        this.weatherHandler = weatherHandler;
        this.position = pos;
        for (int y = base = radarBlockEntity.getBlockPos().getY(); y <= height + base; y += res) {
            Vec3 p = new Vec3(pos.x, (double)y, pos.z);
            ThermodynamicEngine.AtmosphericDataPoint dataPoint = ThermodynamicEngine.samplePoint(weatherHandler, p, level, radarBlockEntity, 0, radarBlockEntity.getBlockPos().getY());
            this.data.put(y, dataPoint);
        }
    }

    public Sounding(WeatherHandler weatherHandler, Vec3 pos, Level level, int res, int height, int advance) {
        int base;
        this.weatherHandler = weatherHandler;
        this.position = pos;
        for (int y = base = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int)pos.x, (int)pos.z); y <= height + base; y += res) {
            Vec3 p = new Vec3(pos.x, (double)y, pos.z);
            ThermodynamicEngine.AtmosphericDataPoint dataPoint = ThermodynamicEngine.samplePoint(weatherHandler, p, level, null, advance);
            this.data.put(y, dataPoint);
        }
    }

    public Sounding(WeatherHandler weatherHandler, Vec3 pos, Level level, int res, int height, RadarBlockEntity radarBlockEntity, int advance) {
        int base;
        this.weatherHandler = weatherHandler;
        this.position = pos;
        for (int y = base = radarBlockEntity.getBlockPos().getY(); y <= height + base; y += res) {
            Vec3 p = new Vec3(pos.x, (double)y, pos.z);
            ThermodynamicEngine.AtmosphericDataPoint dataPoint = ThermodynamicEngine.samplePoint(weatherHandler, p, level, radarBlockEntity, 0, radarBlockEntity.getBlockPos().getY());
            this.data.put(y, dataPoint);
        }
    }

    public Sounding(WeatherHandler weatherHandler, Vec3 pos) {
        this.weatherHandler = weatherHandler;
        this.position = pos;
    }

    public Sounding(WeatherHandler weatherHandler, CompoundTag compoundTag, Vec3 pos) {
        this.weatherHandler = weatherHandler;
        this.position = pos;
        Set keys = compoundTag.getAllKeys();
        for (String key : keys) {
            int height = Integer.parseInt(key);
            CompoundTag layer = compoundTag.getCompound(key);
            this.data.put(height, ThermodynamicEngine.deserializeDataPoint(layer));
        }
    }

    public CAPE getCAPE(Parcel parcel) {
        float CAPE2 = 0.0f;
        float CINH = 0.0f;
        float CAPE3 = 0.0f;
        List set = this.data.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList();
        float delta = 0.0f;
        if (set.size() > 1) {
            delta = (Integer)set.get(1).getKey() - (Integer)set.getFirst().getKey();
        }
        for (Map.Entry entry : set) {
            float lCAPE;
            int h = (Integer)entry.getKey();
            ThermodynamicEngine.AtmosphericDataPoint dataPoint = (ThermodynamicEngine.AtmosphericDataPoint)entry.getValue();
            Float parcelTemp = parcel.profile.getOrDefault(Float.valueOf(dataPoint.pressure()), null);
            if (parcelTemp == null || set.size() <= 1) continue;
            if (h <= 3000 && dataPoint.pressure() <= parcel.lfcP) {
                lCAPE = (Util.celsiusToKelvin(parcelTemp.floatValue()) - Util.celsiusToKelvin(dataPoint.virtualTemperature())) / Util.celsiusToKelvin(dataPoint.virtualTemperature()) * 9.807f;
                CAPE3 += (lCAPE *= delta);
            }
            if (dataPoint.pressure() >= parcel.elP && dataPoint.pressure() <= parcel.lfcP) {
                lCAPE = (Util.celsiusToKelvin(parcelTemp.floatValue()) - Util.celsiusToKelvin(dataPoint.virtualTemperature())) / Util.celsiusToKelvin(dataPoint.virtualTemperature()) * 9.807f;
                CAPE2 += Math.max(lCAPE *= delta, 0.0f);
            }
            if (!(dataPoint.pressure() > parcel.lfcP)) continue;
            float lCINH = (Util.celsiusToKelvin(parcelTemp.floatValue()) - Util.celsiusToKelvin(dataPoint.virtualTemperature())) / Util.celsiusToKelvin(dataPoint.virtualTemperature()) * 9.807f;
            lCINH *= delta;
            CINH += Math.min(lCINH *= 0.75f, 0.0f);
        }
        return new CAPE(CAPE2, CINH, CAPE3);
    }

    @Nullable
    public ThermodynamicEngine.AtmosphericDataPoint getFromPressure(float p) {
        List set = this.data.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList();
        for (Map.Entry entry : set) {
            if (!(((ThermodynamicEngine.AtmosphericDataPoint)entry.getValue()).pressure() < p)) continue;
            return (ThermodynamicEngine.AtmosphericDataPoint)entry.getValue();
        }
        return null;
    }

    public float getRisk(int advance) {
        ThermodynamicEngine.AtmosphericDataPoint sfc = this.getFromHeight(0);
        CAPE CAPE2 = this.getCAPE(this.getSBParcel());
        float risk = 1.0f;
        risk *= Math.clamp(CAPE2.CAPE() / 2000.0f, 0.0f, 2.0f);
        risk *= Math.clamp((CAPE2.CAPE3() - 25.0f) / 50.0f, 0.0f, 1.25f);
        Float lr = this.getLapseRate(0, 3000);
        if (lr == null) {
            lr = Float.valueOf(0.0f);
        }
        risk *= Math.clamp((lr.floatValue() - 5.0f) / 1.5f, 0.75f, 1.25f);
        risk *= 1.0f - Math.clamp(CAPE2.CINH() / -500.0f, 0.0f, 1.0f);
        if (sfc != null) {
            risk *= Math.clamp((sfc.dewpoint() - 7.0f) / 11.0f, 0.15f, 1.25f);
        }
        risk = Math.clamp(risk, 0.0f, 1.75f);
        if (this.position != null && ThermodynamicEngine.noise != null) {
            float SRH = ((float)ThermodynamicEngine.noise.getValue(this.position.x / 5000.0, (double)((float)(this.weatherHandler.getWorld().getDayTime() + (long)advance) / 15000.0f), this.position.z / 5000.0) + 1.0f) / 2.0f;
            SRH = (float)Math.pow(SRH, 1.5);
            risk *= Math.clamp((SRH *= 400.0f) / 325.0f, 0.0f, 1.25f);
            risk = Math.clamp(risk, 0.0f, 1.75f);
        }
        return (float)Math.pow(Math.clamp(risk / 1.75f, 0.0f, 1.0f), ServerConfig.riskCurve + (double)0.1f) * 1.75f;
    }

    @Nullable
    public Float getLapseRate(int lower, int upper) {
        ThermodynamicEngine.AtmosphericDataPoint dataPointLower = this.getFromHeight(lower);
        ThermodynamicEngine.AtmosphericDataPoint dataPointUpper = this.getFromHeight(upper);
        if (dataPointLower != null && dataPointUpper != null) {
            float delta = upper - lower;
            return Float.valueOf((dataPointLower.temperature() - dataPointUpper.temperature()) / (delta / 1000.0f));
        }
        return null;
    }

    @Nullable
    public ThermodynamicEngine.AtmosphericDataPoint getFromHeight(int h) {
        ThermodynamicEngine.AtmosphericDataPoint dataPoint = this.data.getOrDefault(h, null);
        if (dataPoint != null) {
            return dataPoint;
        }
        List set = this.data.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList();
        for (Map.Entry entry : set) {
            if ((Integer)entry.getKey() < h) continue;
            return (ThermodynamicEngine.AtmosphericDataPoint)entry.getValue();
        }
        return null;
    }

    @Nullable
    public Parcel getSBParcel() {
        List set = this.data.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList();
        Iterator iterator = set.iterator();
        if (iterator.hasNext()) {
            Map.Entry entry = iterator.next();
            return new Parcel(this, (ThermodynamicEngine.AtmosphericDataPoint)entry.getValue());
        }
        return null;
    }

    public CompoundTag serializeNBT() {
        CompoundTag compoundTag = new CompoundTag();
        SequencedCollection set = this.data.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList().reversed();
        for (Map.Entry entry : set) {
            compoundTag.put(String.valueOf(entry.getKey()), (Tag)((ThermodynamicEngine.AtmosphericDataPoint)entry.getValue()).serializeNBT());
        }
        return compoundTag;
    }

    public String toString() {
        StringBuilder str = new StringBuilder();
        SequencedCollection set = this.data.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList().reversed();
        for (Map.Entry entry : set) {
            str.append(String.format("%s m: %s", entry.getKey(), entry.getValue())).append("\n");
        }
        return str.toString();
    }

    public static Vec2 getPosition(float temp, float pressure, float minPressure, float maxPressure, float tempRange) {
        float tPerc = (temp -= tempRange / 4.0f) / tempRange;
        float y = (float)(1.0 - (Math.log10(pressure) - Math.log10(minPressure)) / (Math.log10(maxPressure) - Math.log10(minPressure)));
        float heightPerc = -1.0f + y * 2.0f;
        return new Vec2(tPerc += (heightPerc + 1.0f) / 1.5f, heightPerc);
    }

    public static class Parcel {
        public Sounding sounding;
        public ThermodynamicEngine.AtmosphericDataPoint parcel;
        public Map<Float, Float> profile = new HashMap<Float, Float>();
        public float lclP;
        public float lfcP = -1.0f;
        public float elP = -1.0f;

        public Parcel(Sounding sounding, ThermodynamicEngine.AtmosphericDataPoint parcel) {
            float t;
            float p;
            ThermodynamicEngine.AtmosphericDataPoint dataPoint;
            this.sounding = sounding;
            this.parcel = parcel;
            LCL lcl = Parcel.DryLift(parcel.pressure(), parcel.temperature(), parcel.dewpoint());
            this.lclP = lcl.pressure();
            List set = sounding.data.entrySet().stream().toList();
            HashMap<Integer, ThermodynamicEngine.AtmosphericDataPoint> lower = new HashMap<Integer, ThermodynamicEngine.AtmosphericDataPoint>();
            HashMap<Integer, ThermodynamicEngine.AtmosphericDataPoint> upper = new HashMap<Integer, ThermodynamicEngine.AtmosphericDataPoint>();
            float lowMinP = 10000.0f;
            for (Map.Entry entry : set) {
                int height = (Integer)entry.getKey();
                ThermodynamicEngine.AtmosphericDataPoint dataPoint2 = (ThermodynamicEngine.AtmosphericDataPoint)entry.getValue();
                if (dataPoint2.pressure() > parcel.pressure()) continue;
                if (dataPoint2.pressure() >= lcl.pressure()) {
                    lower.put(height, dataPoint2);
                    lowMinP = Math.min(dataPoint2.pressure(), lowMinP);
                    continue;
                }
                upper.put(height, dataPoint2);
            }
            Map<Float, Float> tLower = this.GetDryLapse(lower, parcel.virtualTemperature(), parcel.dewpoint(), parcel.pressure());
            Map<Float, Float> tUpper = this.GetMoistLapse(upper, tLower.getOrDefault(Float.valueOf(lowMinP), Float.valueOf(lcl.temp())).floatValue(), lowMinP);
            this.profile.putAll(tLower);
            this.profile.putAll(tUpper);
            List profileSetAsc = this.profile.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList();
            SequencedCollection profileSetDesc = profileSetAsc.reversed();
            List soundingSetAsc = sounding.data.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList();
            SequencedCollection soundingSetDesc = soundingSetAsc.reversed();
            for (Map.Entry entry : soundingSetAsc) {
                dataPoint = (ThermodynamicEngine.AtmosphericDataPoint)entry.getValue();
                p = dataPoint.pressure();
                t = this.profile.getOrDefault(Float.valueOf(p), Float.valueOf(-100.0f)).floatValue();
                if (p > this.lclP || !(t >= dataPoint.virtualTemperature())) continue;
                this.lfcP = p;
                break;
            }
            for (Map.Entry entry : soundingSetDesc) {
                dataPoint = (ThermodynamicEngine.AtmosphericDataPoint)entry.getValue();
                p = dataPoint.pressure();
                t = this.profile.getOrDefault(Float.valueOf(p), Float.valueOf(-100.0f)).floatValue();
                if (p >= this.lclP || this.lfcP > 0.0f && p > this.lfcP) break;
                if (!(t >= dataPoint.virtualTemperature())) continue;
                this.elP = p;
                break;
            }
        }

        public Map<Float, Float> GetDryLapse(Map<Integer, ThermodynamicEngine.AtmosphericDataPoint> l, float t, float dp, float p) {
            LCL lcl = Parcel.DryLift(p, t, dp);
            HashMap<Float, Float> r = new HashMap<Float, Float>();
            List set = l.entrySet().stream().toList();
            for (Map.Entry entry : set) {
                ThermodynamicEngine.AtmosphericDataPoint dataPoint = (ThermodynamicEngine.AtmosphericDataPoint)entry.getValue();
                r.put(Float.valueOf(dataPoint.pressure()), Float.valueOf(Mth.lerp((float)(1.0f - (dataPoint.pressure() - lcl.pressure()) / (p - lcl.pressure())), (float)t, (float)lcl.temp())));
            }
            return r;
        }

        public Map<Float, Float> GetMoistLapse(Map<Integer, ThermodynamicEngine.AtmosphericDataPoint> l, float t, float p) {
            HashMap<Float, Float> r = new HashMap<Float, Float>();
            List set = l.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList();
            for (Map.Entry entry : set) {
                ThermodynamicEngine.AtmosphericDataPoint dataPoint = (ThermodynamicEngine.AtmosphericDataPoint)entry.getValue();
                r.put(Float.valueOf(dataPoint.pressure()), Float.valueOf(Parcel.WetLift(p, t, dataPoint.pressure())));
            }
            return r;
        }

        public static float LCLTemp(float t, float dp) {
            float s = t - dp;
            float dlt = s * (1.2185f + 0.001278f * t + s * (-0.00219f + 1.173E-5f * s - 5.2E-6f * t));
            return t - dlt * 0.6f;
        }

        public static float Theta(float p, float t, @Nullable Float p2) {
            if (p2 == null) {
                p2 = Float.valueOf(1000.0f);
            }
            return Util.kelvinToCelsius(Util.celsiusToKelvin(t) * (float)Math.pow(p2.floatValue() / p, Util.ROCP));
        }

        public static float ThaLVL(float theta, float t) {
            return 1000.0f / (float)Math.pow(Util.celsiusToKelvin(theta) / Util.celsiusToKelvin(t), 1.0f / Util.ROCP);
        }

        public static float SatLift(float p, float thetam, @Nullable Float conv) {
            if (conv == null) {
                conv = Float.valueOf(100.0f);
            }
            if (Math.abs(p - 1000.0f) - 0.001f <= 0.0f) {
                return thetam;
            }
            float eor = 999.0f;
            float pwrp = 0.0f;
            float t1 = 0.0f;
            float t2 = 0.0f;
            float e1 = 0.0f;
            float e2 = 0.0f;
            while (Math.abs(eor) - conv.floatValue() > 0.0f) {
                float rate;
                if (eor == 999.0f) {
                    pwrp = (float)Math.pow(p / 1000.0f, Util.ROCP);
                    t1 = Util.kelvinToCelsius(Util.celsiusToKelvin(thetam) * pwrp);
                    e1 = Parcel.Wobf(t1) - Parcel.Wobf(thetam);
                    rate = 1.0f;
                } else {
                    rate = (t2 - t1) / (e2 - e1);
                    t1 = t2;
                    e1 = e2;
                }
                t2 = t1 - e1 * rate;
                e2 = Util.kelvinToCelsius(Util.celsiusToKelvin(t2) / pwrp);
                e2 += Parcel.Wobf(t2) - Parcel.Wobf(e2) - thetam;
                eor = e2 * rate;
            }
            return t2 - eor;
        }

        public static float Wobf(float t) {
            if ((t -= 20.0f) <= 0.0f) {
                float npol = 1.0f + t * (-0.008841661f + t * (1.4714143E-4f + t * (-9.671988E-7f + t * (-3.260722E-8f + t * -3.8598072E-10f))));
                npol = 15.13f / (float)Math.pow(npol, 4.0);
                return npol;
            }
            float ppol = t * (4.961892E-7f + t * (-6.1059366E-9f + t * (3.940155E-11f + t * (-1.258813E-13f + t * 1.668828E-16f))));
            ppol = 1.0f + t * (0.0036182988f + t * (-1.3603273E-5f + ppol));
            ppol = 29.93f / (float)Math.pow(ppol, 4.0) + 0.96f * t - 14.8f;
            return ppol;
        }

        public static float WetLift(float p, float t, float p2) {
            float thta = Parcel.Theta(p, t, null);
            float thetam = thta - Parcel.Wobf(thta) + Parcel.Wobf(t);
            return Parcel.SatLift(p2, thetam, null);
        }

        public static LCL DryLift(float p, float t, float dp) {
            float t2 = Parcel.LCLTemp(t, dp);
            float p2 = Parcel.ThaLVL(Parcel.Theta(p, t, null), t2);
            return new LCL(p2, t2);
        }

        public record LCL(float pressure, float temp) {
        }
    }

    public record CAPE(float CAPE, float CINH, float CAPE3) {
    }
}

