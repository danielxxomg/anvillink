// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.domain;

import java.util.Optional;

/**
 * Pure-domain parser for the canonical repair sign text. No Bukkit types.
 *
 * <p>Accepts only front-side line 1 exactly {@code [repair]} (case-insensitive) and line 2 exactly
 * {@code HAND} or {@code ALL} (case-insensitive). Wrong location, empty, or invalid values return
 * empty.
 */
public final class SignParser {

  public record ParseResult(RepairMode mode) {}

  public Optional<ParseResult> parse(String line1, String line2) {
    if (line1 == null || line2 == null) {
      return Optional.empty();
    }
    String normalizedLine1 = line1.trim();
    String normalizedLine2 = line2.trim();
    if (normalizedLine1.isEmpty() || normalizedLine2.isEmpty()) {
      return Optional.empty();
    }
    if (!"[repair]".equalsIgnoreCase(normalizedLine1)) {
      return Optional.empty();
    }
    return RepairMode.parse(normalizedLine2).map(ParseResult::new);
  }
}
