package com.restonic4.logistics.mixin.audio;

import com.restonic4.logistics.audio.AudioDecoder;
import com.restonic4.logistics.audio.ClientAudioManager;
import net.minecraft.Util;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Lets audio-station sounds resolve their bytes from the client cache instead of the resource
 * pack. When the engine asks {@link SoundBufferLibrary} to stream one of our registered sound
 * locations, we supply a decoded WAV/OGG stream (with the right start offset + looping) and
 * cancel the vanilla resource-manager lookup. This is the single interception point that keeps
 * playback going through MC's engine on both Fabric and Forge.
 */
@Mixin(SoundBufferLibrary.class)
public class SoundBufferLibraryMixin {

    @Inject(method = "getStream", at = @At("HEAD"), cancellable = true)
    private void logistics$getStream(ResourceLocation location, boolean looping,
                                     CallbackInfoReturnable<CompletableFuture<AudioStream>> cir) {
        ClientAudioManager.Playback playback = ClientAudioManager.getPlayback(location);
        if (playback == null) return;

        CompletableFuture<AudioStream> future = CompletableFuture.supplyAsync(() -> {
            try {
                return AudioDecoder.open(playback.cacheFile(), playback.startMs(), playback.loop());
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, Util.backgroundExecutor());

        cir.setReturnValue(future);
    }
}
