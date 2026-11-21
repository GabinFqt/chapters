package com.gabinx.chapters.e2e.client;

import com.gabinx.chapters.compat.jei.ChaptersJeiModPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
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

    public static boolean craftingRecipeVisible(Identifier recipeId, Item output) {
        IJeiRuntime runtime = ChaptersJeiModPlugin.runtimeOrNull();
        if (runtime == null) {
            return false;
        }
        var focus = runtime.getJeiHelpers().getFocusFactory()
                .createFocus(RecipeIngredientRole.OUTPUT, VanillaTypes.ITEM_STACK, new ItemStack(output));
        return runtime.getRecipeManager()
                .createRecipeLookup(RecipeTypes.CRAFTING)
                .limitFocus(List.of(focus))
                .get()
                .anyMatch(holder -> recipeId.equals(holder.id().identifier()));
    }

    public static boolean spyglassRecipeVisible() {
        return craftingRecipeVisible(Identifier.fromNamespaceAndPath("minecraft", "spyglass"), Items.SPYGLASS);
    }
}
