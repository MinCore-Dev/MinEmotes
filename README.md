# MinEmotes

Simple server-side emotes for Fabric: **/crawl**, **/sit**, **/lay**, **/belly**. No client mod required.  
Built as a **MinCore** add-on. LuckPerms supported via Fabric Permissions API.

## Features
- `/crawl` — true crawl/swim pose, move in 1×1 tunnels
- `/sit` — seated pose via invisible marker ArmorStand
- `/lay` — lying pose (vehicle trick; no bed mechanics)
- `/belly` — face-down prone variant
- All features **toggleable** in config
- **Permissions** per command (LuckPerms-compatible)

## Requirements
- Fabric 1.21.8 (server), Java 21
- **MinCore >= 0.2.0** (hard dependency)
- Fabric API
- Fabric Permissions API (LuckPerms)

## Install
1. Put MinEmotes and MinCore in `mods/` (do **not** bundle MinCore; Modrinth can auto-resolve).
2. Start once to generate `config/minemotes.json5.example`.
3. Adjust config; `/minemotes reload` to apply.

## Commands & permissions
- `/crawl [on|off|toggle]` — `minemotes.crawl`
- `/sit` — `minemotes.sit`
- `/lay` — `minemotes.lay`
- `/belly` — `minemotes.belly`
- `/minemotes reload` — `minemotes.admin`


## Permission gateway
MinEmotes checks permissions in this order:
1) **LuckPerms API** (if LuckPerms is installed)  
2) **Fabric Permissions API** (if present)  
3) **Vanilla OP level** fallback (default OP≥2 for admin commands)

## Permission gateway
This mod **prefers MinCore’s gateway** (`dev.mincore.perms.Perms`) for permission checks, with a local fallback to LuckPerms → Fabric Permissions API → OP.
