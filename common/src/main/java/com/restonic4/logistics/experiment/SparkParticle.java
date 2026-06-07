package com.restonic4.logistics.experiment;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;

public class SparkParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    protected SparkParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, SpriteSet sprites) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.sprites = sprites;

        // Apply incoming physics velocity vectors
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;

        // Tweak these values to make it look exactly how you want
        this.friction = 0.96F; // Drifts a bit before losing horizontal speed
        this.gravity = 0.4F; // Pulls them downward over time (arcs)
        this.quadSize *= 0.85F; // Scale size of the spark
        this.lifetime = 12 + this.random.nextInt(12); // Lifetime in ticks
        this.hasPhysics = false; // Allows the particle to pass through block bounds!

        this.setSpriteFromAge(sprites);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE; // Standard particle rendering sheet
    }

    @Override
    public void tick() {
        super.tick();
        // Cycles through the texture animation array as it ages
        this.setSpriteFromAge(this.sprites);
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new SparkParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites);
        }
    }
}
