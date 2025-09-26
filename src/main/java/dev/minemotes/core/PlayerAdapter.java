package dev.minemotes.core;

import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/** Abstraction around {@link ServerPlayerEntity} for easier testing. */
interface PlayerAdapter {
  UUID uuid();

  String uuidString();

  String name();

  boolean isSpectator();

  boolean isRemoved();

  boolean isDead();

  boolean isSleeping();

  boolean isTouchingWater();

  boolean hasVehicle();

  int vehicleId();

  boolean isOnGround();

  boolean isFlying();

  int hurtTime();

  boolean startRiding(Entity seat, boolean force);

  void stopRiding();

  Vec3d position();

  float yaw();

  float pitch();

  void setPitch(float pitch);

  void setSwimming(boolean swimming);

  void setPose(EntityPose pose);

  Box boundingBox();

  boolean isSpaceEmpty(Box box);

  String worldKey();

  ServerWorld world();

  ServerPlayerEntity entity();

  static PlayerAdapter fabric(ServerPlayerEntity player) {
    return new FabricPlayerAdapter(player);
  }
}

final class FabricPlayerAdapter implements PlayerAdapter {
  private final ServerPlayerEntity player;

  FabricPlayerAdapter(ServerPlayerEntity player) {
    this.player = player;
  }

  @Override
  public UUID uuid() {
    return player.getUuid();
  }

  @Override
  public String uuidString() {
    return player.getUuidAsString();
  }

  @Override
  public String name() {
    return player.getName().getString();
  }

  @Override
  public boolean isSpectator() {
    return player.isSpectator();
  }

  @Override
  public boolean isRemoved() {
    return player.isRemoved();
  }

  @Override
  public boolean isDead() {
    return player.isDead();
  }

  @Override
  public boolean isSleeping() {
    return player.isSleeping();
  }

  @Override
  public boolean isTouchingWater() {
    return player.isTouchingWater();
  }

  @Override
  public boolean hasVehicle() {
    return player.hasVehicle();
  }

  @Override
  public int vehicleId() {
    return player.hasVehicle() && player.getVehicle() != null ? player.getVehicle().getId() : -1;
  }

  @Override
  public boolean isOnGround() {
    return player.isOnGround();
  }

  @Override
  public boolean isFlying() {
    return player.getAbilities().flying;
  }

  @Override
  public int hurtTime() {
    return player.hurtTime;
  }

  @Override
  public boolean startRiding(Entity seat, boolean force) {
    return player.startRiding(seat, force);
  }

  @Override
  public void stopRiding() {
    player.stopRiding();
  }

  @Override
  public Vec3d position() {
    return player.getPos();
  }

  @Override
  public float yaw() {
    return player.getYaw();
  }

  @Override
  public float pitch() {
    return player.getPitch();
  }

  @Override
  public void setPitch(float pitch) {
    player.setPitch(pitch);
  }

  @Override
  public void setSwimming(boolean swimming) {
    player.setSwimming(swimming);
  }

  @Override
  public void setPose(EntityPose pose) {
    player.setPose(pose);
  }

  @Override
  public Box boundingBox() {
    return player.getBoundingBox();
  }

  @Override
  public boolean isSpaceEmpty(Box box) {
    return player.getWorld().isSpaceEmpty(player, box);
  }

  @Override
  public String worldKey() {
    return player.getWorld().getRegistryKey().getValue().toString();
  }

  @Override
  public ServerWorld world() {
    return (ServerWorld) player.getWorld();
  }

  @Override
  public ServerPlayerEntity entity() {
    return player;
  }
}
