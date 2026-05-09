package com.gabinx.chapters.compat.jei;

import com.gabinx.chapters.Chapters;
import com.gabinx.chapters.compat.RecipeViewerCompat;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.IRecipeLookup;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Hides locked items, fluids, Mekanism chemicals, locked recipe ids, and recipes that output locked items/fluids/chemicals
 * in JEI once the runtime exists.
 * Kept in this type so the mod can load without JEI (this class is loaded only when JEI scans for {@link JeiPlugin}).
 */
@JeiPlugin
public final class ChaptersJeiModPlugin implements IModPlugin {
    private static final Object LOCK = new Object();

    @Nullable
    private static IJeiRuntime runtime;

    private static final Set<ResourceLocation> lastLockedItems = new LinkedHashSet<>();
    private static final Set<ResourceLocation> lastLockedFluids = new LinkedHashSet<>();
    private static final Set<ResourceLocation> lastLockedChemicals = new LinkedHashSet<>();
    private static final Set<ResourceLocation> lastLockedRecipes = new LinkedHashSet<>();

    /** Item id → ingredient stacks removed from JEI so we can add them back verbatim. */
    private static final Map<ResourceLocation, List<ItemStack>> ingredientSnapshots = new HashMap<>();

    /** Fluid id → ingredient stacks removed from JEI so we can add them back verbatim. */
    private static final Map<ResourceLocation, List<FluidStack>> fluidIngredientSnapshots = new HashMap<>();

    /** Mekanism chemical id → ingredient stacks removed from JEI so we can add them back verbatim. */
    private static final Map<ResourceLocation, List<Object>> chemicalIngredientSnapshots = new HashMap<>();

    /** Item id → recipes hidden for this lock pass (list keeps duplicates in sync with ref counts). */
    private static final Map<ResourceLocation, List<HiddenRecipe>> recipesByLockingItem = new HashMap<>();

    private static final Map<ResourceLocation, List<HiddenRecipe>> recipesByLockingFluid = new HashMap<>();

    private static final Map<ResourceLocation, List<HiddenRecipe>> recipesByLockingChemical = new HashMap<>();

    private static final Map<ResourceLocation, List<HiddenRecipe>> recipesByLockingRecipeId = new HashMap<>();

    private static final Map<HiddenRecipe, Integer> recipeHideRefCount = new HashMap<>();

    static void applyLocked(
            Set<ResourceLocation> newLockedItems,
            Set<ResourceLocation> newLockedFluids,
            Set<ResourceLocation> newLockedChemicals,
            Set<ResourceLocation> newLockedRecipes
    ) {
        synchronized (LOCK) {
            if (runtime == null) {
                return;
            }

            IJeiRuntime jei = runtime;
            IIngredientManager ingredients = jei.getIngredientManager();
            IRecipeManager recipeManager = jei.getRecipeManager();
            Optional<IIngredientType<FluidStack>> fluidTypeOpt = ingredients.getIngredientTypeChecked(FluidStack.class);

            Set<ResourceLocation> itemsToLock = new LinkedHashSet<>(newLockedItems);
            itemsToLock.removeAll(lastLockedItems);
            Set<ResourceLocation> itemsToUnlock = new LinkedHashSet<>(lastLockedItems);
            itemsToUnlock.removeAll(newLockedItems);

            // Restore every newly-unlocked ingredient first, then reconcile recipes (golden apple crafting needs apple +
            // gold ingot stacks back in JEI before output-recipe lookups/re-hides reliably apply across versions).
            for (ResourceLocation id : itemsToUnlock) {
                restoreItemIngredients(ingredients, id);
            }
            for (ResourceLocation id : itemsToUnlock) {
                revealRecipesForItem(jei, recipeManager, id);
            }
            // Hide crafting recipes BEFORE removing stacks from JEI ingredients: focus-based recipe lookup relies on the
            // ingredient index still listing outputs; otherwise lookups return nothing, we never register hides, yet the
            // UI can stay inconsistent until unlock (golden apple unlocked but craft tab empty).
            hideOutputRecipesForItemsBatch(jei, recipeManager, itemsToLock);
            for (ResourceLocation id : itemsToLock) {
                hideIngredientsForItem(ingredients, id);
            }

            lastLockedItems.clear();
            lastLockedItems.addAll(newLockedItems);

            if (fluidTypeOpt.isPresent()) {
                IIngredientType<FluidStack> fluidType = fluidTypeOpt.get();
                Set<ResourceLocation> fluidsToLock = new LinkedHashSet<>(newLockedFluids);
                fluidsToLock.removeAll(lastLockedFluids);
                Set<ResourceLocation> fluidsToUnlock = new LinkedHashSet<>(lastLockedFluids);
                fluidsToUnlock.removeAll(newLockedFluids);

                for (ResourceLocation id : fluidsToUnlock) {
                    restoreFluidIngredients(ingredients, fluidType, id);
                }
                for (ResourceLocation id : fluidsToUnlock) {
                    revealRecipesForFluid(jei, recipeManager, fluidType, id);
                }
                for (ResourceLocation id : fluidsToLock) {
                    hideOutputRecipesForFluid(jei, recipeManager, fluidType, id);
                    hideIngredientsForFluid(ingredients, fluidType, id);
                }
            }

            lastLockedFluids.clear();
            lastLockedFluids.addAll(newLockedFluids);

            Optional<IIngredientType<Object>> chemicalTypeOpt = mekanismChemicalIngredientType();
            if (chemicalTypeOpt.isPresent()) {
                IIngredientType<Object> chemicalType = chemicalTypeOpt.get();
                Set<ResourceLocation> chemicalsToLock = new LinkedHashSet<>(newLockedChemicals);
                chemicalsToLock.removeAll(lastLockedChemicals);
                Set<ResourceLocation> chemicalsToUnlock = new LinkedHashSet<>(lastLockedChemicals);
                chemicalsToUnlock.removeAll(newLockedChemicals);

                for (ResourceLocation id : chemicalsToUnlock) {
                    restoreChemicalIngredients(ingredients, chemicalType, id);
                }
                for (ResourceLocation id : chemicalsToUnlock) {
                    revealRecipesForChemical(jei, recipeManager, chemicalType, id);
                }
                for (ResourceLocation id : chemicalsToLock) {
                    hideOutputRecipesForChemical(jei, recipeManager, chemicalType, id);
                    hideIngredientsForChemical(ingredients, chemicalType, id);
                }
            }

            lastLockedChemicals.clear();
            lastLockedChemicals.addAll(newLockedChemicals);

            Set<ResourceLocation> recipesToLock = new LinkedHashSet<>(newLockedRecipes);
            recipesToLock.removeAll(lastLockedRecipes);
            Set<ResourceLocation> recipesToUnlock = new LinkedHashSet<>(lastLockedRecipes);
            recipesToUnlock.removeAll(newLockedRecipes);

            for (ResourceLocation id : recipesToUnlock) {
                revealRecipesForRecipeId(recipeManager, id);
            }
            hideRecipesWithIds(jei, recipeManager, recipesToLock);

            lastLockedRecipes.clear();
            lastLockedRecipes.addAll(newLockedRecipes);
        }
    }

    private static void hideRecipesWithIds(
            IJeiRuntime jei,
            IRecipeManager recipeManager,
            Set<ResourceLocation> idsToLock
    ) {
        if (idsToLock.isEmpty()) {
            return;
        }
        Map<ResourceLocation, List<HiddenRecipe>> contributedById = new LinkedHashMap<>();
        for (ResourceLocation id : idsToLock) {
            contributedById.put(id, new ArrayList<>());
        }
        for (RecipeType<?> recipeType : jei.getJeiHelpers().getAllRecipeTypes().toList()) {
            IRecipeLookup<?> lookup = recipeManager.createRecipeLookup(recipeType);
            for (Object recipe : lookup.get().toList()) {
                if (!(recipe instanceof RecipeHolder<?> holder)) {
                    continue;
                }
                ResourceLocation hid = holder.id();
                if (!idsToLock.contains(hid)) {
                    continue;
                }
                HiddenRecipe hr = new HiddenRecipe(recipeType, recipe);
                contributedById.get(hid).add(hr);
                int refs = recipeHideRefCount.merge(hr, 1, Integer::sum);
                if (refs == 1) {
                    hideRecipesUnchecked(recipeManager, recipeType, List.of(recipe));
                }
            }
        }
        for (ResourceLocation rid : idsToLock) {
            List<HiddenRecipe> contribution = contributedById.get(rid);
            if (contribution != null && !contribution.isEmpty()) {
                recipesByLockingRecipeId.put(rid, List.copyOf(contribution));
            }
        }
    }

    private static void revealRecipesForRecipeId(IRecipeManager recipeManager, ResourceLocation recipeId) {
        List<HiddenRecipe> contributed = recipesByLockingRecipeId.remove(recipeId);
        revealContributedRecipes(recipeManager, contributed);
    }

    private static void restoreItemIngredients(IIngredientManager ingredients, ResourceLocation itemId) {
        List<ItemStack> stacks = ingredientSnapshots.remove(itemId);
        if (stacks != null && !stacks.isEmpty()) {
            ingredients.addIngredientsAtRuntime(VanillaTypes.ITEM_STACK, stacks);
        }
    }

    private static void restoreFluidIngredients(IIngredientManager ingredients, IIngredientType<FluidStack> fluidType, ResourceLocation fluidId) {
        List<FluidStack> stacks = fluidIngredientSnapshots.remove(fluidId);
        if (stacks != null && !stacks.isEmpty()) {
            ingredients.addIngredientsAtRuntime(fluidType, stacks);
        }
    }

    private static void hideIngredientsForItem(IIngredientManager ingredients, ResourceLocation itemId) {
        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (new ItemStack(item).isEmpty()) {
            return;
        }
        List<ItemStack> toHide = ingredients.getAllItemStacks().stream()
                .filter(s -> !s.isEmpty() && BuiltInRegistries.ITEM.getKey(s.getItem()).equals(itemId))
                .toList();
        if (!toHide.isEmpty()) {
            ingredients.removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, toHide);
            ingredientSnapshots.put(itemId, List.copyOf(toHide));
        }
    }

    private static void hideIngredientsForFluid(
        IIngredientManager ingredients,
        IIngredientType<FluidStack> fluidType,
        ResourceLocation fluidId
    ) {
        Fluid fluid = BuiltInRegistries.FLUID.get(fluidId);
        if (fluid == null || fluid == Fluids.EMPTY) {
            return;
        }
        List<FluidStack> toHide = ingredients.getAllIngredients(fluidType).stream()
                .filter(fs -> !fs.isEmpty() && BuiltInRegistries.FLUID.getKey(fs.getFluid()).equals(fluidId))
                .toList();
        if (!toHide.isEmpty()) {
            ingredients.removeIngredientsAtRuntime(fluidType, toHide);
            fluidIngredientSnapshots.put(fluidId, List.copyOf(toHide));
        }
    }

    /**
     * One pass per recipe type over combined output foci for every newly locked item (instead of N × recipeTypes).
     */
    private static void hideOutputRecipesForItemsBatch(
            IJeiRuntime jei,
            IRecipeManager recipeManager,
            Set<ResourceLocation> itemsToLock
    ) {
        if (itemsToLock.isEmpty()) {
            return;
        }

        HolderLookup.Provider registryAccess = safeRegistryAccess();
        if (registryAccess == null) {
            for (ResourceLocation itemId : itemsToLock) {
                hideOutputRecipesForItem(jei, recipeManager, itemId);
            }
            return;
        }

        List<IFocus<?>> allFoci = new ArrayList<>();
        for (ResourceLocation itemId : itemsToLock) {
            ItemStack probe = new ItemStack(BuiltInRegistries.ITEM.get(itemId));
            if (!probe.isEmpty()) {
                allFoci.add(jei.getJeiHelpers().getFocusFactory()
                        .createFocus(RecipeIngredientRole.OUTPUT, VanillaTypes.ITEM_STACK, probe));
            }
        }
        if (allFoci.isEmpty()) {
            return;
        }

        Map<ResourceLocation, List<HiddenRecipe>> contributedByItem = new LinkedHashMap<>();
        for (ResourceLocation id : itemsToLock) {
            contributedByItem.put(id, new ArrayList<>());
        }

        for (RecipeType<?> recipeType : jei.getJeiHelpers().getAllRecipeTypes().toList()) {
            IRecipeLookup<?> lookup = recipeManager.createRecipeLookup(recipeType);
            List<?> found = lookup.limitFocus(allFoci).get().toList();
            for (Object recipe : found) {
                HiddenRecipe hr = new HiddenRecipe(recipeType, recipe);
                Set<ResourceLocation> outputs = extractOutputItemIds(recipe, registryAccess);
                if (outputs.isEmpty()) {
                    continue;
                }
                for (ResourceLocation itemId : itemsToLock) {
                    if (outputs.contains(itemId)) {
                        contributedByItem.get(itemId).add(hr);
                        int refs = recipeHideRefCount.merge(hr, 1, Integer::sum);
                        if (refs == 1) {
                            hideRecipesUnchecked(recipeManager, recipeType, List.of(recipe));
                        }
                    }
                }
            }
        }

        for (ResourceLocation itemId : itemsToLock) {
            List<HiddenRecipe> contribution = contributedByItem.get(itemId);
            if (contribution != null && !contribution.isEmpty()) {
                recipesByLockingItem.put(itemId, List.copyOf(contribution));
            }
        }
    }

    @Nullable
    private static HolderLookup.Provider safeRegistryAccess() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null ? mc.level.registryAccess() : null;
    }

    private static Set<ResourceLocation> extractOutputItemIds(Object recipe, HolderLookup.Provider registryAccess) {
        Set<ResourceLocation> ids = new LinkedHashSet<>();
        if (recipe instanceof RecipeHolder<?> holder && holder.value() instanceof Recipe<?> value) {
            ItemStack out = value.getResultItem(registryAccess);
            if (!out.isEmpty()) {
                ResourceLocation key = BuiltInRegistries.ITEM.getKey(out.getItem());
                if (key != null) {
                    ids.add(key);
                }
            }
        }
        return ids;
    }

    private static void hideOutputRecipesForItem(IJeiRuntime jei, IRecipeManager recipeManager, ResourceLocation itemId) {
        ItemStack probe = new ItemStack(BuiltInRegistries.ITEM.get(itemId));
        if (probe.isEmpty()) {
            return;
        }

        IFocus<ItemStack> focus = jei.getJeiHelpers().getFocusFactory()
                .createFocus(RecipeIngredientRole.OUTPUT, VanillaTypes.ITEM_STACK, probe);
        List<? extends IFocus<?>> foci = List.of(focus);

        List<HiddenRecipe> contributed = collectOutputRecipes(jei, recipeManager, foci);
        if (!contributed.isEmpty()) {
            recipesByLockingItem.put(itemId, List.copyOf(contributed));
        }
    }

    private static void hideOutputRecipesForFluid(
        IJeiRuntime jei,
        IRecipeManager recipeManager,
        IIngredientType<FluidStack> fluidType,
        ResourceLocation fluidId
    ) {
        Fluid fluid = BuiltInRegistries.FLUID.get(fluidId);
        if (fluid == null || fluid == Fluids.EMPTY) {
            return;
        }
        FluidStack probe = new FluidStack(fluid, 1000);
        if (probe.isEmpty()) {
            return;
        }

        IFocus<FluidStack> focus = jei.getJeiHelpers().getFocusFactory()
                .createFocus(RecipeIngredientRole.OUTPUT, fluidType, probe);
        List<? extends IFocus<?>> foci = List.of(focus);

        List<HiddenRecipe> contributed = collectOutputRecipes(jei, recipeManager, foci);
        if (!contributed.isEmpty()) {
            recipesByLockingFluid.put(fluidId, List.copyOf(contributed));
        }
    }

    private static List<HiddenRecipe> collectOutputRecipes(IJeiRuntime jei, IRecipeManager recipeManager, List<? extends IFocus<?>> foci) {
        List<HiddenRecipe> contributed = new ArrayList<>();
        for (RecipeType<?> recipeType : jei.getJeiHelpers().getAllRecipeTypes().toList()) {
            IRecipeLookup<?> lookup = recipeManager.createRecipeLookup(recipeType);
            List<?> found = lookup.limitFocus(foci).get().toList();
            for (Object recipe : found) {
                HiddenRecipe hr = new HiddenRecipe(recipeType, recipe);
                contributed.add(hr);

                int refs = recipeHideRefCount.merge(hr, 1, Integer::sum);
                if (refs == 1) {
                    hideRecipesUnchecked(recipeManager, recipeType, List.of(recipe));
                }
            }
        }
        return contributed;
    }

    private static void revealRecipesForItem(IJeiRuntime jei, IRecipeManager recipeManager, ResourceLocation itemId) {
        List<HiddenRecipe> contributed = recipesByLockingItem.remove(itemId);
        revealContributedRecipes(recipeManager, contributed);
        // Idempotent fallback: clears any orphaned hideRecipes state (e.g. after older hide-order bugs or ref drift).
        ensureOutputRecipesVisibleForItem(jei, recipeManager, itemId);
    }

    private static void revealRecipesForFluid(
        IJeiRuntime jei,
        IRecipeManager recipeManager,
        IIngredientType<FluidStack> fluidType,
        ResourceLocation fluidId
    ) {
        List<HiddenRecipe> contributed = recipesByLockingFluid.remove(fluidId);
        revealContributedRecipes(recipeManager, contributed);
        ensureOutputRecipesVisibleForFluid(jei, recipeManager, fluidType, fluidId);
    }

    private static void ensureOutputRecipesVisibleForItem(IJeiRuntime jei, IRecipeManager recipeManager, ResourceLocation itemId) {
        IIngredientManager ingredients = jei.getIngredientManager();
        var focusFactory = jei.getJeiHelpers().getFocusFactory();
        List<IFocus<?>> foci = new ArrayList<>();

        ingredients.getAllItemStacks().stream()
                .filter(s -> !s.isEmpty() && BuiltInRegistries.ITEM.getKey(s.getItem()).equals(itemId))
                .forEach(s -> foci.add(focusFactory.createFocus(RecipeIngredientRole.OUTPUT, VanillaTypes.ITEM_STACK, s)));

        if (foci.isEmpty()) {
            ItemStack probe = new ItemStack(BuiltInRegistries.ITEM.get(itemId));
            if (!probe.isEmpty()) {
                foci.add(focusFactory.createFocus(RecipeIngredientRole.OUTPUT, VanillaTypes.ITEM_STACK, probe));
            }
        }

        if (!foci.isEmpty()) {
            sweepUnhideOutputRecipes(jei, recipeManager, foci);
        }
    }

    private static void ensureOutputRecipesVisibleForFluid(
        IJeiRuntime jei,
        IRecipeManager recipeManager,
        IIngredientType<FluidStack> fluidType,
        ResourceLocation fluidId
    ) {
        Fluid fluid = BuiltInRegistries.FLUID.get(fluidId);
        if (fluid == null || fluid == Fluids.EMPTY) {
            return;
        }
        FluidStack probe = new FluidStack(fluid, 1000);
        if (probe.isEmpty()) {
            return;
        }
        IFocus<FluidStack> focus = jei.getJeiHelpers().getFocusFactory()
                .createFocus(RecipeIngredientRole.OUTPUT, fluidType, probe);
        sweepUnhideOutputRecipes(jei, recipeManager, List.of(focus));
    }

    private static Optional<IIngredientType<Object>> mekanismChemicalIngredientType() {
        if (!ModList.get().isLoaded("mekanism")) {
            return Optional.empty();
        }
        try {
            Class<?> mz = Class.forName("mekanism.client.recipe_viewer.jei.MekanismJEI");
            Object type = mz.getField("TYPE_CHEMICAL").get(null);
            @SuppressWarnings("unchecked")
            IIngredientType<Object> cast = (IIngredientType<Object>) type;
            return Optional.of(cast);
        } catch (ReflectiveOperationException | ClassCastException e) {
            return Optional.empty();
        }
    }

    private static ResourceLocation chemicalIngredientId(Object ingredient) {
        try {
            Class<?> mz = Class.forName("mekanism.client.recipe_viewer.jei.MekanismJEI");
            Object helper = mz.getField("CHEMICAL_STACK_HELPER").get(null);
            Class<?> cs = Class.forName("mekanism.api.chemical.ChemicalStack");
            Object id = helper.getClass().getMethod("getResourceLocation", cs).invoke(helper, ingredient);
            return id instanceof ResourceLocation rl ? rl : null;
        } catch (ReflectiveOperationException | ClassCastException e) {
            return null;
        }
    }

    @Nullable
    private static Object chemicalProbeStack(ResourceLocation chemicalId) {
        try {
            Class<?> api = Class.forName("mekanism.api.MekanismAPI");
            Object registry = api.getField("CHEMICAL_REGISTRY").get(null);
            Object chemical = registry.getClass().getMethod("get", ResourceLocation.class).invoke(registry, chemicalId);
            if (chemical == null) {
                return null;
            }
            Class<?> chemicalClass = Class.forName("mekanism.api.chemical.Chemical");
            Class<?> stackClass = Class.forName("mekanism.api.chemical.ChemicalStack");
            return stackClass.getConstructor(chemicalClass, long.class).newInstance(chemical, 1L);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void restoreChemicalIngredients(
            IIngredientManager ingredients,
            IIngredientType<Object> chemicalType,
            ResourceLocation chemicalId
    ) {
        List<Object> stacks = chemicalIngredientSnapshots.remove(chemicalId);
        if (stacks != null && !stacks.isEmpty()) {
            ingredients.addIngredientsAtRuntime((IIngredientType) chemicalType, (List) stacks);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void hideIngredientsForChemical(
            IIngredientManager ingredients,
            IIngredientType<Object> chemicalType,
            ResourceLocation chemicalId
    ) {
        List<Object> toHide = ingredients.getAllIngredients((IIngredientType) chemicalType).stream()
                .filter(o -> chemicalId.equals(chemicalIngredientId(o)))
                .toList();
        if (!toHide.isEmpty()) {
            ingredients.removeIngredientsAtRuntime((IIngredientType) chemicalType, (List) toHide);
            chemicalIngredientSnapshots.put(chemicalId, List.copyOf(toHide));
        }
    }

    private static void hideOutputRecipesForChemical(
            IJeiRuntime jei,
            IRecipeManager recipeManager,
            IIngredientType<Object> chemicalType,
            ResourceLocation chemicalId
    ) {
        Object probe = chemicalProbeStack(chemicalId);
        if (probe == null) {
            return;
        }
        IFocus<Object> focus = jei.getJeiHelpers().getFocusFactory()
                .createFocus(RecipeIngredientRole.OUTPUT, chemicalType, probe);
        List<? extends IFocus<?>> foci = List.of(focus);

        List<HiddenRecipe> contributed = collectOutputRecipes(jei, recipeManager, foci);
        if (!contributed.isEmpty()) {
            recipesByLockingChemical.put(chemicalId, List.copyOf(contributed));
        }
    }

    private static void revealRecipesForChemical(
            IJeiRuntime jei,
            IRecipeManager recipeManager,
            IIngredientType<Object> chemicalType,
            ResourceLocation chemicalId
    ) {
        List<HiddenRecipe> contributed = recipesByLockingChemical.remove(chemicalId);
        revealContributedRecipes(recipeManager, contributed);
        ensureOutputRecipesVisibleForChemical(jei, recipeManager, chemicalType, chemicalId);
    }

    private static void ensureOutputRecipesVisibleForChemical(
            IJeiRuntime jei,
            IRecipeManager recipeManager,
            IIngredientType<Object> chemicalType,
            ResourceLocation chemicalId
    ) {
        Object probe = chemicalProbeStack(chemicalId);
        if (probe == null) {
            return;
        }
        IFocus<Object> focus = jei.getJeiHelpers().getFocusFactory()
                .createFocus(RecipeIngredientRole.OUTPUT, chemicalType, probe);
        sweepUnhideOutputRecipes(jei, recipeManager, List.of(focus));
    }

    @SuppressWarnings("rawtypes")
    private static void sweepUnhideOutputRecipes(
        IJeiRuntime jei,
        IRecipeManager recipeManager,
        List<? extends IFocus<?>> foci
    ) {
        if (foci.isEmpty()) {
            return;
        }
        for (RecipeType<?> recipeType : jei.getJeiHelpers().getAllRecipeTypes().toList()) {
            IRecipeLookup<?> lookup = recipeManager.createRecipeLookup(recipeType);
            // Hidden recipes must be visible to the lookup, otherwise reconcile never fires unhide.
            List<?> found = lookup.limitFocus(foci).includeHidden().get().toList();
            for (Object recipe : found) {
                HiddenRecipe hr = new HiddenRecipe(recipeType, recipe);
                // Do not unhide while another lock reason still applies (e.g. recipe id stage after item namespace
                // unlock); revealContributedRecipes leaves refs > 0 in that case.
                Integer refs = recipeHideRefCount.get(hr);
                if (refs != null && refs > 0) {
                    continue;
                }
                recipeHideRefCount.remove(hr);
                unhideRecipesUnchecked(recipeManager, recipeType, List.of(recipe));
            }
        }
    }

    private static void revealContributedRecipes(IRecipeManager recipeManager, @Nullable List<HiddenRecipe> contributed) {
        if (contributed == null || contributed.isEmpty()) {
            return;
        }
        for (HiddenRecipe hr : contributed) {
            Integer refs = recipeHideRefCount.get(hr);
            if (refs == null) {
                unhideRecipesUnchecked(recipeManager, hr.type, List.of(hr.recipe));
                continue;
            }
            if (refs <= 1) {
                recipeHideRefCount.remove(hr);
                unhideRecipesUnchecked(recipeManager, hr.type, List.of(hr.recipe));
            } else {
                recipeHideRefCount.put(hr, refs - 1);
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void hideRecipesUnchecked(IRecipeManager mgr, RecipeType<?> recipeType, List<?> recipes) {
        if (!recipes.isEmpty()) {
            mgr.hideRecipes((RecipeType) recipeType, (List) recipes);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void unhideRecipesUnchecked(IRecipeManager mgr, RecipeType<?> recipeType, List<?> recipes) {
        if (!recipes.isEmpty()) {
            mgr.unhideRecipes((RecipeType) recipeType, (List) recipes);
        }
    }

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(Chapters.MOD_ID, "chapters_main");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        synchronized (LOCK) {
            runtime = jeiRuntime;
            // Apply whatever StageManager + ClientStageCache already know (handles sync-before-JEI order).
            RecipeViewerCompat.refresh();
        }
    }

    @Override
    public void onRuntimeUnavailable() {
        synchronized (LOCK) {
            runtime = null;
            lastLockedItems.clear();
            lastLockedFluids.clear();
            lastLockedChemicals.clear();
            lastLockedRecipes.clear();
            ingredientSnapshots.clear();
            fluidIngredientSnapshots.clear();
            chemicalIngredientSnapshots.clear();
            recipesByLockingItem.clear();
            recipesByLockingFluid.clear();
            recipesByLockingChemical.clear();
            recipesByLockingRecipeId.clear();
            recipeHideRefCount.clear();
        }
    }

    private record HiddenRecipe(RecipeType<?> type, Object recipe) {
        HiddenRecipe {
            Objects.requireNonNull(type);
            Objects.requireNonNull(recipe);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof HiddenRecipe other)) {
                return false;
            }
            if (!type.equals(other.type)) {
                return false;
            }
            if (recipe instanceof RecipeHolder<?> h1 && other.recipe instanceof RecipeHolder<?> h2) {
                return h1.id().equals(h2.id());
            }
            return recipe == other.recipe;
        }

        @Override
        public int hashCode() {
            int th = type.hashCode();
            if (recipe instanceof RecipeHolder<?> h) {
                return 31 * th + h.id().hashCode();
            }
            return 31 * th + System.identityHashCode(recipe);
        }
    }
}
