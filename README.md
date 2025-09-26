# MinEmotes

MinEmotes is a lightweight Fabric server-side emote add-on for [MinCore](https://github.com/MinCore-Dev/MinCore). It provides `/crawl`, `/sit`, `/chair`, `/lay`, and `/belly` without requiring any client mods. Seats are implemented with invisible ArmorStand vehicles and crawl uses the native swimming pose.

## Features

- Five emotes with per-emote toggles and offsets, including stair-aware chairs.
- Global enable/disable switch and per-world allow list.
- Cooldown handling and automatic cancellation on movement, damage, or danger (configurable).
- MinCore ledger logging for emote start/stop events when MinCore is present.
- JSON5 configuration with hot reload (`/minemotes reload`).
- Permission gateway that prefers **MinCore** and falls back to LuckPerms → Fabric Permissions API → vanilla OP levels.

## Requirements

- Java 21
- Minecraft 1.21.8 (Fabric)
- [Fabric API](https://modrinth.com/mod/fabric-api)
- [MinCore ≥ 0.2.0](https://github.com/MinCore-Dev/MinCore) (declared as a dependency)
- Optional: LuckPerms and/or Fabric Permissions API v0 for richer permission handling

## Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/crawl [on|off|toggle]` | `minemotes.crawl` | Toggle crawling (swimming pose). |
| `/sit` | `minemotes.sit` | Sit on an invisible marker seat. |
| `/chair` | `minemotes.chair` | Sit aligned to the targeted stair block or fall back to `/sit`. |
| `/lay` | `minemotes.lay` | Lie on your back using the vehicle method. |
| `/belly` | `minemotes.belly` | Lie face-down using the vehicle method. |
| `/minemotes reload` | `minemotes.admin` | Reload the JSON5 configuration from disk. |

If a command is disabled in the configuration or the executing player lacks permission, a localized error message is returned. Cooldowns are enforced per player.

## Configuration

The configuration file is stored at `config/minemotes.json5` (generated on first run). Every feature can be disabled or tuned. Example:

```json5
{
  core: {
    enabled: true,
    cooldownS: 2,
    cancelOnDamage: { crawl: false, sit: true, chair: true, lay: true, belly: true },
    cancelOnMove: { crawl: false, sit: true, chair: true, lay: false, belly: false },
    allowInWater: { sit: false, chair: false, lay: false, belly: false },
    cancelOnDanger: true,
    disableInWorlds: []
  },
  emotes: {
    crawl: { enabled: true, waterOnly: false },
    sit:   { enabled: true, offsetX: 0.0, offsetY: -0.45, offsetZ: 0.0, pitchDegrees: 0.0 },
    chair: { enabled: true, offsetX: 0.0, offsetY: 0.0,  offsetZ: 0.0, pitchDegrees: 0.0 },
    lay:   { enabled: true, offsetX: 0.0, offsetY: -0.90, offsetZ: 0.0, pitchDegrees: 90.0 },
    belly: { enabled: true, offsetX: 0.0, offsetY: -0.92, offsetZ: 0.0, pitchDegrees: -90.0 }
  },
  permissions: {
    crawl: "minemotes.crawl",
    sit:   "minemotes.sit",
    chair: "minemotes.chair",
    lay:   "minemotes.lay",
    belly: "minemotes.belly",
    admin: "minemotes.admin",
    fallbackOpLevels: { crawl: 0, sit: 0, chair: 0, lay: 0, belly: 0, admin: 2 }
  }
}
```

Changes to the config file are detected automatically by a background file watcher. Use `/minemotes reload` to apply edits immediately without restarting the server.

## Permission Gateway

MinEmotes defers to MinCore’s permission helper when available: `dev.mincore.perms.Perms.check(...)`. If MinCore is not present or the gateway is unavailable the mod attempts, in order:

1. LuckPerms API (through `LuckPermsProvider`)
2. Fabric Permissions API v0
3. Vanilla operator level fallback (configurable per node)

This keeps optional dependencies optional while still supporting production-grade permission checks.

## Logging & Analytics

When MinCore is present the mod writes emote start/stop events to the MinCore ledger under the `minemotes` add-on ID. Each entry includes the player UUID and whether the emote started or stopped. If MinCore is absent the logging hook is silently skipped.

## Development

```bash
./gradlew clean build
```

The test suite exercises the config loader, seat manager bookkeeping, and key crawl toggling rules. Hot reload and watcher behaviour are covered by integration tests at runtime.

See [`DEVELOPER_GUIDE.md`](DEVELOPER_GUIDE.md) for permission gateway usage and contributor notes.
