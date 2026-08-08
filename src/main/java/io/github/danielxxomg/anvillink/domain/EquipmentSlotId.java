// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.domain;

import java.util.List;

/**
 * The bounded equipment slot universe used by repair plans. Pure domain — no Bukkit types; the
 * Bukkit adapter maps these IDs to {@code PlayerInventory} slots.
 *
 * <p>Per the approved product decision and equipment-repair spec: {@code HAND} resolves to the
 * main-hand slot only; {@code ALL} resolves to exactly six equipment slots in fixed order — main
 * hand, off hand, helmet, chestplate, leggings, boots — and NEVER storage.
 */
public enum EquipmentSlotId {
  MAIN_HAND,
  OFF_HAND,
  HELMET,
  CHESTPLATE,
  LEGGINGS,
  BOOTS,
  /** Explicit non-target; never included by any mode. */
  STORAGE;

  /**
   * Ordered, deterministic slot list for the given mode. {@link RepairMode#HAND} yields only {@link
   * #MAIN_HAND}; {@link RepairMode#ALL} yields the six equipment slots in fixed order. Storage is
   * never included.
   */
  public static List<EquipmentSlotId> slotsFor(RepairMode mode) {
    return switch (mode) {
      case HAND -> List.of(MAIN_HAND);
      case ALL -> List.of(MAIN_HAND, OFF_HAND, HELMET, CHESTPLATE, LEGGINGS, BOOTS);
    };
  }
}
