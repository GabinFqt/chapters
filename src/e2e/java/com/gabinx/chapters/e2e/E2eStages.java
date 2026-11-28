package com.gabinx.chapters.e2e;

import net.minecraft.resources.ResourceLocation;

/** Stage ids from {@code src/e2e/resources/data/chapters/chapters/stages}. */
public final class E2eStages {
    public static final ResourceLocation SOFT_AMETHYST = id("e2e_soft_amethyst");
    public static final ResourceLocation TEASE_QUARTZ = id("e2e_tease_quartz");
    public static final ResourceLocation FULL_PRISMARINE = id("e2e_full_prismarine");
    public static final ResourceLocation PLANKS = id("e2e_planks");
    public static final ResourceLocation RECIPE_SPYGLASS = id("e2e_recipe_spyglass");
    public static final ResourceLocation LAVA_NO_TRANSFER = id("e2e_lava_no_transfer");
    public static final ResourceLocation WATER_TRANSFER = id("e2e_water_transfer");
    public static final ResourceLocation END_TRAVEL = id("e2e_end_travel");
    public static final ResourceLocation END_TAG = id("e2e_end_tag");
    public static final ResourceLocation OR_A = id("e2e_or_a");
    public static final ResourceLocation OR_B = id("e2e_or_b");
    public static final ResourceLocation BIG = id("e2e_big");

    private E2eStages() {
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("chapters", path);
    }
}
