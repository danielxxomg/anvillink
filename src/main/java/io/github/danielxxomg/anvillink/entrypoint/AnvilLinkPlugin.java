// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.entrypoint;

import io.github.danielxxomg.anvillink.adapter.AdminCommandHandler;
import io.github.danielxxomg.anvillink.adapter.BukkitEquipmentPort;
import io.github.danielxxomg.anvillink.adapter.BukkitSchedulerAdapter;
import io.github.danielxxomg.anvillink.adapter.FileConfigurationPort;
import io.github.danielxxomg.anvillink.adapter.InteractionFilter;
import io.github.danielxxomg.anvillink.adapter.MiniMessageMessagePort;
import io.github.danielxxomg.anvillink.adapter.PdcSignIdentity;
import io.github.danielxxomg.anvillink.adapter.SignLifecycleListener;
import io.github.danielxxomg.anvillink.adapter.VaultEconomyGateway;
import io.github.danielxxomg.anvillink.domain.RepairActivation;
import io.github.danielxxomg.anvillink.domain.ports.EconomyPort;
import io.github.danielxxomg.anvillink.domain.ports.EquipmentPort;
import io.github.danielxxomg.anvillink.domain.ports.MessagePort;
import io.github.danielxxomg.anvillink.domain.ports.OperationalReporter;
import io.github.danielxxomg.anvillink.domain.ports.SchedulerPort;
import io.github.danielxxomg.anvillink.domain.ports.SignPort;
import java.io.File;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.TileState;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

/** Entrypoint: wires adapters, registers listeners, loads config. */
public final class AnvilLinkPlugin extends JavaPlugin implements Listener {

  private FileConfigurationPort configPort;
  private MessagePort messagePort;
  private SchedulerPort scheduler;
  private EconomyPort economy;
  private EquipmentPort equipment;
  private SignPort signs;
  private RepairActivation activation;

  @Override
  public void onEnable() {
    saveDefaultConfig();
    File configFile = new File(getDataFolder(), "config.yml");
    configPort = new FileConfigurationPort(configFile);
    if (!configPort.current().activationEnabled()) {
      getLogger()
          .warning("Invalid initial configuration — repair activation disabled until reload.");
    }
    messagePort = new MiniMessageMessagePort(configPort);
    try {
      scheduler = new BukkitSchedulerAdapter(this);
    } catch (UnsupportedOperationException e) {
      getLogger().severe("Folia is not supported: " + e.getMessage());
      getServer().getPluginManager().disablePlugin(this);
      return;
    }
    economy = new VaultEconomyGateway();
    equipment = new BukkitEquipmentPort(uuid -> Bukkit.getPlayer(uuid));
    signs = bukkitSignPort();
    OperationalReporter reporter =
        (severity, code, ctx) ->
            getLogger()
                .log(
                    severity == OperationalReporter.Severity.HIGH ? Level.SEVERE : Level.INFO,
                    code + " " + ctx);
    activation = new RepairActivation(signs, equipment, economy, scheduler, configPort, reporter);

    getServer().getPluginManager().registerEvents(new SignLifecycleListener(), this);
    getServer().getPluginManager().registerEvents(this, this);

    PluginCommand cmd = getCommand("anvillink");
    if (cmd != null) {
      cmd.setExecutor(new AdminCommandHandler(configPort, messagePort));
    } else {
      getLogger().warning("Command anvillink not declared in plugin.yml");
    }
    getLogger().info("AnvilLink enabled.");
  }

  @Override
  public void onDisable() {
    getLogger().info("AnvilLink disabled.");
  }

  @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
  public void onPlayerInteract(PlayerInteractEvent event) {
    if (!InteractionFilter.shouldProceed(event)) return;
    Block block = event.getClickedBlock();
    if (block == null) return;
    var state = block.getState();
    if (!(state instanceof TileState ts)) return;
    if (!PdcSignIdentity.has(ts)) return;
    Player player = event.getPlayer();
    if (!player.hasPermission("anvillink.use")) return;
    // front text tamper check
    String line1 = "";
    String line2 = "";
    if (state instanceof Sign sign) {
      line1 = sign.getLine(0);
      line2 = sign.getLine(1);
    }
    var blockId =
        block.getWorld().getName() + ":" + block.getX() + "," + block.getY() + "," + block.getZ();
    SignPort.SignId id;
    try {
      id = new SignPort.SignId(blockId);
    } catch (Exception e) {
      return;
    }
    var rec = signs.load(id);
    if (rec.isEmpty()) return;
    // use RepairActivation via sign id + player uuid
    var result = activation.activate(id, player.getUniqueId());
    if (result
        instanceof io.github.danielxxomg.anvillink.domain.TransactionResult.InsufficientFunds) {
      player.sendMessage(messagePort.render("insufficient-funds", java.util.Map.of()));
    } else if (result
        instanceof io.github.danielxxomg.anvillink.domain.TransactionResult.InvalidResponse ir) {
      // tampered-text etc -> map to tampered message when applicable
      String reason = ir.reason();
      if (reason != null && reason.contains("tampered-text")) {
        player.sendMessage(messagePort.render("tampered", java.util.Map.of()));
      } else if (reason != null && reason.contains("activation-disabled")) {
        player.sendMessage(
            messagePort.render("activation-failure", java.util.Map.of("reason", reason)));
      }
    } else if (result
        instanceof io.github.danielxxomg.anvillink.domain.TransactionResult.Success s) {
      if (s.amount().compareTo(BigDecimal.ZERO) == 0) {
        player.sendMessage(messagePort.render("no-eligible-items", java.util.Map.of()));
      }
    }
  }

  private SignPort bukkitSignPort() {
    return new SignPort() {
      @Override
      public Optional<io.github.danielxxomg.anvillink.domain.SignRecord> load(SignId signId) {
        Block block = blockFor(signId);
        if (block == null) return Optional.empty();
        var state = block.getState();
        if (!(state instanceof TileState ts)) return Optional.empty();
        return PdcSignIdentity.read(ts);
      }

      @Override
      public boolean hasPermission(PlayerId playerId, String permission) {
        Player p = Bukkit.getPlayer(playerId.uuid());
        if (p == null) return false;
        return p.hasPermission(permission);
      }

      @Override
      public Optional<FrontText> frontText(SignId signId) {
        Block block = blockFor(signId);
        if (block == null) return Optional.empty();
        var state = block.getState();
        if (!(state instanceof Sign sign)) return Optional.empty();
        return Optional.of(new FrontText(sign.getLine(0), sign.getLine(1)));
      }

      private Block blockFor(SignId id) {
        // id format world:x,y,z
        String v = id.value();
        int colon = v.indexOf(':');
        if (colon < 0) return null;
        String worldName = v.substring(0, colon);
        String coords = v.substring(colon + 1);
        String[] parts = coords.split(",");
        if (parts.length != 3) return null;
        try {
          int x = Integer.parseInt(parts[0].trim());
          int y = Integer.parseInt(parts[1].trim());
          int z = Integer.parseInt(parts[2].trim());
          var world = Bukkit.getWorld(worldName);
          if (world == null) return null;
          return world.getBlockAt(x, y, z);
        } catch (NumberFormatException e) {
          return null;
        }
      }
    };
  }
}
