// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class RepairModeTest {

  @Test
  void parsesCanonicalUpperModes() {
    assertEquals(Optional.of(RepairMode.HAND), RepairMode.parse("HAND"));
    assertEquals(Optional.of(RepairMode.ALL), RepairMode.parse("ALL"));
  }

  @Test
  void parsesLowercaseCaseInsensitively() {
    assertEquals(Optional.of(RepairMode.HAND), RepairMode.parse("hand"));
    assertEquals(Optional.of(RepairMode.ALL), RepairMode.parse("all"));
  }

  @Test
  void rejectsUnknownModes() {
    assertEquals(Optional.empty(), RepairMode.parse("repair"));
    assertEquals(Optional.empty(), RepairMode.parse("OFFHAND"));
    assertEquals(Optional.empty(), RepairMode.parse(""));
    assertEquals(Optional.empty(), RepairMode.parse(null));
  }
}
