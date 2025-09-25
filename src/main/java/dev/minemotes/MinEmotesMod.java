package dev.minemotes;

import dev.minemotes.core.Config;
import dev.minemotes.core.ConfigLoader;
import dev.minemotes.core.SeatManager;
import dev.minemotes.core.EmoteService;
import dev.minemotes.commands.CommandRegistrar;
import net.fabricmc.api.ModInitializer;

public final class MinEmotesMod implements ModInitializer {
  public static final String MODID = "minemotes";
  private static Config config;
  private static SeatManager seatManager;
  private static EmoteService emotes;

  @Override public void onInitialize() {
    config = ConfigLoader.loadOrCreate();
    seatManager = new SeatManager();
    emotes = new EmoteService(config, seatManager);
    CommandRegistrar.registerAll(config, emotes);
    System.out.println("[MinEmotes] Initialized (skeleton)");
  }
}
