package dev.minemotes.perms;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Permission gateway that prefers MinCore then falls back to LuckPerms → Fabric API → OP. */
public final class Perms {
  private static final Logger LOGGER = LogManager.getLogger("minemotes");
  private static final String MINCORE_PERMS_CLASS = "dev.mincore.perms.Perms";
  private static final MethodHandle MINCORE_CHECK;
  private static final MethodHandle MINCORE_CHECK_UUID;

  private static final boolean FABRIC_PERMS_PRESENT = isClassPresent("me.lucko.fabric.api.permissions.v0.Permissions");
  private static final boolean LUCKPERMS_PRESENT = isClassPresent("net.luckperms.api.LuckPermsProvider");
  private static final AtomicBoolean FABRIC_WARNED = new AtomicBoolean();
  private static final AtomicBoolean LUCKPERMS_WARNED = new AtomicBoolean();

  static {
    MethodHandle check = null;
    MethodHandle checkUuid = null;
    try {
      Class<?> clazz = Class.forName(MINCORE_PERMS_CLASS);
      MethodHandles.Lookup lookup = MethodHandles.lookup();
      check =
          lookup.findStatic(
              clazz,
              "check",
              MethodType.methodType(boolean.class, ServerPlayerEntity.class, String.class, int.class));
      checkUuid =
          lookup.findStatic(
              clazz,
              "checkUUID",
              MethodType.methodType(
                  boolean.class, MinecraftServer.class, UUID.class, String.class, int.class));
    } catch (ReflectiveOperationException ignored) {
      check = null;
      checkUuid = null;
    }
    MINCORE_CHECK = check;
    MINCORE_CHECK_UUID = checkUuid;
  }

  private Perms() {}

  public static boolean check(ServerPlayerEntity player, String node, int opLevelFallback) {
    Objects.requireNonNull(player, "player");
    Objects.requireNonNull(node, "node");

    if (MINCORE_CHECK != null) {
      try {
        return (boolean) MINCORE_CHECK.invoke(player, node, opLevelFallback);
      } catch (Throwable t) {
        LOGGER.warn("(minemotes) MinCore permission gateway failed; falling back", t);
      }
    }

    Boolean lpResult = checkLuckPerms(player.getUuid(), node);
    if (lpResult != null) {
      return lpResult;
    }

    if (FABRIC_PERMS_PRESENT) {
      try {
        Class<?> permissions =
            Class.forName("me.lucko.fabric.api.permissions.v0.Permissions", false, Perms.class.getClassLoader());
        return (boolean)
            permissions
                .getMethod("check", ServerPlayerEntity.class, String.class, int.class)
                .invoke(null, player, node, opLevelFallback);
      } catch (ReflectiveOperationException | NoClassDefFoundError e) {
        if (FABRIC_WARNED.compareAndSet(false, true)) {
          LOGGER.debug("(minemotes) Fabric Permissions API unavailable", e);
        }
      }
    }

    return player.hasPermissionLevel(opLevelFallback);
  }

  public static boolean checkUUID(
      MinecraftServer server, UUID uuid, String node, int opLevelFallback) {
    Objects.requireNonNull(server, "server");
    Objects.requireNonNull(uuid, "uuid");
    Objects.requireNonNull(node, "node");

    if (MINCORE_CHECK_UUID != null) {
      try {
        return (boolean) MINCORE_CHECK_UUID.invoke(server, uuid, node, opLevelFallback);
      } catch (Throwable t) {
        LOGGER.warn("(minemotes) MinCore permission gateway failed; falling back", t);
      }
    }

    Boolean lpResult = checkLuckPerms(uuid, node);
    if (lpResult != null) {
      return lpResult;
    }

    ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
    if (player != null && FABRIC_PERMS_PRESENT) {
      try {
        Class<?> permissions =
            Class.forName("me.lucko.fabric.api.permissions.v0.Permissions", false, Perms.class.getClassLoader());
        return (boolean)
            permissions
                .getMethod("check", ServerPlayerEntity.class, String.class, int.class)
                .invoke(null, player, node, opLevelFallback);
      } catch (ReflectiveOperationException | NoClassDefFoundError e) {
        if (FABRIC_WARNED.compareAndSet(false, true)) {
          LOGGER.debug("(minemotes) Fabric Permissions API unavailable", e);
        }
      }
    }

    if (player != null) {
      return player.hasPermissionLevel(opLevelFallback);
    }

    if (opLevelFallback <= 0) {
      return true;
    }

    var cache = server.getUserCache();
    if (cache == null) {
      return false;
    }
    var profileOpt = cache.getByUuid(uuid);
    if (profileOpt.isEmpty()) {
      return false;
    }
    return server.getPermissionLevel(profileOpt.get()) >= opLevelFallback;
  }

  private static Boolean checkLuckPerms(UUID uuid, String node) {
    if (!LUCKPERMS_PRESENT) {
      return null;
    }
    try {
      Class<?> providerClass = Class.forName("net.luckperms.api.LuckPermsProvider");
      Object api = providerClass.getMethod("get").invoke(null);
      if (api == null) {
        return null;
      }
      Object userManager = api.getClass().getMethod("getUserManager").invoke(api);
      Object user = userManager.getClass().getMethod("getUser", UUID.class).invoke(userManager, uuid);
      if (user == null) {
        return null;
      }
      Object cachedData = user.getClass().getMethod("getCachedData").invoke(user);
      Object permissionData = cachedData.getClass().getMethod("getPermissionData").invoke(cachedData);
      Object tristate = permissionData.getClass().getMethod("checkPermission", String.class).invoke(permissionData, node);
      if (tristate == null) {
        return null;
      }
      String name = tristate.getClass().getMethod("name").invoke(tristate).toString();
      if ("TRUE".equals(name)) {
        return Boolean.TRUE;
      }
      if ("FALSE".equals(name)) {
        return Boolean.FALSE;
      }
      return null;
    } catch (ReflectiveOperationException | NoClassDefFoundError e) {
      if (LUCKPERMS_WARNED.compareAndSet(false, true)) {
        LOGGER.debug("(minemotes) LuckPerms API unavailable", e);
      }
      return null;
    }
  }

  private static boolean isClassPresent(String className) {
    try {
      Class.forName(className, false, Perms.class.getClassLoader());
      return true;
    } catch (ClassNotFoundException ignored) {
      return false;
    }
  }
}
