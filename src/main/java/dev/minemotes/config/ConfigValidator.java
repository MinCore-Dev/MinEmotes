package dev.minemotes.config;

import dev.minemotes.core.EmoteType;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Validates {@link Config} bounds and emits human-readable error messages. */
final class ConfigValidator {
  private final Config config;

  ConfigValidator(Config config) {
    this.config = config;
  }

  List<String> validate() {
    List<String> errors = new ArrayList<>();
    validateDuration(config.core().cooldown(), "core.cooldownS", 0.0, 60.0, errors);

    for (Map.Entry<EmoteType, Boolean> entry : config.core().cancelOnDamage().asMap().entrySet()) {
      if (entry.getKey() == EmoteType.ADMIN) continue;
      if (entry.getValue() == null) {
        errors.add("core.cancelOnDamage." + entry.getKey().id() + " must be true or false");
      }
    }

    for (Map.Entry<EmoteType, Boolean> entry : config.core().cancelOnMove().asMap().entrySet()) {
      if (entry.getKey() == EmoteType.ADMIN) continue;
      if (entry.getValue() == null) {
        errors.add("core.cancelOnMove." + entry.getKey().id() + " must be true or false");
      }
    }

    Config.SeatEmoteConfig sit = config.emotes().sit();
    validateOffset("emotes.sit.offsetY", sit.offsetY(), -4.0, 4.0, errors);

    Config.SeatEmoteConfig chair = config.emotes().chair();
    validateOffset("emotes.chair.offsetY", chair.offsetY(), -4.0, 4.0, errors);

    Config.SeatEmoteConfig lay = config.emotes().lay();
    validateOffset("emotes.lay.offsetY", lay.offsetY(), -4.0, 4.0, errors);
    validatePitch("emotes.lay.pitchDegrees", lay.targetPitch(), errors);

    Config.SeatEmoteConfig belly = config.emotes().belly();
    validateOffset("emotes.belly.offsetY", belly.offsetY(), -4.0, 4.0, errors);
    validatePitch("emotes.belly.pitchDegrees", belly.targetPitch(), errors);

    return errors;
  }

  private static void validateDuration(
      Duration duration, String key, double minSeconds, double maxSeconds, List<String> errors) {
    double seconds = duration.toMillis() / 1000.0d;
    if (Double.isNaN(seconds) || seconds < minSeconds || seconds > maxSeconds) {
      errors.add(key + " must be between " + minSeconds + " and " + maxSeconds + " seconds");
    }
  }

  private static void validateOffset(String key, double value, double min, double max, List<String> errors) {
    if (Double.isNaN(value) || value < min || value > max) {
      errors.add(key + " must be between " + min + " and " + max);
    }
  }

  private static void validatePitch(String key, float value, List<String> errors) {
    if (Float.isNaN(value) || value < -90.0f || value > 90.0f) {
      errors.add(key + " must be within [-90, 90]");
    }
  }
}
