package com.gabinx.chapters.e2e;

import com.gabinx.chapters.e2e.gametest.E2eGameTests;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

/**
 * Dev-only mod loaded by the {@code gameTestServer} and {@code clientE2e} runs. It is not in the published jar.
 */
@Mod(ChaptersE2e.MOD_ID)
public final class ChaptersE2e {
    public static final String MOD_ID = "chapters_e2e";
    /** Matches {@code items} in the {@code generateE2ePerfPack} task. */
    public static final int PERF_ITEMS = 2000;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final DeferredRegister<Item> PERF_ITEM_REGISTER = DeferredRegister.create(Registries.ITEM, MOD_ID);

    private static void addPerfItemsToIngredients(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() != CreativeModeTabs.INGREDIENTS) {
            return;
        }
        PERF_ITEM_REGISTER.getEntries().forEach(entry -> event.accept(entry.get()));
    }

    public ChaptersE2e(IEventBus modBus) {
        modBus.addListener(E2eGameTests::registerInstances);
        if (Boolean.getBoolean("chapters.e2e.perf")) {
            for (int i = 0; i < PERF_ITEMS; i++) {
                int index = i;
                Identifier itemId = Identifier.fromNamespaceAndPath(MOD_ID, "perf_" + index);
                PERF_ITEM_REGISTER.register("perf_" + index, () -> new Item(new Item.Properties()
                        .setId(ResourceKey.create(Registries.ITEM, itemId))));
            }
            PERF_ITEM_REGISTER.register(modBus);
            // JEI lists creative-tab items. Without a tab the bulk stage never changes what the player sees.
            modBus.addListener(ChaptersE2e::addPerfItemsToIngredients);
        }
        boolean harness = FMLEnvironment.getDist() == Dist.CLIENT && Boolean.getBoolean("chapters.e2e");
        LOGGER.info("Chapters e2e mod loaded (client harness {})", harness);
        if (!harness) {
            return;
        }
        try {
            Class.forName("com.gabinx.chapters.e2e.client.E2eClientHarness")
                    .getMethod("start")
                    .invoke(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not start the Chapters e2e client harness", e);
        }
    }
}
