package com.restonic4.logistics.blocks.lamp;

import com.restonic4.logistics.experiment.Sounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;

/**
 * A world-positioned looping hum for a single lamp that has static enabled. Its whole lifecycle
 * (start/stop) is owned by {@link LampStaticSoundManager}; this class is just a steady looping
 * instance anchored at the lamp.
 */
public class LampStaticSoundInstance extends AbstractTickableSoundInstance {
    private final BlockPos pos;

    public LampStaticSoundInstance(BlockPos pos) {
        super(Sounds.LAMP_STATIC.getSoundEvent(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.pos = pos.immutable();
        this.looping = true;
        this.delay = 0;
        this.volume = 0.6f;
        this.pitch = 1.0f;
        this.x = pos.getX() + 0.5;
        this.y = pos.getY() + 0.5;
        this.z = pos.getZ() + 0.5;
        this.attenuation = SoundInstance.Attenuation.LINEAR;
    }

    public BlockPos getPos() {
        return pos;
    }

    @Override
    public void tick() {
        // No per-tick work: LampStaticSoundManager stops us when the lamp is gone, the player toggles
        // static off, or we fall out of range.
    }
}
