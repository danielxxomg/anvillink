// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.domain;

import java.util.Locale;
import java.util.Optional;

/**
 * Repair modes recognized on an authorized repair sign. Pure domain — no Bukkit types. Values are
 * stable: ordinal values are persisted in the PDC {@link SignRecord} byte layout and MUST NOT be
 * reordered.
 */
public enum RepairMode {
  /** Repair only the main-hand slot. */
  HAND,
  /** Repair the six equipment slots: main hand, off hand, helmet, chestplate, leggings, boots. */
  ALL;

  /**
   * Case-insensitive parse of a sign line value. Returns {@link Optional#empty()} for anything that
   * is not exactly {@code HAND} or {@code ALL}.
   */
  public static Optional<RepairMode> parse(String raw) {
    if (raw == null) {
      return Optional.empty();
    }
    String normalized = raw.trim().toUpperCase(Locale.ROOT);
    for (RepairMode mode : values()) {
      if (mode.name().equals(normalized)) {
        return Optional.of(mode);
      }
    }
    return Optional.empty();
  }
}
