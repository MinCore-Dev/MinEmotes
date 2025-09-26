package dev.minemotes;

import dev.minemotes.commands.CommandRegistrar;
import dev.minemotes.config.ConfigLoader;
import dev.minemotes.config.ConfigManager;
import dev.minemotes.core.EmoteService;
import dev.minemotes.core.SeatManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class MinEmotesMod implements ModInitializer {
  public static final String MODID = "minemotes";
  private static final Logger LOGGER = LogManager.getLogger("minemotes");

  private static ConfigManager configManager;
  private static SeatManager seatManager;
  private static EmoteService emoteService;

  public static ConfigManager configManager() {
    return configManager;
  }

  public static EmoteService emotes() {
    return emoteService;
  }

  @Override
  public void onInitialize() {
    LOGGER.info("(minemotes) initializing");
    configManager = new ConfigManager(ConfigLoader.defaultPath());
    configManager.start();
    seatManager = new SeatManager();
    emoteService = new EmoteService(() -> configManager.current(), seatManager);
    configManager.addListener(emoteService::onConfigReload);

    CommandRegistrar.registerAll(emoteService, configManager);

    ServerTickEvents.END_SERVER_TICK.register(server -> emoteService.tick(server));

    ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> emoteService.onDisconnect(handler.getPlayer()));

    ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register(
        (player, origin, destination) -> emoteService.onWorldChange(player));

    ServerLivingEntityEvents.AFTER_DEATH.register(
        (entity, source) -> {
          if (entity instanceof ServerPlayerEntity player) {
            emoteService.onDeath(player);
          }
        });

    ServerLifecycleEvents.SERVER_STOPPED.register(
        server -> {
          seatManager.discardAll();
          configManager.close();
        });

    LOGGER.info("(minemotes) initialized");
  }
}
