package dev.minemotes.config;

import com.google.common.collect.ImmutableSet;
import dev.minemotes.core.EmoteType;
import java.time.Duration;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Runtime configuration loaded from {@code config/minemotes.json5}.
 */
public final class Config {
  private final Core core;
  private final Emotes emotes;
  private final Permissions permissions;

  public Config(Core core, Emotes emotes, Permissions permissions) {
    this.core = Objects.requireNonNull(core, "core");
    this.emotes = Objects.requireNonNull(emotes, "emotes");
    this.permissions = Objects.requireNonNull(permissions, "permissions");
  }

  public static Config defaults() {
    Core core =
        new Core(
            true,
            Duration.ofSeconds(2),
            CancelConfig.defaultCancelOnDamage(),
            CancelConfig.defaultCancelOnMove(),
            AllowInWaterConfig.defaultConfig(),
            Collections.emptySet(),
            true);
    Emotes emotes =
        new Emotes(
            new CrawlConfig(true, false),
            new SeatEmoteConfig(true, 0.0d, -0.45d, 0.0d, 0.0f),
            new SeatEmoteConfig(true, 0.0d, 0.0d, 0.0d, 0.0f),
            new SeatEmoteConfig(true, 0.0d, -0.90d, 0.0d, 90.0f),
            new SeatEmoteConfig(true, 0.0d, -0.92d, 0.0d, -90.0f));
    Permissions permissions =
        new Permissions(
            "minemotes.crawl",
            "minemotes.sit",
            "minemotes.chair",
            "minemotes.lay",
            "minemotes.belly",
            "minemotes.admin",
            Map.of(
                EmoteType.CRAWL, 0,
                EmoteType.SIT, 0,
                EmoteType.CHAIR, 0,
                EmoteType.LAY, 0,
                EmoteType.BELLY, 0,
                EmoteType.ADMIN, 2));
    return new Config(core, emotes, permissions);
  }

  public Core core() {
    return core;
  }

  public Emotes emotes() {
    return emotes;
  }

  public Permissions permissions() {
    return permissions;
  }

  public Config withCore(Core newCore) {
    return new Config(newCore, emotes, permissions);
  }

  public Config withEmotes(Emotes newEmotes) {
    return new Config(core, newEmotes, permissions);
  }

  public Config withPermissions(Permissions newPerms) {
    return new Config(core, emotes, newPerms);
  }

  public enum ToggleSetting {
    ENABLED,
    DISABLED;

    public boolean toBoolean() {
      return this == ENABLED;
    }

    public static ToggleSetting of(boolean enabled) {
      return enabled ? ENABLED : DISABLED;
    }
  }

  public static final class Core {
    private final boolean enabled;
    private final Duration cooldown;
    private final CancelConfig cancelOnDamage;
    private final CancelConfig cancelOnMove;
    private final AllowInWaterConfig allowInWater;
    private final Set<String> disableInWorlds;
    private final boolean cancelOnDanger;

    public Core(
        boolean enabled,
        Duration cooldown,
        CancelConfig cancelOnDamage,
        CancelConfig cancelOnMove,
        AllowInWaterConfig allowInWater,
        Set<String> disableInWorlds,
        boolean cancelOnDanger) {
      this.enabled = enabled;
      this.cooldown = Objects.requireNonNull(cooldown, "cooldown");
      this.cancelOnDamage = Objects.requireNonNull(cancelOnDamage, "cancelOnDamage");
      this.cancelOnMove = Objects.requireNonNull(cancelOnMove, "cancelOnMove");
      this.allowInWater = Objects.requireNonNull(allowInWater, "allowInWater");
      this.disableInWorlds = Collections.unmodifiableSet(ImmutableSet.copyOf(disableInWorlds));
      this.cancelOnDanger = cancelOnDanger;
    }

    public boolean enabled() {
      return enabled;
    }

    public Duration cooldown() {
      return cooldown;
    }

    public CancelConfig cancelOnDamage() {
      return cancelOnDamage;
    }

    public CancelConfig cancelOnMove() {
      return cancelOnMove;
    }

    public AllowInWaterConfig allowInWater() {
      return allowInWater;
    }

    public Set<String> disableInWorlds() {
      return disableInWorlds;
    }

    public boolean cancelOnDanger() {
      return cancelOnDanger;
    }

    public Core withEnabled(boolean value) {
      return new Core(
          value, cooldown, cancelOnDamage, cancelOnMove, allowInWater, disableInWorlds, cancelOnDanger);
    }

    public Core withCooldown(Duration value) {
      return new Core(
          enabled, value, cancelOnDamage, cancelOnMove, allowInWater, disableInWorlds, cancelOnDanger);
    }

    public Core withCancelOnDamage(CancelConfig value) {
      return new Core(enabled, cooldown, value, cancelOnMove, allowInWater, disableInWorlds, cancelOnDanger);
    }

    public Core withCancelOnMove(CancelConfig value) {
      return new Core(enabled, cooldown, cancelOnDamage, value, allowInWater, disableInWorlds, cancelOnDanger);
    }

    public Core withAllowInWater(AllowInWaterConfig value) {
      return new Core(enabled, cooldown, cancelOnDamage, cancelOnMove, value, disableInWorlds, cancelOnDanger);
    }

    public Core withCancelOnDanger(boolean value) {
      return new Core(enabled, cooldown, cancelOnDamage, cancelOnMove, allowInWater, disableInWorlds, value);
    }

    public Core withDisableInWorlds(Set<String> worlds) {
      return new Core(enabled, cooldown, cancelOnDamage, cancelOnMove, allowInWater, worlds, cancelOnDanger);
    }
  }

  public static final class CancelConfig {
    private final Map<EmoteType, Boolean> state;

    public CancelConfig(Map<EmoteType, Boolean> state) {
      EnumMap<EmoteType, Boolean> copy = new EnumMap<>(EmoteType.class);
      for (EmoteType type : EmoteType.values()) {
        if (type == EmoteType.ADMIN) {
          continue;
        }
        copy.put(type, state.getOrDefault(type, Boolean.FALSE));
      }
      this.state = Collections.unmodifiableMap(copy);
    }

    public boolean enabledFor(EmoteType type) {
      return state.getOrDefault(type, Boolean.FALSE);
    }

    public Map<EmoteType, Boolean> asMap() {
      return state;
    }

    public static CancelConfig defaultCancelOnDamage() {
      return new CancelConfig(
          Map.of(
              EmoteType.SIT, true,
              EmoteType.CHAIR, true,
              EmoteType.LAY, true,
              EmoteType.BELLY, true,
              EmoteType.CRAWL, false));
    }

    public static CancelConfig defaultCancelOnMove() {
      return new CancelConfig(
          Map.of(
              EmoteType.SIT, true,
              EmoteType.CHAIR, true,
              EmoteType.LAY, false,
              EmoteType.BELLY, false,
              EmoteType.CRAWL, false));
    }
  }

  public static final class AllowInWaterConfig {
    private final Map<EmoteType, Boolean> state;

    public AllowInWaterConfig(Map<EmoteType, Boolean> state) {
      EnumMap<EmoteType, Boolean> copy = new EnumMap<>(EmoteType.class);
      copy.put(EmoteType.SIT, state.getOrDefault(EmoteType.SIT, Boolean.FALSE));
      copy.put(EmoteType.CHAIR, state.getOrDefault(EmoteType.CHAIR, Boolean.FALSE));
      copy.put(EmoteType.LAY, state.getOrDefault(EmoteType.LAY, Boolean.FALSE));
      copy.put(EmoteType.BELLY, state.getOrDefault(EmoteType.BELLY, Boolean.FALSE));
      this.state = Collections.unmodifiableMap(copy);
    }

    public boolean allow(EmoteType type) {
      return state.getOrDefault(type, Boolean.FALSE);
    }

    public Map<EmoteType, Boolean> asMap() {
      return state;
    }

    public static AllowInWaterConfig defaultConfig() {
      return new AllowInWaterConfig(
          Map.of(
              EmoteType.SIT,
              false,
              EmoteType.CHAIR,
              false,
              EmoteType.LAY,
              false,
              EmoteType.BELLY,
              false));
    }
  }

  public static final class Emotes {
    private final CrawlConfig crawl;
    private final SeatEmoteConfig sit;
    private final SeatEmoteConfig chair;
    private final SeatEmoteConfig lay;
    private final SeatEmoteConfig belly;

    public Emotes(
        CrawlConfig crawl,
        SeatEmoteConfig sit,
        SeatEmoteConfig chair,
        SeatEmoteConfig lay,
        SeatEmoteConfig belly) {
      this.crawl = Objects.requireNonNull(crawl, "crawl");
      this.sit = Objects.requireNonNull(sit, "sit");
      this.chair = Objects.requireNonNull(chair, "chair");
      this.lay = Objects.requireNonNull(lay, "lay");
      this.belly = Objects.requireNonNull(belly, "belly");
    }

    public CrawlConfig crawl() {
      return crawl;
    }

    public SeatEmoteConfig sit() {
      return sit;
    }

    public SeatEmoteConfig chair() {
      return chair;
    }

    public SeatEmoteConfig lay() {
      return lay;
    }

    public SeatEmoteConfig belly() {
      return belly;
    }
  }

  public static final class CrawlConfig {
    private final boolean enabled;
    private final boolean waterOnly;

    public CrawlConfig(boolean enabled, boolean waterOnly) {
      this.enabled = enabled;
      this.waterOnly = waterOnly;
    }

    public boolean enabled() {
      return enabled;
    }

    public boolean waterOnly() {
      return waterOnly;
    }
  }

  public static final class SeatEmoteConfig {
    private final boolean enabled;
    private final double offsetX;
    private final double offsetY;
    private final double offsetZ;
    private final float targetPitch;

    public SeatEmoteConfig(boolean enabled, double offsetX, double offsetY, double offsetZ, float targetPitch) {
      this.enabled = enabled;
      this.offsetX = offsetX;
      this.offsetY = offsetY;
      this.offsetZ = offsetZ;
      this.targetPitch = targetPitch;
    }

    public boolean enabled() {
      return enabled;
    }

    public double offsetX() {
      return offsetX;
    }

    public double offsetY() {
      return offsetY;
    }

    public double offsetZ() {
      return offsetZ;
    }

    public float targetPitch() {
      return targetPitch;
    }
  }

  public static final class Permissions {
    private final String crawl;
    private final String sit;
    private final String chair;
    private final String lay;
    private final String belly;
    private final String admin;
    private final Map<EmoteType, Integer> fallbackLevels;

    public Permissions(
        String crawl,
        String sit,
        String chair,
        String lay,
        String belly,
        String admin,
        Map<EmoteType, Integer> fallbackLevels) {
      this.crawl = Objects.requireNonNull(crawl, "crawl");
      this.sit = Objects.requireNonNull(sit, "sit");
      this.chair = Objects.requireNonNull(chair, "chair");
      this.lay = Objects.requireNonNull(lay, "lay");
      this.belly = Objects.requireNonNull(belly, "belly");
      this.admin = Objects.requireNonNull(admin, "admin");
      EnumMap<EmoteType, Integer> copy = new EnumMap<>(EmoteType.class);
      copy.putAll(fallbackLevels);
      if (!copy.containsKey(EmoteType.ADMIN)) {
        copy.put(EmoteType.ADMIN, 2);
      }
      this.fallbackLevels = Collections.unmodifiableMap(copy);
    }

    public String crawl() {
      return crawl;
    }

    public String sit() {
      return sit;
    }

    public String chair() {
      return chair;
    }

    public String lay() {
      return lay;
    }

    public String belly() {
      return belly;
    }

    public String admin() {
      return admin;
    }

    public int fallbackLevel(EmoteType type) {
      return fallbackLevels.getOrDefault(type, 0);
    }
  }

  public List<String> validationErrors() {
    ConfigValidator validator = new ConfigValidator(this);
    return validator.validate();
  }
}
