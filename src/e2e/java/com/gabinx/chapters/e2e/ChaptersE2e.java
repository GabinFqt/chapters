package com.gabinx.chapters.e2e;

import com.gabinx.chapters.e2e.gametest.E2eGameTests;
import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

/**
 * Dev-only mod loaded by the {@code gameTestServer} and {@code clientE2e} runs. It is not in the published jar.
 */
@Mod(ChaptersE2e.MOD_ID)
public final class ChaptersE2e {
    public static final String MOD_ID = "chapters_e2e";
    private static final Logger LOGGER = LogUtils.getLogger();

    public ChaptersE2e(IEventBus modBus) {
        modBus.addListener(E2eGameTests::registerInstances);
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
