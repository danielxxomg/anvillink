// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.adapter;

import io.github.danielxxomg.anvillink.domain.RepairMode;
import io.github.danielxxomg.anvillink.domain.SignParser;
import io.github.danielxxomg.anvillink.domain.SignRecord;
import java.util.Optional;
import org.bukkit.DyeColor;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;

/**
 * Lifecycle adapter: SignChangeEvent (create) and BlockBreakEvent (break). Permission gate:
 * anvillink.create / anvillink.manage. Registered sign = has PDC.
 */
public final class SignLifecycleListener implements Listener {

  private final SignParser parser = new SignParser();

  @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
  public void onSignChange(SignChangeEvent event) {
    BlockState state = event.getBlock().getState();
    boolean isRegistered = state instanceof TileState ts && PdcSignIdentity.has(ts);
    Player player = event.getPlayer();
    String line1 = event.getLine(0);
    String line2 = event.getLine(1);

    if (!isRegistered) {
      // creation flow: must parse [repair]+HAND/ALL
      Optional<SignParser.ParseResult> parsed = parser.parse(line1, line2);
      if (parsed.isEmpty()) return;
      if (!player.hasPermission("anvillink.create")) {
        event.setCancelled(true);
        return;
      }
      RepairMode mode = parsed.get().mode();
      event.setLine(0, "[repair]");
      event.setLine(1, mode.name());
      // persist PDC + color. On Paper 1.21+, the snapshot from before setLine is stale
      // and setColor(DyeColor) targets the legacy field — also mirror canonical lines
      // onto the TileState so any post-event line coalescing does not lose them.
      // PDC+color are written to a fresh state so they survive Bukkit's copy.
      BlockState fresh = event.getBlock().getState();
      boolean wrote = false;
      if (fresh instanceof TileState ts) {
        PdcSignIdentity.write(ts, SignRecord.create(mode, player.getUniqueId()));
        if (ts instanceof Sign sign) {
          sign.setColor(DyeColor.BLUE);
          try {
            sign.setLine(0, "[repair]");
            sign.setLine(1, mode.name());
          } catch (Throwable ignored) {
          }
        }
        ts.update(true, false);
        wrote = true;
      }
      if (!wrote && state instanceof TileState ts) {
        // fallback for test doubles where fresh is not TileState but original is
        PdcSignIdentity.write(ts, SignRecord.create(mode, player.getUniqueId()));
        if (ts instanceof Sign sign) sign.setColor(DyeColor.BLUE);
        ts.update(true, false);
      }
      return;
    }

    // edit of registered sign: requires manage; text stays tampered until rerender
    if (!player.hasPermission("anvillink.manage")) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
  public void onBlockBreak(BlockBreakEvent event) {
    BlockState state = event.getBlock().getState();
    if (!(state instanceof TileState ts)) return;
    if (!PdcSignIdentity.has(ts)) return;
    if (!event.getPlayer().hasPermission("anvillink.manage")) {
      event.setCancelled(true);
    }
  }
}
