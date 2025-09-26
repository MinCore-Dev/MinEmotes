package dev.minemotes.core;

import static org.junit.jupiter.api.Assertions.*;
import dev.minemotes.config.Config;
import java.util.Set;
import java.util.UUID;
import net.minecraft.SharedConstants;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EmoteServiceTest {
  static {
    SharedConstants.createGameVersion();
  }

  private TestPlayerAdapter player;

  @BeforeEach
  void setup() {
    player = new TestPlayerAdapter(UUID.randomUUID(), "Tester", "minecraft:overworld");
    player.setBoundingBox(new Box(0, 0, 0, 1, 1, 1));
    player.setPosition(Vec3d.ZERO);
    player.setYaw(0f);
    player.setPitch(0f);
  }

  @Test
  void crawlEnableThenCooldown() {
    Config config = Config.defaults();
    EmoteService service = new EmoteService(() -> config, new SeatManager());

    EmoteService.Result enable = service.handleCrawl(player, EmoteService.Toggle.ENABLE);
    assertTrue(enable.success());

    EmoteService.Result disable = service.handleCrawl(player, EmoteService.Toggle.DISABLE);
    assertTrue(disable.success());

    EmoteService.Result secondEnable = service.handleCrawl(player, EmoteService.Toggle.ENABLE);
    assertFalse(secondEnable.success());
    assertTrue(secondEnable.message().getString().contains("cooldown"));
  }

  @Test
  void crawlDisabledInWorld() {
    Config defaults = Config.defaults();
    Config.Core core =
        new Config.Core(
            true,
            defaults.core().cooldown(),
            defaults.core().cancelOnDamage(),
            defaults.core().cancelOnMove(),
            defaults.core().allowInWater(),
            Set.of("minecraft:overworld"),
            defaults.core().cancelOnDanger());
    Config config = new Config(core, defaults.emotes(), defaults.permissions());
    EmoteService service = new EmoteService(() -> config, new SeatManager());

    EmoteService.Result result = service.handleCrawl(player, EmoteService.Toggle.ENABLE);
    assertFalse(result.success());
  }
}
