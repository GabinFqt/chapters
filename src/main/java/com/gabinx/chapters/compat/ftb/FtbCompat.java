package com.gabinx.chapters.compat.ftb;

import com.gabinx.chapters.Chapters;
import net.neoforged.fml.ModList;

/**
 * Single entry point for FTB Library / FTB Teams compatibility.
 * <p>
 * This class itself never touches any FTB types directly — it only references
 * {@link FtbLibraryHook} and {@link FtbTeamsListeners}, which are loaded
 * lazily on first invocation. Combined with {@code ModList.isLoaded(...)}
 * gating, that ensures the JVM never tries to resolve FTB symbols when the
 * corresponding mod is absent.
 */
public final class FtbCompat {
    private static final String FTB_LIBRARY = "ftblibrary";
    private static final String FTB_TEAMS = "ftbteams";

    private static boolean libraryActive;
    private static boolean teamsActive;

    private FtbCompat() {
    }

    public static void bootstrap() {
        ModList mods = ModList.get();
        if (mods.isLoaded(FTB_LIBRARY)) {
            try {
                FtbLibraryHook.installStageProvider();
                libraryActive = true;
                Chapters.LOGGER.info("Chapters: registered as FTB Library stage provider");
            } catch (Throwable t) {
                Chapters.LOGGER.error("Chapters: failed to register FTB Library stage provider", t);
            }
        }
        if (mods.isLoaded(FTB_TEAMS)) {
            try {
                FtbTeamsListeners.register();
                teamsActive = true;
            } catch (Throwable t) {
                Chapters.LOGGER.error("Chapters: failed to wire FTB Teams listeners", t);
            }
        }
    }

    public static boolean isLibraryLoaded() {
        return libraryActive;
    }

    public static boolean isTeamsLoaded() {
        return teamsActive;
    }
}
