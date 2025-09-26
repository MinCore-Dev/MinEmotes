package dev.minemotes.core;

import net.minecraft.text.Text;

/** Types of emotes supported by MinEmotes. */
public enum EmoteType {
  CRAWL("crawl"),
  SIT("sit"),
  CHAIR("chair"),
  LAY("lay"),
  BELLY("belly"),
  ADMIN("admin");

  private final String id;

  EmoteType(String id) {
    this.id = id;
  }

  public String id() {
    return id;
  }

  public Text startMessage() {
    return switch (this) {
      case CRAWL -> Text.translatable("minemotes.crawl.on");
      case SIT -> Text.translatable("minemotes.sit.start");
      case CHAIR -> Text.translatable("minemotes.chair.start");
      case LAY -> Text.translatable("minemotes.lay.start");
      case BELLY -> Text.translatable("minemotes.belly.start");
      case ADMIN -> Text.empty();
    };
  }

  public Text stopMessage() {
    return switch (this) {
      case CRAWL -> Text.translatable("minemotes.crawl.off");
      case SIT -> Text.translatable("minemotes.sit.stop");
      case CHAIR -> Text.translatable("minemotes.chair.stop");
      case LAY -> Text.translatable("minemotes.lay.stop");
      case BELLY -> Text.translatable("minemotes.belly.stop");
      case ADMIN -> Text.empty();
    };
  }
}
