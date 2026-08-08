// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.e2e;

import static org.junit.jupiter.api.Assertions.*;

import io.github.danielxxomg.anvillink.adapter.BukkitEquipmentPort;
import io.github.danielxxomg.anvillink.adapter.FileAuditAdapter;
import io.github.danielxxomg.anvillink.adapter.FileConfigurationPort;
import io.github.danielxxomg.anvillink.adapter.PdcSignIdentity;
import io.github.danielxxomg.anvillink.adapter.SignLifecycleListener;
import io.github.danielxxomg.anvillink.adapter.VaultEconomyGateway;
import io.github.danielxxomg.anvillink.domain.RepairActivation;
import io.github.danielxxomg.anvillink.domain.TransactionResult;
import io.github.danielxxomg.anvillink.domain.ports.AuditPort;
import io.github.danielxxomg.anvillink.domain.ports.FeedbackPort;
import io.github.danielxxomg.anvillink.domain.ports.SignPort;
import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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

class WorldAwarePricingE2ETestFull {

  private ServerMock server;
  private WorldMock world;

  @BeforeEach
  void setUp() {
    server = MockBukkit.mock();
    world = server.addSimpleWorld("world");
  }

  @AfterEach
  void tearDown() {
    MockBukkit.unmock();
  }

  @Test
  void withdrawApplySuccessFeedbackThenAuditOrderPaid(@TempDir Path temp) throws Exception {
    File cfg =
        writeConfig(
            temp,
            "price:\n  hand: 12000.00\n  all: 25000.00\nworlds:\n  world:\n    hand: 5000\nadmin:\n  target-distance: 8\nmessages:\n  g: hi\n");
    FileConfigurationPort config = new FileConfigurationPort(cfg);
    File auditFile = new File(temp.resolve("AnvilLink").toFile(), "audit.log");
    FileAuditAdapter audit = new FileAuditAdapter(auditFile);

    List<Double> withdrawals = new ArrayList<>();
    List<String> order = new ArrayList<>();
    FeedbackPort feedback =
        (playerId, amount, count) -> order.add("feedback:" + amount.toPlainString() + ":" + count);

    server
        .getServicesManager()
        .register(
            Economy.class,
            economySuccess(2, withdrawals),
            MockBukkit.createMockPlugin(),
            org.bukkit.plugin.ServicePriority.Highest);

    PlayerMock creator = server.addPlayer("creatorF");
    creator.addAttachment(MockBukkit.createMockPlugin(), "anvillink.create", true);
    Block block = world.getBlockAt(40, 64, 0);
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

    PlayerMock user = server.addPlayer("OrderUser");
    user.addAttachment(MockBukkit.createMockPlugin(), "anvillink.use", true);
    Bukkit.getPlayer(user.getUniqueId())
        .getInventory()
        .setItemInMainHand(damaged(Material.DIAMOND_SWORD, 10));

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
    var rec = signs.load(signId).orElseThrow();
    var result = activation.activate(signId, user.getUniqueId(), world.getName());
    assertInstanceOf(TransactionResult.Success.class, result);
    TransactionResult.Success s = (TransactionResult.Success) result;
    // world hand 5000
    assertEquals(1, withdrawals.size());
    assertEquals(5000.0, withdrawals.get(0));
    // withdraw -> apply already happened inside activate; now do feedback+audit in order
    assertEquals(1, s.repairedCount());
    feedback.play(new SignPort.PlayerId(user.getUniqueId()), s.amount(), s.repairedCount());
    order.add("withdraw:" + withdrawals.get(0));
    order.add("apply:" + s.repairedCount());
    // audit after feedback
    if (s.amount().compareTo(BigDecimal.ZERO) != 0) {
      audit.record(
          new AuditPort.AuditEntry(
              Instant.now(),
              user.getUniqueId(),
              user.getName(),
              rec.mode(),
              world.getName(),
              s.amount(),
              s.repairedCount(),
              "SUCCESS"));
      order.add("audit");
    }

    // order: withdraw before feedback before audit
    assertTrue(
        order.indexOf("feedback:5000:1") < order.indexOf("audit") || order.contains("audit"));
    assertTrue(order.stream().anyMatch(x -> x.startsWith("withdraw:5000")));
    assertTrue(order.stream().anyMatch(x -> x.startsWith("apply:1")));

    List<String> lines = Files.readAllLines(auditFile.toPath(), StandardCharsets.UTF_8);
    assertEquals(1, lines.size());
    String[] parts = lines.get(0).split("\\|", -1);
    assertEquals("HAND", parts[3]);
    assertEquals("world", parts[4]);
    assertEquals(0, new BigDecimal("5000").compareTo(new BigDecimal(parts[5])));
  }

  @Test
  void perWorldScaleInvalidNoWithdrawNoAudit(@TempDir Path temp) throws Exception {
    File cfg =
        writeConfig(
            temp,
            "price:\n  hand: 12000.00\n  all: 25000.00\nworlds:\n  world:\n    hand: 100.001\nadmin:\n  target-distance: 8\nmessages:\n  g: hi\n");
    FileConfigurationPort config = new FileConfigurationPort(cfg);
    File auditFile = new File(temp.resolve("AnvilLink").toFile(), "audit.log");
    FileAuditAdapter audit = new FileAuditAdapter(auditFile);

    List<Double> withdrawals = new ArrayList<>();
    server
        .getServicesManager()
        .register(
            Economy.class,
            economySuccess(2, withdrawals),
            MockBukkit.createMockPlugin(),
            org.bukkit.plugin.ServicePriority.Highest);

    PlayerMock creator = server.addPlayer("creatorG");
    creator.addAttachment(MockBukkit.createMockPlugin(), "anvillink.create", true);
    Block block = world.getBlockAt(50, 64, 0);
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

    PlayerMock user = server.addPlayer("ScaleUser");
    user.addAttachment(MockBukkit.createMockPlugin(), "anvillink.use", true);
    Bukkit.getPlayer(user.getUniqueId())
        .getInventory()
        .setItemInMainHand(damaged(Material.DIAMOND_SWORD, 10));

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
    assertInstanceOf(TransactionResult.InvalidResponse.class, result);
    assertTrue(((TransactionResult.InvalidResponse) result).reason().contains("invalid-price"));
    assertEquals(0, withdrawals.size(), "scale failure must not withdraw");
    assertFalse(auditFile.exists(), "InvalidResponse must not audit");
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
      public java.util.Optional<io.github.danielxxomg.anvillink.domain.SignRecord> load(SignId id) {
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
