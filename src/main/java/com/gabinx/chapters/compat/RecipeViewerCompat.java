package com.gabinx.chapters.compat;

import com.gabinx.chapters.Chapters;
import com.gabinx.chapters.compat.jei.ChaptersJeiPlugin;
import com.gabinx.chapters.logic.ClientRefreshCoalescer;
import com.gabinx.chapters.logic.ClientStageView;
import com.gabinx.chapters.logic.RegistryContentCatalog;
import com.gabinx.chapters.stage.ClientStageIndices;
import net.neoforged.fml.ModList;

import java.util.LinkedHashSet;

public final class RecipeViewerCompat {
    private static final ClientRefreshCoalescer COALESCER = new ClientRefreshCoalescer(RecipeViewerCompat::applyNow);

    private RecipeViewerCompat() {
    }

    /** Mark the recipe viewer dirty; applied once at the end of the client tick. */
    public static void refresh() {
        COALESCER.schedule();
    }

    /** Apply any scheduled refresh. Called from the client tick hook. */
    public static void flushScheduled() {
        COALESCER.flush();
    }

    private static void applyNow() {
        if (!ModList.get().isLoaded("jei")) {
            return;
        }
        ClientStageView.Hidden hidden = ClientStageIndices.hidden(RegistryContentCatalog.INSTANCE);
        long started = System.nanoTime();
        ChaptersJeiPlugin.onLockedIngredientsChanged(
                new LinkedHashSet<>(hidden.items()),
                hidden.fluids(),
                hidden.chemicals(),
                hidden.recipes()
        );
        Chapters.LOGGER.debug(
                "Recipe viewer refresh: locked items: {}, locked fluids: {}, locked chemicals: {}, locked recipes: {}, {} ms",
                hidden.items().size(),
                hidden.fluids().size(),
                hidden.chemicals().size(),
                hidden.recipes().size(),
                (System.nanoTime() - started) / 1_000_000L
        );
    }
}
