package com.restonic4.logistics.experiment;

import com.restonic4.logistics.experiment.KineticCrystalShardItem;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class CrystalShardPotionRecipe extends CustomRecipe {
    public CrystalShardPotionRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        boolean foundShard = false;
        boolean foundPotion = false;

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) {
                if (stack.getItem() instanceof KineticCrystalShardItem) {
                    // Only allow 1 shard.
                    // (We removed the check that rejects shards with existing potions!)
                    if (foundShard) {
                        return false;
                    }
                    foundShard = true;
                } else if (stack.getItem() instanceof PotionItem) {
                    // Only allow 1 potion. Reject empty/invalid potions.
                    if (foundPotion || PotionUtils.getPotion(stack) == Potions.EMPTY) {
                        return false;
                    }
                    foundPotion = true;
                } else {
                    return false; // No other items allowed in the grid
                }
            }
        }
        return foundShard && foundPotion;
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess registryAccess) {
        ItemStack shard = ItemStack.EMPTY;
        ItemStack potionStack = ItemStack.EMPTY;

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) {
                if (stack.getItem() instanceof KineticCrystalShardItem) shard = stack;
                else if (stack.getItem() instanceof PotionItem) potionStack = stack;
            }
        }

        // Copy the shard to retain its energy, color, etc.
        ItemStack result = shard.copy();

        Potion potion = PotionUtils.getPotion(potionStack);
        if (potion != Potions.EMPTY) {
            ResourceLocation potionId = BuiltInRegistries.POTION.getKey(potion);
            result.getOrCreateTag().putString(KineticCrystalShardItem.POTION_KEY, potionId.toString());
        }

        return result;
    }

    // This is the magic method that leaves the empty bottle in the grid!
    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer container) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(container.getContainerSize(), ItemStack.EMPTY);

        for (int i = 0; i < remaining.size(); ++i) {
            ItemStack stack = container.getItem(i);
            if (stack.getItem() instanceof PotionItem) {
                remaining.set(i, new ItemStack(Items.GLASS_BOTTLE));
            } else if (stack.getItem().hasCraftingRemainingItem()) {
                remaining.set(i, new ItemStack(stack.getItem().getCraftingRemainingItem()));
            }
        }
        return remaining;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Recipes.KINETIC_CRYSTAL_SHARD_RECIPE;
    }
}