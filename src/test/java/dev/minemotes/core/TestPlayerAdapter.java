package dev.minemotes.core;

import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

final class TestPlayerAdapter implements PlayerAdapter {
  private final UUID uuid;
  private final String name;
  private final String worldKey;
  private boolean spectator;
  private boolean removed;
  private boolean dead;
  private boolean sleeping;
  private boolean touchingWater;
  private boolean hasVehicle;
  private int vehicleId;
  private boolean flying;
  private boolean onGround = true;
  private int hurtTime;
  private Vec3d position = Vec3d.ZERO;
  private float yaw;
  private float pitch;
  private Box box = new Box(0, 0, 0, 1, 1, 1);
  private boolean spaceEmpty = true;
  private boolean swimming;
  private EntityPose pose = EntityPose.STANDING;

  TestPlayerAdapter(UUID uuid, String name, String worldKey) {
    this.uuid = uuid;
    this.name = name;
    this.worldKey = worldKey;
  }

  void setSpectator(boolean spectator) {
    this.spectator = spectator;
  }

  void setRemoved(boolean removed) {
    this.removed = removed;
  }

  void setDead(boolean dead) {
    this.dead = dead;
  }

  void setSleeping(boolean sleeping) {
    this.sleeping = sleeping;
  }

  void setTouchingWater(boolean touchingWater) {
    this.touchingWater = touchingWater;
  }

  void setVehicle(boolean hasVehicle, int vehicleId) {
    this.hasVehicle = hasVehicle;
    this.vehicleId = vehicleId;
  }

  void setFlying(boolean flying) {
    this.flying = flying;
  }

  void setOnGround(boolean onGround) {
    this.onGround = onGround;
  }

  void setHurtTime(int hurtTime) {
    this.hurtTime = hurtTime;
  }

  void setPosition(Vec3d position) {
    this.position = position;
  }

  void setYaw(float yaw) {
    this.yaw = yaw;
  }

  void setBoundingBox(Box box) {
    this.box = box;
  }

  void setSpaceEmpty(boolean spaceEmpty) {
    this.spaceEmpty = spaceEmpty;
  }

  boolean swimming() {
    return swimming;
  }

  EntityPose pose() {
    return pose;
  }

  @Override
  public UUID uuid() {
    return uuid;
  }

  @Override
  public String uuidString() {
    return uuid.toString();
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public boolean isSpectator() {
    return spectator;
  }

  @Override
  public boolean isRemoved() {
    return removed;
  }

  @Override
  public boolean isDead() {
    return dead;
  }

  @Override
  public boolean isSleeping() {
    return sleeping;
  }

  @Override
  public boolean isTouchingWater() {
    return touchingWater;
  }

  @Override
  public boolean hasVehicle() {
    return hasVehicle;
  }

  @Override
  public int vehicleId() {
    return vehicleId;
  }

  @Override
  public boolean isOnGround() {
    return onGround;
  }

  @Override
  public boolean isFlying() {
    return flying;
  }

  @Override
  public int hurtTime() {
    return hurtTime;
  }

  @Override
  public boolean startRiding(Entity seat, boolean force) {
    this.hasVehicle = true;
    this.vehicleId = seat.getId();
    return true;
  }

  @Override
  public void stopRiding() {
    this.hasVehicle = false;
    this.vehicleId = -1;
  }

  @Override
  public Vec3d position() {
    return position;
  }

  @Override
  public float yaw() {
    return yaw;
  }

  @Override
  public float pitch() {
    return pitch;
  }

  @Override
  public void setPitch(float pitch) {
    this.pitch = pitch;
  }

  @Override
  public void setSwimming(boolean swimming) {
    this.swimming = swimming;
  }

  @Override
  public void setPose(EntityPose pose) {
    this.pose = pose;
  }

  @Override
  public Box boundingBox() {
    return box;
  }

  @Override
  public boolean isSpaceEmpty(Box box) {
    return spaceEmpty;
  }

  @Override
  public String worldKey() {
    return worldKey;
  }

  @Override
  public ServerWorld world() {
    throw new UnsupportedOperationException("not used in tests");
  }

  @Override
  public ServerPlayerEntity entity() {
    return null;
  }
}
