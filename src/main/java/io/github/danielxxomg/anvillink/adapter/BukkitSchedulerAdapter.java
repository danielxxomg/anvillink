// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.adapter;

import io.github.danielxxomg.anvillink.domain.ports.SchedulerPort;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;

/**
 * BukkitScheduler-backed SchedulerPort. Delegates to {@link BukkitScheduler} and rejects Folia via
 * {@code io.papermc.paper.threadedregions} presence.
 */
public final class BukkitSchedulerAdapter implements SchedulerPort {

  private final Plugin plugin;
  private final Server server;

  public BukkitSchedulerAdapter(Plugin plugin) {
    this(plugin, Bukkit.getServer());
  }

  BukkitSchedulerAdapter(Plugin plugin, Server server) {
    this.plugin = plugin;
    this.server = server;
    detectFolia();
  }

  @Override
  public void runOnServerThread(Runnable task) {
    if (isOnServerThread()) {
      task.run();
      return;
    }
    server.getScheduler().runTask(plugin, task);
  }

  @Override
  public boolean isOnServerThread() {
    return server.isPrimaryThread();
  }

  private static void detectFolia() {
    try {
      Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
      throw new UnsupportedOperationException("Folia is not supported (threaded regions detected)");
    } catch (ClassNotFoundException ignored) {
      // not Folia
    }
    try {
      Class.forName("io.papermc.paper.threadedregions.api.RegionizedServerInitEvent");
      throw new UnsupportedOperationException("Folia is not supported (threaded regions detected)");
    } catch (ClassNotFoundException ignored) {
      // not Folia
    }
  }
}
