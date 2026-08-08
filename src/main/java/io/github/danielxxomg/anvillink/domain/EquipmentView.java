// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.domain;

/** Pure-domain provider for bounded equipment slot views. No Bukkit types. */
@FunctionalInterface
public interface EquipmentView {

  ItemView itemAt(EquipmentSlotId slot);
}
