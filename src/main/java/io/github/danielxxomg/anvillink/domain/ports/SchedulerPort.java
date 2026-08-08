// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.domain.ports;

/** Pure-domain scheduler port. No Bukkit types. */
public interface SchedulerPort {
  void runOnServerThread(Runnable task);

  boolean isOnServerThread();
}
