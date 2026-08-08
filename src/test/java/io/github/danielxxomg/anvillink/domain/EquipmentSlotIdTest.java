// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class EquipmentSlotIdTest {

  @Test
  void handModeResolvesOnlyTheMainHandSlot() {
    // equipment-repair Scenario: HAND excludes other equipment.
    assertEquals(
        Set.of(EquipmentSlotId.MAIN_HAND), Set.copyOf(EquipmentSlotId.slotsFor(RepairMode.HAND)));
  }

  @Test
  void allModeResolvesExactlySixEquipmentSlotsInOrder() {
    // equipment-repair Scenario: ALL excludes storage.
    assertEquals(6, EquipmentSlotId.slotsFor(RepairMode.ALL).size());
    assertEquals(6, Set.copyOf(EquipmentSlotId.slotsFor(RepairMode.ALL)).size());
  }

  @Test
  void allModeContainsExactlyTheSixDefinedSlots() {
    assertEquals(
        Set.of(
            EquipmentSlotId.MAIN_HAND,
            EquipmentSlotId.OFF_HAND,
            EquipmentSlotId.HELMET,
            EquipmentSlotId.CHESTPLATE,
            EquipmentSlotId.LEGGINGS,
            EquipmentSlotId.BOOTS),
        Set.copyOf(EquipmentSlotId.slotsFor(RepairMode.ALL)));
  }

  @Test
  void noStorageSlotBelongsToAnyMode() {
    Set<EquipmentSlotId> all =
        EquipmentSlotId.slotsFor(RepairMode.ALL).stream().collect(Collectors.toSet());
    Set<EquipmentSlotId> hand =
        EquipmentSlotId.slotsFor(RepairMode.HAND).stream().collect(Collectors.toSet());
    // Neither mode may include storage (or any other slot).
    assertFalse(all.contains(EquipmentSlotId.STORAGE));
    assertFalse(hand.contains(EquipmentSlotId.STORAGE));
    assertTrue(EquipmentSlotId.slotsFor(RepairMode.ALL).contains(EquipmentSlotId.MAIN_HAND));
    assertTrue(EquipmentSlotId.slotsFor(RepairMode.ALL).contains(EquipmentSlotId.OFF_HAND));
  }
}
