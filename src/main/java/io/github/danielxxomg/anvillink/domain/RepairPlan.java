// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.domain;

import java.util.Collections;
import java.util.List;

/** Deterministic, ordered repair plan. Never mutates items. Pure domain. */
public final class RepairPlan {

  private final List<PlannedSlot> slots;

  public RepairPlan(List<PlannedSlot> slots) {
    this.slots = Collections.unmodifiableList(List.copyOf(slots));
  }

  public List<PlannedSlot> slots() {
    return slots;
  }

  public boolean isEmpty() {
    return slots.isEmpty();
  }
}
