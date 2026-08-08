// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.adapter;

import io.github.danielxxomg.anvillink.domain.MoneyAmount;
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
      initial =
          new ConfigSnapshot(
              BigDecimal.ZERO,
              BigDecimal.ZERO,
              8,
              Collections.emptyMap(),
              false,
              true,
              "BLOCK_ANVIL_USE",
              "CRIT");
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

    String priceHandRaw = null;
    String priceAllRaw = null;
    boolean priceHeaderSeen = false;
    boolean priceScalarPresent = false;
    boolean inPrice = false;
    String distanceRaw = null;
    Map<String, String> messages = new HashMap<>();
    boolean inMessages = false;
    boolean inAdmin = false;
    boolean inFeedback = false;
    String feedbackEnabledRaw = null;
    String feedbackSoundRaw = null;
    String feedbackParticlesRaw = null;

    for (String rawLine : content.split("\n", -1)) {
      String trimmed = rawLine.trim();
      if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
      boolean topLevel = !rawLine.startsWith(" ") && !rawLine.startsWith("\t");
      if (topLevel) {
        inMessages = false;
        inAdmin = false;
        inPrice = false;
        inFeedback = false;
        if (trimmed.startsWith("price:")) {
          String after = stripQuotes(trimmed.substring("price:".length()).trim());
          if (!after.isEmpty()) {
            priceScalarPresent = true;
          } else {
            priceHeaderSeen = true;
            inPrice = true;
          }
        } else if (trimmed.startsWith("messages:")) {
          inMessages = true;
        } else if (trimmed.startsWith("feedback:")) {
          String after = stripQuotes(trimmed.substring("feedback:".length()).trim());
          if (after.isEmpty()) {
            inFeedback = true;
          }
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
        } else if (inPrice) {
          if (trimmed.startsWith("hand:")) {
            priceHandRaw = stripQuotes(trimmed.substring("hand:".length()).trim());
            if (priceHandRaw != null && priceHandRaw.isEmpty()) priceHandRaw = null;
          } else if (trimmed.startsWith("all:")) {
            priceAllRaw = stripQuotes(trimmed.substring("all:".length()).trim());
            if (priceAllRaw != null && priceAllRaw.isEmpty()) priceAllRaw = null;
          }
        } else if (inFeedback) {
          if (trimmed.startsWith("enabled:")) {
            feedbackEnabledRaw = stripQuotes(trimmed.substring("enabled:".length()).trim());
          } else if (trimmed.startsWith("sound:")) {
            feedbackSoundRaw = stripQuotes(trimmed.substring("sound:".length()).trim());
          } else if (trimmed.startsWith("particles:")) {
            feedbackParticlesRaw = stripQuotes(trimmed.substring("particles:".length()).trim());
          }
        } else if (inAdmin) {
          if (trimmed.startsWith("target-distance:")) {
            distanceRaw = stripQuotes(trimmed.substring("target-distance:".length()).trim());
          }
        }
      }
    }

    if (priceScalarPresent) return err("missing price.hand");
    if (!priceHeaderSeen) return err("missing price.hand");
    if (priceHandRaw == null) return err("missing price.hand");
    if (priceAllRaw == null) return err("missing price.all");
    BigDecimal priceHand;
    BigDecimal priceAll;
    try {
      priceHand = new BigDecimal(priceHandRaw);
    } catch (Exception e) {
      return err("invalid price.hand: " + priceHandRaw);
    }
    try {
      priceAll = new BigDecimal(priceAllRaw);
    } catch (Exception e) {
      return err("invalid price.all: " + priceAllRaw);
    }
    if (priceHand.signum() < 0) return err("negative price.hand: " + priceHand);
    if (priceAll.signum() < 0) return err("negative price.all: " + priceAll);
    if (priceHand.compareTo(MoneyAmount.MIN_PRICE) < 0) {
      return err("price.hand below floor: " + priceHand);
    }
    if (priceAll.compareTo(MoneyAmount.MIN_PRICE) < 0) {
      return err("price.all below floor: " + priceAll);
    }
    try {
      new MoneyAmount(priceHand);
      new MoneyAmount(priceAll);
    } catch (IllegalArgumentException e) {
      return err(e.getMessage());
    }

    int distance = 8;
    if (distanceRaw != null) {
      try {
        distance = Integer.parseInt(distanceRaw);
      } catch (NumberFormatException e) {
        return err("invalid target-distance: " + distanceRaw);
      }
    }
    if (distance < 1 || distance > 32) return err("target-distance out of range 1-32: " + distance);

    boolean feedbackEnabled = true;
    if (feedbackEnabledRaw != null) {
      String v = feedbackEnabledRaw.trim().toLowerCase();
      if (v.equals("false") || v.equals("no") || v.equals("off")) feedbackEnabled = false;
      else if (v.equals("true") || v.equals("yes") || v.equals("on")) feedbackEnabled = true;
      else return err("invalid feedback.enabled: " + feedbackEnabledRaw);
    }
    String feedbackSound =
        feedbackSoundRaw != null && !feedbackSoundRaw.isEmpty()
            ? feedbackSoundRaw
            : "BLOCK_ANVIL_USE";
    String feedbackParticles =
        feedbackParticlesRaw != null && !feedbackParticlesRaw.isEmpty()
            ? feedbackParticlesRaw
            : "CRIT";

    ConfigSnapshot snap =
        new ConfigSnapshot(
            priceHand,
            priceAll,
            distance,
            Collections.unmodifiableMap(messages),
            true,
            feedbackEnabled,
            feedbackSound,
            feedbackParticles);
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
