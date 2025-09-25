package dev.minemotes.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigLoader {
  private static final Path LIVE = Path.of("config/minemotes.json5");
  private static final Path EXAMPLE = Path.of("config/minemotes.json5.example");

  public static Config loadOrCreate() {
    try {
      if (!Files.exists(EXAMPLE)) {
        Files.createDirectories(EXAMPLE.getParent());
        String ex = "core: {\n  enabled: true,\n  cooldownS: 2,\n  cancelOnDamage: { sit: true, lay: true, belly: true, crawl: false },\n  cancelOnMove:   { sit: true, lay: false, belly: false, crawl: false },\n  allowInWater:   { sit: false, lay: false, belly: false },\n  disableInWorlds: []\n}\nemotes: {\n  crawl: { enabled: true },\n  sit:   { enabled: true, offsetY: -0.45 },\n  lay:   { enabled: true, offsetY: -0.90, pitchDegrees: 90 },\n  belly: { enabled: true, offsetY: -0.92, pitchDegrees: -90 }\n}\npermissions: {\n  crawl: \"minemotes.crawl\",\n  sit:   \"minemotes.sit\",\n  lay:   \"minemotes.lay\",\n  belly: \"minemotes.belly\",\n  admin: \"minemotes.admin\"\n}\n";
        Files.writeString(EXAMPLE, ex, StandardCharsets.UTF_8);
      }
      // Minimal loader: return defaults. Codex to replace with JSON5 parser.
      return new Config();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
