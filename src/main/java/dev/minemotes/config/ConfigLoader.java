package dev.minemotes.config;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;
import blue.endless.jankson.api.SyntaxError;
import dev.minemotes.core.EmoteType;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Loads {@link Config} from disk using JSON5 (Jankson). */
public final class ConfigLoader {
  private static final Logger LOGGER = LogManager.getLogger("minemotes");
  private static final Jankson JANKSON = Jankson.builder().build();
  private static final String FILE_NAME = "minemotes.json5";

  private ConfigLoader() {}

  public static Path defaultPath() {
    return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
  }

  public static Config loadOrCreate() {
    return loadOrCreate(defaultPath());
  }

  public static Config loadOrCreate(Path path) {
    try {
      if (Files.notExists(path)) {
        Config defaults = Config.defaults();
        save(path, defaults);
        return defaults;
      }
      return load(path);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to load config " + path, e);
    }
  }

  public static Config load(Path path) throws IOException {
    try {
      JsonObject json = JANKSON.load(path.toFile());
      Config config = fromJson(json, Config.defaults());
      List<String> errors = config.validationErrors();
      if (!errors.isEmpty()) {
        for (String error : errors) {
          LOGGER.warn("(minemotes) config validation: {}", error);
        }
      }
      return config;
    } catch (SyntaxError error) {
      throw new IOException("Invalid JSON5 in " + path + ": " + error.getCompleteMessage(), error);
    }
  }

  public static void save(Path path, Config config) throws IOException {
    Files.createDirectories(path.getParent());
    JsonObject json = toJson(config);
    String serialized = json.toJson(true, true);
    Files.writeString(path, serialized, StandardCharsets.UTF_8);
  }

  private static Config fromJson(JsonObject json, Config defaults) {
    Config.Core core = parseCore(json.getObject("core"), defaults.core());
    Config.Emotes emotes = parseEmotes(json.getObject("emotes"), defaults.emotes());
    Config.Permissions perms = parsePermissions(json.getObject("permissions"), defaults.permissions());
    return new Config(core, emotes, perms);
  }

  private static Config.Core parseCore(JsonObject json, Config.Core defaults) {
    if (json == null) {
      return defaults;
    }
    boolean enabled = bool(json, "enabled", defaults.enabled());
    double cooldownSeconds = number(json, "cooldownS", defaults.cooldown().toMillis() / 1000.0d);
    Config.CancelConfig cancelOnDamage =
        parseCancel(json.getObject("cancelOnDamage"), defaults.cancelOnDamage());
    Config.CancelConfig cancelOnMove = parseCancel(json.getObject("cancelOnMove"), defaults.cancelOnMove());
    Config.AllowInWaterConfig allowInWater =
        parseAllowInWater(json.getObject("allowInWater"), defaults.allowInWater());
    Set<String> disableInWorlds = defaults.disableInWorlds();
    var disableElement = json.get("disableInWorlds");
    if (disableElement instanceof JsonArray array) {
      disableInWorlds =
          array.stream()
              .map(
                  value ->
                      value instanceof JsonPrimitive primitive ? primitive.asString() : null)
              .filter(Objects::nonNull)
              .collect(Collectors.toCollection(LinkedHashSet::new));
    }
    boolean cancelOnDanger = bool(json, "cancelOnDanger", defaults.cancelOnDanger());
    return new Config.Core(
        enabled,
        Duration.ofMillis((long) (cooldownSeconds * 1000.0d)),
        cancelOnDamage,
        cancelOnMove,
        allowInWater,
        disableInWorlds,
        cancelOnDanger);
  }

  private static Config.CancelConfig parseCancel(JsonObject json, Config.CancelConfig defaults) {
    if (json == null) {
      return defaults;
    }
    Map<EmoteType, Boolean> map = new LinkedHashMap<>();
    for (EmoteType type : EmoteType.values()) {
      if (type == EmoteType.ADMIN) continue;
      map.put(type, bool(json, type.id(), defaults.enabledFor(type)));
    }
    return new Config.CancelConfig(map);
  }

  private static Config.AllowInWaterConfig parseAllowInWater(
      JsonObject json, Config.AllowInWaterConfig defaults) {
    if (json == null) {
      return defaults;
    }
    Map<EmoteType, Boolean> map = new LinkedHashMap<>();
    map.put(EmoteType.SIT, bool(json, "sit", defaults.allow(EmoteType.SIT)));
    map.put(EmoteType.CHAIR, bool(json, "chair", defaults.allow(EmoteType.CHAIR)));
    map.put(EmoteType.LAY, bool(json, "lay", defaults.allow(EmoteType.LAY)));
    map.put(EmoteType.BELLY, bool(json, "belly", defaults.allow(EmoteType.BELLY)));
    return new Config.AllowInWaterConfig(map);
  }

  private static Config.Emotes parseEmotes(JsonObject json, Config.Emotes defaults) {
    if (json == null) {
      return defaults;
    }
    Config.CrawlConfig crawl = parseCrawl(json.getObject("crawl"), defaults.crawl());
    Config.SeatEmoteConfig sit = parseSeat(json.getObject("sit"), defaults.sit());
    Config.SeatEmoteConfig chair = parseSeat(json.getObject("chair"), defaults.chair());
    Config.SeatEmoteConfig lay = parseSeat(json.getObject("lay"), defaults.lay());
    Config.SeatEmoteConfig belly = parseSeat(json.getObject("belly"), defaults.belly());
    return new Config.Emotes(crawl, sit, chair, lay, belly);
  }

  private static Config.CrawlConfig parseCrawl(JsonObject json, Config.CrawlConfig defaults) {
    if (json == null) {
      return defaults;
    }
    boolean enabled = bool(json, "enabled", defaults.enabled());
    boolean waterOnly = bool(json, "waterOnly", defaults.waterOnly());
    return new Config.CrawlConfig(enabled, waterOnly);
  }

  private static Config.SeatEmoteConfig parseSeat(JsonObject json, Config.SeatEmoteConfig defaults) {
    if (json == null) {
      return defaults;
    }
    boolean enabled = bool(json, "enabled", defaults.enabled());
    double offsetX = number(json, "offsetX", defaults.offsetX());
    double offsetY = number(json, "offsetY", defaults.offsetY());
    double offsetZ = number(json, "offsetZ", defaults.offsetZ());
    float pitch = (float) number(json, "pitchDegrees", defaults.targetPitch());
    return new Config.SeatEmoteConfig(enabled, offsetX, offsetY, offsetZ, pitch);
  }

  private static Config.Permissions parsePermissions(JsonObject json, Config.Permissions defaults) {
    if (json == null) {
      return defaults;
    }
    String crawl = string(json, "crawl", defaults.crawl());
    String sit = string(json, "sit", defaults.sit());
    String chair = string(json, "chair", defaults.chair());
    String lay = string(json, "lay", defaults.lay());
    String belly = string(json, "belly", defaults.belly());
    String admin = string(json, "admin", defaults.admin());
    Map<EmoteType, Integer> fallback = new LinkedHashMap<>();
    JsonObject levels = json.getObject("fallbackOpLevels");
    if (levels != null) {
      for (EmoteType type : EmoteType.values()) {
        if (!levels.containsKey(type.id())) continue;
        fallback.put(type, levels.getInt(type.id(), defaults.fallbackLevel(type)));
      }
    }
    return new Config.Permissions(crawl, sit, chair, lay, belly, admin, fallback);
  }

  private static JsonObject toJson(Config config) {
    JsonObject root = new JsonObject();
    root.put("core", coreToJson(config.core()));
    root.put("emotes", emotesToJson(config.emotes()));
    root.put("permissions", permissionsToJson(config.permissions()));
    return root;
  }

  private static JsonObject coreToJson(Config.Core core) {
    JsonObject json = new JsonObject();
    json.put("enabled", new JsonPrimitive(core.enabled()));
    json.put("cooldownS", new JsonPrimitive(core.cooldown().toMillis() / 1000.0d));
    json.put("cancelOnDamage", cancelToJson(core.cancelOnDamage()));
    json.put("cancelOnMove", cancelToJson(core.cancelOnMove()));
    json.put("allowInWater", allowToJson(core.allowInWater()));
    JsonArray worlds = new JsonArray();
    for (String world : core.disableInWorlds()) {
      worlds.add(new JsonPrimitive(world));
    }
    json.put("disableInWorlds", worlds);
    json.put("cancelOnDanger", new JsonPrimitive(core.cancelOnDanger()));
    return json;
  }

  private static JsonObject cancelToJson(Config.CancelConfig cancel) {
    JsonObject json = new JsonObject();
    for (Map.Entry<EmoteType, Boolean> entry : cancel.asMap().entrySet()) {
      json.put(entry.getKey().id(), new JsonPrimitive(entry.getValue()));
    }
    return json;
  }

  private static JsonObject allowToJson(Config.AllowInWaterConfig allow) {
    JsonObject json = new JsonObject();
    for (Map.Entry<EmoteType, Boolean> entry : allow.asMap().entrySet()) {
      json.put(entry.getKey().id(), new JsonPrimitive(entry.getValue()));
    }
    return json;
  }

  private static JsonObject emotesToJson(Config.Emotes emotes) {
    JsonObject json = new JsonObject();
    json.put("crawl", crawlToJson(emotes.crawl()));
    json.put("sit", seatToJson(emotes.sit()));
    json.put("chair", seatToJson(emotes.chair()));
    json.put("lay", seatToJson(emotes.lay()));
    json.put("belly", seatToJson(emotes.belly()));
    return json;
  }

  private static JsonObject crawlToJson(Config.CrawlConfig crawl) {
    JsonObject json = new JsonObject();
    json.put("enabled", new JsonPrimitive(crawl.enabled()));
    json.put("waterOnly", new JsonPrimitive(crawl.waterOnly()));
    return json;
  }

  private static JsonObject seatToJson(Config.SeatEmoteConfig seat) {
    JsonObject json = new JsonObject();
    json.put("enabled", new JsonPrimitive(seat.enabled()));
    json.put("offsetX", new JsonPrimitive(seat.offsetX()));
    json.put("offsetY", new JsonPrimitive(seat.offsetY()));
    json.put("offsetZ", new JsonPrimitive(seat.offsetZ()));
    json.put("pitchDegrees", new JsonPrimitive(seat.targetPitch()));
    return json;
  }

  private static JsonObject permissionsToJson(Config.Permissions perms) {
    JsonObject json = new JsonObject();
    json.put("crawl", new JsonPrimitive(perms.crawl()));
    json.put("sit", new JsonPrimitive(perms.sit()));
    json.put("chair", new JsonPrimitive(perms.chair()));
    json.put("lay", new JsonPrimitive(perms.lay()));
    json.put("belly", new JsonPrimitive(perms.belly()));
    json.put("admin", new JsonPrimitive(perms.admin()));
    JsonObject fallback = new JsonObject();
    for (EmoteType type : EmoteType.values()) {
      int level = perms.fallbackLevel(type);
      fallback.put(type.id(), new JsonPrimitive(level));
    }
    json.put("fallbackOpLevels", fallback);
    return json;
  }

  private static boolean bool(JsonObject json, String key, boolean defaultValue) {
    if (json == null || !json.containsKey(key)) {
      return defaultValue;
    }
    JsonElement element = json.get(key);
    if (element instanceof JsonPrimitive primitive) {
      return primitive.asBoolean(defaultValue);
    }
    return defaultValue;
  }

  private static double number(JsonObject json, String key, double defaultValue) {
    if (json == null || !json.containsKey(key)) {
      return defaultValue;
    }
    JsonElement element = json.get(key);
    if (element instanceof JsonPrimitive primitive) {
      try {
        return primitive.asDouble(defaultValue);
      } catch (NumberFormatException ignored) {
      }
    }
    return defaultValue;
  }

  private static String string(JsonObject json, String key, String defaultValue) {
    if (json == null || !json.containsKey(key)) {
      return defaultValue;
    }
    JsonElement element = json.get(key);
    if (element instanceof JsonPrimitive primitive) {
      String value = primitive.asString();
      return value != null ? value : defaultValue;
    }
    return defaultValue;
  }
}
