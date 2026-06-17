package com.restonic4.logistics.mixin.registries;

import com.google.common.collect.ImmutableMap;
import com.restonic4.logistics.registry.variants.VariantPackSource;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.TreeMap;

/**
 * Injects the generated variant pack into every {@link PackRepository} (both the client resource
 * repository and the server data repository), so it is discovered and selected on every reload.
 * This is the single hook through which all runtime-generated variant resources enter the game —
 * the same "mixin the loader" idiom as {@code TagLoaderMixin} / {@code LootDataManagerMixin}.
 *
 * <p>We hook {@code discoverAvailable()} (the per-reload pack collection) rather than the
 * constructor's {@code sources} field on purpose: Create's Porting Lib also mixes into this class
 * and mutates {@code sources} after construction (its {@code AddPackFindersEvent}). Reassigning
 * {@code sources} there raced with that and intermittently left it immutable, crashing Porting
 * Lib's {@code sources.add(...)}. Adding to the discovered-pack map instead touches nothing shared.
 */
@Mixin(PackRepository.class)
public class PackRepositoryMixin {
    @Inject(method = "discoverAvailable", at = @At("RETURN"), cancellable = true)
    private void logistics$addGeneratedPack(CallbackInfoReturnable<Map<String, Pack>> cir) {
        Map<String, Pack> available = new TreeMap<>(cir.getReturnValue());
        new VariantPackSource().loadPacks(pack -> available.put(pack.getId(), pack));
        cir.setReturnValue(ImmutableMap.copyOf(available));
    }
}
