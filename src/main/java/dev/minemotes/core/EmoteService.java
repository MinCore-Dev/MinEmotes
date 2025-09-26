package dev.minemotes.core;

import dev.minemotes.config.Config;
import dev.minemotes.perms.Perms;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import net.minecraft.block.BlockState;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.entity.EntityPose;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Handles command execution and runtime state for all emotes. */
public final class EmoteService {
  private static final Logger LOGGER = LogManager.getLogger("minemotes");

  private final Supplier<Config> configSupplier;
  private final SeatManager seatManager;
  private final MinCoreLedgerBridge ledgerBridge = new MinCoreLedgerBridge();

  private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
  private final Set<UUID> crawling = ConcurrentHashMap.newKeySet();

  public EmoteService(Supplier<Config> configSupplier, SeatManager seatManager) {
    this.configSupplier = Objects.requireNonNull(configSupplier, "configSupplier");
    this.seatManager = Objects.requireNonNull(seatManager, "seatManager");
  }

  public Result handleCrawl(ServerPlayerEntity player, Toggle toggle) {
    return handleCrawl(PlayerAdapter.fabric(player), toggle);
  }

  Result handleCrawl(PlayerAdapter player, Toggle toggle) {
    Config config = configSupplier.get();
    if (!config.core().enabled() || !config.emotes().crawl().enabled()) {
      return Result.error(Text.translatable("minemotes.cmd.disabled"));
    }
    if (isWorldDisabled(player, config)) {
      return Result.error(Text.translatable("minemotes.cmd.disabled"));
    }
    ServerPlayerEntity entity = player.entity();
    if (entity != null
        && !Perms.check(
            entity, config.permissions().crawl(), config.permissions().fallbackLevel(EmoteType.CRAWL))) {
      return Result.error(Text.translatable("commands.generic.unknown"));
    }
    UUID id = player.uuid();
    boolean currently = crawling.contains(id);
    boolean target = switch (toggle) {
      case ENABLE -> true;
      case DISABLE -> false;
      case TOGGLE -> !currently;
    };

    if (target == currently) {
      return Result.ok(currently ? EmoteType.CRAWL.stopMessage() : EmoteType.CRAWL.startMessage());
    }

    if (target) {
      if (isOnCooldown(player, config.core().cooldown())) {
        double seconds = remainingCooldownSeconds(player, config.core().cooldown());
        return Result.error(Text.translatable("minemotes.cmd.cooldown", String.format("%.1f", seconds)));
      }
      Optional<Text> deny = validateCrawl(player, config);
      if (deny.isPresent()) {
        return Result.error(deny.get());
      }
      crawling.add(id);
      player.setSwimming(true);
      player.setPose(EntityPose.SWIMMING);
      ledgerBridge.log(player, EmoteType.CRAWL, "start");
      markCooldown(player, config.core().cooldown());
      return Result.ok(EmoteType.CRAWL.startMessage());
    } else {
      crawling.remove(id);
      player.setSwimming(false);
      player.setPose(EntityPose.STANDING);
      ledgerBridge.log(player, EmoteType.CRAWL, "stop");
      return Result.ok(EmoteType.CRAWL.stopMessage());
    }
  }

  public Result handleSeat(ServerPlayerEntity player, EmoteType type) {
    return handleSeat(PlayerAdapter.fabric(player), type);
  }

  Result handleSeat(PlayerAdapter player, EmoteType type) {
    Config config = configSupplier.get();
    if (!config.core().enabled()) {
      return Result.error(Text.translatable("minemotes.cmd.disabled"));
    }
    if (isWorldDisabled(player, config)) {
      return Result.error(Text.translatable("minemotes.cmd.disabled"));
    }
    Config.SeatEmoteConfig seatCfg = seatConfig(config, type);
    if (seatCfg == null || !seatCfg.enabled()) {
      return Result.error(Text.translatable("minemotes.cmd.disabled"));
    }
    ServerPlayerEntity entity = player.entity();
    if (entity == null) {
      return Result.error(Text.translatable("minemotes.cmd.disabled"));
    }
    if (!Perms.check(entity, permissionFor(type, config), config.permissions().fallbackLevel(type))) {
      return Result.error(Text.translatable("commands.generic.unknown"));
    }
    Optional<SeatManager.SeatSession> existing = seatManager.session(player);
    if (existing.isPresent()) {
      SeatManager.SeatSession session = existing.get();
      if (session.type() == type) {
        seatManager.removeSeat(player, SeatManager.SeatRemovalReason.COMMAND);
        ledgerBridge.log(player, type, "stop");
        return Result.ok(type.stopMessage());
      }
      seatManager.removeSeat(player, SeatManager.SeatRemovalReason.REPLACED);
    }

    if (isOnCooldown(player, config.core().cooldown())) {
      double seconds = remainingCooldownSeconds(player, config.core().cooldown());
      return Result.error(Text.translatable("minemotes.cmd.cooldown", String.format("%.1f", seconds)));
    }

    Optional<Text> deny = validateSeat(player, type, config);
    if (deny.isPresent()) {
      return Result.error(deny.get());
    }

    Vec3d overrideOffset = null;
    Float yawOverride = null;
    if (type == EmoteType.CHAIR) {
      Optional<ChairPlacement> placement = computeChairPlacement(player);
      if (placement.isEmpty()) {
        return handleSeat(player, EmoteType.SIT);
      }
      ChairPlacement chairPlacement = placement.get();
      overrideOffset = chairPlacement.offset();
      yawOverride = chairPlacement.yaw();
    }

    SeatManager.SeatResult result = seatManager.createSeat(player, type, seatCfg, overrideOffset, yawOverride);
    if (!result.success()) {
      LOGGER.warn("(minemotes) failed to spawn seat for {}: {}", player.name(), result.error());
      return Result.error(Text.translatable("minemotes.cmd.disabled"));
    }

    ledgerBridge.log(player, type, "start");
    markCooldown(player, config.core().cooldown());
    return Result.ok(type.startMessage());
  }

  public void tick(MinecraftServer server) {
    Config config = configSupplier.get();
    if (!config.core().enabled()) {
      for (ServerPlayerEntity entity : server.getPlayerManager().getPlayerList()) {
        stopAll(PlayerAdapter.fabric(entity), SeatManager.SeatRemovalReason.DISABLED);
      }
      return;
    }

    for (ServerPlayerEntity entity : server.getPlayerManager().getPlayerList()) {
      PlayerAdapter player = PlayerAdapter.fabric(entity);
      if (isWorldDisabled(player, config)) {
        stopAll(player, SeatManager.SeatRemovalReason.DISABLED);
        continue;
      }
      tickCrawl(player, config);
      tickSeat(player, config);
    }
    seatManager.cleanupOrphans();
  }

  public void onDisconnect(ServerPlayerEntity player) {
    crawling.remove(player.getUuid());
    seatManager.removeSeat(PlayerAdapter.fabric(player), SeatManager.SeatRemovalReason.DISCONNECT);
  }

  public void onDeath(ServerPlayerEntity player) {
    crawling.remove(player.getUuid());
    seatManager.removeSeat(PlayerAdapter.fabric(player), SeatManager.SeatRemovalReason.DEATH);
  }

  public void onWorldChange(ServerPlayerEntity player) {
    seatManager.removeSeat(PlayerAdapter.fabric(player), SeatManager.SeatRemovalReason.WORLD_CHANGE);
    crawling.remove(player.getUuid());
  }

  public void onTeleport(ServerPlayerEntity player) {
    seatManager.removeSeat(PlayerAdapter.fabric(player), SeatManager.SeatRemovalReason.TELEPORT);
  }

  public void onConfigReload(Config config) {
    if (!config.core().enabled()) {
      // Stop everyone immediately.
      crawling.clear();
      seatManager.discardAll();
    }
  }

  private void tickCrawl(PlayerAdapter player, Config config) {
    if (!crawling.contains(player.uuid())) {
      return;
    }
    if (config.core().cancelOnDamage().enabledFor(EmoteType.CRAWL) && player.hurtTime() > 0) {
      crawling.remove(player.uuid());
      player.setSwimming(false);
      player.setPose(EntityPose.STANDING);
      return;
    }
    if (!config.emotes().crawl().enabled()) {
      crawling.remove(player.uuid());
      return;
    }
    if (player.isRemoved() || player.isSpectator() || player.isDead()) {
      crawling.remove(player.uuid());
      return;
    }
    if (config.emotes().crawl().waterOnly() && !player.isTouchingWater()) {
      crawling.remove(player.uuid());
      player.setSwimming(false);
      player.setPose(EntityPose.STANDING);
      return;
    }
    if (!player.isOnGround() && player.isFlying()) {
      crawling.remove(player.uuid());
      return;
    }
    if (player.hasVehicle()) {
      crawling.remove(player.uuid());
      player.setSwimming(false);
      player.setPose(EntityPose.STANDING);
      return;
    }
    if (config.core().cancelOnDanger() && isSuffocating(player)) {
      crawling.remove(player.uuid());
      player.setSwimming(false);
      player.setPose(EntityPose.STANDING);
      return;
    }
    player.setSwimming(true);
    player.setPose(EntityPose.SWIMMING);
  }

  private void tickSeat(PlayerAdapter player, Config config) {
    seatManager
        .session(player)
        .ifPresent(
            session -> {
              if (!seatEnabled(session.type(), config)) {
                seatManager.removeSeat(player, SeatManager.SeatRemovalReason.DISABLED);
                return;
              }
              if (!player.hasVehicle() || player.vehicleId() != session.seat().id()) {
                seatManager.removeSeat(player, SeatManager.SeatRemovalReason.UNKNOWN);
                return;
              }
              if (player.isRemoved() || player.isDead()) {
                seatManager.removeSeat(player, SeatManager.SeatRemovalReason.DEATH);
                return;
              }
              if (!config.core().allowInWater().allow(session.type()) && player.isTouchingWater()) {
                seatManager.removeSeat(player, SeatManager.SeatRemovalReason.MOVE);
                return;
              }
              if (player.isFlying()) {
                seatManager.removeSeat(player, SeatManager.SeatRemovalReason.MOVE);
                return;
              }
              Vec3d currentPos = player.position();
              if (config.core().cancelOnMove().enabledFor(session.type())) {
                Vec3d last = session.lastPlayerPos();
                if (last != null && last.squaredDistanceTo(currentPos) > 0.01) {
                  seatManager.removeSeat(player, SeatManager.SeatRemovalReason.MOVE);
                  return;
                }
              }
              session.updateLastPos(currentPos);
              Vec3d seatPos = currentPos.add(session.offset());
              session.seat().reposition(seatPos, player.yaw());
              adjustPitch(player, session.targetPitch());
              if (config.core().cancelOnDamage().enabledFor(session.type()) && player.hurtTime() > 0) {
                seatManager.removeSeat(player, SeatManager.SeatRemovalReason.DAMAGE);
              }
            });
  }

  private void adjustPitch(PlayerAdapter player, float target) {
    float current = player.pitch();
    float diff = target - current;
    float step = Math.max(-8f, Math.min(8f, diff));
    player.setPitch(current + step);
  }

  private Optional<Text> validateCrawl(PlayerAdapter player, Config config) {
    if (player.isSpectator() || player.isRemoved()) {
      return Optional.of(Text.translatable("minemotes.cmd.disabled"));
    }
    if (player.isFlying()) {
      return Optional.of(Text.translatable("minemotes.cmd.disabled"));
    }
    if (player.hasVehicle()) {
      return Optional.of(Text.translatable("minemotes.cmd.disabled"));
    }
    if (config.emotes().crawl().waterOnly() && !player.isTouchingWater()) {
      return Optional.of(Text.translatable("minemotes.cmd.disabled"));
    }
    return Optional.empty();
  }

  private Optional<Text> validateSeat(PlayerAdapter player, EmoteType type, Config config) {
    if (player.isSpectator() || player.isRemoved()) {
      return Optional.of(Text.translatable("minemotes.cmd.disabled"));
    }
    if (player.isFlying() || player.isSleeping()) {
      return Optional.of(Text.translatable("minemotes.cmd.disabled"));
    }
    if (player.hasVehicle()) {
      return Optional.of(Text.translatable("minemotes.cmd.disabled"));
    }
    if (!config.core().allowInWater().allow(type) && player.isTouchingWater()) {
      return Optional.of(Text.translatable("minemotes.cmd.disabled"));
    }
    return Optional.empty();
  }

  private boolean seatEnabled(EmoteType type, Config config) {
    Config.SeatEmoteConfig cfg = seatConfig(config, type);
    return cfg != null && cfg.enabled();
  }

  private Config.SeatEmoteConfig seatConfig(Config config, EmoteType type) {
    return switch (type) {
      case SIT -> config.emotes().sit();
      case CHAIR -> config.emotes().chair();
      case LAY -> config.emotes().lay();
      case BELLY -> config.emotes().belly();
      default -> null;
    };
  }

  private String permissionFor(EmoteType type, Config config) {
    return switch (type) {
      case SIT -> config.permissions().sit();
      case CHAIR -> config.permissions().chair();
      case LAY -> config.permissions().lay();
      case BELLY -> config.permissions().belly();
      default -> config.permissions().crawl();
    };
  }

  private boolean isOnCooldown(PlayerAdapter player, Duration cooldown) {
    long now = System.nanoTime();
    long ready = cooldowns.getOrDefault(player.uuid(), 0L);
    return now < ready;
  }

  private void markCooldown(PlayerAdapter player, Duration cooldown) {
    long readyAt = System.nanoTime() + cooldown.toNanos();
    cooldowns.put(player.uuid(), readyAt);
  }

  private double remainingCooldownSeconds(PlayerAdapter player, Duration cooldown) {
    long now = System.nanoTime();
    long ready = cooldowns.getOrDefault(player.uuid(), 0L);
    long remaining = Math.max(0L, ready - now);
    return remaining / 1_000_000_000.0d;
  }

  private boolean isWorldDisabled(PlayerAdapter player, Config config) {
    return config.core().disableInWorlds().contains(player.worldKey());
  }

  private void stopAll(PlayerAdapter player, SeatManager.SeatRemovalReason reason) {
    crawling.remove(player.uuid());
    seatManager.removeSeat(player, reason);
    player.setSwimming(false);
    player.setPose(EntityPose.STANDING);
  }

  private boolean isSuffocating(PlayerAdapter player) {
    Box box = player.boundingBox().offset(0, 0.2, 0);
    return !player.isSpaceEmpty(box);
  }

  private Optional<ChairPlacement> computeChairPlacement(PlayerAdapter player) {
    ServerPlayerEntity entity = player.entity();
    if (entity == null) {
      return Optional.empty();
    }
    HitResult hit = entity.raycast(5.0d, 0.0f, false);
    if (!(hit instanceof BlockHitResult blockHit)) {
      return Optional.empty();
    }
    BlockPos pos = blockHit.getBlockPos();
    BlockState state = entity.getWorld().getBlockState(pos);
    if (!(state.getBlock() instanceof StairsBlock)) {
      return Optional.empty();
    }
    Direction facing = state.get(StairsBlock.FACING);
    BlockHalf half = state.get(StairsBlock.HALF);
    double topY = pos.getY() + (half == BlockHalf.TOP ? 1.0d : 0.5d);
    Vec3d base = new Vec3d(pos.getX() + 0.5d, topY, pos.getZ() + 0.5d);
    Vec3d offset = base.subtract(player.position());
    return Optional.of(new ChairPlacement(offset, yawFor(facing)));
  }

  private record ChairPlacement(Vec3d offset, float yaw) {}

  private static float yawFor(Direction direction) {
    return switch (direction) {
      case NORTH -> 180.0f;
      case SOUTH -> 0.0f;
      case WEST -> 90.0f;
      case EAST -> 270.0f;
      default -> 0.0f;
    };
  }

  public enum Toggle {
    ENABLE,
    DISABLE,
    TOGGLE;

    public static Toggle fromArgument(String value) {
      return switch (value.toLowerCase()) {
        case "on", "enable", "start" -> ENABLE;
        case "off", "disable", "stop" -> DISABLE;
        default -> TOGGLE;
      };
    }
  }

  public record Result(boolean success, Text message) {
    public static Result ok(Text message) {
      return new Result(true, message);
    }

    public static Result error(Text message) {
      return new Result(false, message);
    }
  }
}
