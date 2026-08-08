// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure-domain planner. Resolves exactly {@code HAND=[main]} or {@code ALL=[main, off, helmet,
 * chest, leggings, boots]} in fixed order, never storage. Includes only non-empty, Damageable,
 * positive-damage, non-unbreakable items. Each planned slot captures its full pre-repair snapshot;
 * planning never mutates an item.
 */
public final class RepairPlanner {

  public RepairPlan plan(RepairMode mode, EquipmentView view) {
    List<PlannedSlot> result = new ArrayList<>();
    for (EquipmentSlotId slot : EquipmentSlotId.slotsFor(mode)) {
      ItemView item = view.itemAt(slot);
      if (item == null) {
        continue;
      }
      if (item.isEmpty()) {
        continue;
      }
      if (!item.isDamageable()) {
        continue;
      }
      if (item.damage() <= 0) {
        continue;
      }
      if (item.isUnbreakable()) {
        continue;
      }
      result.add(new PlannedSlot(slot, item.snapshot()));
    }
    return new RepairPlan(result);
  }
}
