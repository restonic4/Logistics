package com.restonic4.logistics.mixin;

import com.restonic4.logistics.registry.PlatformRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;
import java.util.function.Supplier;

@Mixin(CreativeModeTab.class)
public abstract class CreativeModeTabMixin {
    @Shadow private Collection<ItemStack> displayItems;
    @Shadow private Set<ItemStack> displayItemsSearchTab;

    @Inject(method = "buildContents", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/CreativeModeTab;rebuildSearchTree()V"))
    private void logistics$injectItems(CreativeModeTab.ItemDisplayParameters parameters, CallbackInfo ci) {
        final CreativeModeTab self = (CreativeModeTab) (Object) this;

        final ResourceKey<CreativeModeTab> tabKey = BuiltInRegistries.CREATIVE_MODE_TAB
                .getResourceKey(self)
                .orElseThrow(() -> new IllegalStateException("Creative tab has not been registered: " + self));

        if (self.isAlignedRight() && tabKey != CreativeModeTabs.OP_BLOCKS) {
            return;
        }

        List<Supplier<Item>> injections = PlatformRegistry.getCreativeTabInjections(tabKey);
        if (injections.isEmpty()) return;

        LinkedList<ItemStack> mutableDisplay = new LinkedList<>(displayItems);
        LinkedList<ItemStack> mutableSearchTab = new LinkedList<>(displayItemsSearchTab);

        for (Supplier<Item> supplier : injections) {
            Item item = supplier.get();
            if (item == null) continue;

            ItemStack stack = new ItemStack(item);
            mutableDisplay.add(stack);
            mutableSearchTab.add(stack);
        }

        displayItems.clear();
        displayItems.addAll(mutableDisplay);

        displayItemsSearchTab.clear();
        displayItemsSearchTab.addAll(mutableSearchTab);
    }
}