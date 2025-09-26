# MinEmotes Developer Guide

This document highlights integration points for MinCore-based deployments and gives contributors a quick reference.

## Permission Gateway

Always prefer MinCore’s gateway when available:

```java
if (dev.mincore.perms.Perms.check(player, "minemotes.sit", 0)) {
  // allowed
}
```

If MinCore is missing or exposes an older gateway, fall back to the local adapter:

```java
if (dev.minemotes.perms.Perms.check(player, "minemotes.sit", 0)) {
  // LuckPerms → Fabric Permissions API → OP fallback
}
```

The adapter detects LuckPerms and the Fabric Permissions API at runtime without making them hard dependencies. Fallback OP levels are configurable per node via `config/minemotes.json5`.

## Configuration & Hot Reload

Configuration lives in `config/minemotes.json5` (JSON5 syntax). A file watcher reloads the configuration automatically when it changes; admins can also run `/minemotes reload`. The reload command requires `minemotes.admin` and re-validates bounds (cooldown, offsets, etc.).

Key sections:

- `core`: enable flag, cooldown, cancel-on-move/damage settings, per-world disable list.
- `emotes`: enable switches, seat offsets, and pitch targets.
- `permissions`: permission node names and OP fallbacks for the gateway.

## Event Hooks & Cleanup

- Seats are implemented with invisible ArmorStand markers. The `SeatManager` tracks player ↔ seat relationships and cleans up on logout, world change, death, teleport, and damage (depending on config flags).
- The `/chair` command raycasts for stair blocks to anchor the seat; if no stair is targeted it defers to the normal `/sit` pose.
- Crawl mode is toggled per player and re-applies the swimming pose each tick. Danger checks (headroom) can cancel the crawl automatically.

## MinCore Ledger Logging

When MinCore is loaded, MinEmotes uses `dev.mincore.api.MinCoreApi.ledger()` to log `minemotes` events (start/stop) with a small JSON payload. If MinCore is absent the hook is skipped gracefully.

## Testing

Unit tests cover:

- Config loader round-trip parsing and overrides.
- Seat manager bookkeeping with a stub seat factory.
- Crawl toggling (enable/disable and cooldown behaviour).

Run the suite with:

```bash
./gradlew test
```

## Conventions

- Java 21, Fabric Loom 1.11.
- Keep JDBC/network work off the main thread (not required for the emote features but relevant for future expansion).
- No bundled MinCore—declare it as a dependency in `fabric.mod.json` and Modrinth metadata only.
