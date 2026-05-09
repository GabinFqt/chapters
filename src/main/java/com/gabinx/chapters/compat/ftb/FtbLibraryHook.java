package com.gabinx.chapters.compat.ftb;

import dev.ftb.mods.ftblibrary.integration.stages.StageHelper;

/**
 * Holds the actual FTB Library entry point so that it is only resolved by
 * the JVM when {@link FtbCompat} confirms FTB Library is on the classpath.
 */
final class FtbLibraryHook {
    private FtbLibraryHook() {
    }

    static void installStageProvider() {
        StageHelper.getInstance().setProviderImpl(new ChaptersStageProvider());
    }
}
