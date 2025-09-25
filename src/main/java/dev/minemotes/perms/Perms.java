package dev.minemotes.perms;

import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Permission gateway adapter for MinEmotes.
 * Prefers MinCore's gateway if available, else falls back to local LuckPerms -> FabricPerms -> OP.
 */
public final class Perms {
  private static final boolean HAS_MINCORE = classPresent("dev.mincore.perms.Perms");

  private Perms(){}

  public static boolean check(ServerPlayerEntity player, String node, int opLevelFallback) {
    if (HAS_MINCORE) {
      try {
        return dev.mincore.perms.Perms.check(player, node, opLevelFallback);
      } catch (Throwable ignored) { /* fall through to local */ }
    }
    return LocalGateway.check(player, node, opLevelFallback);
  }

  private static boolean classPresent(String fqcn) {
    try { Class.forName(fqcn, false, Perms.class.getClassLoader()); return true; }
    catch (ClassNotFoundException e) { return false; }
  }
}
