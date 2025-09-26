package dev.minemotes.config;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Loads configuration on start and watches for changes. Reloads are processed on a dedicated
 * executor to avoid blocking the server main thread.
 */
public final class ConfigManager implements AutoCloseable {
  private static final Logger LOGGER = LogManager.getLogger("minemotes");
  private static final Duration RELOAD_DEBOUNCE = Duration.ofMillis(200);

  private final Path path;
  private final ExecutorService executor;
  private final List<Consumer<Config>> listeners = new CopyOnWriteArrayList<>();

  private volatile Config current;
  private WatchService watchService;
  private WatchKey watchKey;

  public ConfigManager(Path path) {
    this.path = Objects.requireNonNull(path, "path");
    this.executor = Executors.newSingleThreadExecutor(r -> {
      Thread thread = new Thread(r, "MinEmotes-Config");
      thread.setDaemon(true);
      return thread;
    });
  }

  public void start() {
    this.current = ConfigLoader.loadOrCreate(path);
    try {
      this.watchService = FileSystems.getDefault().newWatchService();
      Path dir = path.getParent();
      if (dir != null) {
        Files.createDirectories(dir);
        this.watchKey =
            dir.register(
                watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_DELETE);
        executor.execute(this::watchLoop);
      }
    } catch (IOException e) {
      LOGGER.warn("(minemotes) Unable to start config watcher", e);
    }
  }

  public Config current() {
    return current;
  }

  public void addListener(Consumer<Config> listener) {
    listeners.add(listener);
    listener.accept(current);
  }

  public synchronized void reloadNow() {
    try {
      Config next = ConfigLoader.loadOrCreate(path);
      this.current = next;
      notifyListeners(next);
    } catch (RuntimeException ex) {
      LOGGER.error("(minemotes) Reload failed", ex);
      throw ex;
    }
  }

  private void watchLoop() {
    long lastReload = 0L;
    while (!Thread.currentThread().isInterrupted()) {
      try {
        WatchKey key = watchService.take();
        boolean relevant = false;
        for (WatchEvent<?> event : key.pollEvents()) {
          Path changed = (Path) event.context();
          if (changed != null && changed.getFileName().equals(path.getFileName())) {
            relevant = true;
          }
        }
        if (relevant) {
          long now = System.nanoTime();
          if (now - lastReload >= RELOAD_DEBOUNCE.toNanos()) {
            lastReload = now;
            safeReload();
          }
        }
        boolean valid = key.reset();
        if (!valid) {
          break;
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
    }
  }

  private void safeReload() {
    try {
      Config next = ConfigLoader.loadOrCreate(path);
      this.current = next;
      notifyListeners(next);
    } catch (RuntimeException ex) {
      LOGGER.error("(minemotes) Hot reload failed", ex);
    }
  }

  private void notifyListeners(Config config) {
    for (Consumer<Config> listener : listeners) {
      try {
        listener.accept(config);
      } catch (Throwable t) {
        LOGGER.warn("(minemotes) listener threw", t);
      }
    }
  }

  @Override
  public void close() {
    if (watchKey != null) {
      watchKey.cancel();
    }
    if (watchService != null) {
      try {
        watchService.close();
      } catch (IOException ignored) {
      }
    }
    executor.shutdownNow();
    try {
      executor.awaitTermination(500, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
