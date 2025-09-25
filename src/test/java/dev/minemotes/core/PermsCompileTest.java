package dev.minemotes.core;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class PermsCompileTest {
  @Test
  void gatewayClassPresent() {
    assertDoesNotThrow(() -> Class.forName("dev.minemotes.perms.Perms"));
  }
}
