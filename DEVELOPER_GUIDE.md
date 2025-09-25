# MinEmotes Developer Guide

Uses Fabric Permissions API (LuckPerms) for node checks.

## Nodes
- `minemotes.crawl`
- `minemotes.sit`
- `minemotes.lay`
- `minemotes.belly`
- `minemotes.admin`

Example check:
```java
boolean allowed = me.lucko.fabric.api.permissions.v0.Permissions.check(player, "minemotes.sit", 2);
```

## Config
See `config/minemotes.json5` (generated example on first run). Everything is toggleable.
