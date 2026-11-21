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
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

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

    private static final Set<Identifier> lastLockedItems = new LinkedHashSet<>();
    private static final Set<Identifier> lastLockedFluids = new LinkedHashSet<>();
    private static final Set<Identifier> lastLockedChemicals = new LinkedHashSet<>();
    private static final Set<Identifier> lastLockedRecipes = new LinkedHashSet<>();

    /** Item id → ingredient stacks removed from JEI so we can add them back verbatim. */
    private static final Map<Identifier, List<ItemStack>> ingredientSnapshots = new HashMap<>();

    /** Fluid id → ingredient stacks removed from JEI so we can add them back verbatim. */
    private static final Map<Identifier, List<FluidStack>> fluidIngredientSnapshots = new HashMap<>();

    /** Mekanism chemical id → ingredient stacks removed from JEI so we can add them back verbatim. */
    private static final Map<Identifier, List<Object>> chemicalIngredientSnapshots = new HashMap<>();

    /** Item id → recipes hidden for this lock pass (list keeps duplicates in sync with ref counts). */
    private static final Map<Identifier, List<HiddenRecipe>> recipesByLockingItem = new HashMap<>();

    private static final Map<Identifier, List<HiddenRecipe>> recipesByLockingFluid = new HashMap<>();

    private static final Map<Identifier, List<HiddenRecipe>> recipesByLockingChemical = new HashMap<>();

    private static final Map<Identifier, List<HiddenRecipe>> recipesByLockingRecipeId = new HashMap<>();

    private static final Map<HiddenRecipe, Integer> recipeHideRefCount = new HashMap<>();

    static void applyLocked(
            Set<Identifier> newLockedItems,
            Set<Identifier> newLockedFluids,
            Set<Identifier> newLockedChemicals,
            Set<Identifier> newLockedRecipes
    ) {
        synchronized (LOCK) {
            if (runtime == null) {
                return;
            }

            IJeiRuntime jei = runtime;
            IIngredientManager ingredients = jei.getIngredientManager();
            IRecipeManager recipeManager = jei.getRecipeManager();
            Optional<IIngredientType<FluidStack>> fluidTypeOpt = ingredients.getIngredientTypeChecked(FluidStack.class);

            Set<Identifier> itemsToLock = new LinkedHashSet<>(newLockedItems);
            itemsToLock.removeAll(lastLockedItems);
            Set<Identifier> itemsToUnlock = new LinkedHashSet<>(lastLockedItems);
            itemsToUnlock.removeAll(newLockedItems);

            // Restore every newly-unlocked ingredient first (one JEI add call), then reconcile recipes.
            // The recipe sweep reads getAllItemStacks(), so stacks must be back before it runs.
            restoreItemIngredientsBatch(ingredients, itemsToUnlock);
            revealRecipesForItemsBatch(jei, recipeManager, itemsToUnlock);
            // Hide crafting recipes BEFORE removing stacks from JEI ingredients: focus-based recipe lookup relies on the
            // ingredient index still listing outputs; otherwise lookups return nothing, we never register hides, yet the
            // UI can stay inconsistent until unlock (golden apple unlocked but craft tab empty).
            hideOutputRecipesForItemsBatch(jei, recipeManager, itemsToLock);
            hideIngredientsForItemsBatch(ingredients, itemsToLock);

            lastLockedItems.clear();
            lastLockedItems.addAll(newLockedItems);

            if (fluidTypeOpt.isPresent()) {
                IIngredientType<FluidStack> fluidType = fluidTypeOpt.get();
                Set<Identifier> fluidsToLock = new LinkedHashSet<>(newLockedFluids);
                fluidsToLock.removeAll(lastLockedFluids);
                Set<Identifier> fluidsToUnlock = new LinkedHashSet<>(lastLockedFluids);
                fluidsToUnlock.removeAll(newLockedFluids);

                restoreFluidIngredientsBatch(ingredients, fluidType, fluidsToUnlock);
                revealRecipesForFluidsBatch(jei, recipeManager, fluidType, fluidsToUnlock);
                for (Identifier id : fluidsToLock) {
                    hideOutputRecipesForFluid(jei, recipeManager, fluidType, id);
                }
                hideIngredientsForFluidsBatch(ingredients, fluidType, fluidsToLock);
            }

            lastLockedFluids.clear();
            lastLockedFluids.addAll(newLockedFluids);

            Optional<IIngredientType<Object>> chemicalTypeOpt = mekanismChemicalIngredientType();
            if (chemicalTypeOpt.isPresent()) {
                IIngredientType<Object> chemicalType = chemicalTypeOpt.get();
                Set<Identifier> chemicalsToLock = new LinkedHashSet<>(newLockedChemicals);
                chemicalsToLock.removeAll(lastLockedChemicals);
                Set<Identifier> chemicalsToUnlock = new LinkedHashSet<>(lastLockedChemicals);
                chemicalsToUnlock.removeAll(newLockedChemicals);

                restoreChemicalIngredientsBatch(ingredients, chemicalType, chemicalsToUnlock);
                revealRecipesForChemicalsBatch(jei, recipeManager, chemicalType, chemicalsToUnlock);
                for (Identifier id : chemicalsToLock) {
                    hideOutputRecipesForChemical(jei, recipeManager, chemicalType, id);
                }
                hideIngredientsForChemicalsBatch(ingredients, chemicalType, chemicalsToLock);
            }

            lastLockedChemicals.clear();
            lastLockedChemicals.addAll(newLockedChemicals);

            Set<Identifier> recipesToLock = new LinkedHashSet<>(newLockedRecipes);
            recipesToLock.removeAll(lastLockedRecipes);
            Set<Identifier> recipesToUnlock = new LinkedHashSet<>(lastLockedRecipes);
            recipesToUnlock.removeAll(newLockedRecipes);

            revealRecipesForRecipeIdsBatch(recipeManager, recipesToUnlock);
            hideRecipesWithIds(jei, recipeManager, recipesToLock);

            lastLockedRecipes.clear();
            lastLockedRecipes.addAll(newLockedRecipes);
        }
    }

    private static void hideRecipesWithIds(
            IJeiRuntime jei,
            IRecipeManager recipeManager,
            Set<Identifier> idsToLock
    ) {
        if (idsToLock.isEmpty()) {
            return;
        }
        Map<Identifier, List<HiddenRecipe>> contributedById = new LinkedHashMap<>();
        for (Identifier id : idsToLock) {
            contributedById.put(id, new ArrayList<>());
        }
        Map<IRecipeType<?>, List<Object>> firstHideByType = new LinkedHashMap<>();
        for (IRecipeType<?> recipeType : jei.getJeiHelpers().getAllRecipeTypes().toList()) {
            IRecipeLookup<?> lookup = recipeManager.createRecipeLookup(recipeType);
            for (Object recipe : lookup.get().toList()) {
                if (!(recipe instanceof RecipeHolder<?> holder)) {
                    continue;
                }
                Identifier hid = holder.id().identifier();
                if (!idsToLock.contains(hid)) {
                    continue;
                }
                HiddenRecipe hr = new HiddenRecipe(recipeType, recipe);
                contributedById.get(hid).add(hr);
                int refs = recipeHideRefCount.merge(hr, 1, Integer::sum);
                if (refs == 1) {
                    firstHideByType.computeIfAbsent(recipeType, t -> new ArrayList<>()).add(recipe);
                }
            }
        }
        flushHideByType(recipeManager, firstHideByType);
        for (Identifier rid : idsToLock) {
            List<HiddenRecipe> contribution = contributedById.get(rid);
            if (contribution != null && !contribution.isEmpty()) {
                recipesByLockingRecipeId.put(rid, List.copyOf(contribution));
            }
        }
    }

    private static void revealRecipesForRecipeIdsBatch(IRecipeManager recipeManager, Set<Identifier> recipeIds) {
        if (recipeIds.isEmpty()) {
            return;
        }
        revealContributedRecipes(recipeManager, takeContributions(recipesByLockingRecipeId, recipeIds));
    }

    /** Removes and concatenates every id's contributions. Duplicates stay: each one decrements the hide ref count. */
    private static List<HiddenRecipe> takeContributions(
            Map<Identifier, List<HiddenRecipe>> byId,
            Set<Identifier> ids
    ) {
        List<HiddenRecipe> taken = new ArrayList<>();
        for (Identifier id : ids) {
            List<HiddenRecipe> contribution = byId.remove(id);
            if (contribution != null) {
                taken.addAll(contribution);
            }
        }
        return taken;
    }

    private static void restoreItemIngredientsBatch(IIngredientManager ingredients, Set<Identifier> itemIds) {
        List<ItemStack> toAdd = takeSnapshots(ingredientSnapshots, itemIds);
        if (!toAdd.isEmpty()) {
            ingredients.addIngredientsAtRuntime(VanillaTypes.ITEM_STACK, toAdd);
        }
    }

    private static void restoreFluidIngredientsBatch(
            IIngredientManager ingredients,
            IIngredientType<FluidStack> fluidType,
            Set<Identifier> fluidIds
    ) {
        List<FluidStack> toAdd = takeSnapshots(fluidIngredientSnapshots, fluidIds);
        if (!toAdd.isEmpty()) {
            ingredients.addIngredientsAtRuntime(fluidType, toAdd);
        }
    }

    private static void hideIngredientsForItemsBatch(IIngredientManager ingredients, Set<Identifier> itemIds) {
        if (itemIds.isEmpty()) {
            return;
        }
        Map<Identifier, List<ItemStack>> stacksById = new LinkedHashMap<>();
        for (ItemStack stack : ingredients.getAllItemStacks()) {
            if (stack.isEmpty()) {
                continue;
            }
            Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (itemIds.contains(id)) {
                stacksById.computeIfAbsent(id, k -> new ArrayList<>()).add(stack);
            }
        }
        if (!stacksById.isEmpty()) {
            ingredients.removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, flatten(stacksById));
            putSnapshots(ingredientSnapshots, stacksById);
        }
    }

    private static void hideIngredientsForFluidsBatch(
            IIngredientManager ingredients,
            IIngredientType<FluidStack> fluidType,
            Set<Identifier> fluidIds
    ) {
        if (fluidIds.isEmpty()) {
            return;
        }
        Map<Identifier, List<FluidStack>> stacksById = new LinkedHashMap<>();
        for (FluidStack stack : ingredients.getAllIngredients(fluidType)) {
            if (stack.isEmpty()) {
                continue;
            }
            Identifier id = BuiltInRegistries.FLUID.getKey(stack.getFluid());
            if (fluidIds.contains(id)) {
                stacksById.computeIfAbsent(id, k -> new ArrayList<>()).add(stack);
            }
        }
        if (!stacksById.isEmpty()) {
            ingredients.removeIngredientsAtRuntime(fluidType, flatten(stacksById));
            putSnapshots(fluidIngredientSnapshots, stacksById);
        }
    }

    private static <T> List<T> takeSnapshots(Map<Identifier, List<T>> snapshots, Set<Identifier> ids) {
        List<T> taken = new ArrayList<>();
        for (Identifier id : ids) {
            List<T> stacks = snapshots.remove(id);
            if (stacks != null) {
                taken.addAll(stacks);
            }
        }
        return taken;
    }

    private static <T> void putSnapshots(Map<Identifier, List<T>> snapshots, Map<Identifier, List<T>> stacksById) {
        stacksById.forEach((id, stacks) -> snapshots.put(id, List.copyOf(stacks)));
    }

    private static <T> List<T> flatten(Map<Identifier, List<T>> stacksById) {
        List<T> all = new ArrayList<>();
        stacksById.values().forEach(all::addAll);
        return all;
    }

    /**
     * One pass per recipe type over combined output foci for every newly locked item (instead of N × recipeTypes).
     */
    private static void hideOutputRecipesForItemsBatch(
            IJeiRuntime jei,
            IRecipeManager recipeManager,
            Set<Identifier> itemsToLock
    ) {
        if (itemsToLock.isEmpty()) {
            return;
        }

        HolderLookup.Provider registryAccess = safeRegistryAccess();
        if (registryAccess == null) {
            for (Identifier itemId : itemsToLock) {
                hideOutputRecipesForItem(jei, recipeManager, itemId);
            }
            return;
        }

        List<IFocus<?>> allFoci = new ArrayList<>();
        for (Identifier itemId : itemsToLock) {
            ItemStack probe = BuiltInRegistries.ITEM.get(itemId).map(ItemStack::new).orElse(ItemStack.EMPTY);
            if (!probe.isEmpty()) {
                allFoci.add(jei.getJeiHelpers().getFocusFactory()
                        .createFocus(RecipeIngredientRole.OUTPUT, VanillaTypes.ITEM_STACK, probe));
            }
        }
        if (allFoci.isEmpty()) {
            return;
        }

        Map<Identifier, List<HiddenRecipe>> contributedByItem = new LinkedHashMap<>();
        for (Identifier id : itemsToLock) {
            contributedByItem.put(id, new ArrayList<>());
        }

        Map<IRecipeType<?>, List<Object>> firstHideByType = new LinkedHashMap<>();
        for (IRecipeType<?> recipeType : jei.getJeiHelpers().getAllRecipeTypes().toList()) {
            IRecipeLookup<?> lookup = recipeManager.createRecipeLookup(recipeType);
            List<?> found = lookup.limitFocus(allFoci).get().toList();
            for (Object recipe : found) {
                HiddenRecipe hr = new HiddenRecipe(recipeType, recipe);
                Set<Identifier> outputs = extractOutputItemIds(recipe, registryAccess);
                if (outputs.isEmpty()) {
                    continue;
                }
                for (Identifier itemId : itemsToLock) {
                    if (outputs.contains(itemId)) {
                        contributedByItem.get(itemId).add(hr);
                        int refs = recipeHideRefCount.merge(hr, 1, Integer::sum);
                        if (refs == 1) {
                            firstHideByType.computeIfAbsent(recipeType, t -> new ArrayList<>()).add(recipe);
                        }
                    }
                }
            }
        }
        flushHideByType(recipeManager, firstHideByType);

        for (Identifier itemId : itemsToLock) {
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

    private static Set<Identifier> extractOutputItemIds(Object recipe, HolderLookup.Provider registryAccess) {
        Set<Identifier> ids = new LinkedHashSet<>();
        if (!(recipe instanceof RecipeHolder<?> holder)) {
            return ids;
        }
        ContextMap context = new ContextMap.Builder()
                .withParameter(SlotDisplayContext.REGISTRIES, registryAccess)
                .create(SlotDisplayContext.CONTEXT);
        for (RecipeDisplay display : holder.value().display()) {
            for (ItemStack out : display.result().resolveForStacks(context)) {
                if (out.isEmpty()) {
                    continue;
                }
                Identifier key = BuiltInRegistries.ITEM.getKey(out.getItem());
                if (key != null) {
                    ids.add(key);
                }
            }
        }
        return ids;
    }

    private static void hideOutputRecipesForItem(IJeiRuntime jei, IRecipeManager recipeManager, Identifier itemId) {
        ItemStack probe = BuiltInRegistries.ITEM.get(itemId).map(ItemStack::new).orElse(ItemStack.EMPTY);
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
        Identifier fluidId
    ) {
        Fluid fluid = BuiltInRegistries.FLUID.getValue(fluidId);
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
        Map<IRecipeType<?>, List<Object>> firstHideByType = new LinkedHashMap<>();
        for (IRecipeType<?> recipeType : jei.getJeiHelpers().getAllRecipeTypes().toList()) {
            IRecipeLookup<?> lookup = recipeManager.createRecipeLookup(recipeType);
            List<?> found = lookup.limitFocus(foci).get().toList();
            for (Object recipe : found) {
                HiddenRecipe hr = new HiddenRecipe(recipeType, recipe);
                contributed.add(hr);

                int refs = recipeHideRefCount.merge(hr, 1, Integer::sum);
                if (refs == 1) {
                    firstHideByType.computeIfAbsent(recipeType, t -> new ArrayList<>()).add(recipe);
                }
            }
        }
        flushHideByType(recipeManager, firstHideByType);
        return contributed;
    }

    private static void revealRecipesForItemsBatch(
            IJeiRuntime jei,
            IRecipeManager recipeManager,
            Set<Identifier> itemIds
    ) {
        if (itemIds.isEmpty()) {
            return;
        }
        revealContributedRecipes(recipeManager, takeContributions(recipesByLockingItem, itemIds));
        // Idempotent fallback: clears any orphaned hideRecipes state (e.g. after older hide-order bugs or ref drift).
        ensureOutputRecipesVisibleForItems(jei, recipeManager, itemIds);
    }

    private static void revealRecipesForFluidsBatch(
            IJeiRuntime jei,
            IRecipeManager recipeManager,
            IIngredientType<FluidStack> fluidType,
            Set<Identifier> fluidIds
    ) {
        if (fluidIds.isEmpty()) {
            return;
        }
        revealContributedRecipes(recipeManager, takeContributions(recipesByLockingFluid, fluidIds));
        var focusFactory = jei.getJeiHelpers().getFocusFactory();
        List<IFocus<?>> foci = new ArrayList<>();
        for (Identifier fluidId : fluidIds) {
            Fluid fluid = BuiltInRegistries.FLUID.getValue(fluidId);
            if (fluid == null || fluid == Fluids.EMPTY) {
                continue;
            }
            FluidStack probe = new FluidStack(fluid, 1000);
            if (!probe.isEmpty()) {
                foci.add(focusFactory.createFocus(RecipeIngredientRole.OUTPUT, fluidType, probe));
            }
        }
        sweepUnhideOutputRecipes(jei, recipeManager, foci);
    }

    /**
     * One pass over JEI's item list, then one recipe-type sweep for every newly unlocked item.
     * Ids with no live stack still get a probe focus, matching the previous per-id fallback.
     */
    private static void ensureOutputRecipesVisibleForItems(
            IJeiRuntime jei,
            IRecipeManager recipeManager,
            Set<Identifier> itemIds
    ) {
        var focusFactory = jei.getJeiHelpers().getFocusFactory();
        List<IFocus<?>> foci = new ArrayList<>();
        Set<Identifier> seen = new LinkedHashSet<>();
        for (ItemStack stack : jei.getIngredientManager().getAllItemStacks()) {
            if (stack.isEmpty()) {
                continue;
            }
            Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (!itemIds.contains(id)) {
                continue;
            }
            seen.add(id);
            foci.add(focusFactory.createFocus(RecipeIngredientRole.OUTPUT, VanillaTypes.ITEM_STACK, stack));
        }
        for (Identifier id : itemIds) {
            if (seen.contains(id)) {
                continue;
            }
            ItemStack probe = BuiltInRegistries.ITEM.get(id).map(ItemStack::new).orElse(ItemStack.EMPTY);
            if (!probe.isEmpty()) {
                foci.add(focusFactory.createFocus(RecipeIngredientRole.OUTPUT, VanillaTypes.ITEM_STACK, probe));
            }
        }
        sweepUnhideOutputRecipes(jei, recipeManager, foci);
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

    /** Resolves Mekanism's stack helper once per batch; {@code null} when Mekanism's JEI integration is missing. */
    @Nullable
    private static Function<Object, Identifier> chemicalIdResolver() {
        try {
            Class<?> mz = Class.forName("mekanism.client.recipe_viewer.jei.MekanismJEI");
            Object helper = mz.getField("CHEMICAL_STACK_HELPER").get(null);
            Class<?> cs = Class.forName("mekanism.api.chemical.ChemicalStack");
            Method getId = helper.getClass().getMethod("getIdentifier", cs);
            return ingredient -> {
                try {
                    return getId.invoke(helper, ingredient) instanceof Identifier rl ? rl : null;
                } catch (ReflectiveOperationException | IllegalArgumentException e) {
                    return null;
                }
            };
        } catch (ReflectiveOperationException | ClassCastException e) {
            return null;
        }
    }

    @Nullable
    private static Object chemicalProbeStack(Identifier chemicalId) {
        try {
            Class<?> api = Class.forName("mekanism.api.MekanismAPI");
            Object registry = api.getField("CHEMICAL_REGISTRY").get(null);
            Object chemical = registry.getClass().getMethod("get", Identifier.class).invoke(registry, chemicalId);
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
    private static void restoreChemicalIngredientsBatch(
            IIngredientManager ingredients,
            IIngredientType<Object> chemicalType,
            Set<Identifier> chemicalIds
    ) {
        List<Object> toAdd = takeSnapshots(chemicalIngredientSnapshots, chemicalIds);
        if (!toAdd.isEmpty()) {
            ingredients.addIngredientsAtRuntime((IIngredientType) chemicalType, (List) toAdd);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void hideIngredientsForChemicalsBatch(
            IIngredientManager ingredients,
            IIngredientType<Object> chemicalType,
            Set<Identifier> chemicalIds
    ) {
        if (chemicalIds.isEmpty()) {
            return;
        }
        Function<Object, Identifier> idOf = chemicalIdResolver();
        if (idOf == null) {
            return;
        }
        Map<Identifier, List<Object>> stacksById = new LinkedHashMap<>();
        for (Object stack : ingredients.getAllIngredients((IIngredientType) chemicalType)) {
            Identifier id = idOf.apply(stack);
            if (id != null && chemicalIds.contains(id)) {
                stacksById.computeIfAbsent(id, k -> new ArrayList<>()).add(stack);
            }
        }
        if (!stacksById.isEmpty()) {
            ingredients.removeIngredientsAtRuntime((IIngredientType) chemicalType, (List) flatten(stacksById));
            putSnapshots(chemicalIngredientSnapshots, stacksById);
        }
    }

    private static void hideOutputRecipesForChemical(
            IJeiRuntime jei,
            IRecipeManager recipeManager,
            IIngredientType<Object> chemicalType,
            Identifier chemicalId
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

    private static void revealRecipesForChemicalsBatch(
            IJeiRuntime jei,
            IRecipeManager recipeManager,
            IIngredientType<Object> chemicalType,
            Set<Identifier> chemicalIds
    ) {
        if (chemicalIds.isEmpty()) {
            return;
        }
        revealContributedRecipes(recipeManager, takeContributions(recipesByLockingChemical, chemicalIds));
        var focusFactory = jei.getJeiHelpers().getFocusFactory();
        List<IFocus<?>> foci = new ArrayList<>();
        for (Identifier chemicalId : chemicalIds) {
            Object probe = chemicalProbeStack(chemicalId);
            if (probe != null) {
                foci.add(focusFactory.createFocus(RecipeIngredientRole.OUTPUT, chemicalType, probe));
            }
        }
        sweepUnhideOutputRecipes(jei, recipeManager, foci);
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
        Map<IRecipeType<?>, List<Object>> unhideByType = new LinkedHashMap<>();
        for (IRecipeType<?> recipeType : jei.getJeiHelpers().getAllRecipeTypes().toList()) {
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
                unhideByType.computeIfAbsent(recipeType, t -> new ArrayList<>()).add(recipe);
            }
        }
        flushUnhideByType(recipeManager, unhideByType);
    }

    private static void revealContributedRecipes(IRecipeManager recipeManager, @Nullable List<HiddenRecipe> contributed) {
        if (contributed == null || contributed.isEmpty()) {
            return;
        }
        Map<IRecipeType<?>, List<Object>> unhideByType = new LinkedHashMap<>();
        // One unlock can carry the same recipe from several ids; unhide it once.
        Set<HiddenRecipe> queued = new LinkedHashSet<>();
        for (HiddenRecipe hr : contributed) {
            Integer refs = recipeHideRefCount.get(hr);
            if (refs == null) {
                queueUnhide(unhideByType, queued, hr);
                continue;
            }
            if (refs <= 1) {
                recipeHideRefCount.remove(hr);
                queueUnhide(unhideByType, queued, hr);
            } else {
                recipeHideRefCount.put(hr, refs - 1);
            }
        }
        flushUnhideByType(recipeManager, unhideByType);
    }

    private static void queueUnhide(
            Map<IRecipeType<?>, List<Object>> unhideByType,
            Set<HiddenRecipe> queued,
            HiddenRecipe hr
    ) {
        if (queued.add(hr)) {
            unhideByType.computeIfAbsent(hr.type, t -> new ArrayList<>()).add(hr.recipe);
        }
    }

    private static void flushHideByType(IRecipeManager recipeManager, Map<IRecipeType<?>, List<Object>> byType) {
        for (Map.Entry<IRecipeType<?>, List<Object>> e : byType.entrySet()) {
            hideRecipesUnchecked(recipeManager, e.getKey(), e.getValue());
        }
    }

    private static void flushUnhideByType(IRecipeManager recipeManager, Map<IRecipeType<?>, List<Object>> byType) {
        for (Map.Entry<IRecipeType<?>, List<Object>> e : byType.entrySet()) {
            unhideRecipesUnchecked(recipeManager, e.getKey(), e.getValue());
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void hideRecipesUnchecked(IRecipeManager mgr, IRecipeType<?> recipeType, List<?> recipes) {
        if (!recipes.isEmpty()) {
            mgr.hideRecipes((IRecipeType) recipeType, (List) recipes);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void unhideRecipesUnchecked(IRecipeManager mgr, IRecipeType<?> recipeType, List<?> recipes) {
        if (!recipes.isEmpty()) {
            mgr.unhideRecipes((IRecipeType) recipeType, (List) recipes);
        }
    }

    @Override
    public Identifier getPluginUid() {
        return Identifier.fromNamespaceAndPath(Chapters.MOD_ID, "chapters_main");
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

    private record HiddenRecipe(IRecipeType<?> type, Object recipe) {
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
