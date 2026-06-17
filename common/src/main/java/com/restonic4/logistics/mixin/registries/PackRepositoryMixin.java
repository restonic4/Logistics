package com.restonic4.logistics.mixin.registries;

import com.google.common.collect.ImmutableSet;
import com.restonic4.logistics.registry.variants.VariantPackSource;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.RepositorySource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Adds the {@link VariantPackSource} to every {@link PackRepository} (both the client resource
 * repository and the server data repository), so our generated pack is discovered and selected
 * during reloads. This is the single hook through which all runtime-generated variant resources
 * enter the game — the same "mixin the loader, read from a registry" idiom as
 * {@code TagLoaderMixin} / {@code LootDataManagerMixin}, one level up at the pack source.
 */
@Mixin(PackRepository.class)
public class PackRepositoryMixin {
    @Mutable
    @Shadow
    @Final
    private Set<RepositorySource> sources;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void logistics$addGeneratedSource(RepositorySource[] sources, CallbackInfo ci) {
        Set<RepositorySource> combined = new LinkedHashSet<>(this.sources);
        combined.add(new VariantPackSource());
        this.sources = ImmutableSet.copyOf(combined);
    }
}
