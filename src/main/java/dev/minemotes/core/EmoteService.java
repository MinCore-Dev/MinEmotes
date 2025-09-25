package dev.minemotes.core;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class EmoteService {
  private final Config cfg;
  private final SeatManager seats;
  private final Set<UUID> crawl = new HashSet<>();

  public EmoteService(Config cfg, SeatManager seats) { this.cfg = cfg; this.seats = seats; }

  // TODO: Implement actual server integration
  public boolean toggleCrawl(UUID playerId, boolean enable) { return true; }
  public boolean sit(UUID playerId) { return true; }
  public boolean lay(UUID playerId) { return true; }
  public boolean belly(UUID playerId) { return true; }
  public void stop(UUID playerId) { }
}
