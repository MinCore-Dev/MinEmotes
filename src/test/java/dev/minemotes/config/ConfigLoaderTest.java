package dev.minemotes.config;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ConfigLoaderTest {
  @Test
  void loadOrCreateWritesDefaults() throws IOException {
    Path dir = Files.createTempDirectory("minemotes-config");
    Path path = dir.resolve("minemotes.json5");

    Config config = ConfigLoader.loadOrCreate(path);

    assertTrue(Files.exists(path));
    assertTrue(config.core().enabled());
    assertTrue(config.emotes().sit().enabled());
    assertTrue(config.emotes().chair().enabled());
  }

  @Test
  void loadReadsOverrides() throws IOException {
    Path dir = Files.createTempDirectory("minemotes-config");
    Path path = dir.resolve("minemotes.json5");
    Files.writeString(
        path,
        "{" +
            "core:{enabled:true,cooldownS:5}," +
            "emotes:{sit:{enabled:false}}," +
            "permissions:{admin:\"test.admin\"}" +
            "}");

    Config config = ConfigLoader.load(path);

    assertEquals(5.0, config.core().cooldown().toSeconds());
    assertFalse(config.emotes().sit().enabled());
    assertEquals("test.admin", config.permissions().admin());
  }
}
