// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.adapter;

import io.github.danielxxomg.anvillink.domain.ports.ConfigurationPort;
import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * File-backed ConfigurationPort. AtomicReference swap only after validation. Invalid reload retains
 * prior; invalid startup disables activation. Parses YAML directly (no Bukkit YamlConfiguration) to
 * avoid SnakeYAML version skew between Paper 1.18.2 (1.30) and test pin (2.2).
 */
public final class FileConfigurationPort implements ConfigurationPort {

  private final File file;
  private final AtomicReference<ConfigSnapshot> ref;

  public FileConfigurationPort(File file) {
    this.file = file;
    ConfigSnapshot initial = tryLoad();
    if (initial == null) {
      initial = new ConfigSnapshot(BigDecimal.ZERO, 8, Collections.emptyMap(), false);
    }
    this.ref = new AtomicReference<>(initial);
  }

  @Override
  public ConfigSnapshot current() {
    return ref.get();
  }

  @Override
  public ReloadOutcome reload() {
    ConfigSnapshot prior = ref.get();
    SnapshotOrError parsed = parseFile();
    if (parsed.error != null) {
      return new ReloadOutcome.Failure(parsed.error, prior);
    }
    ref.set(parsed.snapshot);
    return new ReloadOutcome.Success(parsed.snapshot);
  }

  private ConfigSnapshot tryLoad() {
    return parseFile().snapshot;
  }

  private SnapshotOrError parseFile() {
    if (!file.exists() || !file.isFile()) {
      return err("missing file: " + file.getPath());
    }
    String content;
    try {
      content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
    } catch (Exception e) {
      return err("cannot read: " + e.getMessage());
    }

    String priceRaw = null;
    String distanceRaw = null;
    Map<String, String> messages = new HashMap<>();
    boolean inMessages = false;
    boolean inAdmin = false;

    for (String rawLine : content.split("\n", -1)) {
      String trimmed = rawLine.trim();
      if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
      boolean topLevel = !rawLine.startsWith(" ") && !rawLine.startsWith("\t");
      if (topLevel) {
        inMessages = false;
        inAdmin = false;
        if (trimmed.startsWith("price:")) {
          priceRaw = stripQuotes(trimmed.substring("price:".length()).trim());
          if (priceRaw.isEmpty()) priceRaw = null;
        } else if (trimmed.startsWith("messages:")) {
          inMessages = true;
        } else if (trimmed.equals("admin:")) {
          inAdmin = true;
        } else if (trimmed.startsWith("admin.target-distance:")) {
          distanceRaw = stripQuotes(trimmed.substring("admin.target-distance:".length()).trim());
        }
      } else {
        if (inMessages) {
          int colon = trimmed.indexOf(':');
          if (colon > 0) {
            String k = trimmed.substring(0, colon).trim();
            String v = stripQuotes(trimmed.substring(colon + 1).trim());
            if (!k.isEmpty()) messages.put(k, v);
          }
        } else if (inAdmin) {
          if (trimmed.startsWith("target-distance:")) {
            distanceRaw = stripQuotes(trimmed.substring("target-distance:".length()).trim());
          }
        }
      }
    }

    if (priceRaw == null) return err("missing price");
    BigDecimal price;
    try {
      price = new BigDecimal(priceRaw);
    } catch (Exception e) {
      return err("invalid price: " + priceRaw);
    }
    if (price.signum() < 0) return err("negative price: " + price);

    int distance = 8;
    if (distanceRaw != null) {
      try {
        distance = Integer.parseInt(distanceRaw);
      } catch (NumberFormatException e) {
        return err("invalid target-distance: " + distanceRaw);
      }
    }
    if (distance < 1 || distance > 32) return err("target-distance out of range 1-32: " + distance);

    ConfigSnapshot snap =
        new ConfigSnapshot(price, distance, Collections.unmodifiableMap(messages), true);
    return new SnapshotOrError(snap, null);
  }

  private static String stripQuotes(String s) {
    if (s.length() >= 2
        && ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'")))) {
      return s.substring(1, s.length() - 1);
    }
    return s;
  }

  private static SnapshotOrError err(String reason) {
    return new SnapshotOrError(null, reason);
  }

  private static final class SnapshotOrError {
    final ConfigSnapshot snapshot;
    final String error;

    SnapshotOrError(ConfigSnapshot s, String e) {
      this.snapshot = s;
      this.error = e;
    }
  }
}
