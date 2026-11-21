package com.gabinx.chapters.e2e;

import net.minecraft.resources.Identifier;

/** Stage ids from {@code src/e2e/resources/data/chapters/chapters/stages}. */
public final class E2eStages {
    public static final Identifier SOFT_AMETHYST = id("e2e_soft_amethyst");
    public static final Identifier TEASE_QUARTZ = id("e2e_tease_quartz");
    public static final Identifier FULL_PRISMARINE = id("e2e_full_prismarine");
    public static final Identifier PLANKS = id("e2e_planks");
    public static final Identifier RECIPE_SPYGLASS = id("e2e_recipe_spyglass");
    public static final Identifier LAVA_NO_TRANSFER = id("e2e_lava_no_transfer");
    public static final Identifier WATER_TRANSFER = id("e2e_water_transfer");
    public static final Identifier END_TRAVEL = id("e2e_end_travel");
    public static final Identifier OR_A = id("e2e_or_a");
    public static final Identifier OR_B = id("e2e_or_b");
    public static final Identifier BIG = id("e2e_big");

    private E2eStages() {
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("chapters", path);
    }
}
