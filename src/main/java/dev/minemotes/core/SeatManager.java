package dev.minemotes.core;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SeatManager {
  private final Map<UUID, Object> seats = new HashMap<>(); // placeholder

  public boolean hasSeat(UUID player) { return seats.containsKey(player); }
  public void assignSeat(UUID player, Object seat) { seats.put(player, seat); }
  public void clearSeat(UUID player) { seats.remove(player); }
  public void clearAll() { seats.clear(); }
}
