// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.integration;

import static org.junit.jupiter.api.Assertions.*;

import io.github.danielxxomg.anvillink.adapter.BukkitEquipmentPort;
import io.github.danielxxomg.anvillink.adapter.FileConfigurationPort;
import io.github.danielxxomg.anvillink.adapter.PdcSignIdentity;
import io.github.danielxxomg.anvillink.adapter.SignLifecycleListener;
import io.github.danielxxomg.anvillink.adapter.VaultEconomyGateway;
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

class PricePerModeE2ETest {

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
              Class<?> rt = method.getReturnType();
              if (rt == boolean.class) return false;
              if (rt == int.class) return 0;
              if (rt == double.class) return 0.0;
              return null;
            });
  }

  private SignPort bukkitSignPort() {
    return new SignPort() {
      public Optional<io.github.danielxxomg.anvillink.domain.SignRecord> load(SignId id) {
        Block b = blockFor(id);
        if (b == null) return Optional.empty();
        var st = b.getState();
        if (!(st instanceof org.bukkit.block.TileState ts)) return Optional.empty();
        return PdcSignIdentity.read(ts);
      }

      public boolean hasPermission(PlayerId pid, String perm) {
        var p = Bukkit.getPlayer(pid.uuid());
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

  private Block createSign(String mode, int offset) {
    PlayerMock creator = server.addPlayer("creator-" + UUID.randomUUID());
    creator.addAttachment(MockBukkit.createMockPlugin(), "anvillink.create", true);
    Block block = world.getBlockAt(offset, 64, offset);
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

  @Test
  void scalarPrice_startupDisabledNoRepair(@TempDir Path temp) throws Exception {
    File cfg =
        writeConfig(temp, "price: 25.00\nadmin:\n  target-distance: 8\nmessages:\n  g: hi\n");
    FileConfigurationPort config = new FileConfigurationPort(cfg);
    assertFalse(config.current().activationEnabled(), "scalar must fail-closed at startup");

    Block block = createSign("HAND", 10);
    List<Double> withdrawals = new ArrayList<>();
    Economy eco = economySuccess(2, withdrawals);
    server
        .getServicesManager()
        .register(
            Economy.class,
            eco,
            MockBukkit.createMockPlugin(),
            org.bukkit.plugin.ServicePriority.Highest);
    SignPort signs = bukkitSignPort();
    SchedulerPort scheduler = syncScheduler();
    BukkitEquipmentPort equipment = new BukkitEquipmentPort(uuid -> Bukkit.getPlayer(uuid));
    VaultEconomyGateway gateway = new VaultEconomyGateway(() -> server);
    PlayerMock user = server.addPlayer("scalar-user");
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
    assertInstanceOf(TransactionResult.InvalidResponse.class, result);
    assertTrue(
        ((TransactionResult.InvalidResponse) result).reason().contains("activation-disabled"));
    assertTrue(withdrawals.isEmpty(), "disabled activation must not withdraw");
  }

  @Test
  void reloadFromValidToScalar_retainsPriorPrices(@TempDir Path temp) throws Exception {
    Path file = temp.resolve("config.yml");
    Files.writeString(
        file,
        "price:\n  hand: 12000.00\n  all: 25000.00\nadmin:\n  target-distance: 8\nmessages:\n  g: hi\n",
        StandardCharsets.UTF_8);
    FileConfigurationPort config = new FileConfigurationPort(file.toFile());
    assertTrue(config.current().activationEnabled());
    assertEquals(new BigDecimal("12000.00"), config.current().priceHand());

    Files.writeString(
        file,
        "price: 25.00\nadmin:\n  target-distance: 8\nmessages:\n  g: hi\n",
        StandardCharsets.UTF_8);
    var outcome = config.reload();
    assertInstanceOf(ConfigurationPort.ReloadOutcome.Failure.class, outcome);
    assertEquals(new BigDecimal("12000.00"), config.current().priceHand());
    assertEquals(new BigDecimal("25000.00"), config.current().priceAll());
    assertTrue(
        config.current().activationEnabled(), "failed reload must retain prior valid snapshot");
  }

  @Test
  void handWithdrawsHandAllWithdrawsAll(@TempDir Path temp) throws Exception {
    File cfg =
        writeConfig(
            temp,
            "price:\n  hand: 10000\n  all: 20000\nadmin:\n  target-distance: 8\nmessages:\n  g: hi\n");
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

    // HAND sign
    Block handBlock = createSign("HAND", 11);
    SignPort signs = bukkitSignPort();
    SchedulerPort scheduler = syncScheduler();
    BukkitEquipmentPort equipment = new BukkitEquipmentPort(uuid -> Bukkit.getPlayer(uuid));
    VaultEconomyGateway gateway = new VaultEconomyGateway(() -> server);
    PlayerMock handUser = server.addPlayer("hand-user");
    handUser.addAttachment(MockBukkit.createMockPlugin(), "anvillink.use", true);
    Bukkit.getPlayer(handUser.getUniqueId())
        .getInventory()
        .setItemInMainHand(damaged(Material.DIAMOND_SWORD, 10));
    var handId =
        new SignPort.SignId(
            handBlock.getWorld().getName()
                + ":"
                + handBlock.getX()
                + ","
                + handBlock.getY()
                + ","
                + handBlock.getZ());
    var handAct =
        new io.github.danielxxomg.anvillink.domain.RepairActivation(
            signs, equipment, gateway, scheduler, config, (s, c, ctx) -> {});
    var handResult = handAct.activate(handId, handUser.getUniqueId());
    assertInstanceOf(TransactionResult.Success.class, handResult);
    assertEquals(
        0, ((TransactionResult.Success) handResult).amount().compareTo(new BigDecimal("10000")));
    assertEquals(1, withdrawals.size());
    assertEquals(10000.0, withdrawals.get(0));

    withdrawals.clear();

    // ALL sign
    Block allBlock = createSign("ALL", 12);
    PlayerMock allUser = server.addPlayer("all-user");
    allUser.addAttachment(MockBukkit.createMockPlugin(), "anvillink.use", true);
    var inv = Bukkit.getPlayer(allUser.getUniqueId()).getInventory();
    inv.setItemInMainHand(damaged(Material.DIAMOND_SWORD, 5));
    inv.setHelmet(damaged(Material.DIAMOND_HELMET, 5));
    // 2 damaged for ALL
    var allId =
        new SignPort.SignId(
            allBlock.getWorld().getName()
                + ":"
                + allBlock.getX()
                + ","
                + allBlock.getY()
                + ","
                + allBlock.getZ());
    var allAct =
        new io.github.danielxxomg.anvillink.domain.RepairActivation(
            signs, equipment, gateway, scheduler, config, (s, c, ctx) -> {});
    var allResult = allAct.activate(allId, allUser.getUniqueId());
    assertInstanceOf(TransactionResult.Success.class, allResult);
    assertEquals(
        0, ((TransactionResult.Success) allResult).amount().compareTo(new BigDecimal("20000")));
    assertEquals(1, withdrawals.size());
    assertEquals(20000.0, withdrawals.get(0));
  }

  @Test
  void emptyPlan_noCharge(@TempDir Path temp) throws Exception {
    File cfg =
        writeConfig(
            temp,
            "price:\n  hand: 12000.00\n  all: 25000.00\nadmin:\n  target-distance: 8\nmessages:\n  g: hi\n");
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

    Block block = createSign("HAND", 13);
    SignPort signs = bukkitSignPort();
    SchedulerPort scheduler = syncScheduler();
    BukkitEquipmentPort equipment = new BukkitEquipmentPort(uuid -> Bukkit.getPlayer(uuid));
    VaultEconomyGateway gateway = new VaultEconomyGateway(() -> server);
    PlayerMock user = server.addPlayer("empty-user");
    user.addAttachment(MockBukkit.createMockPlugin(), "anvillink.use", true);
    // no damaged items
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
    assertTrue(withdrawals.isEmpty(), "empty plan must not charge");
  }

  @Test
  void validReloadSwapsPrices(@TempDir Path temp) throws Exception {
    Path file = temp.resolve("config.yml");
    Files.writeString(
        file,
        "price:\n  hand: 12000.00\n  all: 25000.00\nadmin:\n  target-distance: 8\nmessages:\n  g: hi\n",
        StandardCharsets.UTF_8);
    FileConfigurationPort config = new FileConfigurationPort(file.toFile());
    Files.writeString(
        file,
        "price:\n  hand: 15000.00\n  all: 30000.00\nadmin:\n  target-distance: 8\nmessages:\n  g: hi\n",
        StandardCharsets.UTF_8);
    var outcome = config.reload();
    assertInstanceOf(ConfigurationPort.ReloadOutcome.Success.class, outcome);
    assertEquals(new BigDecimal("15000.00"), config.current().priceHand());
    assertEquals(new BigDecimal("30000.00"), config.current().priceAll());
  }
}
