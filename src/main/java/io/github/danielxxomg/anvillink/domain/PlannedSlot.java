// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.domain;

/** One ordered, eligible slot inside a {@link RepairPlan}. */
public record PlannedSlot(EquipmentSlotId slot, ItemSnapshot snapshot) {}
