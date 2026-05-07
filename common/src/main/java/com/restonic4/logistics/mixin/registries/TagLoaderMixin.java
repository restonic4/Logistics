package com.restonic4.logistics.mixin.registries;

import com.restonic4.logistics.Constants;
import com.restonic4.logistics.Logistics;
import com.restonic4.logistics.registry.PlatformRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagEntry;
import net.minecraft.tags.TagLoader;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mixin(TagLoader.class)
public class TagLoaderMixin {
    @Final @Shadow private String directory;

    @Inject(method = "load", at = @At("RETURN"))
    private void logistics$onLoad(ResourceManager manager, CallbackInfoReturnable<Map<ResourceLocation, List<TagLoader.EntryWithSource>>> cir) {
        if (!directory.equals("tags/blocks")) return;

        Map<ResourceLocation, List<TagLoader.EntryWithSource>> map = cir.getReturnValue();
        PlatformRegistry.getAndFreezeBlockTagInjections().forEach((tagId, blockIds) ->
                blockIds.forEach(blockId -> logistics$addToTag(map, tagId, blockId))
        );
    }

    @Unique
    private void logistics$addToTag(
            Map<ResourceLocation, List<TagLoader.EntryWithSource>> map,
            ResourceLocation tagId,
            ResourceLocation entryId
    ) {
        TagEntry tagEntry = TagEntry.element(entryId);
        TagLoader.EntryWithSource entryWithSource = new TagLoader.EntryWithSource(tagEntry, Constants.MOD_ID + ":datagen");

        map.computeIfAbsent(tagId, k -> new ArrayList<>()).add(entryWithSource);
        Constants.LOG.info("Injected tag: {} -> {}", entryId, tagId);
    }
}
