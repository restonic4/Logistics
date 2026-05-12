package com.restonic4.logistics.registry.entries;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public final class SoundEventEntry {
    private final ResourceLocation id;
    private boolean loaded = false;
    @Nullable private Supplier<SoundEvent> soundEvent;

    public SoundEventEntry(ResourceLocation id) {
        this.id = id;
    }

    public void markLoaded(@Nullable Supplier<SoundEvent> soundEvent) {
        this.soundEvent = soundEvent;
        this.loaded = true;
    }

    private void assertLoaded() {
        if (!loaded) throw new IllegalStateException("SoundEventEntry '" + id + "' accessed before platform registration completed.");
    }

    public ResourceLocation getId() { return id; }

    public SoundEvent getSoundEvent() {
        assertLoaded();
        if (soundEvent == null) throw new IllegalStateException("Entry '" + id + "' has no sound event.");
        return soundEvent.get();
    }

    @Nullable public SoundEvent getSoundEventOrNull() {
        assertLoaded();
        return soundEvent != null ? soundEvent.get() : null;
    }
}