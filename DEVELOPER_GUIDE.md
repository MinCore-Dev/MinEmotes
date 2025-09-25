# MinEmotes Developer Guide

Uses a permission gateway: LuckPerms → Fabric Permissions API → OP fallback.

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


## Permission checks
Use the gateway:
```java
boolean allowed = dev.minemotes.perms.Perms.check(player, "minemotes.sit", 2);
```
Do not hard depend on LuckPerms; the gateway detects it at runtime.
