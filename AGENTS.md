# MinEmotes — AGENTS.md (Master Spec) v0.1.0

**Purpose:** A tiny Fabric server-side add-on for **MinCore** that provides simple emotes:
`/crawl`, `/sit`, `/lay`, `/belly`. No client mod required. **Lay uses the vehicle method** (ArmorStand rider), not bed tricks.

**Hard requirements:**
- **MinCore** is a hard dependency. Do **not** bundle MinCore.
- **LuckPerms support** via **Fabric Permissions API** (`me.lucko:fabric-permissions-api`). Use permission nodes for each command.
- **Everything must be toggleable in config** (enable/disable whole feature & each emote).
- No world edits for lay; **vehicle method only**.
- All server logic **off the main thread** where I/O would occur (no DB used; emotes core logic can run on main thread as it manipulates entities).

---

## 1) Compatibility & Packaging
- Minecraft (Fabric): 1.21.8, server-side only
- Java: 21 LTS
- Loom: 1.11.x
- Mod id: `minemotes`
- Group: `dev.minemotes`
- Public API: not required; commands only
- Dependencies:
  - `fabric-api` (runtime)
  - `mincore >= 0.2.0` (hard depends; declared in `fabric.mod.json`)
  - `fabric-permissions-api` (compile/runtime; LuckPerms-compatible)

**Do NOT bundle MinCore**. Modrinth dependency metadata must declare MinCore as **required** so it is auto-downloaded.

---

## 2) Commands & Permissions
- `/crawl [on|off|toggle]` — node: `minemotes.crawl`
- `/sit` — node: `minemotes.sit`
- `/lay` — node: `minemotes.lay`
- `/belly` — node: `minemotes.belly`
- `/minemotes reload` — node: `minemotes.admin`

Implement via Fabric command API. Respect **cooldown** (config). Use **Fabric Permissions API** for checks:
```java
import me.lucko.fabric.api.permissions.v0.Permissions;
// example:
boolean allowed = Permissions.check(serverPlayer, "minemotes.sit", 2); // fallback OP level 2
```

When a feature is **disabled in config**, command should respond with an i18n message and do nothing.

---

## 3) Implementation Details (server-only)

### 3.1 Crawl (real crawl/swim pose)
- Toggle flag per player `crawlEnabled`.
- Each server tick (or on movement events), when enabled:
  - `player.setSwimming(true);`
  - `player.setPose(EntityPose.SWIMMING);`
- Auto-exit on: elytra gliding, creative fly, water-only restriction (config), mounted (not our seat), world change.
- Suffocation guard: if head space too small and `cancelOnDanger=true` then auto-exit with message.

### 3.2 Seat entity (shared by sit/lay/belly)
- Spawn **ArmorStand** with:
  - `Marker=true`, `Invisible=true`, `NoGravity=true`, `Small=true`, `Silent=true`, invulnerable & persistent.
- Position at player feet plus offset (config per emote).
- `player.startRiding(seat, true)`.
- Dismount on: jump, movement (config), damage (config), teleport/world change, death, logout, command.
- Maintain indexes:
  - `seats: Map<UUID, EntityRef>`; reverse index entityId→UUID to clean up.
- Always Despawn seat on dismount/disable.

### 3.3 Sit
- Use seat with `offsetY` (default −0.45). Optional face toward last movement yaw.

### 3.4 Lay (vehicle method, not bed)
- Use seat with lower `offsetY` (default −0.90).
- On tick while seat-mounted and in lay mode, gently lerp player pitch toward **+90°** (face-up).
- Keep player mostly stationary; do not move the seat unless configured.

### 3.5 Belly (prone face-down)
- Same as lay but lerp pitch toward **−90°** (face-down).
- Slight Z-offset optional (`offsetZ`), default 0.

### 3.6 Cleanup & Guards
- Deny emotes if: dead, already riding non-seat, gliding, creative flying, in water (for sit/lay/belly if config says), sleeping, in vehicle not ours.
- Clean up on: dismount, damage (conditional), teleport/world change, death, logout, plugin disable.

---

## 4) Config (JSON5) — everything toggleable
File: `config/minemotes.json5` (example generated on first run).

```json5
core: {
  enabled: true,
  cooldownS: 2,
  cancelOnDamage: { sit: true, lay: true, belly: true, crawl: false },
  cancelOnMove:   { sit: true, lay: false, belly: false, crawl: false },
  allowInWater:   { sit: false, lay: false, belly: false },
  disableInWorlds: []
}
emotes: {
  crawl: { enabled: true },
  sit:   { enabled: true, offsetY: -0.45 },
  lay:   { enabled: true, offsetY: -0.90, pitchDegrees: 90 },
  belly: { enabled: true, offsetY: -0.92, pitchDegrees: -90 }
}
permissions: {
  crawl: "minemotes.crawl",
  sit:   "minemotes.sit",
  lay:   "minemotes.lay",
  belly: "minemotes.belly",
  admin: "minemotes.admin"
}
```

**Hot reload**: implement a file watcher to reload and atomically swap config in-memory. Commands should pick up new values immediately.

---

## 5) i18n
- Keys under `assets/minemotes/lang/en_us.json`:
  - `minemotes.cmd.disabled`
  - `minemotes.cmd.cooldown`
  - `minemotes.crawl.on`, `minemotes.crawl.off`, `minemotes.crawl.toggled`
  - `minemotes.sit.start`, `minemotes.sit.stop`
  - `minemotes.lay.start`, `minemotes.lay.stop`
  - `minemotes.belly.start`, `minemotes.belly.stop`

---

## 6) Tests (JUnit 5)
- Config loader roundtrip.
- Seat manager: spawn & cleanup bookkeeping (no actual server tick; mock minimal parts).
- Crawl toggling rules (unit-level).

---

## 7) Deliverables
- Source with entrypoint `dev.minemotes.MinEmotesMod`
- Commands: `/crawl`, `/sit`, `/lay`, `/belly`, `/minemotes reload`
- Config loader & hot reload
- LuckPerms support via Fabric Permissions API
- README.md, DEVELOPER_GUIDE.md, AGENTS.md
- Example config in `config-examples/`
- Modrinth publishing templates
- Tests compile & run

---

## 8) Packaging rule (critical)
- **Do NOT bundle or ship MinCore** inside MinEmotes or its releases.
- Rely on `fabric.mod.json` `depends` + Modrinth metadata for auto-resolve.


---

## 9) Permission gateway (LuckPerms-first)
- Implement a **permission gateway** with the following order:
  1) **LuckPerms official API** via `LuckPermsProvider` (preferred; respects contexts).
  2) **Fabric Permissions API** if present.
  3) **Vanilla OP level** fallback (configurable default level per command; use 2 for admin by default).
- Ship it as `dev.minemotes.perms.Perms#check(ServerPlayerEntity, node, opLevel)`.
- Do **not** declare LuckPerms or Fabric Permissions API as hard dependencies in `fabric.mod.json`; list them under `suggests` only.

---

## 9) Permission gateway (MinCore-first)
- Prefer **MinCore’s gateway**: call `dev.mincore.perms.Perms.check(player, node, opLevel)`.
- If unavailable (older MinCore), fall back to a local gateway: LuckPerms → Fabric Permissions API → OP fallback (no hard depends).
