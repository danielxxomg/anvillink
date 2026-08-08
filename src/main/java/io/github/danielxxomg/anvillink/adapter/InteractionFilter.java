// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.adapter;

import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * Filters duplicate per-hand PlayerInteractEvent. Only main-hand right-click enters the transaction
 * boundary (repair-signs Scenario: Duplicate events do not double-charge).
 */
public final class InteractionFilter {

  private InteractionFilter() {}

  public static boolean shouldProceed(PlayerInteractEvent event) {
    if (event.getHand() != EquipmentSlot.HAND) return false;
    Action a = event.getAction();
    return a == Action.RIGHT_CLICK_BLOCK || a == Action.RIGHT_CLICK_AIR;
  }
}
