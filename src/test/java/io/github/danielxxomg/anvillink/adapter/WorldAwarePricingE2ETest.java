// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.adapter;

import static org.junit.jupiter.api.Assertions.*;

import io.github.danielxxomg.anvillink.domain.RepairActivation;
import io.github.danielxxomg.anvillink.domain.SignRecord;
import io.github.danielxxomg.anvillink.domain.TransactionResult;
import io.github.danielxxomg.anvillink.domain.ports.SignPort;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

class WorldAwarePricingE2ETest {

  private ServerMock server;
  private WorldMock world;
  private WorldMock nether;

  @BeforeEach
  void setUp() {
    server = MockBukkit.mock();
    world = server.addSimpleWorld("world");
    nether = server.addSimpleWorld("world_nether");
  }

  @AfterEach
  void tearDown() {
    MockBukkit.unmock();
  }

  @Test
  void handInWorldCharges5000Not12000(@TempDir Path temp) throws Exception {
    File cfg =
        writeConfig(
            temp,
            "price:\n  hand: 12000.00\n  all: 25000.00\nworlds:\n  world:\n    hand: 5000\nadmin:\n  target-distance: 8\nmessages:\n  g: hi\n");
    FileConfigurationPort config = new FileConfigurationPort(cfg);
    List<Double> withdrawals = new ArrayList<>();
    Economy eco = economySuccess(2, withdrawals);
    server
        .getServicesManager()
        .register(
            Economy.class,
            eco,
            MockBukkit.createMockPlugin(),
            org.bukkit.plugin.ServicePriority.Highest);

    PlayerMock creator = server.addPlayer("creator");
    creator.addAttachment(MockBukkit.createMockPlugin(), "anvillink.create", true);
    Block block = world.getBlockAt(0, 64, 0);
    block.setType(Material.OAK_SIGN);
    new SignLifecycleListener()
        .onSignChange(
            new org.bukkit.event.block.SignChangeEvent(
                block, creator, new String[] {"[repair]", "HAND", "", ""}));
    if (block.getState() instanceof Sign s) {
      s.setLine(0, "[repair]");
      s.setLine(1, "HAND");
      s.update(true, false);
    }

    PlayerMock user = server.addPlayer("user");
    user.addAttachment(MockBukkit.createMockPlugin(), "anvillink.use", true);
    ItemStack sword = damaged(Material.DIAMOND_SWORD, 10);
    Bukkit.getPlayer(user.getUniqueId()).getInventory().setItemInMainHand(sword);

    var signId =
        new SignPort.SignId(
            block.getWorld().getName()
                + ":"
                + block.getX()
                + ","
                + block.getY()
                + ","
                + block.getZ());
    var signs = bukkitSignPort();
    var equipment = new BukkitEquipmentPort(uuid -> Bukkit.getPlayer(uuid));
    var economy = new VaultEconomyGateway(() -> server);
    var activation =
        new RepairActivation(
            signs, equipment, economy, syncScheduler(), config, (sev, code, ctx) -> {});
    var result = activation.activate(signId, user.getUniqueId(), world.getName());
    assertInstanceOf(TransactionResult.Success.class, result);
    assertEquals(1, withdrawals.size());
    assertEquals(5000.0, withdrawals.get(0));
  }

  @Test
  void allInWorldChargesGlobal25000(@TempDir Path temp) throws Exception {
    File cfg =
        writeConfig(
            temp,
            "price:\n  hand: 12000.00\n  all: 25000.00\nworlds:\n  world:\n    hand: 5000\nadmin:\n  target-distance: 8\nmessages:\n  g: hi\n");
    FileConfigurationPort config = new FileConfigurationPort(cfg);
    List<Double> withdrawals = new ArrayList<>();
    Economy eco = economySuccess(2, withdrawals);
    server
        .getServicesManager()
        .register(
            Economy.class,
            eco,
            MockBukkit.createMockPlugin(),
            org.bukkit.plugin.ServicePriority.Highest);

    PlayerMock creator = server.addPlayer("creator2");
    creator.addAttachment(MockBukkit.createMockPlugin(), "anvillink.create", true);
    Block block = world.getBlockAt(1, 64, 0);
    block.setType(Material.OAK_SIGN);
    new SignLifecycleListener()
        .onSignChange(
            new org.bukkit.event.block.SignChangeEvent(
                block, creator, new String[] {"[repair]", "ALL", "", ""}));
    if (block.getState() instanceof Sign s) {
      s.setLine(0, "[repair]");
      s.setLine(1, "ALL");
      s.update(true, false);
    }

    PlayerMock user = server.addPlayer("user2");
    user.addAttachment(MockBukkit.createMockPlugin(), "anvillink.use", true);
    // equip all six
    var p = Bukkit.getPlayer(user.getUniqueId());
    p.getInventory().setItemInMainHand(damaged(Material.DIAMOND_SWORD, 5));
    p.getInventory().setHelmet(damaged(Material.DIAMOND_HELMET, 5));
    p.getInventory().setChestplate(damaged(Material.DIAMOND_CHESTPLATE, 5));
    p.getInventory().setLeggings(damaged(Material.DIAMOND_LEGGINGS, 5));
    p.getInventory().setBoots(damaged(Material.DIAMOND_BOOTS, 5));
    p.getInventory().setItemInOffHand(damaged(Material.SHIELD, 5));

    var signId =
        new SignPort.SignId(
            block.getWorld().getName()
                + ":"
                + block.getX()
                + ","
                + block.getY()
                + ","
                + block.getZ());
    var signs = bukkitSignPort();
    var equipment = new BukkitEquipmentPort(uuid -> Bukkit.getPlayer(uuid));
    var economy = new VaultEconomyGateway(() -> server);
    var activation =
        new RepairActivation(
            signs, equipment, economy, syncScheduler(), config, (sev, code, ctx) -> {});
    var result = activation.activate(signId, user.getUniqueId(), world.getName());
    assertInstanceOf(TransactionResult.Success.class, result);
    assertEquals(1, withdrawals.size());
    assertEquals(25000.0, withdrawals.get(0));
  }

  @Test
  void netherAllCharges1000(@TempDir Path temp) throws Exception {
    File cfg =
        writeConfig(
            temp,
            "price:\n  hand: 12000.00\n  all: 25000.00\nworlds:\n  world_nether:\n    all: 1000\nadmin:\n  target-distance: 8\nmessages:\n  g: hi\n");
    FileConfigurationPort config = new FileConfigurationPort(cfg);
    List<Double> withdrawals = new ArrayList<>();
    Economy eco = economySuccess(2, withdrawals);
    server
        .getServicesManager()
        .register(
            Economy.class,
            eco,
            MockBukkit.createMockPlugin(),
            org.bukkit.plugin.ServicePriority.Highest);

    PlayerMock creator = server.addPlayer("creator3");
    creator.addAttachment(MockBukkit.createMockPlugin(), "anvillink.create", true);
    Block block = nether.getBlockAt(0, 64, 0);
    block.setType(Material.OAK_SIGN);
    new SignLifecycleListener()
        .onSignChange(
            new org.bukkit.event.block.SignChangeEvent(
                block, creator, new String[] {"[repair]", "ALL", "", ""}));
    if (block.getState() instanceof Sign s) {
      s.setLine(0, "[repair]");
      s.setLine(1, "ALL");
      s.update(true, false);
    }

    PlayerMock user = server.addPlayer("user3");
    user.addAttachment(MockBukkit.createMockPlugin(), "anvillink.use", true);
    var p = Bukkit.getPlayer(user.getUniqueId());
    p.getInventory().setItemInMainHand(damaged(Material.DIAMOND_SWORD, 5));
    p.getInventory().setHelmet(damaged(Material.DIAMOND_HELMET, 5));
    p.getInventory().setChestplate(damaged(Material.DIAMOND_CHESTPLATE, 5));
    p.getInventory().setLeggings(damaged(Material.DIAMOND_LEGGINGS, 5));
    p.getInventory().setBoots(damaged(Material.DIAMOND_BOOTS, 5));
    p.getInventory().setItemInOffHand(damaged(Material.SHIELD, 5));

    var signId =
        new SignPort.SignId(
            block.getWorld().getName()
                + ":"
                + block.getX()
                + ","
                + block.getY()
                + ","
                + block.getZ());
    var signs = bukkitSignPort();
    var equipment = new BukkitEquipmentPort(uuid -> Bukkit.getPlayer(uuid));
    var economy = new VaultEconomyGateway(() -> server);
    var activation =
        new RepairActivation(
            signs, equipment, economy, syncScheduler(), config, (sev, code, ctx) -> {});
    var result = activation.activate(signId, user.getUniqueId(), nether.getName());
    assertInstanceOf(TransactionResult.Success.class, result);
    assertEquals(1, withdrawals.size());
    assertEquals(1000.0, withdrawals.get(0));
  }

  private File writeConfig(Path dir, String content) throws Exception {
    Path file = dir.resolve("config.yml");
    Files.writeString(file, content, StandardCharsets.UTF_8);
    return file.toFile();
  }

  private static ItemStack damaged(Material mat, int dmg) {
    ItemStack s = new ItemStack(mat);
    Damageable m = (Damageable) s.getItemMeta();
    m.setDamage(dmg);
    s.setItemMeta(m);
    return s;
  }

  private Economy economySuccess(int fd, List<Double> withdrawals) {
    return (Economy)
        java.lang.reflect.Proxy.newProxyInstance(
            Economy.class.getClassLoader(),
            new Class<?>[] {Economy.class},
            (proxy, method, args) -> {
              String n = method.getName();
              if (n.equals("fractionalDigits")) return fd;
              if (n.equals("withdrawPlayer") && args.length == 2) {
                double amount = (double) args[1];
                withdrawals.add(amount);
                return new EconomyResponse(
                    amount, 10000, EconomyResponse.ResponseType.SUCCESS, null);
              }
              if (n.equals("depositPlayer") && args.length == 2)
                return new EconomyResponse(
                    (double) args[1], 10000, EconomyResponse.ResponseType.SUCCESS, null);
              if (n.equals("isEnabled")) return true;
              if (n.equals("getName")) return "MockEco";
              if (n.equals("hasBankSupport")) return false;
              if (n.equals("format")) return String.valueOf(args[0]);
              if (n.equals("currencyNamePlural")) return "dollars";
              if (n.equals("currencyNameSingular")) return "dollar";
              Class<?> rt = method.getReturnType();
              if (rt == boolean.class) return false;
              if (rt == int.class) return 0;
              if (rt == long.class) return 0L;
              if (rt == double.class) return 0.0;
              return null;
            });
  }

  private SignPort bukkitSignPort() {
    return new SignPort() {
      public java.util.Optional<SignRecord> load(SignId id) {
        Block b = blockFor(id);
        if (b == null) return java.util.Optional.empty();
        var st = b.getState();
        if (!(st instanceof org.bukkit.block.TileState ts)) return java.util.Optional.empty();
        return PdcSignIdentity.read(ts);
      }

      public boolean hasPermission(PlayerId pid, String perm) {
        Player p = Bukkit.getPlayer(pid.uuid());
        return p != null && p.hasPermission(perm);
      }

      public java.util.Optional<FrontText> frontText(SignId id) {
        Block b = blockFor(id);
        if (b == null) return java.util.Optional.empty();
        var st = b.getState();
        if (!(st instanceof Sign s)) return java.util.Optional.empty();
        return java.util.Optional.of(new FrontText(s.getLine(0), s.getLine(1)));
      }

      private Block blockFor(SignId id) {
        String v = id.value();
        int c = v.indexOf(':');
        if (c < 0) return null;
        String w = v.substring(0, c);
        String[] parts = v.substring(c + 1).split(",");
        if (parts.length != 3) return null;
        try {
          int x = Integer.parseInt(parts[0].trim());
          int y = Integer.parseInt(parts[1].trim());
          int z = Integer.parseInt(parts[2].trim());
          var world2 = Bukkit.getWorld(w);
          return world2 == null ? null : world2.getBlockAt(x, y, z);
        } catch (NumberFormatException e) {
          return null;
        }
      }
    };
  }

  private io.github.danielxxomg.anvillink.domain.ports.SchedulerPort syncScheduler() {
    return new io.github.danielxxomg.anvillink.domain.ports.SchedulerPort() {
      public void runOnServerThread(Runnable r) {
        r.run();
      }

      public boolean isOnServerThread() {
        return true;
      }
    };
  }
}
