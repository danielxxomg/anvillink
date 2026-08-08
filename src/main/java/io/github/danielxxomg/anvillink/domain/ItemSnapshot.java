// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.domain;

/**
 * Opaque, full item-state snapshot for one equipment slot. Pure domain — no Bukkit types. Captured
 * before payment; used only for restoration of mutated slots on apply failure.
 */
public record ItemSnapshot(boolean empty, boolean damageable, int damage, boolean unbreakable) {}
