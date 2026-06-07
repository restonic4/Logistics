package com.restonic4.logistics.mixin.experiment;

import com.restonic4.logistics.experiment.Particles;
import com.restonic4.logistics.experiment.SparkParticle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleEngine.class)
public class ParticleEngineMixin {
    @Shadow
    private <T extends ParticleOptions> void register(ParticleType<T> particleType, ParticleEngine.SpriteParticleRegistration<T> particleMetaFactory) {
        throw new IllegalStateException("Shadow failure");
    }

    @Inject(method = "registerProviders", at = @At("RETURN"))
    private void onRegisterProviders(CallbackInfo ci) {
        this.register(Particles.SPARK, SparkParticle.Provider::new);
    }
}