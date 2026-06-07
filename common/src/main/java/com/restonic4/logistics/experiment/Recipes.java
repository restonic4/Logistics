package com.restonic4.logistics.experiment;

import com.restonic4.logistics.Logistics;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;

public class Recipes {
    public static RecipeSerializer<CrystalShardPotionRecipe> KINETIC_CRYSTAL_SHARD_RECIPE = new SimpleCraftingRecipeSerializer<>(CrystalShardPotionRecipe::new);

    public static void register() {
        Registry.register(
                BuiltInRegistries.RECIPE_SERIALIZER,
                Logistics.id("crystal_shard_potion"),
                KINETIC_CRYSTAL_SHARD_RECIPE
        );
    }
}
