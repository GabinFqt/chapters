package com.gabinx.stagecraft.compat.kubejs;

import com.gabinx.stagecraft.compat.kubejs.bindings.PlayerStagesBinding;
import com.gabinx.stagecraft.compat.kubejs.bindings.StagecraftEventsBinding;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingRegistry;

public final class StagecraftKubeJSPlugin implements KubeJSPlugin {
    @Override
    public void registerBindings(BindingRegistry bindings) {
        bindings.add("StagecraftEvents", new StagecraftEventsBinding());
        bindings.add("PlayerStages", new PlayerStagesBinding());
    }
}
