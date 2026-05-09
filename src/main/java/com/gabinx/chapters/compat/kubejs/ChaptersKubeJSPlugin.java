package com.gabinx.chapters.compat.kubejs;

import com.gabinx.chapters.compat.kubejs.bindings.PlayerStagesBinding;
import com.gabinx.chapters.compat.kubejs.bindings.ChaptersEventsBinding;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingRegistry;

public final class ChaptersKubeJSPlugin implements KubeJSPlugin {
    @Override
    public void registerBindings(BindingRegistry bindings) {
        bindings.add("ChaptersEvents", new ChaptersEventsBinding());
        bindings.add("PlayerStages", new PlayerStagesBinding());
    }
}
