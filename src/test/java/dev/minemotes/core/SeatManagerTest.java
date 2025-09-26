package dev.minemotes.core;

import static org.junit.jupiter.api.Assertions.*;
import dev.minemotes.config.Config;
import java.util.UUID;
import net.minecraft.SharedConstants;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;

class SeatManagerTest {
  static {
    SharedConstants.createGameVersion();
  }

  @Test
  void seatLifecycleRegistersAndCleansUp() {
    StubSeat seat = new StubSeat();
    SeatManager manager = new SeatManager((player, position, yaw) -> seat);
    TestPlayerAdapter player = new TestPlayerAdapter(UUID.randomUUID(), "Tester", "minecraft:overworld");
    player.setPosition(Vec3d.ZERO);
    Config.SeatEmoteConfig cfg = new Config.SeatEmoteConfig(true, 0, 0, 0, 0);

    SeatManager.SeatResult result = manager.createSeat(player, EmoteType.SIT, cfg);
    assertTrue(result.success());
    assertTrue(manager.session(player).isPresent());

    manager.removeSeat(player, SeatManager.SeatRemovalReason.COMMAND);
    assertTrue(manager.session(player).isEmpty());
  }

  @Test
  void overrideOffsetAndYawApplied() {
    StubSeat seat = new StubSeat();
    final Vec3d[] capturedPos = new Vec3d[1];
    final float[] capturedYaw = new float[1];
    SeatManager manager =
        new SeatManager(
            (player, position, yaw) -> {
              capturedPos[0] = position;
              capturedYaw[0] = yaw;
              return seat;
            });
    TestPlayerAdapter player = new TestPlayerAdapter(UUID.randomUUID(), "Tester", "minecraft:overworld");
    player.setPosition(new Vec3d(1, 2, 3));
    player.setYaw(15f);
    Config.SeatEmoteConfig cfg = new Config.SeatEmoteConfig(true, 0.1, -0.2, 0.3, 0);

    Vec3d override = new Vec3d(0.4, 0.6, -0.5);
    float overrideYaw = 90f;

    SeatManager.SeatResult result = manager.createSeat(player, EmoteType.SIT, cfg, override, overrideYaw);

    assertTrue(result.success());
    Vec3d expectedOffset = new Vec3d(cfg.offsetX(), cfg.offsetY(), cfg.offsetZ()).add(override);
    Vec3d expectedSpawn = player.position().add(expectedOffset);
    assertEquals(expectedSpawn.x, capturedPos[0].x, 1e-6);
    assertEquals(expectedSpawn.y, capturedPos[0].y, 1e-6);
    assertEquals(expectedSpawn.z, capturedPos[0].z, 1e-6);
    assertEquals(overrideYaw, capturedYaw[0]);
    SeatManager.SeatSession session = manager.session(player).orElseThrow();
    Vec3d sessionOffset = session.offset();
    assertEquals(expectedOffset.x, sessionOffset.x, 1e-6);
    assertEquals(expectedOffset.y, sessionOffset.y, 1e-6);
    assertEquals(expectedOffset.z, sessionOffset.z, 1e-6);
  }

  private static final class StubSeat implements SeatManager.Seat {
    private boolean spawned;
    private boolean discarded;

    @Override
    public boolean spawn() {
      return spawned = true;
    }

    @Override
    public boolean startRiding(PlayerAdapter player) {
      return true;
    }

    @Override
    public void discard() {
      discarded = true;
    }

    @Override
    public boolean isRemoved() {
      return discarded;
    }

    @Override
    public int id() {
      return 123;
    }

    @Override
    public void reposition(Vec3d pos, float yaw) {}
  }
}
