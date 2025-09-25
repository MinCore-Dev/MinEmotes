package dev.minemotes.commands;

/**
 * Placeholder for Fabric command registration. Demonstrates permission checks.
 */
public final class CommandRegistrar {
  public static void registerAll(dev.minemotes.core.Config cfg, dev.minemotes.core.EmoteService svc) {
    System.out.println("[MinEmotes] CommandRegistrar.registerAll() (stub)");
    // Example usage (pseudo-code):
    // ServerPlayerEntity p = ...;
    // if (!dev.minemotes.perms.Perms.check(p, cfg.permissions.sit, 2)) {
    //   // send no-permission message
    //   return;
    // }
    // svc.sit(p.getUuid());
  }
}

// NOTE: Permission checks should use MinCore’s gateway first (with local fallback).
