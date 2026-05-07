package com.restonic4.logistics.mixin.registries;

import com.restonic4.logistics.Constants;
import com.restonic4.logistics.registry.PlatformRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

@Mixin(LootDataManager.class)
public class LootDataManagerMixin {
    @Inject(method = "apply", at = @At("HEAD"))
    @SuppressWarnings("unchecked")
    private void logistics$onApply(Map<LootDataType<?>, Map<ResourceLocation, ?>> data, CallbackInfo ci) {
        Map<ResourceLocation, Object> tables = (Map<ResourceLocation, Object>) data.computeIfAbsent(LootDataType.TABLE, k -> new HashMap<>());

        PlatformRegistry.getAndFreezeSelfDropLootInjections().forEach(entry -> {
            ResourceLocation tableId = new ResourceLocation(entry.blockId().getNamespace(), "blocks/" + entry.blockId().getPath());

            LootPool.Builder pool = LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1))
                    .add(LootItem.lootTableItem(
                            BuiltInRegistries.BLOCK.get(entry.blockId()).asItem()
                    ));

            if (entry.survivesExplosion()) {
                pool.when(ExplosionCondition.survivesExplosion());
            }

            LootTable table = LootTable.lootTable()
                    .withPool(pool)
                    .setParamSet(LootContextParamSets.BLOCK)
                    .build();

            tables.putIfAbsent(tableId, table);
            Constants.LOG.info("Injected loot table: {} (survivesExplosion={})", entry.blockId(), entry.survivesExplosion());
        });
    }
}
