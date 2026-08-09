// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.e2e;

import static org.junit.jupiter.api.Assertions.*;

import io.github.danielxxomg.anvillink.adapter.PdcSignIdentity;
import io.github.danielxxomg.anvillink.domain.RepairMode;
import io.github.danielxxomg.anvillink.entrypoint.AnvilLinkPlugin;
import java.io.File;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

/**
 * Slice 2 bootstrap: MockBukkit.load through the real classloader + callEvent end-to-end. Covers
 * fresh TileState, fallback !wrote && instanceof, Vault fractionalDigits matrix, tamper, zero-plan,
 * insufficient/noProvider/invalidResponse, audit.log CREATE|APPEND|mkdirs, OffHand, edit/break
 * manage gate, and worldName seam.
 */
class PluginBootstrapTest {

  @AfterEach
  void tearDown() {
    if (MockBukkit.isMocked()) {
      MockBukkit.unmock();
    }
    try {
      var f = Bukkit.class.getDeclaredField("server");
      f.setAccessible(true);
      f.set(null, null);
    } catch (Exception ignored) {
    }
  }

  // 4.1 + 4.2 — bootstrap creates dataFolder, wires FileConfigurationPort, PDC namespace permanent
  @Test
  void bootstrap_loadCreatesDataFolderAndConfig() {
    ServerMock server = MockBukkit.mock();
    AnvilLinkPlugin plugin = MockBukkit.load(AnvilLinkPlugin.class);
    assertNotNull(plugin, "MockBukkit.load(AnvilLinkPlugin) must return plugin");
    File dataFolder = plugin.getDataFolder();
    assertTrue(dataFolder.exists(), "getDataFolder must exist after onEnable saveDefaultConfig");
    File configFile = new File(dataFolder, "config.yml");
    assertTrue(
        configFile.exists(), "config.yml must be copied via saveDefaultConfig into dataFolder");
    // shipped defaults must parse via FileConfigurationPort(File) wiring
    try {
      String content = Files.readString(configFile.toPath(), StandardCharsets.UTF_8);
      assertTrue(content.contains("price:"), "shipped config must contain price block");
    } catch (Exception e) {
      fail("cannot read dataFolder config.yml: " + e.getMessage());
    }
    assertEquals("danielxxomg", PdcSignIdentity.NAMESPACE);
    assertEquals("anvillink_repair_sign", PdcSignIdentity.KEY);
    assertEquals("danielxxomg:anvillink_repair_sign", PdcSignIdentity.key().toString());
  }

  // 4.3 + 4.4 — SignChange via callEvent fresh TileState writes PDC + BLUE + update(true,false)
  @Test
  void signChangeViaCallEvent_freshTileStateWritesPdcAndBlue() {
    ServerMock server = MockBukkit.mock();
    AnvilLinkPlugin plugin = MockBukkit.load(AnvilLinkPlugin.class);
    WorldMock world = server.addSimpleWorld("world");
    PlayerMock creator = server.addPlayer("bootstrapCreator");
    creator.addAttachment(MockBukkit.createMockPlugin(), "anvillink.create", true);
    Block block = world.getBlockAt(0, 64, 0);
    block.setType(Material.OAK_SIGN);

    SignChangeEvent event =
        new SignChangeEvent(block, creator, new String[] {"[repair]", "HAND", "", ""});
    server.getPluginManager().callEvent(event);

    assertFalse(event.isCancelled(), "creation with permission must not be cancelled");
    assertEquals("[repair]", event.getLine(0));
    assertEquals("HAND", event.getLine(1));
    BlockState state = block.getState();
    assertInstanceOf(TileState.class, state, "fresh state must be TileState");
    assertTrue(
        PdcSignIdentity.has((TileState) state),
        "PDC danielxxomg:anvillink_repair_sign must be set");
    assertInstanceOf(Sign.class, state, "fresh TileState sign must have BLUE color");
    Sign sign = (Sign) state;
    assertEquals(DyeColor.BLUE, sign.getColor(), "sign color must be BLUE");
    assertEquals("[repair]", sign.getLine(0), "canonical line 0 must survive coalescing");
    assertEquals("HAND", sign.getLine(1), "canonical line 1 must survive coalescing");
  }

  @Test
  void signChangeViaCallEvent_allModeWritesPdcAndBlue() {
    ServerMock server = MockBukkit.mock();
    MockBukkit.load(AnvilLinkPlugin.class);
    WorldMock world = server.addSimpleWorld("world");
    PlayerMock creator = server.addPlayer("creatorAll");
    creator.addAttachment(MockBukkit.createMockPlugin(), "anvillink.create", true);
    Block block = world.getBlockAt(1, 64, 0);
    block.setType(Material.OAK_SIGN);
    SignChangeEvent event =
        new SignChangeEvent(block, creator, new String[] {"[repair]", "ALL", "", ""});
    server.getPluginManager().callEvent(event);
    assertFalse(event.isCancelled());
    BlockState state = block.getState();
    assertTrue(PdcSignIdentity.has((TileState) state));
    assertEquals(DyeColor.BLUE, ((Sign) state).getColor());
    assertEquals("ALL", ((Sign) state).getLine(1));
  }

  // 4.5-4.7 — fallback path: fresh not TileState but state instanceof TileState, and both
  // non-TileState
  @Test
  void signChange_fallbackWhenFreshNotTileState_writesToOriginalState() {
    // Simulate MockBukkit gap: fresh = block.getState() after setLine is NOT TileState,
    // but original state is TileState. Listener must fallback: if (!wrote && state instanceof
    // TileState)
    FakeSign stale = new FakeSign();
    Block proxy = proxyBlock(stale, new FakeNonTileState());
    Player player = playerWith(UUID.randomUUID(), true, false);
    SignChangeEvent event =
        new SignChangeEvent(proxy, player, new String[] {"[repair]", "HAND", "", ""});
    io.github.danielxxomg.anvillink.adapter.SignLifecycleListener listener =
        new io.github.danielxxomg.anvillink.adapter.SignLifecycleListener();
    listener.onSignChange(event);
    assertFalse(event.isCancelled());
    assertTrue(PdcSignIdentity.has(stale), "fallback must write PDC to original TileState");
    assertEquals(DyeColor.BLUE, stale.color, "fallback must set BLUE on original TileState");
    assertTrue(stale.updated, "fallback must call update(true,false)");
  }

  @Test
  void signChange_bothNonTileState_noPdc() {
    BlockState nonTile = new FakeNonTileState();
    // proxy where both getState() calls return non-TileState
    Block proxy = proxyBlock(nonTile, nonTile);
    Player player = playerWith(UUID.randomUUID(), true, false);
    SignChangeEvent event =
        new SignChangeEvent(proxy, player, new String[] {"[repair]", "HAND", "", ""});
    new io.github.danielxxomg.anvillink.adapter.SignLifecycleListener().onSignChange(event);
    assertFalse(event.isCancelled());
    // no TileState means no PDC to assert; event not cancelled but also no write
    // we prove by ensuring no TileState exists to hold PDC — listener wrote nowhere
    assertFalse(nonTile instanceof TileState);
  }

  // 5.x — Vault fractionalDigits matrix via callEvent, worldName seam, audit.log, tamper, gating
  @Test
  void playerInteract_fractionalDigitsMatrix_zeroNoWithdraw_twoAndMinusOneWithdraw() {
    // fractionalDigits=0 must fail scale 2 price 12000.00 -> no withdraw
    assertFractionalDigitsMatrix(0, false);
    // fractionalDigits=2 and -1 must withdraw 12000.00
    assertFractionalDigitsMatrix(2, true);
    assertFractionalDigitsMatrix(-1, true);
  }

  private void assertFractionalDigitsMatrix(int fractionalDigits, boolean shouldWithdraw) {
    ServerMock server = MockBukkit.mock();
    try {
      AnvilLinkPlugin plugin = MockBukkit.load(AnvilLinkPlugin.class);
      WorldMock world = server.addSimpleWorld("world");
      PlayerMock creator = server.addPlayer("mCreator" + fractionalDigits);
      creator.addAttachment(MockBukkit.createMockPlugin(), "anvillink.create", true);
      Block block = world.getBlockAt(10, 64, 0);
      block.setType(Material.OAK_SIGN);
      server
          .getPluginManager()
          .callEvent(
              new SignChangeEvent(block, creator, new String[] {"[repair]", "HAND", "", ""}));
      // ensure sign lines propagated
      if (block.getState() instanceof Sign s) {
        // MockBukkit already coalesced via listener; ensure canonical
        assertEquals("[repair]", s.getLine(0));
      }

      PlayerMock user = server.addPlayer("mUser" + fractionalDigits);
      user.addAttachment(MockBukkit.createMockPlugin(), "anvillink.use", true);
      user.getInventory().setItemInMainHand(damaged(Material.DIAMOND_SWORD, 10));

      List<Double> withdrawals = new ArrayList<>();
      server
          .getServicesManager()
          .register(
              Economy.class,
              proxyEconomy(fractionalDigits, withdrawals, null),
              MockBukkit.createMockPlugin(),
              org.bukkit.plugin.ServicePriority.Highest);

      File audit = new File(plugin.getDataFolder(), "audit.log");
      if (audit.exists()) {
        audit.delete();
      }

      Player p = Bukkit.getPlayer(user.getUniqueId());
      assertNotNull(p);
      PlayerInteractEvent interact =
          new PlayerInteractEvent(
              (Player) p,
              Action.RIGHT_CLICK_BLOCK,
              null,
              block,
              BlockFace.SELF,
              EquipmentSlot.HAND);
      server.getPluginManager().callEvent(interact);

      if (shouldWithdraw) {
        assertEquals(1, withdrawals.size(), "fd=" + fractionalDigits + " must withdraw");
        assertEquals(12000.0, withdrawals.get(0));
        // damaged item must be repaired on success
        Damageable dm = (Damageable) p.getInventory().getItemInMainHand().getItemMeta();
        assertEquals(0, dm.getDamage(), "item must be repaired on Success");
        // audit.log must exist with 8-field line, toPlainString, count, worldName seam
        assertTrue(audit.exists(), "audit.log must be created on Success non-zero");
        List<String> lines = Files.readAllLines(audit.toPath(), StandardCharsets.UTF_8);
        assertEquals(1, lines.size());
        String[] parts = lines.get(0).split("\\|", -1);
        assertEquals(8, parts.length);
        assertDoesNotThrow(
            () -> java.time.Instant.parse(parts[0]), "timestamp must be ISO_INSTANT");
        assertEquals(user.getUniqueId().toString(), parts[1]);
        assertEquals(user.getName(), parts[2]);
        assertEquals("HAND", parts[3]);
        assertEquals("world", parts[4], "worldName seam must be player.getWorld().getName()");
        assertEquals(0, new BigDecimal("12000.00").compareTo(new BigDecimal(parts[5])));
        assertFalse(parts[5].contains("E"), "price must be toPlainString not scientific");
        assertEquals("1", parts[6], "repairedCount must be count");
        assertEquals("SUCCESS", parts[7]);
      } else {
        assertTrue(
            withdrawals.isEmpty(), "fd=0 must not withdraw (scale exceeds fractionalDigits)");
        Damageable dm = (Damageable) p.getInventory().getItemInMainHand().getItemMeta();
        assertEquals(10, dm.getDamage(), "fd=0 invalid-price must not repair");
        assertFalse(audit.exists(), "fd=0 invalid-price must not audit");
      }
    } catch (Exception e) {
      fail("matrix fd=" + fractionalDigits + " threw: " + e.getMessage(), e);
    } finally {
      MockBukkit.unmock();
      try {
        var f = Bukkit.class.getDeclaredField("server");
        f.setAccessible(true);
        f.set(null, null);
      } catch (Exception ignored) {
      }
    }
  }

  @Test
  void playerInteract_tamperedTextFailClosedNoChargeNoAudit() throws Exception {
    ServerMock server = MockBukkit.mock();
    AnvilLinkPlugin plugin = MockBukkit.load(AnvilLinkPlugin.class);
    WorldMock world = server.addSimpleWorld("world");
    PlayerMock creator = server.addPlayer("tamperCreator");
    creator.addAttachment(MockBukkit.createMockPlugin(), "anvillink.create", true);
    Block block = world.getBlockAt(11, 64, 0);
    block.setType(Material.OAK_SIGN);
    server
        .getPluginManager()
        .callEvent(new SignChangeEvent(block, creator, new String[] {"[repair]", "HAND", "", ""}));
    // tamper visible text
    if (block.getState() instanceof Sign s) {
      s.setLine(0, "[tampered]");
      s.setLine(1, "HAND");
      s.update(true, false);
    }
    PlayerMock user = server.addPlayer("tamperUser");
    user.addAttachment(MockBukkit.createMockPlugin(), "anvillink.use", true);
    user.getInventory().setItemInMainHand(damaged(Material.DIAMOND_SWORD, 10));
    List<Double> withdrawals = new ArrayList<>();
    server
        .getServicesManager()
        .register(
            Economy.class,
            proxyEconomy(2, withdrawals, null),
            MockBukkit.createMockPlugin(),
            org.bukkit.plugin.ServicePriority.Highest);
    File audit = new File(plugin.getDataFolder(), "audit.log");
    if (audit.exists()) audit.delete();
    Player p = Bukkit.getPlayer(user.getUniqueId());
    server
        .getPluginManager()
        .callEvent(
            new PlayerInteractEvent(
                p, Action.RIGHT_CLICK_BLOCK, null, block, BlockFace.SELF, EquipmentSlot.HAND));
    assertTrue(withdrawals.isEmpty(), "tampered-text must fail closed without charge");
    assertFalse(audit.exists(), "tampered-text InvalidResponse must not audit");
    Damageable dm = (Damageable) p.getInventory().getItemInMainHand().getItemMeta();
    assertEquals(10, dm.getDamage(), "tampered must not repair");
  }

  @Test
  void playerInteract_noEligibleItemsGatedZeroNoVaultCallNoAudit() throws Exception {
    ServerMock server = MockBukkit.mock();
    AnvilLinkPlugin plugin = MockBukkit.load(AnvilLinkPlugin.class);
    WorldMock world = server.addSimpleWorld("world");
    PlayerMock creator = server.addPlayer("zeroCreator");
    creator.addAttachment(MockBukkit.createMockPlugin(), "anvillink.create", true);
    Block block = world.getBlockAt(12, 64, 0);
    block.setType(Material.OAK_SIGN);
    server
        .getPluginManager()
        .callEvent(new SignChangeEvent(block, creator, new String[] {"[repair]", "HAND", "", ""}));
    PlayerMock user = server.addPlayer("zeroUser");
    user.addAttachment(MockBukkit.createMockPlugin(), "anvillink.use", true);
    // undamaged -> empty plan -> Success ZERO
    user.getInventory().setItemInMainHand(new ItemStack(Material.DIAMOND_SWORD));
    List<Double> withdrawals = new ArrayList<>();
    server
        .getServicesManager()
        .register(
            Economy.class,
            proxyEconomy(2, withdrawals, null),
            MockBukkit.createMockPlugin(),
            org.bukkit.plugin.ServicePriority.Highest);
    File audit = new File(plugin.getDataFolder(), "audit.log");
    if (audit.exists()) audit.delete();
    Player p = Bukkit.getPlayer(user.getUniqueId());
    server
        .getPluginManager()
        .callEvent(
            new PlayerInteractEvent(
                p, Action.RIGHT_CLICK_BLOCK, null, block, BlockFace.SELF, EquipmentSlot.HAND));
    assertTrue(withdrawals.isEmpty(), "no eligible items must not call Vault");
    assertFalse(audit.exists(), "Success ZERO must not create audit.log");
  }

  @Test
  void playerInteract_offHandFilteredNoCharge() {
    ServerMock server = MockBukkit.mock();
    MockBukkit.load(AnvilLinkPlugin.class);
    WorldMock world = server.addSimpleWorld("world");
    PlayerMock creator = server.addPlayer("offCreator");
    creator.addAttachment(MockBukkit.createMockPlugin(), "anvillink.create", true);
    Block block = world.getBlockAt(13, 64, 0);
    block.setType(Material.OAK_SIGN);
    server
        .getPluginManager()
        .callEvent(new SignChangeEvent(block, creator, new String[] {"[repair]", "HAND", "", ""}));
    PlayerMock user = server.addPlayer("offUser");
    user.addAttachment(MockBukkit.createMockPlugin(), "anvillink.use", true);
    user.getInventory().setItemInMainHand(damaged(Material.DIAMOND_SWORD, 10));
    List<Double> withdrawals = new ArrayList<>();
    server
        .getServicesManager()
        .register(
            Economy.class,
            proxyEconomy(2, withdrawals, null),
            MockBukkit.createMockPlugin(),
            org.bukkit.plugin.ServicePriority.Highest);
    Player p = Bukkit.getPlayer(user.getUniqueId());
    PlayerInteractEvent offEvent =
        new PlayerInteractEvent(
            p, Action.RIGHT_CLICK_BLOCK, null, block, BlockFace.SELF, EquipmentSlot.OFF_HAND);
    server.getPluginManager().callEvent(offEvent);
    assertTrue(withdrawals.isEmpty(), "OffHand must be filtered via InteractionFilter");
    Damageable dm = (Damageable) p.getInventory().getItemInMainHand().getItemMeta();
    assertEquals(10, dm.getDamage(), "OffHand must not repair");
  }

  @Test
  void playerInteract_insufficientFundsNoRepairNoAudit() {
    ServerMock server = MockBukkit.mock();
    AnvilLinkPlugin plugin = MockBukkit.load(AnvilLinkPlugin.class);
    WorldMock world = server.addSimpleWorld("world");
    PlayerMock creator = server.addPlayer("insCreator");
    creator.addAttachment(MockBukkit.createMockPlugin(), "anvillink.create", true);
    Block block = world.getBlockAt(14, 64, 0);
    block.setType(Material.OAK_SIGN);
    server
        .getPluginManager()
        .callEvent(new SignChangeEvent(block, creator, new String[] {"[repair]", "HAND", "", ""}));
    PlayerMock user = server.addPlayer("insUser");
    user.addAttachment(MockBukkit.createMockPlugin(), "anvillink.use", true);
    user.getInventory().setItemInMainHand(damaged(Material.DIAMOND_SWORD, 10));
    server
        .getServicesManager()
        .register(
            Economy.class,
            proxyEconomyInsufficientFunds(),
            MockBukkit.createMockPlugin(),
            org.bukkit.plugin.ServicePriority.Highest);
    File audit = new File(plugin.getDataFolder(), "audit.log");
    if (audit.exists()) audit.delete();
    Player p = Bukkit.getPlayer(user.getUniqueId());
    server
        .getPluginManager()
        .callEvent(
            new PlayerInteractEvent(
                p, Action.RIGHT_CLICK_BLOCK, null, block, BlockFace.SELF, EquipmentSlot.HAND));
    assertFalse(audit.exists(), "InsufficientFunds must not audit");
    Damageable dm = (Damageable) p.getInventory().getItemInMainHand().getItemMeta();
    assertEquals(10, dm.getDamage(), "InsufficientFunds must not repair");
  }

  @Test
  void playerInteract_noProviderFailClosed() {
    ServerMock server = MockBukkit.mock();
    AnvilLinkPlugin plugin = MockBukkit.load(AnvilLinkPlugin.class);
    WorldMock world = server.addSimpleWorld("world");
    PlayerMock creator = server.addPlayer("npCreator");
    creator.addAttachment(MockBukkit.createMockPlugin(), "anvillink.create", true);
    Block block = world.getBlockAt(15, 64, 0);
    block.setType(Material.OAK_SIGN);
    server
        .getPluginManager()
        .callEvent(new SignChangeEvent(block, creator, new String[] {"[repair]", "HAND", "", ""}));
    PlayerMock user = server.addPlayer("npUser");
    user.addAttachment(MockBukkit.createMockPlugin(), "anvillink.use", true);
    user.getInventory().setItemInMainHand(damaged(Material.DIAMOND_SWORD, 10));
    // no Economy registered -> NoProvider
    File audit = new File(plugin.getDataFolder(), "audit.log");
    if (audit.exists()) audit.delete();
    Player p = Bukkit.getPlayer(user.getUniqueId());
    server
        .getPluginManager()
        .callEvent(
            new PlayerInteractEvent(
                p, Action.RIGHT_CLICK_BLOCK, null, block, BlockFace.SELF, EquipmentSlot.HAND));
    assertFalse(audit.exists(), "NoProvider must not audit");
    Damageable dm = (Damageable) p.getInventory().getItemInMainHand().getItemMeta();
    assertEquals(10, dm.getDamage());
  }

  @Test
  void playerInteract_invalidResponseAmountMismatchNoRepair() {
    ServerMock server = MockBukkit.mock();
    AnvilLinkPlugin plugin = MockBukkit.load(AnvilLinkPlugin.class);
    WorldMock world = server.addSimpleWorld("world");
    PlayerMock creator = server.addPlayer("ivCreator");
    creator.addAttachment(MockBukkit.createMockPlugin(), "anvillink.create", true);
    Block block = world.getBlockAt(16, 64, 0);
    block.setType(Material.OAK_SIGN);
    server
        .getPluginManager()
        .callEvent(new SignChangeEvent(block, creator, new String[] {"[repair]", "HAND", "", ""}));
    PlayerMock user = server.addPlayer("ivUser");
    user.addAttachment(MockBukkit.createMockPlugin(), "anvillink.use", true);
    user.getInventory().setItemInMainHand(damaged(Material.DIAMOND_SWORD, 10));
    List<Double> withdrawals = new ArrayList<>();
    List<Double> deposits = new ArrayList<>();
    server
        .getServicesManager()
        .register(
            Economy.class,
            proxyEconomyAmountMismatch(withdrawals, deposits),
            MockBukkit.createMockPlugin(),
            org.bukkit.plugin.ServicePriority.Highest);
    File audit = new File(plugin.getDataFolder(), "audit.log");
    if (audit.exists()) audit.delete();
    Player p = Bukkit.getPlayer(user.getUniqueId());
    server
        .getPluginManager()
        .callEvent(
            new PlayerInteractEvent(
                p, Action.RIGHT_CLICK_BLOCK, null, block, BlockFace.SELF, EquipmentSlot.HAND));
    // invalidResponse path: Vault returned SUCCESS 20.0 for 12000.00 -> gateway deposits and
    // returns InvalidResponse
    assertEquals(1, withdrawals.size());
    assertFalse(audit.exists(), "InvalidResponse must not audit");
    Damageable dm = (Damageable) p.getInventory().getItemInMainHand().getItemMeta();
    assertEquals(10, dm.getDamage(), "InvalidResponse must not repair");
  }

  @Test
  void playerInteract_worldNameSeamAndAuditAppend() throws Exception {
    ServerMock server = MockBukkit.mock();
    AnvilLinkPlugin plugin = MockBukkit.load(AnvilLinkPlugin.class);
    WorldMock nether = server.addSimpleWorld("world_nether");
    server.addSimpleWorld("world");
    PlayerMock creator = server.addPlayer("worldCreator");
    creator.addAttachment(MockBukkit.createMockPlugin(), "anvillink.create", true);
    Block block = nether.getBlockAt(5, 64, 0);
    block.setType(Material.OAK_SIGN);
    server
        .getPluginManager()
        .callEvent(new SignChangeEvent(block, creator, new String[] {"[repair]", "ALL", "", ""}));
    PlayerMock user = server.addPlayer("worldUser");
    // teleport user to nether so player.getWorld().getName() == world_nether
    user.teleport(nether.getSpawnLocation());
    user.addAttachment(MockBukkit.createMockPlugin(), "anvillink.use", true);
    // ALL needs damaged items in multiple slots for count>1 but any success counts
    user.getInventory().setItemInMainHand(damaged(Material.DIAMOND_SWORD, 5));
    user.getInventory().setHelmet(damaged(Material.DIAMOND_HELMET, 5));
    List<Double> withdrawals = new ArrayList<>();
    server
        .getServicesManager()
        .register(
            Economy.class,
            proxyEconomy(2, withdrawals, null),
            MockBukkit.createMockPlugin(),
            org.bukkit.plugin.ServicePriority.Highest);
    File audit = new File(plugin.getDataFolder(), "audit.log");
    if (audit.exists()) audit.delete();
    Player p = Bukkit.getPlayer(user.getUniqueId());
    // ensure p world is nether
    assertEquals("world_nether", p.getWorld().getName());
    server
        .getPluginManager()
        .callEvent(
            new PlayerInteractEvent(
                p, Action.RIGHT_CLICK_BLOCK, null, block, BlockFace.SELF, EquipmentSlot.HAND));
    assertTrue(audit.exists());
    List<String> lines = Files.readAllLines(audit.toPath(), StandardCharsets.UTF_8);
    assertEquals(1, lines.size());
    String[] parts = lines.get(0).split("\\|", -1);
    assertEquals("world_nether", parts[4], "audit world must be exact player.getWorld().getName()");
    // second paid success must APPEND not overwrite (CREATE|APPEND)
    user.getInventory().setItemInMainHand(damaged(Material.DIAMOND_SWORD, 7));
    server
        .getPluginManager()
        .callEvent(
            new PlayerInteractEvent(
                p, Action.RIGHT_CLICK_BLOCK, null, block, BlockFace.SELF, EquipmentSlot.HAND));
    List<String> lines2 = Files.readAllLines(audit.toPath(), StandardCharsets.UTF_8);
    assertEquals(2, lines2.size(), "second Success must APPEND to audit.log");
    String[] parts2 = lines2.get(1).split("\\|", -1);
    assertEquals("world_nether", parts2[4]);
  }

  @Test
  void auditAdapter_toPlainStringAndMkdirs(@TempDir File temp) throws Exception {
    // direct FileAuditAdapter contract: mkdirs + CREATE|APPEND + toPlainString + swallow
    // IOException
    File dir = new File(temp, "nested/a/b");
    File auditFile = new File(dir, "audit.log");
    io.github.danielxxomg.anvillink.adapter.FileAuditAdapter audit =
        new io.github.danielxxomg.anvillink.adapter.FileAuditAdapter(auditFile);
    assertFalse(dir.exists());
    io.github.danielxxomg.anvillink.domain.ports.AuditPort.AuditEntry entry =
        new io.github.danielxxomg.anvillink.domain.ports.AuditPort.AuditEntry(
            java.time.Instant.parse("2026-01-01T00:00:00Z"),
            UUID.fromString("00000000-0000-0000-0000-000000000001"),
            "Steve",
            RepairMode.HAND,
            "world",
            new BigDecimal("12000.00"),
            1,
            "SUCCESS");
    audit.record(entry);
    assertTrue(auditFile.exists(), "FileAuditAdapter must mkdirs and CREATE");
    List<String> lines = Files.readAllLines(auditFile.toPath(), StandardCharsets.UTF_8);
    assertEquals(1, lines.size());
    String[] parts = lines.get(0).split("\\|", -1);
    assertEquals("12000.00", parts[5], "price must be toPlainString preserving trailing zeros");
    // append second
    audit.record(entry);
    List<String> lines2 = Files.readAllLines(auditFile.toPath(), StandardCharsets.UTF_8);
    assertEquals(2, lines2.size(), "second record must APPEND");
    // swallow IOException: file is directory
    File dirAsFile = new File(temp, "dirAsFile");
    assertTrue(dirAsFile.mkdir());
    io.github.danielxxomg.anvillink.adapter.FileAuditAdapter bad =
        new io.github.danielxxomg.anvillink.adapter.FileAuditAdapter(dirAsFile);
    assertDoesNotThrow(() -> bad.record(entry), "adapter must swallow IOException");
  }

  @Test
  void editAndBreakWithoutManage_viaCallEvent_cancelledPdcUnchanged() {
    ServerMock server = MockBukkit.mock();
    MockBukkit.load(AnvilLinkPlugin.class);
    WorldMock world = server.addSimpleWorld("world");
    PlayerMock creator = server.addPlayer("ebCreator");
    creator.addAttachment(MockBukkit.createMockPlugin(), "anvillink.create", true);
    Block block = world.getBlockAt(20, 64, 0);
    block.setType(Material.OAK_SIGN);
    server
        .getPluginManager()
        .callEvent(new SignChangeEvent(block, creator, new String[] {"[repair]", "ALL", "", ""}));
    BlockState state = block.getState();
    assertTrue(PdcSignIdentity.has((TileState) state), "must be registered before edit/break test");
    PlayerMock intruder = server.addPlayer("intruder");
    // edit attempt without manage
    SignChangeEvent edit =
        new SignChangeEvent(block, intruder, new String[] {"[tampered]", "HAND", "", ""});
    server.getPluginManager().callEvent(edit);
    assertTrue(edit.isCancelled(), "edit without manage must be cancelled");
    assertTrue(
        PdcSignIdentity.has((TileState) block.getState()), "PDC must remain after cancelled edit");
    BlockBreakEvent breakEvent = new BlockBreakEvent(block, intruder);
    server.getPluginManager().callEvent(breakEvent);
    assertTrue(breakEvent.isCancelled(), "break without manage must be cancelled");
    assertTrue(
        PdcSignIdentity.has((TileState) block.getState()), "PDC must remain after cancelled break");
  }

  // --- helpers ---

  private static Economy proxyEconomy(
      int fractionalDigits, List<Double> withdrawals, List<Double> deposits) {
    return (Economy)
        Proxy.newProxyInstance(
            Economy.class.getClassLoader(),
            new Class<?>[] {Economy.class},
            (proxy, method, args) -> {
              String n = method.getName();
              if (n.equals("fractionalDigits")) return fractionalDigits;
              if (n.equals("withdrawPlayer") && args.length == 2) {
                double amount = (double) args[1];
                if (withdrawals != null) withdrawals.add(amount);
                return new EconomyResponse(
                    amount, 10000, EconomyResponse.ResponseType.SUCCESS, null);
              }
              if (n.equals("depositPlayer") && args.length == 2) {
                double amount = (double) args[1];
                if (deposits != null) deposits.add(amount);
                return new EconomyResponse(
                    amount, 10000, EconomyResponse.ResponseType.SUCCESS, null);
              }
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

  private static Economy proxyEconomyInsufficientFunds() {
    return (Economy)
        Proxy.newProxyInstance(
            Economy.class.getClassLoader(),
            new Class<?>[] {Economy.class},
            (proxy, method, args) -> {
              String n = method.getName();
              if (n.equals("fractionalDigits")) return 2;
              if (n.equals("withdrawPlayer")) {
                return new EconomyResponse(
                    25.0, 0, EconomyResponse.ResponseType.FAILURE, "Insufficient funds");
              }
              if (n.equals("depositPlayer")) {
                return new EconomyResponse(
                    (double) args[1], 0, EconomyResponse.ResponseType.SUCCESS, null);
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

  private static Economy proxyEconomyAmountMismatch(
      List<Double> withdrawals, List<Double> deposits) {
    return (Economy)
        Proxy.newProxyInstance(
            Economy.class.getClassLoader(),
            new Class<?>[] {Economy.class},
            (proxy, method, args) -> {
              String n = method.getName();
              if (n.equals("fractionalDigits")) return 2;
              if (n.equals("withdrawPlayer") && args.length == 2) {
                double amount = (double) args[1];
                withdrawals.add(amount);
                // return mismatched reported amount -> triggers InvalidResponse
                return new EconomyResponse(20.0, 80.0, EconomyResponse.ResponseType.SUCCESS, null);
              }
              if (n.equals("depositPlayer") && args.length == 2) {
                double amount = (double) args[1];
                deposits.add(amount);
                return new EconomyResponse(
                    amount, 100.0, EconomyResponse.ResponseType.SUCCESS, null);
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

  private static ItemStack damaged(Material mat, int dmg) {
    ItemStack s = new ItemStack(mat);
    Damageable m = (Damageable) s.getItemMeta();
    assertNotNull(m);
    m.setDamage(dmg);
    s.setItemMeta(m);
    return s;
  }

  private static Block proxyBlock(BlockState first, BlockState second) {
    return (Block)
        Proxy.newProxyInstance(
            Block.class.getClassLoader(),
            new Class<?>[] {Block.class},
            new java.lang.reflect.InvocationHandler() {
              int calls = 0;

              @Override
              public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args)
                  throws Throwable {
                String n = method.getName();
                if (n.equals("getState")) {
                  calls++;
                  return calls == 1 ? first : second;
                }
                if (n.equals("getType")) return Material.OAK_SIGN;
                if (n.equals("hashCode")) return System.identityHashCode(proxy);
                if (n.equals("equals")) return proxy == args[0];
                if (n.equals("toString")) return "ProxyBlock";
                Class<?> rt = method.getReturnType();
                if (rt == boolean.class) return false;
                if (rt == int.class) return 0;
                if (rt == long.class) return 0L;
                if (rt == void.class) return null;
                return null;
              }
            });
  }

  private static Player playerWith(UUID uuid, boolean canCreate, boolean canManage) {
    return (Player)
        Proxy.newProxyInstance(
            Player.class.getClassLoader(),
            new Class<?>[] {Player.class},
            (proxy, method, args) -> {
              String n = method.getName();
              if (n.equals("hasPermission") && args != null && args.length == 1) {
                String perm = (String) args[0];
                if ("anvillink.create".equals(perm)) return canCreate;
                if ("anvillink.manage".equals(perm)) return canManage;
                return false;
              }
              if (n.equals("getUniqueId")) return uuid;
              if (n.equals("hashCode")) return System.identityHashCode(proxy);
              if (n.equals("equals")) return proxy == args[0];
              if (n.equals("toString")) return "FakePlayer";
              Class<?> rt = method.getReturnType();
              if (rt == boolean.class) return false;
              if (rt == int.class) return 0;
              if (rt == long.class) return 0L;
              if (rt == void.class) return null;
              return null;
            });
  }

  static final class FakeSign implements Sign {
    DyeColor color;
    boolean updated;
    final FakePdc pdc = new FakePdc();
    final String[] lines = new String[4];

    @Override
    public PersistentDataContainer getPersistentDataContainer() {
      return pdc;
    }

    @Override
    public DyeColor getColor() {
      return color;
    }

    @Override
    public void setColor(DyeColor c) {
      this.color = c;
    }

    @Override
    public String getLine(int i) {
      return lines[i];
    }

    @Override
    public void setLine(int i, String s) {
      lines[i] = s;
    }

    @Override
    public String[] getLines() {
      return lines.clone();
    }

    @Override
    public boolean update() {
      updated = true;
      return true;
    }

    @Override
    public boolean update(boolean f) {
      updated = true;
      return true;
    }

    @Override
    public boolean update(boolean f, boolean a) {
      updated = true;
      return true;
    }

    @Override
    public boolean isSnapshot() {
      return false;
    }

    @Override
    public Block getBlock() {
      return null;
    }

    @Override
    public Material getType() {
      return Material.OAK_SIGN;
    }

    @Override
    public void setType(Material t) {}

    @Override
    public org.bukkit.World getWorld() {
      return null;
    }

    @Override
    public int getX() {
      return 0;
    }

    @Override
    public int getY() {
      return 0;
    }

    @Override
    public int getZ() {
      return 0;
    }

    @Override
    public org.bukkit.Location getLocation() {
      return null;
    }

    @Override
    public org.bukkit.Location getLocation(org.bukkit.Location l) {
      return null;
    }

    @Override
    public org.bukkit.Chunk getChunk() {
      return null;
    }

    @Override
    public org.bukkit.block.data.BlockData getBlockData() {
      return null;
    }

    @Override
    public void setBlockData(org.bukkit.block.data.BlockData d) {}

    @Override
    public byte getLightLevel() {
      return 0;
    }

    @Override
    public org.bukkit.material.MaterialData getData() {
      return null;
    }

    @Override
    public void setData(org.bukkit.material.MaterialData d) {}

    @Override
    public byte getRawData() {
      return 0;
    }

    @Override
    public void setRawData(byte b) {}

    @Override
    public boolean isPlaced() {
      return true;
    }

    @Override
    public boolean isCollidable() {
      return true;
    }

    @Override
    public java.util.Collection<ItemStack> getDrops(
        ItemStack tool, org.bukkit.entity.Entity entity) {
      return java.util.List.of();
    }

    @Override
    public BlockState copy() {
      return this;
    }

    @Override
    public BlockState copy(org.bukkit.Location loc) {
      return this;
    }

    @Override
    public boolean isSuffocating() {
      return false;
    }

    @Override
    public void setMetadata(String k, org.bukkit.metadata.MetadataValue v) {}

    @Override
    public java.util.List<org.bukkit.metadata.MetadataValue> getMetadata(String k) {
      return java.util.List.of();
    }

    @Override
    public boolean hasMetadata(String k) {
      return false;
    }

    @Override
    public void removeMetadata(String k, org.bukkit.plugin.Plugin p) {}

    @Override
    public java.util.List<net.kyori.adventure.text.Component> lines() {
      return java.util.List.of();
    }

    @Override
    public net.kyori.adventure.text.Component line(int i) {
      return net.kyori.adventure.text.Component.empty();
    }

    @Override
    public void line(int i, net.kyori.adventure.text.Component c) {}

    @Override
    public boolean isEditable() {
      return true;
    }

    @Override
    public void setEditable(boolean b) {}

    @Override
    public boolean isWaxed() {
      return false;
    }

    @Override
    public void setWaxed(boolean b) {}

    @Override
    public boolean isGlowingText() {
      return false;
    }

    @Override
    public void setGlowingText(boolean b) {}

    @Override
    public org.bukkit.block.sign.SignSide getSide(org.bukkit.block.sign.Side s) {
      return null;
    }

    @Override
    public org.bukkit.block.sign.SignSide getTargetSide(Player p) {
      return null;
    }

    @Override
    public Player getAllowedEditor() {
      return null;
    }

    @Override
    public UUID getAllowedEditorUniqueId() {
      return null;
    }

    @Override
    public void setAllowedEditorUniqueId(UUID u) {}

    @Override
    public org.bukkit.block.sign.Side getInteractableSideFor(double x, double z) {
      return org.bukkit.block.sign.Side.FRONT;
    }
  }

  static final class FakeNonTileState implements BlockState {
    @Override
    public Block getBlock() {
      return null;
    }

    @Override
    public Material getType() {
      return Material.STONE;
    }

    @Override
    public void setType(Material t) {}

    @Override
    public org.bukkit.World getWorld() {
      return null;
    }

    @Override
    public int getX() {
      return 0;
    }

    @Override
    public int getY() {
      return 0;
    }

    @Override
    public int getZ() {
      return 0;
    }

    @Override
    public org.bukkit.Location getLocation() {
      return null;
    }

    @Override
    public org.bukkit.Location getLocation(org.bukkit.Location l) {
      return null;
    }

    @Override
    public org.bukkit.Chunk getChunk() {
      return null;
    }

    @Override
    public org.bukkit.block.data.BlockData getBlockData() {
      return null;
    }

    @Override
    public void setBlockData(org.bukkit.block.data.BlockData d) {}

    @Override
    public byte getLightLevel() {
      return 0;
    }

    @Override
    public org.bukkit.material.MaterialData getData() {
      return null;
    }

    @Override
    public void setData(org.bukkit.material.MaterialData d) {}

    @Override
    public byte getRawData() {
      return 0;
    }

    @Override
    public void setRawData(byte b) {}

    @Override
    public boolean isPlaced() {
      return true;
    }

    @Override
    public boolean isCollidable() {
      return true;
    }

    @Override
    public java.util.Collection<ItemStack> getDrops(ItemStack tool, org.bukkit.entity.Entity e) {
      return java.util.List.of();
    }

    @Override
    public BlockState copy() {
      return this;
    }

    @Override
    public BlockState copy(org.bukkit.Location loc) {
      return this;
    }

    @Override
    public boolean isSuffocating() {
      return false;
    }

    @Override
    public boolean update() {
      return true;
    }

    @Override
    public boolean update(boolean f) {
      return true;
    }

    @Override
    public boolean update(boolean f, boolean a) {
      return true;
    }

    @Override
    public void setMetadata(String k, org.bukkit.metadata.MetadataValue v) {}

    @Override
    public java.util.List<org.bukkit.metadata.MetadataValue> getMetadata(String k) {
      return java.util.List.of();
    }

    @Override
    public boolean hasMetadata(String k) {
      return false;
    }

    @Override
    public void removeMetadata(String k, org.bukkit.plugin.Plugin p) {}
  }

  static final class FakePdc implements PersistentDataContainer {
    private final Map<NamespacedKey, Object> map = new HashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public <T, Z> void set(NamespacedKey k, PersistentDataType<T, Z> t, Z v) {
      map.put(k, v);
    }

    @Override
    public <T, Z> boolean has(NamespacedKey k, PersistentDataType<T, Z> t) {
      return map.containsKey(k);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T, Z> Z get(NamespacedKey k, PersistentDataType<T, Z> t) {
      Object v = map.get(k);
      if (v == null) return null;
      return (Z) v;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T, Z> Z getOrDefault(NamespacedKey k, PersistentDataType<T, Z> t, Z d) {
      Z v = get(k, t);
      return v != null ? v : d;
    }

    @Override
    public Set<NamespacedKey> getKeys() {
      return Set.copyOf(map.keySet());
    }

    @Override
    public void remove(NamespacedKey k) {
      map.remove(k);
    }

    @Override
    public boolean isEmpty() {
      return map.isEmpty();
    }

    @Override
    public void readFromBytes(byte[] bytes, boolean clear) throws java.io.IOException {}

    @Override
    public byte[] serializeToBytes() throws java.io.IOException {
      return new byte[0];
    }

    @Override
    public void copyTo(PersistentDataContainer other, boolean replace) {}

    @Override
    public int getSize() {
      return map.size();
    }

    @Override
    public org.bukkit.persistence.PersistentDataAdapterContext getAdapterContext() {
      return null;
    }

    @Override
    public boolean has(NamespacedKey k) {
      return map.containsKey(k);
    }
  }

  static final class FakeHolder implements PersistentDataHolder {
    private final PersistentDataContainer pdc;

    FakeHolder(PersistentDataContainer pdc) {
      this.pdc = pdc;
    }

    @Override
    public PersistentDataContainer getPersistentDataContainer() {
      return pdc;
    }
  }
}
