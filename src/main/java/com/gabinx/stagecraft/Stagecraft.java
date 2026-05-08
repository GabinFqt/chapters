package com.gabinx.stagecraft;

import com.mojang.logging.LogUtils;
import com.gabinx.stagecraft.command.StagecraftCommand;
import com.gabinx.stagecraft.compat.RecipeViewerBootstrap;
import com.gabinx.stagecraft.compat.kubejs.KubeJSStageBridge;
import com.gabinx.stagecraft.event.CraftingHandler;
import com.gabinx.stagecraft.event.InventoryAuditor;
import com.gabinx.stagecraft.event.PickupHandler;
import com.gabinx.stagecraft.event.PlayerJoinHandler;
import com.gabinx.stagecraft.stage.StageManager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;

@Mod(Stagecraft.MOD_ID)
public final class Stagecraft {
    public static final String MOD_ID = "stagecraft";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Stagecraft(IEventBus modBus) {
        StagecraftRegistries.register(modBus);

        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(this::onAddReloadListener);
        NeoForge.EVENT_BUS.addListener(PickupHandler::onPickup);
        NeoForge.EVENT_BUS.addListener(CraftingHandler::onCrafted);
        NeoForge.EVENT_BUS.addListener(InventoryAuditor::onTick);
        NeoForge.EVENT_BUS.addListener(KubeJSStageBridge::onServerTickPost);
        NeoForge.EVENT_BUS.addListener(PlayerJoinHandler::onLogin);
        NeoForge.EVENT_BUS.addListener(PlayerJoinHandler::onRespawn);

        RecipeViewerBootstrap.logDetectedRecipeViewers();
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        StagecraftCommand.register(event.getDispatcher());
    }

    private void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(StageManager.get());
    }
}
