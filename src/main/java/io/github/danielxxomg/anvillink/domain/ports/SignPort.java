// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.domain.ports;

import io.github.danielxxomg.anvillink.domain.SignRecord;
import java.util.Optional;
import java.util.UUID;

/** Pure-domain sign-identity port. No Bukkit types. */
public interface SignPort {
  record SignId(String value) {
    public SignId {
      if (value == null || value.isBlank()) {
        throw new IllegalArgumentException("sign id must not be blank");
      }
    }
  }

  record PlayerId(UUID uuid) {
    public PlayerId {
      if (uuid == null) {
        throw new IllegalArgumentException("player id must not be null");
      }
    }
  }

  Optional<SignRecord> load(SignId signId);

  boolean hasPermission(PlayerId playerId, String permission);

  Optional<FrontText> frontText(SignId signId);

  record FrontText(String line1, String line2) {}
}
