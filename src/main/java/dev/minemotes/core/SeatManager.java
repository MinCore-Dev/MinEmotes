package dev.minemotes.core;

import dev.minemotes.config.Config;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Manages invisible marker seats for sit/lay/belly emotes. */
public final class SeatManager {
  private static final Logger LOGGER = LogManager.getLogger("minemotes");

  private final Map<UUID, SeatSession> seats = new ConcurrentHashMap<>();
  private final Map<Integer, UUID> seatByEntityId = new ConcurrentHashMap<>();
  private final SeatFactory seatFactory;

  public SeatManager() {
    this(new ArmorStandSeatFactory());
  }

  SeatManager(SeatFactory seatFactory) {
    this.seatFactory = seatFactory;
  }

  public Optional<SeatSession> session(ServerPlayerEntity player) {
    return session(PlayerAdapter.fabric(player));
  }

  Optional<SeatSession> session(PlayerAdapter player) {
    return Optional.ofNullable(seats.get(player.uuid()));
  }

  public SeatResult createSeat(ServerPlayerEntity player, EmoteType type, Config.SeatEmoteConfig cfg) {
    return createSeat(PlayerAdapter.fabric(player), type, cfg);
  }

  SeatResult createSeat(PlayerAdapter player, EmoteType type, Config.SeatEmoteConfig cfg) {
    return createSeat(player, type, cfg, null, null);
  }

  SeatResult createSeat(
      PlayerAdapter player,
      EmoteType type,
      Config.SeatEmoteConfig cfg,
      Vec3d overrideOffset,
      Float yawOverride) {
    Objects.requireNonNull(player, "player");
    Objects.requireNonNull(cfg, "cfg");

    removeSeat(player, SeatRemovalReason.REPLACED);

    Vec3d offset = new Vec3d(cfg.offsetX(), cfg.offsetY(), cfg.offsetZ());
    if (overrideOffset != null) {
      offset = offset.add(overrideOffset);
    }
    float yaw = yawOverride != null ? yawOverride : player.yaw();
    Vec3d spawnPos = player.position().add(offset);

    Seat seat = seatFactory.create(player, spawnPos, yaw);
    if (seat == null) {
      return SeatResult.failure("seat.create");
    }

    if (!seat.spawn()) {
      seat.discard();
      return SeatResult.failure("seat.spawn");
    }

    if (!seat.startRiding(player)) {
      seat.discard();
      return SeatResult.failure("seat.ride");
    }

    SeatSession session =
        new SeatSession(
            player.uuid(),
            player,
            type,
            seat,
            offset,
            cfg.targetPitch(),
            player.position());
    seats.put(player.uuid(), session);
    seatByEntityId.put(seat.id(), player.uuid());
    return SeatResult.success(session);
  }

  public void removeSeat(ServerPlayerEntity player, SeatRemovalReason reason) {
    removeSeat(PlayerAdapter.fabric(player), reason);
  }

  void removeSeat(PlayerAdapter player, SeatRemovalReason reason) {
    if (player == null) {
      return;
    }
    SeatSession session = seats.remove(player.uuid());
    if (session != null) {
      seatByEntityId.remove(session.seat().id());
      session.seat().discard();
      if (player.hasVehicle()) {
        player.stopRiding();
      }
      LOGGER.debug("(minemotes) seat removed: {} ({})", player.name(), reason);
    }
  }

  public void removeSeat(int entityId, SeatRemovalReason reason) {
    UUID owner = seatByEntityId.remove(entityId);
    if (owner == null) {
      return;
    }
    SeatSession session = seats.remove(owner);
    if (session != null) {
      session.seat().discard();
      session.player().stopRiding();
      LOGGER.debug("(minemotes) seat entity {} removed ({})", entityId, reason);
    }
  }

  public void cleanupOrphans() {
    for (SeatSession session : seats.values()) {
      if (session.seat().isRemoved()) {
        removeSeat(session.seat().id(), SeatRemovalReason.ORPHANED);
      }
    }
  }

  public void discardAll() {
    Collection<SeatSession> copy = seats.values();
    for (SeatSession session : copy) {
      session.seat().discard();
      session.player().stopRiding();
    }
    seats.clear();
    seatByEntityId.clear();
  }

  public enum SeatRemovalReason {
    COMMAND,
    DAMAGE,
    MOVE,
    TELEPORT,
    WORLD_CHANGE,
    DISCONNECT,
    DEATH,
    DISABLED,
    REPLACED,
    ORPHANED,
    UNKNOWN
  }

  public record SeatResult(boolean success, String error, SeatSession session) {
    public static SeatResult success(SeatSession session) {
      return new SeatResult(true, null, session);
    }

    public static SeatResult failure(String error) {
      return new SeatResult(false, error, null);
    }
  }

  public static final class SeatSession {
    private final UUID playerId;
    private final PlayerAdapter player;
    private final EmoteType type;
    private final Seat seat;
    private final Vec3d offset;
    private final float targetPitch;
    private Vec3d lastPlayerPos;

    SeatSession(
        UUID playerId,
        PlayerAdapter player,
        EmoteType type,
        Seat seat,
        Vec3d offset,
        float targetPitch,
        Vec3d lastPlayerPos) {
      this.playerId = playerId;
      this.player = player;
      this.type = type;
      this.seat = seat;
      this.offset = offset;
      this.targetPitch = targetPitch;
      this.lastPlayerPos = lastPlayerPos;
    }

    public UUID playerId() {
      return playerId;
    }

    public PlayerAdapter player() {
      return player;
    }

    public EmoteType type() {
      return type;
    }

    public Seat seat() {
      return seat;
    }

    public Vec3d offset() {
      return offset;
    }

    public float targetPitch() {
      return targetPitch;
    }

    public Vec3d lastPlayerPos() {
      return lastPlayerPos;
    }

    public void updateLastPos(Vec3d pos) {
      this.lastPlayerPos = pos;
    }
  }

  interface SeatFactory {
    Seat create(PlayerAdapter player, Vec3d position, float yaw);
  }

  interface Seat {
    boolean spawn();

    boolean startRiding(PlayerAdapter player);

    void discard();

    boolean isRemoved();

    int id();

    void reposition(Vec3d pos, float yaw);
  }

  private static final class ArmorStandSeatFactory implements SeatFactory {
    @Override
    public Seat create(PlayerAdapter player, Vec3d position, float yaw) {
      ServerWorld world = player.world();
      ArmorStandEntity stand = new ArmorStandEntity(world, position.x, position.y, position.z);
      stand.setInvisible(true);
      stand.setNoGravity(true);
      stand.setSilent(true);
      stand.setInvulnerable(true);
      stand.refreshPositionAndAngles(position.x, position.y, position.z, yaw, 0.0f);
      applySeatFlags(stand);
      return new ArmorStandSeat(world, stand);
    }
  }

  private static final class ArmorStandSeat implements Seat {
    private final ServerWorld world;
    private final ArmorStandEntity stand;

    ArmorStandSeat(ServerWorld world, ArmorStandEntity stand) {
      this.world = world;
      this.stand = stand;
    }

    @Override
    public boolean spawn() {
      return world.spawnEntity(stand);
    }

    @Override
    public boolean startRiding(PlayerAdapter player) {
      return player.startRiding(stand, true);
    }

    @Override
    public void discard() {
      stand.remove(RemovalReason.DISCARDED);
    }

    @Override
    public boolean isRemoved() {
      return !stand.isAlive() || stand.isRemoved();
    }

    @Override
    public int id() {
      return stand.getId();
    }

    @Override
    public void reposition(Vec3d pos, float yaw) {
      stand.refreshPositionAndAngles(pos.x, pos.y, pos.z, yaw, stand.getPitch());
    }
  }

  private static void applySeatFlags(ArmorStandEntity stand) {
    invokeBoolean(stand, "setMarker", true);
    invokeBoolean(stand, "setSmall", true);
  }

  private static void invokeBoolean(ArmorStandEntity stand, String methodName, boolean value) {
    try {
      Method method = ArmorStandEntity.class.getDeclaredMethod(methodName, boolean.class);
      method.setAccessible(true);
      method.invoke(stand, value);
    } catch (ReflectiveOperationException ignored) {
    }
  }
}
