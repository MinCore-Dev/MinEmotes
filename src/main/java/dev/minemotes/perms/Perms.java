package dev.minemotes.perms;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.network.ServerPlayerEntity;

public final class Perms {
  private static final boolean HAS_LP = classPresent("net.luckperms.api.LuckPermsProvider");
  private static final boolean HAS_FAB_PERMS = FabricLoader.getInstance().isModLoaded("fabric-permissions-api-v0");

  private Perms(){}

  public static boolean check(ServerPlayerEntity player, String node, int opLevelFallback) {
    // 1) LuckPerms
    if (HAS_LP) {
      try {
        var lp = net.luckperms.api.LuckPermsProvider.get();
        var user = lp.getUserManager().getUser(player.getUuid());
        if (user != null) {
          var result = user.getCachedData()
              .getPermissionData(lp.getContextManager().getQueryOptions(user))
              .checkPermission(node);
          return result.asBoolean();
        }
      } catch (Throwable ignored) { /* fall through */ }
    }
    // 2) Fabric Permissions API
    if (HAS_FAB_PERMS) {
      try {
        return me.lucko.fabric.api.permissions.v0.Permissions.check(player, node, opLevelFallback);
      } catch (Throwable ignored) { /* fall through */ }
    }
    // 3) Vanilla OP level
    var server = player.getServer();
    if (server == null) return false;
    boolean isOp = server.getPlayerManager().isOperator(player.getGameProfile());
    int level = isOp ? 4 : 0;
    return level >= opLevelFallback;
  }

  private static boolean classPresent(String fqcn) {
    try { Class.forName(fqcn, false, Perms.class.getClassLoader()); return true; }
    catch (ClassNotFoundException e) { return false; }
  }
}
