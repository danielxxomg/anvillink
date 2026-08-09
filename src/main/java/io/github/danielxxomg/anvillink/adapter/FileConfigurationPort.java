// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.adapter;

import io.github.danielxxomg.anvillink.domain.MoneyAmount;
import io.github.danielxxomg.anvillink.domain.WorldPrice;
import io.github.danielxxomg.anvillink.domain.ports.ConfigurationPort;
import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

/**
 * File-backed ConfigurationPort. AtomicReference swap only after validation. Invalid reload retains
 * prior; invalid startup disables activation. Parses YAML directly (no Bukkit YamlConfiguration) to
 * avoid SnakeYAML version skew between Paper 1.18.2 (1.30) and test pin (2.2).
 */
public final class FileConfigurationPort implements ConfigurationPort {

  private static final Logger LOG = Logger.getLogger(FileConfigurationPort.class.getName());

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
              Collections.emptyMap(),
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
    boolean inWorlds = false;
    boolean inWorld = false;
    String currentWorld = null;
    Map<String, Map<String, String>> worldsRaw = new HashMap<>();
    Set<String> worldsDupWarned = new HashSet<>();
    String distanceRaw = null;
    Map<String, String> messages = new HashMap<>();
    boolean inMessages = false;
    boolean inAdmin = false;
    boolean inFeedback = false;
    String feedbackEnabledRaw = null;
    String feedbackSoundRaw = null;
    String feedbackParticlesRaw = null;

    String[] lines = content.split("\n", -1);
    for (int li = 0; li < lines.length; li++) {
      String rawLine = lines[li];
      String trimmed = rawLine.trim();
      if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
      int indent = countIndent(rawLine);
      boolean topLevel = indent == 0;
      if (topLevel) {
        inMessages = false;
        inAdmin = false;
        inPrice = false;
        inFeedback = false;
        inWorlds = false;
        inWorld = false;
        currentWorld = null;
        if (trimmed.startsWith("price:")) {
          String after =
              stripQuotes(stripInlineComment(trimmed.substring("price:".length()).trim()));
          if (!after.isEmpty()) {
            priceScalarPresent = true;
          } else {
            priceHeaderSeen = true;
            inPrice = true;
          }
        } else if (trimmed.startsWith("worlds:")) {
          String after =
              stripQuotes(stripInlineComment(trimmed.substring("worlds:".length()).trim()));
          if (!after.isEmpty()) {
            // worlds: scalar is invalid shape; warn ignore? spec says worlds optional block
            // treat non-empty as invalid worlds scalar -> fail whole file
            // but spec says empty worlds valid, unknown subkeys warn; scalar worlds warn not
            // invalid
            // For safety: if scalar non-empty, warn and ignore
            LOG.warning("worlds: scalar value ignored: " + after);
          }
          inWorlds = true;
        } else if (trimmed.startsWith("messages:")) {
          inMessages = true;
        } else if (trimmed.startsWith("feedback:")) {
          String after =
              stripQuotes(stripInlineComment(trimmed.substring("feedback:".length()).trim()));
          if (after.isEmpty()) {
            inFeedback = true;
          }
        } else if (trimmed.equals("admin:")) {
          inAdmin = true;
        } else if (trimmed.startsWith("admin.target-distance:")) {
          distanceRaw =
              stripQuotes(
                  stripInlineComment(trimmed.substring("admin.target-distance:".length()).trim()));
        }
      } else {
        if (inWorlds) {
          if (indent == 2) {
            // world name level under worlds:
            int colon = trimmed.indexOf(':');
            if (colon <= 0) {
              LOG.warning("worlds: ignoring malformed world entry: " + trimmed);
              inWorld = false;
              currentWorld = null;
              continue;
            }
            String worldNameRaw = trimmed.substring(0, colon).trim();
            String after = stripQuotes(stripInlineComment(trimmed.substring(colon + 1).trim()));
            String worldName = stripWorldName(worldNameRaw);
            if (worldName.isEmpty()) {
              LOG.warning("worlds: empty world name ignored");
              inWorld = false;
              currentWorld = null;
              continue;
            }
            if (worldsRaw.containsKey(worldName) && !worldsDupWarned.contains(worldName)) {
              LOG.warning("worlds." + worldName + " duplicated, last wins");
              worldsDupWarned.add(worldName);
            }
            // new world entry starts; if scalar after colon non-empty warn? worlds.world: 123
            // invalid
            if (!after.isEmpty()) {
              LOG.warning("worlds." + worldName + ": scalar ignored: " + after);
              inWorld = false;
              currentWorld = null;
              continue;
            }
            worldsRaw.putIfAbsent(worldName, new HashMap<>());
            // if dup, keep existing map but will overwrite fields (last wins)
            if (worldsDupWarned.contains(worldName)) {
              worldsRaw.put(worldName, new HashMap<>(worldsRaw.get(worldName)));
              // actually we already have it; we will overwrite fields below; keep map
              // re-use existing to allow merging last-wins per field
            }
            currentWorld = worldName;
            inWorld = true;
          } else if (indent == 4 && inWorld && currentWorld != null) {
            int colon = trimmed.indexOf(':');
            if (colon <= 0) {
              LOG.warning("worlds." + currentWorld + ": malformed entry ignored: " + trimmed);
              continue;
            }
            String k = trimmed.substring(0, colon).trim();
            String v = stripQuotes(stripInlineComment(trimmed.substring(colon + 1).trim()));
            if (v != null && v.isEmpty()) v = "";
            if (k.equals("hand") || k.equals("all")) {
              Map<String, String> m = worldsRaw.get(currentWorld);
              if (m.containsKey(k)) {
                LOG.warning("worlds." + currentWorld + "." + k + " duplicated, last wins");
              }
              m.put(k, v);
            } else {
              LOG.warning("worlds." + currentWorld + "." + k + " unknown subkey ignored");
            }
          } else if (indent == 4 && !inWorld) {
            // indented content outside a world entry: ignore
          }
        }
        if (inMessages) {
          int colon = trimmed.indexOf(':');
          if (colon > 0) {
            String k = trimmed.substring(0, colon).trim();
            String v = stripQuotes(stripInlineComment(trimmed.substring(colon + 1).trim()));
            if (!k.isEmpty()) messages.put(k, v);
          }
        } else if (inPrice) {
          if (trimmed.startsWith("hand:")) {
            priceHandRaw =
                stripQuotes(stripInlineComment(trimmed.substring("hand:".length()).trim()));
            if (priceHandRaw != null && priceHandRaw.isEmpty()) priceHandRaw = null;
          } else if (trimmed.startsWith("all:")) {
            priceAllRaw =
                stripQuotes(stripInlineComment(trimmed.substring("all:".length()).trim()));
            if (priceAllRaw != null && priceAllRaw.isEmpty()) priceAllRaw = null;
          }
        } else if (inFeedback) {
          if (trimmed.startsWith("enabled:")) {
            feedbackEnabledRaw =
                stripQuotes(stripInlineComment(trimmed.substring("enabled:".length()).trim()));
          } else if (trimmed.startsWith("sound:")) {
            feedbackSoundRaw =
                stripQuotes(stripInlineComment(trimmed.substring("sound:".length()).trim()));
          } else if (trimmed.startsWith("particles:")) {
            feedbackParticlesRaw =
                stripQuotes(stripInlineComment(trimmed.substring("particles:".length()).trim()));
          }
        } else if (inAdmin) {
          if (trimmed.startsWith("target-distance:")) {
            distanceRaw =
                stripQuotes(
                    stripInlineComment(trimmed.substring("target-distance:".length()).trim()));
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

    // validate worlds entries: present hand/all must be finite >=0
    Map<String, WorldPrice> worldPrices = new HashMap<>();
    for (Map.Entry<String, Map<String, String>> e : worldsRaw.entrySet()) {
      String wname = e.getKey();
      Map<String, String> fields = e.getValue();
      // empty worlds entry -> valid partial (no price), yields WorldPrice(null,null) -> global
      // fallback
      // But to allow lenient empty worlds valid, we keep nulls; activation resolver will fallback
      // per-field
      String handRaw = fields.get("hand");
      String allRaw = fields.get("all");
      BigDecimal hand = null;
      BigDecimal all = null;
      if (handRaw != null) {
        if (handRaw.isEmpty()) {
          return err("worlds." + wname + ".hand: empty");
        }
        try {
          hand = new BigDecimal(handRaw);
        } catch (Exception ex) {
          return err("worlds." + wname + ".hand: " + handRaw);
        }
        if (hand.signum() < 0) return err("worlds." + wname + ".hand: " + handRaw);
        try {
          new MoneyAmount(hand);
        } catch (IllegalArgumentException ex) {
          return err("worlds." + wname + ".hand: " + ex.getMessage());
        }
      }
      if (allRaw != null) {
        if (allRaw.isEmpty()) {
          return err("worlds." + wname + ".all: empty");
        }
        try {
          all = new BigDecimal(allRaw);
        } catch (Exception ex) {
          return err("worlds." + wname + ".all: " + allRaw);
        }
        if (all.signum() < 0) return err("worlds." + wname + ".all: " + allRaw);
        try {
          new MoneyAmount(all);
        } catch (IllegalArgumentException ex) {
          return err("worlds." + wname + ".all: " + ex.getMessage());
        }
      }
      worldPrices.put(wname, new WorldPrice(hand, all));
    }

    ConfigSnapshot snap =
        new ConfigSnapshot(
            priceHand,
            priceAll,
            worldPrices,
            distance,
            Collections.unmodifiableMap(messages),
            true,
            feedbackEnabled,
            feedbackSound,
            feedbackParticles);
    return new SnapshotOrError(snap, null);
  }

  private static int countIndent(String s) {
    int i = 0;
    while (i < s.length() && (s.charAt(i) == ' ' || s.charAt(i) == '\t')) i++;
    // treat tab as 2 spaces for our counts; simplest: count spaces, tabs as 2
    int c = 0;
    for (int j = 0; j < i; j++) c += s.charAt(j) == '\t' ? 2 : 1;
    return c;
  }

  private static String stripWorldName(String raw) {
    String s = raw.trim();
    if (s.length() >= 2
        && ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'")))) {
      s = s.substring(1, s.length() - 1);
    }
    return s;
  }

  private static String stripQuotes(String s) {
    if (s.length() >= 2
        && ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'")))) {
      return s.substring(1, s.length() - 1);
    }
    return s;
  }

  private static String stripInlineComment(String s) {
    if (s == null || s.isEmpty()) return s;
    boolean inSingle = false;
    boolean inDouble = false;
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c == '\'' && !inDouble) {
        inSingle = !inSingle;
      } else if (c == '"' && !inSingle) {
        inDouble = !inDouble;
      } else if (c == '#' && !inSingle && !inDouble) {
        if (i == 0 || s.charAt(i - 1) == ' ' || s.charAt(i - 1) == '\t') {
          return s.substring(0, i).trim();
        }
      }
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
