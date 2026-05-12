package com.restonic4.logistics.mixin.registries;

import com.restonic4.logistics.Constants;
import com.restonic4.logistics.registry.SoundRegistry;
import com.restonic4.logistics.registry.builders.SoundBuilder;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

@Mixin(SoundManager.class)
public class SoundManagerMixin {
    @Mutable @Final @Shadow private Map<ResourceLocation, WeighedSoundEvents> registry;

    @Inject(
            method = "apply(Lnet/minecraft/client/sounds/SoundManager$Preparations;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At("TAIL")
    )
    private void logistics$injectSounds(
            SoundManager.Preparations preparations,
            ResourceManager resourceManager,
            ProfilerFiller profiler,
            CallbackInfo ci
    ) {
        Map<ResourceLocation, SoundBuilder.SoundEventData> injections = SoundRegistry.getAndFreeze();
        if (injections.isEmpty()) return;

        if (!(this.registry instanceof HashMap)) {
            this.registry = new HashMap<>(this.registry);
        }

        injections.forEach((id, data) -> {
            WeighedSoundEvents weighed = new WeighedSoundEvents(id, data.subtitle());

            for (SoundBuilder.SoundDefinition def : data.sounds()) {
                Sound.Type type = def.type() == SoundBuilder.SoundDefinition.Type.FILE
                        ? Sound.Type.FILE
                        : Sound.Type.SOUND_EVENT;

                // Constructor from your decompiled source:
                // String location, SampledFloat volume, SampledFloat pitch, int weight,
                // Type type, boolean stream, boolean preload, int attenuationDistance
                Sound sound = new Sound(
                        def.name().toString(),
                        (RandomSource r) -> def.volume(),
                        (RandomSource r) -> def.pitch(),
                        def.weight(),
                        type,
                        def.shouldStream(),
                        def.shouldPreload(),
                        def.attenuationDistance()
                );

                weighed.addSound(sound);
            }

            this.registry.putIfAbsent(id, weighed);
            Constants.LOG.info("Injected sound event: {} ({} sounds)", id, data.sounds().size());
        });
    }
}