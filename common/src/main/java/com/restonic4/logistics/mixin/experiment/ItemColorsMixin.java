package com.restonic4.logistics.mixin.experiment;

import com.restonic4.logistics.experiment.CrystalShardItem;
import com.restonic4.logistics.experiment.Items;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemColors.class)
public class ItemColorsMixin {
    @Inject(method = "createDefault", at = @At("RETURN"))
    private static void registerCustomItemColors(BlockColors blockColors, CallbackInfoReturnable<ItemColors> cir) {
        ItemColors itemColors = cir.getReturnValue();

        itemColors.register((ItemStack stack, int tintIndex) -> {
            if (tintIndex == 0 && stack.getItem() instanceof CrystalShardItem crystalItem) {
                return crystalItem.getColor(stack);
            }
            return -1;
        }, Items.CRYSTAL_SHARD.getItem());
    }
}