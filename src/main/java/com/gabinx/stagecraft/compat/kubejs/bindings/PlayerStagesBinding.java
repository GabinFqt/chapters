package com.gabinx.stagecraft.compat.kubejs.bindings;

import net.minecraft.server.level.ServerPlayer;

public final class PlayerStagesBinding {
    public StagesWrapper of(ServerPlayer player) {
        return new StagesWrapper(player);
    }
}
