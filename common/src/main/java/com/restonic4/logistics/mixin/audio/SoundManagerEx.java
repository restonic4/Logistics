package com.restonic4.logistics.mixin.audio;

import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SoundManager.class)
public interface SoundManagerEx {
    @Accessor("soundEngine")
    SoundEngine logistics$getSoundEngine();
}