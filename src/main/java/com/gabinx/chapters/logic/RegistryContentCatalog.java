package com.gabinx.chapters.logic;

import com.gabinx.chapters.compat.mekanism.MekanismChemicalIndex;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.fml.ModList;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Production catalog backed by {@link BuiltInRegistries} (and Mekanism when present).
 */
public final class RegistryContentCatalog implements ContentCatalog {
    public static final RegistryContentCatalog INSTANCE = new RegistryContentCatalog();

    private RegistryContentCatalog() {
    }

    @Override
    public Set<ResourceLocation> itemsInTag(ResourceLocation tagId) {
        Set<ResourceLocation> out = new LinkedHashSet<>();
        TagKey<Item> tag = TagKey.create(BuiltInRegistries.ITEM.key(), tagId);
        for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
            ResourceLocation key = BuiltInRegistries.ITEM.getKey(holder.value());
            if (key != null) {
                out.add(key);
            }
        }
        return out;
    }

    @Override
    public Set<ResourceLocation> itemsInNamespace(String namespace) {
        Set<ResourceLocation> out = new LinkedHashSet<>();
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
            if (key != null && namespace.equals(key.getNamespace())) {
                out.add(key);
            }
        }
        return out;
    }

    @Override
    public Set<ResourceLocation> fluidsInTag(ResourceLocation tagId) {
        Set<ResourceLocation> out = new LinkedHashSet<>();
        TagKey<Fluid> tag = TagKey.create(BuiltInRegistries.FLUID.key(), tagId);
        for (Holder<Fluid> holder : BuiltInRegistries.FLUID.getTagOrEmpty(tag)) {
            ResourceLocation kind = com.gabinx.chapters.stage.StageDefinition.fluidKindRegistryKey(holder.value());
            if (kind != null) {
                out.add(kind);
            }
        }
        return out;
    }

    @Override
    public Set<ResourceLocation> fluidsInNamespace(String namespace) {
        Set<ResourceLocation> out = new LinkedHashSet<>();
        for (Fluid fluid : BuiltInRegistries.FLUID) {
            if (fluid == null || fluid == Fluids.EMPTY) {
                continue;
            }
            ResourceLocation kind = com.gabinx.chapters.stage.StageDefinition.fluidKindRegistryKey(fluid);
            if (kind != null && namespace.equals(kind.getNamespace())) {
                out.add(kind);
            }
        }
        return out;
    }

    @Override
    public Set<ResourceLocation> chemicalsInTag(ResourceLocation tagId) {
        if (!ModList.get().isLoaded("mekanism")) {
            return Set.of();
        }
        return MekanismChemicalIndex.chemicalsInTag(tagId);
    }

    @Override
    public Set<ResourceLocation> chemicalsInNamespace(String namespace) {
        if (!ModList.get().isLoaded("mekanism")) {
            return Set.of();
        }
        return MekanismChemicalIndex.chemicalsInNamespace(namespace);
    }

    @Override
    public Set<ResourceLocation> bucketItemsForFluid(ResourceLocation fluidKindId) {
        Fluid fluid = BuiltInRegistries.FLUID.get(fluidKindId);
        if (fluid == null || fluid == Fluids.EMPTY) {
            return Set.of();
        }
        Item bucket = fluid.getBucket();
        if (bucket == null || new ItemStack(bucket).isEmpty()) {
            return Set.of();
        }
        ResourceLocation bucketId = BuiltInRegistries.ITEM.getKey(bucket);
        return bucketId == null ? Set.of() : Set.of(bucketId);
    }
}
