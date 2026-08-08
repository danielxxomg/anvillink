// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.integration;

import static org.junit.jupiter.api.Assertions.*;

import io.github.danielxxomg.anvillink.adapter.BukkitEquipmentPort;
import io.github.danielxxomg.anvillink.adapter.BukkitFeedbackAdapter;
import io.github.danielxxomg.anvillink.adapter.FileConfigurationPort;
import io.github.danielxxomg.anvillink.adapter.PdcSignIdentity;
import io.github.danielxxomg.anvillink.adapter.SignLifecycleListener;
import io.github.danielxxomg.anvillink.adapter.VaultEconomyGateway;
import io.github.danielxxomg.anvillink.domain.SignRecord;
import io.github.danielxxomg.anvillink.domain.TransactionResult;
import io.github.danielxxomg.anvillink.domain.ports.*;
import java.io.File;
import java.math.BigDecimal;
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
import org.bukkit.event.block.SignChangeEvent;
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

class FeedbackE2ETest {

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

  // --- helpers ---

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
                    amount, 1000, EconomyResponse.ResponseType.SUCCESS, null);
              }
              if (n.equals("depositPlayer") && args.length == 2) {
                return new EconomyResponse(
                    (double) args[1], 1000, EconomyResponse.ResponseType.SUCCESS, null);
              }
              if (n.equals("isEnabled")) return true;
              if (n.equals("getName")) return "MockEco";
              if (n.equals("hasBankSupport")) return false;
              if (n.equals("format")) return String.valueOf(args[0]);
              Class<?> rt = method.getReturnType();
              if (rt == boolean.class) return false;
              if (rt == int.class) return 0;
              if (rt == double.class) return 0.0;
              return null;
            });
  }

  private SignPort bukkitSignPort() {
    return new SignPort() {
      public Optional<SignRecord> load(SignId id) {
        Block b = blockFor(id);
        if (b == null) return Optional.empty();
        var st = b.getState();
        if (!(st instanceof org.bukkit.block.TileState ts)) return Optional.empty();
        return PdcSignIdentity.read(ts);
      }

      public boolean hasPermission(PlayerId pid, String perm) {
        Player p = Bukkit.getPlayer(pid.uuid());
        return p != null && p.hasPermission(perm);
      }

      public Optional<FrontText> frontText(SignId id) {
        Block b = blockFor(id);
        if (b == null) return Optional.empty();
        var st = b.getState();
        if (!(st instanceof Sign s)) return Optional.empty();
        return Optional.of(new FrontText(s.getLine(0), s.getLine(1)));
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

  private SchedulerPort syncScheduler() {
    return new SchedulerPort() {
      public void runOnServerThread(Runnable r) {
        r.run();
      }

      public boolean isOnServerThread() {
        return true;
      }
    };
  }

  private Block createSign(String mode) {
    // create via listener to get PDC
    PlayerMock creator = server.addPlayer("creator-" + UUID.randomUUID());
    creator.addAttachment(MockBukkit.createMockPlugin(), "anvillink.create", true);
    Block block = world.getBlockAt(new Random().nextInt(100), 64, new Random().nextInt(100));
    block.setType(Material.OAK_SIGN);
    var sce = new SignChangeEvent(block, creator, new String[] {"[repair]", mode, "", ""});
    new SignLifecycleListener().onSignChange(sce);
    if (block.getState() instanceof Sign s) {
      s.setLine(0, sce.getLine(0));
      s.setLine(1, sce.getLine(1));
      s.update(true, false);
    }
    return block;
  }

  private static class CapturingMessage implements MessagePort {
    int calls;
    String lastKey;
    Map<String, String> lastPlaceholders;
    String toReturn = "rendered";

    @Override
    public String render(String k, Map<String, String> ph) {
      calls++;
      lastKey = k;
      lastPlaceholders = ph != null ? new HashMap<>(ph) : null;
      return toReturn;
    }
  }

  // --- tests ---

  @Test
  void paidHand_success_rendersOnceWithCount1AndPrice(@TempDir Path temp) throws Exception {
    File cfg =
        writeConfig(
            temp,
            "price:\n  hand: 12000.00\n  all: 25000.00\n"
                + "feedback:\n  enabled: true\n  sound: BLOCK_ANVIL_USE\n  particles: CRIT\n"
                + "admin:\n  target-distance: 8\n"
                + "messages:\n  repair-success: \"<green>Repaired {count} items for {price}.</green>\"\n");
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

    Block block = createSign("HAND");
    SignPort signs = bukkitSignPort();
    SchedulerPort scheduler = syncScheduler();
    BukkitEquipmentPort equipment = new BukkitEquipmentPort(uuid -> Bukkit.getPlayer(uuid));
    VaultEconomyGateway gateway = new VaultEconomyGateway(() -> server);
    CapturingMessage msg = new CapturingMessage();
    BukkitFeedbackAdapter feedback =
        new BukkitFeedbackAdapter(config, msg, scheduler, uuid -> Bukkit.getPlayer(uuid));

    PlayerMock user = server.addPlayer("user-hand");
    user.addAttachment(MockBukkit.createMockPlugin(), "anvillink.use", true);
    // damage main hand
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
    var activation =
        new io.github.danielxxomg.anvillink.domain.RepairActivation(
            signs, equipment, gateway, scheduler, config, (s, c, ctx) -> {});
    var result = activation.activate(signId, user.getUniqueId());
    assertInstanceOf(TransactionResult.Success.class, result);
    TransactionResult.Success success = (TransactionResult.Success) result;
    assertEquals(0, success.amount().compareTo(new BigDecimal("12000.00")));
    assertEquals(1, success.repairedCount());
    assertEquals(1, withdrawals.size());
    assertEquals(12000.0, withdrawals.get(0));

    // plugin wiring: gate on amount != ZERO
    if (success.amount().compareTo(BigDecimal.ZERO) != 0) {
      feedback.play(
          new SignPort.PlayerId(user.getUniqueId()), success.amount(), success.repairedCount());
    }
    assertEquals(1, msg.calls, "must render exactly once");
    assertEquals("repair-success", msg.lastKey);
    assertEquals("1", msg.lastPlaceholders.get("count"));
    // Vault gateway strips trailing zeros via BigDecimal.valueOf(double): success amount is 12000.0
    assertEquals(success.amount().toPlainString(), msg.lastPlaceholders.get("price"));
  }

  @Test
  void paidAll_threeSlots_count3(@TempDir Path temp) throws Exception {
    File cfg =
        writeConfig(
            temp,
            "price:\n  hand: 12000.00\n  all: 20000\n"
                + "feedback:\n  enabled: true\n"
                + "admin:\n  target-distance: 8\n"
                + "messages:\n  repair-success: \"<green>{count} {price}</green>\"\n");
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

    Block block = createSign("ALL");
    SignPort signs = bukkitSignPort();
    SchedulerPort scheduler = syncScheduler();
    BukkitEquipmentPort equipment = new BukkitEquipmentPort(uuid -> Bukkit.getPlayer(uuid));
    VaultEconomyGateway gateway = new VaultEconomyGateway(() -> server);
    CapturingMessage msg = new CapturingMessage();
    BukkitFeedbackAdapter feedback =
        new BukkitFeedbackAdapter(config, msg, scheduler, uuid -> Bukkit.getPlayer(uuid));

    PlayerMock user = server.addPlayer("user-all");
    user.addAttachment(MockBukkit.createMockPlugin(), "anvillink.use", true);
    var inv = Bukkit.getPlayer(user.getUniqueId()).getInventory();
    inv.setItemInMainHand(damaged(Material.DIAMOND_SWORD, 10));
    inv.setHelmet(damaged(Material.DIAMOND_HELMET, 5));
    inv.setChestplate(damaged(Material.DIAMOND_CHESTPLATE, 6));
    // other slots empty -> exactly 3 damaged for ALL

    var signId =
        new SignPort.SignId(
            block.getWorld().getName()
                + ":"
                + block.getX()
                + ","
                + block.getY()
                + ","
                + block.getZ());
    var activation =
        new io.github.danielxxomg.anvillink.domain.RepairActivation(
            signs, equipment, gateway, scheduler, config, (s, c, ctx) -> {});
    var result = activation.activate(signId, user.getUniqueId());
    assertInstanceOf(TransactionResult.Success.class, result);
    TransactionResult.Success success = (TransactionResult.Success) result;
    assertEquals(3, success.repairedCount());
    assertEquals(0, new BigDecimal("20000").compareTo(success.amount()));
    if (success.amount().compareTo(BigDecimal.ZERO) != 0) {
      feedback.play(
          new SignPort.PlayerId(user.getUniqueId()), success.amount(), success.repairedCount());
    }
    assertEquals(1, msg.calls);
    assertEquals("3", msg.lastPlaceholders.get("count"));
    assertEquals(success.amount().toPlainString(), msg.lastPlaceholders.get("price"));
  }

  @Test
  void emptyPlan_zeroNoRender(@TempDir Path temp) throws Exception {
    File cfg =
        writeConfig(
            temp,
            "price:\n  hand: 12000.00\n  all: 25000.00\n"
                + "feedback:\n  enabled: true\n"
                + "admin:\n  target-distance: 8\nmessages:\n  repair-success: \"ok\"\n");
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

    Block block = createSign("HAND");
    SignPort signs = bukkitSignPort();
    SchedulerPort scheduler = syncScheduler();
    BukkitEquipmentPort equipment = new BukkitEquipmentPort(uuid -> Bukkit.getPlayer(uuid));
    VaultEconomyGateway gateway = new VaultEconomyGateway(() -> server);
    CapturingMessage msg = new CapturingMessage();
    BukkitFeedbackAdapter feedback =
        new BukkitFeedbackAdapter(config, msg, scheduler, uuid -> Bukkit.getPlayer(uuid));

    PlayerMock user = server.addPlayer("user-empty");
    user.addAttachment(MockBukkit.createMockPlugin(), "anvillink.use", true);
    // no damaged items -> empty plan
    var signId =
        new SignPort.SignId(
            block.getWorld().getName()
                + ":"
                + block.getX()
                + ","
                + block.getY()
                + ","
                + block.getZ());
    var activation =
        new io.github.danielxxomg.anvillink.domain.RepairActivation(
            signs, equipment, gateway, scheduler, config, (s, c, ctx) -> {});
    var result = activation.activate(signId, user.getUniqueId());
    assertInstanceOf(TransactionResult.Success.class, result);
    assertEquals(0, ((TransactionResult.Success) result).amount().compareTo(BigDecimal.ZERO));
    assertEquals(0, ((TransactionResult.Success) result).repairedCount());
    assertTrue(withdrawals.isEmpty());
    // wiring must NOT call feedback when amount == ZERO
    TransactionResult.Success s = (TransactionResult.Success) result;
    if (s.amount().compareTo(BigDecimal.ZERO) != 0) {
      feedback.play(new SignPort.PlayerId(user.getUniqueId()), s.amount(), s.repairedCount());
    }
    assertEquals(0, msg.calls, "empty plan must not render");
  }

  @Test
  void disabled_silentEvenOnPaidSuccess(@TempDir Path temp) throws Exception {
    File cfg =
        writeConfig(
            temp,
            "price:\n  hand: 12000.00\n  all: 25000.00\n"
                + "feedback:\n  enabled: false\n"
                + "admin:\n  target-distance: 8\nmessages:\n  repair-success: \"ok\"\n");
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

    Block block = createSign("HAND");
    SignPort signs = bukkitSignPort();
    SchedulerPort scheduler = syncScheduler();
    BukkitEquipmentPort equipment = new BukkitEquipmentPort(uuid -> Bukkit.getPlayer(uuid));
    VaultEconomyGateway gateway = new VaultEconomyGateway(() -> server);
    CapturingMessage msg = new CapturingMessage();
    BukkitFeedbackAdapter feedback =
        new BukkitFeedbackAdapter(config, msg, scheduler, uuid -> Bukkit.getPlayer(uuid));

    PlayerMock user = server.addPlayer("user-disabled");
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
    var activation =
        new io.github.danielxxomg.anvillink.domain.RepairActivation(
            signs, equipment, gateway, scheduler, config, (s, c, ctx) -> {});
    var result = activation.activate(signId, user.getUniqueId());
    assertInstanceOf(TransactionResult.Success.class, result);
    TransactionResult.Success s = (TransactionResult.Success) result;
    // disabled adapter no-ops even when wiring calls it
    feedback.play(new SignPort.PlayerId(user.getUniqueId()), s.amount(), s.repairedCount());
    assertEquals(0, msg.calls, "disabled must be silent even on paid success");
    assertEquals(1, withdrawals.size());
  }

  @Test
  void throwSwallowed_transactionStillSuccessNoDeposit(@TempDir Path temp) throws Exception {
    File cfg =
        writeConfig(
            temp,
            "price:\n  hand: 12000.00\n  all: 25000.00\n"
                + "feedback:\n  enabled: true\n"
                + "admin:\n  target-distance: 8\nmessages:\n  repair-success: \"ok\"\n");
    FileConfigurationPort config = new FileConfigurationPort(cfg);
    List<Double> withdrawals = new ArrayList<>();
    List<Double> deposits = new ArrayList<>();
    Economy eco =
        (Economy)
            java.lang.reflect.Proxy.newProxyInstance(
                Economy.class.getClassLoader(),
                new Class<?>[] {Economy.class},
                (proxy, method, args) -> {
                  String n = method.getName();
                  if (n.equals("fractionalDigits")) return 2;
                  if (n.equals("withdrawPlayer") && args.length == 2) {
                    withdrawals.add((double) args[1]);
                    return new EconomyResponse(
                        (double) args[1], 1000, EconomyResponse.ResponseType.SUCCESS, null);
                  }
                  if (n.equals("depositPlayer") && args.length == 2) {
                    deposits.add((double) args[1]);
                    return new EconomyResponse(
                        (double) args[1], 1000, EconomyResponse.ResponseType.SUCCESS, null);
                  }
                  if (n.equals("isEnabled")) return true;
                  if (n.equals("getName")) return "MockEco";
                  if (n.equals("hasBankSupport")) return false;
                  Class<?> rt = method.getReturnType();
                  if (rt == boolean.class) return false;
                  if (rt == int.class) return 0;
                  if (rt == double.class) return 0.0;
                  return null;
                });
    server
        .getServicesManager()
        .register(
            Economy.class,
            eco,
            MockBukkit.createMockPlugin(),
            org.bukkit.plugin.ServicePriority.Highest);

    Block block = createSign("HAND");
    SignPort signs = bukkitSignPort();
    SchedulerPort scheduler = syncScheduler();
    BukkitEquipmentPort equipment = new BukkitEquipmentPort(uuid -> Bukkit.getPlayer(uuid));
    VaultEconomyGateway gateway = new VaultEconomyGateway(() -> server);
    MessagePort throwing =
        (k, ph) -> {
          throw new RuntimeException("render boom");
        };
    BukkitFeedbackAdapter feedback =
        new BukkitFeedbackAdapter(config, throwing, scheduler, uuid -> Bukkit.getPlayer(uuid));

    PlayerMock user = server.addPlayer("user-throw");
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
    var activation =
        new io.github.danielxxomg.anvillink.domain.RepairActivation(
            signs, equipment, gateway, scheduler, config, (s, c, ctx) -> {});
    var result = activation.activate(signId, user.getUniqueId());
    assertInstanceOf(TransactionResult.Success.class, result);
    TransactionResult.Success s = (TransactionResult.Success) result;
    // feedback throw must be swallowed, transaction remains Success, no compensating deposit
    assertDoesNotThrow(
        () ->
            feedback.play(
                new SignPort.PlayerId(user.getUniqueId()), s.amount(), s.repairedCount()));
    assertEquals(1, withdrawals.size());
    assertTrue(deposits.isEmpty(), "feedback throw must not trigger compensating deposit");
  }

  @Test
  void singleWithdrawal_preservedForFeedbackPath(@TempDir Path temp) throws Exception {
    File cfg =
        writeConfig(
            temp,
            "price:\n  hand: 12000.00\n  all: 25000.00\n"
                + "feedback:\n  enabled: true\n"
                + "admin:\n  target-distance: 8\nmessages:\n  repair-success: \"ok\"\n");
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

    Block block = createSign("HAND");
    SignPort signs = bukkitSignPort();
    SchedulerPort scheduler = syncScheduler();
    BukkitEquipmentPort equipment = new BukkitEquipmentPort(uuid -> Bukkit.getPlayer(uuid));
    VaultEconomyGateway gateway = new VaultEconomyGateway(() -> server);
    CapturingMessage msg = new CapturingMessage();
    BukkitFeedbackAdapter feedback =
        new BukkitFeedbackAdapter(config, msg, scheduler, uuid -> Bukkit.getPlayer(uuid));

    PlayerMock user = server.addPlayer("user-once");
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
    var activation =
        new io.github.danielxxomg.anvillink.domain.RepairActivation(
            signs, equipment, gateway, scheduler, config, (s, c, ctx) -> {});
    var result = activation.activate(signId, user.getUniqueId());
    TransactionResult.Success s = (TransactionResult.Success) result;
    feedback.play(new SignPort.PlayerId(user.getUniqueId()), s.amount(), s.repairedCount());
    assertEquals(1, withdrawals.size(), "single withdrawal even with feedback");
    assertEquals(1, msg.calls);
  }
}
