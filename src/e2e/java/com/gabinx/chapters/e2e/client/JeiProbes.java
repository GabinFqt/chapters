package com.gabinx.chapters.e2e.client;

import com.gabinx.chapters.compat.jei.ChaptersJeiModPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

/** Reads JEI visibility. Hidden ingredients and recipes are what the player would not see. */
public final class JeiProbes {
    private JeiProbes() {
    }

    public static boolean ready() {
        return ChaptersJeiModPlugin.runtimeOrNull() != null;
    }

    public static boolean itemVisible(Item item) {
        IJeiRuntime runtime = ChaptersJeiModPlugin.runtimeOrNull();
        if (runtime == null) {
            return false;
        }
        return runtime.getJeiHelpers().getIngredientVisibility().isIngredientVisible(VanillaTypes.ITEM_STACK, new ItemStack(item));
    }

    public static boolean fluidVisible(Fluid fluid) {
        IJeiRuntime runtime = ChaptersJeiModPlugin.runtimeOrNull();
        if (runtime == null) {
            return false;
        }
        return runtime.getJeiHelpers().getIngredientVisibility().isIngredientVisible(NeoForgeTypes.FLUID_STACK, new FluidStack(fluid, 1000));
    }

    /** True when the item is in the list the player scrolls, not only marked visible. */
    public static boolean itemListed(Item item) {
        IJeiRuntime runtime = ChaptersJeiModPlugin.runtimeOrNull();
        if (runtime == null) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        return runtime.getIngredientFilter().getFilteredItemStacks().stream()
                .anyMatch(stack -> id.equals(BuiltInRegistries.ITEM.getKey(stack.getItem())));
    }

    public static boolean craftingRecipeVisible(ResourceLocation recipeId, Item output) {
        return recipeVisible(RecipeTypes.CRAFTING, recipeId, output);
    }

    public static boolean spyglassRecipeVisible() {
        return craftingRecipeVisible(ResourceLocation.fromNamespaceAndPath("minecraft", "spyglass"), Items.SPYGLASS);
    }

    /** Nether quartz ore still smelts while quartz itself stays listed ({@code hideInJei: false}). */
    public static boolean quartzSmeltingVisible() {
        return recipeVisible(RecipeTypes.SMELTING, ResourceLocation.fromNamespaceAndPath("minecraft", "quartz"), Items.QUARTZ);
    }

    private static boolean recipeVisible(RecipeType<?> type, ResourceLocation recipeId, Item output) {
        IJeiRuntime runtime = ChaptersJeiModPlugin.runtimeOrNull();
        if (runtime == null) {
            return false;
        }
        var focus = runtime.getJeiHelpers().getFocusFactory()
                .createFocus(RecipeIngredientRole.OUTPUT, VanillaTypes.ITEM_STACK, new ItemStack(output));
        return runtime.getRecipeManager()
                .createRecipeLookup(type)
                .limitFocus(List.of(focus))
                .get()
                .anyMatch(holder -> holder instanceof net.minecraft.world.item.crafting.RecipeHolder<?> recipe
                        && recipeId.equals(recipe.id()));
    }
}
