package com.restonic4.logistics.experiment;

import com.restonic4.logistics.Logistics;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public class Particles {
    public static final SimpleParticleType SPARK = new SimpleParticleType(false);

    public static void register() {
        Registry.register(
                BuiltInRegistries.PARTICLE_TYPE,
                Logistics.id("spark"),
                SPARK
        );
    }
}
