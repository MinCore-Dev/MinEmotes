package dev.minemotes.core;

import dev.minemotes.MinEmotesMod;
import java.lang.reflect.Method;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Reflection bridge into MinCore's ledger API. */
final class MinCoreLedgerBridge {
  private static final Logger LOGGER = LogManager.getLogger("minemotes");
  private static final String LEDGER_CLASS = "dev.mincore.api.Ledger";
  private static final String MINCORE_API_CLASS = "dev.mincore.api.MinCoreApi";

  private final Method ledgerAccessor;
  private final Method logMethod;

  MinCoreLedgerBridge() {
    Method accessor = null;
    Method log = null;
    try {
      Class<?> api = Class.forName(MINCORE_API_CLASS);
      accessor = api.getMethod("ledger");
      Class<?> ledgerClass = Class.forName(LEDGER_CLASS);
      log =
          ledgerClass.getMethod(
              "log",
              String.class,
              String.class,
              UUID.class,
              UUID.class,
              long.class,
              String.class,
              boolean.class,
              String.class,
              String.class,
              String.class,
              String.class);
    } catch (ReflectiveOperationException e) {
      LOGGER.debug("(minemotes) MinCore ledger API unavailable", e);
    }
    this.ledgerAccessor = accessor;
    this.logMethod = log;
  }

  void log(PlayerAdapter player, EmoteType type, String state) {
    if (ledgerAccessor == null || logMethod == null) {
      return;
    }
    try {
      Object ledger = ledgerAccessor.invoke(null);
      if (ledger == null) {
        return;
      }
      String reason = "emote:" + type.id();
      String extraJson =
          "{" +
          "\"player\":\"" + player.uuidString() + "\"," +
          "\"state\":\"" + state + "\"}";
      logMethod.invoke(
          ledger,
          MinEmotesMod.MODID,
          type.id(),
          player.uuid(),
          null,
          0L,
          reason,
          true,
          null,
          null,
          null,
          extraJson);
    } catch (ReflectiveOperationException e) {
      LOGGER.debug("(minemotes) Failed to log to MinCore ledger", e);
    }
  }
}
