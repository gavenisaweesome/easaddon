/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.EvictingQueue
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.Maps
 *  com.google.common.collect.Queues
 *  com.google.gson.JsonObject
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.blaze3d.vertex.BufferBuilder
 *  com.mojang.blaze3d.vertex.BufferUploader
 *  com.mojang.blaze3d.vertex.MeshData
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.Tesselator
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
 *  javax.annotation.Nullable
 *  net.minecraft.CrashReport
 *  net.minecraft.CrashReportCategory
 *  net.minecraft.ReportedException
 *  net.minecraft.Util
 *  net.minecraft.client.Camera
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.particle.Particle
 *  net.minecraft.client.particle.ParticleDescription
 *  net.minecraft.client.particle.ParticleProvider
 *  net.minecraft.client.particle.ParticleRenderType
 *  net.minecraft.client.particle.SpriteSet
 *  net.minecraft.client.particle.TrackingEmitter
 *  net.minecraft.client.renderer.GameRenderer
 *  net.minecraft.client.renderer.LightTexture
 *  net.minecraft.client.renderer.MultiBufferSource$BufferSource
 *  net.minecraft.client.renderer.culling.Frustum
 *  net.minecraft.client.renderer.texture.SpriteLoader
 *  net.minecraft.client.renderer.texture.SpriteLoader$Preparations
 *  net.minecraft.client.renderer.texture.TextureAtlas
 *  net.minecraft.client.renderer.texture.TextureAtlasSprite
 *  net.minecraft.client.renderer.texture.TextureManager
 *  net.minecraft.core.particles.ParticleGroup
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.resources.FileToIdConverter
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.packs.resources.PreparableReloadListener
 *  net.minecraft.server.packs.resources.PreparableReloadListener$PreparationBarrier
 *  net.minecraft.server.packs.resources.Resource
 *  net.minecraft.server.packs.resources.ResourceManager
 *  net.minecraft.util.GsonHelper
 *  net.minecraft.util.RandomSource
 *  net.minecraft.util.profiling.ProfilerFiller
 *  net.neoforged.neoforge.client.ClientHooks
 *  org.apache.commons.compress.utils.Lists
 *  org.joml.Matrix4fStack
 *  org.joml.Matrix4fc
 */
package dev.protomanly.pmweather.particle;

import com.google.common.collect.EvictingQueue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.protomanly.pmweather.PMWeather;
import dev.protomanly.pmweather.config.ClientConfig;
import dev.protomanly.pmweather.particle.EntityRotFX;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleDescription;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TrackingEmitter;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.particles.ParticleGroup;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.client.ClientHooks;
import org.apache.commons.compress.utils.Lists;
import org.joml.Matrix4fStack;
import org.joml.Matrix4fc;

public class ParticleManager
implements PreparableReloadListener {
    private static final FileToIdConverter PARTICLE_LISTER = FileToIdConverter.json((String)"particles");
    private static final ResourceLocation PARTICLES_ATLAS_INFO = ResourceLocation.withDefaultNamespace((String)"particles");
    private static final List<ParticleRenderType> RENDER_ORDER = ImmutableList.of((Object)ParticleRenderType.TERRAIN_SHEET, (Object)ParticleRenderType.PARTICLE_SHEET_OPAQUE, (Object)ParticleRenderType.PARTICLE_SHEET_LIT, (Object)ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT, (Object)ParticleRenderType.CUSTOM, (Object)EntityRotFX.SORTED_OPAQUE_BLOCK, (Object)EntityRotFX.SORTED_TRANSLUCENT);
    protected ClientLevel level;
    private final TextureAtlas textureAtlas;
    private final TextureManager textureManager;
    private final Map<ResourceLocation, ParticleProvider<?>> providers = new HashMap();
    private final Map<ResourceLocation, MutableSpriteSet> spriteSets = Maps.newHashMap();
    private final Queue<Particle> particlesToAdd = Queues.newArrayDeque();
    private final Queue<TrackingEmitter> trackingEmitters = Queues.newArrayDeque();
    private final Map<ParticleRenderType, Queue<Particle>> particles = Maps.newTreeMap((Comparator)ClientHooks.makeParticleRenderTypeComparator(RENDER_ORDER));
    private final Object2IntOpenHashMap<ParticleGroup> trackedParticleCounts = new Object2IntOpenHashMap();

    public ParticleManager(ClientLevel level, TextureManager textureManager) {
        this.textureAtlas = new TextureAtlas(TextureAtlas.LOCATION_PARTICLES);
        this.level = level;
        this.textureManager = textureManager;
    }

    public CompletableFuture<Void> reload(PreparableReloadListener.PreparationBarrier preparationBarrier, ResourceManager resourceManager, ProfilerFiller profilerFiller, ProfilerFiller profilerFiller1, Executor executor, Executor executor1) {
        CompletionStage completableFuture = CompletableFuture.supplyAsync(() -> PARTICLE_LISTER.listMatchingResources(resourceManager), executor).thenCompose(locationResourceMap -> {
            ArrayList list = new ArrayList(locationResourceMap.size());
            locationResourceMap.forEach((k, v) -> {
                ResourceLocation resourceLocation = PARTICLE_LISTER.fileToId(k);
                list.add(CompletableFuture.supplyAsync(() -> {
                    record ParticleDefinition(ResourceLocation resourceLocation, Optional<List<ResourceLocation>> sprites) {
                    }
                    return new ParticleDefinition(resourceLocation, this.loadParticleDescription(resourceLocation, (Resource)v));
                }, executor));
            });
            return Util.sequence(list);
        });
        CompletionStage completableFuture1 = SpriteLoader.create((TextureAtlas)this.textureAtlas).loadAndStitch(resourceManager, PARTICLES_ATLAS_INFO, 0, executor).thenCompose(SpriteLoader.Preparations::waitForUpload);
        return ((CompletableFuture)CompletableFuture.allOf(new CompletableFuture[]{completableFuture1, completableFuture}).thenCompose(arg_0 -> ((PreparableReloadListener.PreparationBarrier)preparationBarrier).wait(arg_0))).thenAcceptAsync(arg_0 -> this.lambda$reload$5(profilerFiller1, (CompletableFuture)completableFuture1, (CompletableFuture)completableFuture, arg_0), executor1);
    }

    private Optional<List<ResourceLocation>> loadParticleDescription(ResourceLocation resourceLocation, Resource resource) {
        Optional<List<ResourceLocation>> optional;
        block9: {
            if (!this.spriteSets.containsKey(resourceLocation)) {
                PMWeather.LOGGER.debug("Redundant texture list for particle: {}", (Object)resourceLocation);
                return Optional.empty();
            }
            BufferedReader reader = resource.openAsReader();
            try {
                ParticleDescription particleDescription = ParticleDescription.fromJson((JsonObject)GsonHelper.parse((Reader)reader));
                optional = Optional.of(particleDescription.getTextures());
                if (reader == null) break block9;
            }
            catch (Throwable throwable) {
                try {
                    if (reader != null) {
                        try {
                            ((Reader)reader).close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                catch (IOException e) {
                    throw new IllegalStateException("Failed to load description for particle " + String.valueOf(resourceLocation), e);
                }
            }
            ((Reader)reader).close();
        }
        return optional;
    }

    @Nullable
    private <T extends ParticleOptions> Particle makeParticle(T particleOptions, double x, double y, double z, double xMotion, double yMotion, double zMotion) {
        ParticleProvider<?> particleProvider = this.providers.get(BuiltInRegistries.PARTICLE_TYPE.getKey((Object)particleOptions.getType()));
        return particleProvider == null ? null : particleProvider.createParticle(particleOptions, this.level, x, y, z, xMotion, yMotion, zMotion);
    }

    public void add(Particle particle) {
        Optional optional = particle.getParticleGroup();
        if (optional.isPresent()) {
            if (this.hasSpaceInParticleLimit((ParticleGroup)optional.get())) {
                this.particlesToAdd.add(particle);
                this.updateCount((ParticleGroup)optional.get(), 1);
            }
        } else {
            this.particlesToAdd.add(particle);
        }
    }

    public void tick() {
        this.level.getProfiler().push("pmweather_particle_tick");
        this.particles.forEach((particleRenderType, particles1) -> {
            this.level.getProfiler().push("pmweather_particle_tick_" + particleRenderType.toString());
            this.tickParticleList((Collection<Particle>)particles1);
            this.level.getProfiler().pop();
        });
        if (!this.trackingEmitters.isEmpty()) {
            ArrayList list = Lists.newArrayList();
            for (TrackingEmitter trackingEmitter : this.trackingEmitters) {
                trackingEmitter.tick();
                if (trackingEmitter.isAlive()) continue;
                list.add(trackingEmitter);
            }
            this.trackingEmitters.removeAll(list);
        }
        if (!this.particlesToAdd.isEmpty()) {
            Particle particle;
            while ((particle = this.particlesToAdd.poll()) != null) {
                this.particles.computeIfAbsent(particle.getRenderType(), particleRenderType -> EvictingQueue.create((int)32768)).add(particle);
            }
        }
        this.level.getProfiler().pop();
    }

    private void tickParticleList(Collection<Particle> particles) {
        if (!particles.isEmpty()) {
            Iterator<Particle> iterator = particles.iterator();
            while (iterator.hasNext()) {
                Particle particle = iterator.next();
                this.tickParticle(particle);
                if (particle.isAlive()) continue;
                particle.getParticleGroup().ifPresent(particleGroup -> this.updateCount((ParticleGroup)particleGroup, -1));
                iterator.remove();
            }
        }
    }

    private void updateCount(ParticleGroup particleGroup, int count) {
        this.trackedParticleCounts.addTo((Object)particleGroup, count);
    }

    private void tickParticle(Particle particle) {
        try {
            particle.tick();
        }
        catch (Throwable throwable) {
            CrashReport crashReport = CrashReport.forThrowable((Throwable)throwable, (String)"Ticking Particle");
            CrashReportCategory crashReportCategory = crashReport.addCategory("Particle being ticked");
            crashReportCategory.setDetail("Particle", () -> ((Particle)particle).toString());
            crashReportCategory.setDetail("Particle Type", () -> ((ParticleRenderType)particle.getRenderType()).toString());
            throw new ReportedException(crashReport);
        }
    }

    public void render(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, LightTexture lightTexture, Camera camera, float partialTicks, @Nullable Frustum frustum) {
        this.level.getProfiler().push("pmweather_particle_render");
        float fogStart = RenderSystem.getShaderFogStart();
        float fogEnd = RenderSystem.getShaderFogEnd();
        RenderSystem.setShaderFogStart((float)fogStart);
        RenderSystem.setShaderFogEnd((float)(fogEnd * 2.0f));
        lightTexture.turnOnLightLayer();
        RenderSystem.enableDepthTest();
        RenderSystem.activeTexture((int)33986);
        RenderSystem.activeTexture((int)33984);
        Matrix4fStack matrix4fStack = RenderSystem.getModelViewStack();
        matrix4fStack.pushMatrix();
        matrix4fStack.mul((Matrix4fc)poseStack.last().pose());
        RenderSystem.applyModelViewMatrix();
        RenderSystem.disableCull();
        boolean particleCount = false;
        for (ParticleRenderType particleRenderType : this.particles.keySet()) {
            this.level.getProfiler().push(particleRenderType.toString());
            if (particleRenderType == ParticleRenderType.NO_RENDER) continue;
            Iterable iterable = this.particles.get(particleRenderType);
            if (iterable != null) {
                RenderSystem.setShader(GameRenderer::getParticleShader);
                Tesselator tesselator = Tesselator.getInstance();
                BufferBuilder bufferBuilder = particleRenderType.begin(tesselator, this.textureManager);
                HashMap sortedList = new HashMap();
                int maxRenderOrder = 0;
                for (Particle particle : iterable) {
                    int renderOrder = 10;
                    if (particle instanceof EntityRotFX) {
                        EntityRotFX entityRotFX = (EntityRotFX)particle;
                        renderOrder = entityRotFX.renderOrder;
                    }
                    if (renderOrder > maxRenderOrder) {
                        maxRenderOrder = renderOrder;
                    }
                    if (sortedList.containsKey(renderOrder)) {
                        ((List)sortedList.get(renderOrder)).add(particle);
                        continue;
                    }
                    ArrayList<Particle> list = new ArrayList<Particle>();
                    list.add(particle);
                    sortedList.put(renderOrder, list);
                }
                for (int i = 0; i <= maxRenderOrder; ++i) {
                    if (!sortedList.containsKey(i)) continue;
                    List particlesSorted = (List)sortedList.get(i);
                    particlesSorted.sort((p1, p2) -> {
                        double d1 = p1.getPos().distanceToSqr(camera.getPosition());
                        double d2 = p2.getPos().distanceToSqr(camera.getPosition());
                        return Double.compare(d2, d1);
                    });
                    for (Particle particle : particlesSorted) {
                        if (particle instanceof EntityRotFX) {
                            EntityRotFX entityRotFX = (EntityRotFX)particle;
                            if (camera.getPosition().distanceToSqr(particle.getPos()) > (double)(entityRotFX.renderRange * entityRotFX.renderRange) || frustum != null && !frustum.isVisible(entityRotFX.getBoundingBoxForRender())) {
                                continue;
                            }
                        } else if (camera.getPosition().distanceToSqr(particle.getPos()) > 65536.0 || frustum != null && !frustum.isVisible(particle.getRenderBoundingBox(partialTicks))) continue;
                        if (camera.getPosition().distanceToSqr(particle.getPos()) > (double)(ClientConfig.maxParticleSpawnDistanceFromPlayer * ClientConfig.maxParticleSpawnDistanceFromPlayer)) continue;
                        try {
                            particle.render((VertexConsumer)bufferBuilder, camera, partialTicks);
                        }
                        catch (Throwable throwable) {
                            CrashReport crashReport = CrashReport.forThrowable((Throwable)throwable, (String)"Rendering Particle");
                            CrashReportCategory crashReportCategory = crashReport.addCategory("Particle being rendered");
                            crashReportCategory.setDetail("Particle", () -> ((Particle)particle).toString());
                            crashReportCategory.setDetail("Particle Type", () -> ((ParticleRenderType)particleRenderType).toString());
                            throw new ReportedException(crashReport);
                        }
                    }
                }
                MeshData meshData = bufferBuilder.build();
                if (meshData != null) {
                    BufferUploader.drawWithShader((MeshData)meshData);
                }
            }
            this.level.getProfiler().pop();
        }
        matrix4fStack.popMatrix();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.depthMask((boolean)true);
        RenderSystem.disableBlend();
        lightTexture.turnOffLightLayer();
        RenderSystem.setShaderFogStart((float)fogStart);
        RenderSystem.setShaderFogEnd((float)fogEnd);
        this.level.getProfiler().pop();
    }

    public void setLevel(@Nullable ClientLevel level) {
        this.level = level;
        this.clearParticles();
        this.trackingEmitters.clear();
    }

    private boolean hasSpaceInParticleLimit(ParticleGroup particleGroup) {
        return this.trackedParticleCounts.getInt((Object)particleGroup) < particleGroup.getLimit();
    }

    public void clearParticles() {
        this.particles.clear();
        this.particlesToAdd.clear();
        this.trackingEmitters.clear();
        this.trackedParticleCounts.clear();
    }

    public Map<ParticleRenderType, Queue<Particle>> getParticles() {
        return this.particles;
    }

    private /* synthetic */ void lambda$reload$5(ProfilerFiller profilerFiller1, CompletableFuture completableFuture1, CompletableFuture completableFuture, Void v) {
        this.clearParticles();
        profilerFiller1.startTick();
        profilerFiller1.push("upload");
        SpriteLoader.Preparations preparations = (SpriteLoader.Preparations)completableFuture1.join();
        this.textureAtlas.upload(preparations);
        profilerFiller1.popPush("bindSpriteSets");
        HashSet set = new HashSet();
        TextureAtlasSprite textureAtlasSprite = preparations.missing();
        ((List)completableFuture.join()).forEach(particleDefinition -> {
            Optional<List<ResourceLocation>> optionalResourceLocations = particleDefinition.sprites();
            if (!optionalResourceLocations.isEmpty()) {
                ArrayList<TextureAtlasSprite> textureAtlasSprites = new ArrayList<TextureAtlasSprite>();
                for (ResourceLocation resourceLocation : optionalResourceLocations.get()) {
                    TextureAtlasSprite textureAtlasSprite1 = (TextureAtlasSprite)preparations.regions().get(resourceLocation);
                    if (textureAtlasSprite1 == null) {
                        set.add(resourceLocation);
                        textureAtlasSprites.add(textureAtlasSprite);
                        continue;
                    }
                    textureAtlasSprites.add(textureAtlasSprite1);
                }
                if (textureAtlasSprites.isEmpty()) {
                    textureAtlasSprites.add(textureAtlasSprite);
                }
                this.spriteSets.get(particleDefinition.resourceLocation()).rebind(textureAtlasSprites);
            }
        });
        if (!set.isEmpty()) {
            PMWeather.LOGGER.warn("Missing particle sprites: {}", (Object)set.stream().sorted().map(ResourceLocation::toString).collect(Collectors.joining(",")));
        }
        profilerFiller1.pop();
        profilerFiller1.endTick();
    }

    static class MutableSpriteSet
    implements SpriteSet {
        private List<TextureAtlasSprite> sprites;

        MutableSpriteSet() {
        }

        public TextureAtlasSprite get(int i, int i1) {
            return this.sprites.get(i * (this.sprites.size() - 1) / i1);
        }

        public TextureAtlasSprite get(RandomSource randomSource) {
            return this.sprites.get(randomSource.nextInt(this.sprites.size()));
        }

        public void rebind(List<TextureAtlasSprite> textureAtlasSprites) {
            this.sprites = ImmutableList.copyOf(textureAtlasSprites);
        }
    }
}

