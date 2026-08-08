// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.domain;

/** Pure-domain view of a single equipment slot's item. No Bukkit types. */
public interface ItemView {

  boolean isEmpty();

  boolean isDamageable();

  int damage();

  boolean isUnbreakable();

  ItemSnapshot snapshot();
}
