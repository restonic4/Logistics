package com.restonic4.logistics.mixin.audio;

import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SoundEngine.class)
public interface SoundEngineEx {
    @Accessor("channelAccess")
    ChannelAccess logistics$getChannelAccess();
}