package com.restonic4.logistics.registry.builders;

import com.restonic4.logistics.platform.Services;
import com.restonic4.logistics.registry.SoundRegistry;
import com.restonic4.logistics.registry.entries.SoundEventEntry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class SoundBuilder {
    private final ResourceLocation id;
    private String subtitle;
    private boolean replace = false;
    private final List<SoundDefinition> sounds = new ArrayList<>();
    private float fixedRange = -1; // < 0 = variable range

    public SoundBuilder(ResourceLocation id) {
        this.id = id;
    }

    public SoundBuilder subtitle(String subtitleTranslationKey) {
        this.subtitle = subtitleTranslationKey;
        return this;
    }

    public SoundBuilder replace() {
        this.replace = true;
        return this;
    }

    /** Fixed range = non-attenuating (e.g. UI sounds). Default is variable range. */
    public SoundBuilder fixedRange(float range) {
        this.fixedRange = range;
        return this;
    }

    /** Path relative to assets/&lt;namespace&gt;/sounds/ */
    public SoundBuilder sound(String path) {
        return sound(new SoundDefinition(new ResourceLocation(id.getNamespace(), path)));
    }

    public SoundBuilder sound(ResourceLocation path) {
        return sound(new SoundDefinition(path));
    }

    public SoundBuilder sound(SoundDefinition definition) {
        this.sounds.add(definition);
        return this;
    }

    public SoundEventEntry register() {
        Supplier<SoundEvent> factory = fixedRange > 0
                ? () -> SoundEvent.createFixedRangeEvent(id, fixedRange)
                : () -> SoundEvent.createVariableRangeEvent(id);

        SoundEventEntry entry = Services.PLATFORM_REGISTRY.fromSoundBuilder(id, factory);
        SoundRegistry.register(id, new SoundEventData(subtitle, replace, List.copyOf(sounds)));
        return entry;
    }

    public record SoundEventData(String subtitle, boolean replace, List<SoundDefinition> sounds) {}

    public static class SoundDefinition {
        private final ResourceLocation name;
        private float volume = 1.0f;
        private float pitch = 1.0f;
        private int weight = 1;
        private boolean stream = false;
        private boolean preload = false;
        private int attenuationDistance = 16;
        private Type type = Type.FILE;

        public SoundDefinition(ResourceLocation name) {
            this.name = name;
        }

        // Setters (builder chain)
        public SoundDefinition volume(float v) { this.volume = v; return this; }
        public SoundDefinition pitch(float p) { this.pitch = p; return this; }
        public SoundDefinition weight(int w) { this.weight = w; return this; }
        public SoundDefinition stream() { this.stream = true; return this; }
        public SoundDefinition preload() { this.preload = true; return this; }
        public SoundDefinition attenuationDistance(int d) { this.attenuationDistance = d; return this; }
        public SoundDefinition type(Type t) { this.type = t; return this; }

        // Getters — renamed so they do not collide with the setters above
        public ResourceLocation name() { return name; }
        public float volume() { return volume; }
        public float pitch() { return pitch; }
        public int weight() { return weight; }
        public boolean shouldStream() { return stream; }
        public boolean shouldPreload() { return preload; }
        public int attenuationDistance() { return attenuationDistance; }
        public Type type() { return type; }

        public enum Type { FILE, EVENT }
    }
}