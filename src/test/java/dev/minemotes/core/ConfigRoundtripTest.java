package dev.minemotes.core;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class ConfigRoundtripTest {
  @Test
  void defaultsLoad() {
    Config c = ConfigLoader.loadOrCreate();
    assertTrue(c.core.enabled);
    assertTrue(c.emotes.sit.enabled);
  }
}
