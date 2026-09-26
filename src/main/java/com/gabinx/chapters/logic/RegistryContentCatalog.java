package com.gabinx.chapters.logic;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
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
    public Set<Identifier> itemsInTag(Identifier tagId) {
        Set<Identifier> out = new LinkedHashSet<>();
        TagKey<Item> tag = TagKey.create(BuiltInRegistries.ITEM.key(), tagId);
        for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
            Identifier key = BuiltInRegistries.ITEM.getKey(holder.value());
            if (key != null) {
                out.add(key);
            }
        }
        return out;
    }

    @Override
    public Set<Identifier> itemsInNamespace(String namespace) {
        Set<Identifier> out = new LinkedHashSet<>();
        for (Item item : BuiltInRegistries.ITEM) {
            Identifier key = BuiltInRegistries.ITEM.getKey(item);
            if (key != null && namespace.equals(key.getNamespace())) {
                out.add(key);
            }
        }
        return out;
    }

    @Override
    public Set<Identifier> fluidsInTag(Identifier tagId) {
        Set<Identifier> out = new LinkedHashSet<>();
        TagKey<Fluid> tag = TagKey.create(BuiltInRegistries.FLUID.key(), tagId);
        for (Holder<Fluid> holder : BuiltInRegistries.FLUID.getTagOrEmpty(tag)) {
            Identifier kind = com.gabinx.chapters.stage.StageDefinition.fluidKindRegistryKey(holder.value());
            if (kind != null) {
                out.add(kind);
            }
        }
        return out;
    }

    @Override
    public Set<Identifier> fluidsInNamespace(String namespace) {
        Set<Identifier> out = new LinkedHashSet<>();
        for (Fluid fluid : BuiltInRegistries.FLUID) {
            if (fluid == null || fluid == Fluids.EMPTY) {
                continue;
            }
            Identifier kind = com.gabinx.chapters.stage.StageDefinition.fluidKindRegistryKey(fluid);
            if (kind != null && namespace.equals(kind.getNamespace())) {
                out.add(kind);
            }
        }
        return out;
    }

    @Override
    public Set<Identifier> chemicalsInTag(Identifier tagId) {
        return invokeMekanismSet("chemicalsInTag", Identifier.class, tagId);
    }

    @Override
    public Set<Identifier> chemicalsInNamespace(String namespace) {
        return invokeMekanismSet("chemicalsInNamespace", String.class, namespace);
    }

    @Override
    public Set<Identifier> bucketItemsForFluid(Identifier fluidKindId) {
        Fluid fluid = BuiltInRegistries.FLUID.getValue(fluidKindId);
        if (fluid == null || fluid == Fluids.EMPTY) {
            return Set.of();
        }
        Item bucket = fluid.getBucket();
        if (bucket == null || new ItemStack(bucket).isEmpty()) {
            return Set.of();
        }
        Identifier bucketId = BuiltInRegistries.ITEM.getKey(bucket);
        return bucketId == null ? Set.of() : Set.of(bucketId);
    }

    @SuppressWarnings("unchecked")
    private static Set<Identifier> invokeMekanismSet(String method, Class<?> argType, Object arg) {
        if (!ModList.get().isLoaded("mekanism")) {
            return Set.of();
        }
        try {
            Class<?> indexClass = Class.forName("com.gabinx.chapters.compat.mekanism.MekanismChemicalIndex");
            Object raw = indexClass.getMethod(method, argType).invoke(null, arg);
            return Set.copyOf((Set<Identifier>) raw);
        } catch (ReflectiveOperationException e) {
            return Set.of();
        }
    }
}
