// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.adapter;

import static org.junit.jupiter.api.Assertions.*;

import io.github.danielxxomg.anvillink.domain.EquipmentSlotId;
import io.github.danielxxomg.anvillink.domain.ItemSnapshot;
import io.github.danielxxomg.anvillink.domain.RepairMode;
import io.github.danielxxomg.anvillink.domain.ports.EquipmentPort;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BukkitEquipmentPortTest {

  @BeforeEach
  void installFakeBukkit() {
    try {
      var f = Bukkit.class.getDeclaredField("server");
      f.setAccessible(true);
      f.set(null, fakeServer());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @AfterEach
  void restoreBukkit() {
    try {
      var f = Bukkit.class.getDeclaredField("server");
      f.setAccessible(true);
      f.set(null, fakeServer());
    } catch (Exception ignored) {
    }
  }

  @Test
  void viewOf_resolvesHandAndAll_withoutStorage() throws Exception {
    var inv = inventoryWith(Map.of(EquipmentSlotId.MAIN_HAND, 10, EquipmentSlotId.HELMET, 5));
    // debug to file since testLogging hides stdout
    String dbg =
        "mainHand="
            + inv.getItemInMainHand()
            + " type="
            + (inv.getItemInMainHand() == null ? "null" : inv.getItemInMainHand().getType())
            + " metaNull="
            + (inv.getItemInMainHand().getItemMeta() == null)
            + " helmet="
            + inv.getHelmet()
            + " serverNull="
            + (Bukkit.getServer() == null);
    Files.writeString(Path.of("/tmp/bukkit_debug_view.txt"), dbg);
    Files.writeString(Path.of("./bukkit_debug_view.txt"), dbg);
    var player = playerWith(inv);
    var port = new BukkitEquipmentPort(uuid -> player);
    var view = port.viewOf(new EquipmentPort.PlayerHandle(player.getUniqueId()));
    assertNotNull(view);
    String dbg2 =
        "mainHand isEmpty="
            + view.itemAt(EquipmentSlotId.MAIN_HAND).isEmpty()
            + " dmg="
            + view.itemAt(EquipmentSlotId.MAIN_HAND).damage()
            + " damageable="
            + view.itemAt(EquipmentSlotId.MAIN_HAND).isDamageable()
            + " helmet isEmpty="
            + view.itemAt(EquipmentSlotId.HELMET).isEmpty();
    Files.writeString(Path.of("/tmp/bukkit_debug_view2.txt"), dbg2);
    assertFalse(view.itemAt(EquipmentSlotId.MAIN_HAND).isEmpty(), dbg2);
    assertTrue(view.itemAt(EquipmentSlotId.OFF_HAND).isEmpty());
    assertFalse(view.itemAt(EquipmentSlotId.HELMET).isEmpty(), dbg2);
    assertTrue(view.itemAt(EquipmentSlotId.STORAGE).isEmpty());
  }

  @Test
  void applyRepair_setsDamageZero_onPlannedSlots() {
    var inv = inventoryWith(Map.of(EquipmentSlotId.MAIN_HAND, 12, EquipmentSlotId.HELMET, 7));
    var player = playerWith(inv);
    var port = new BukkitEquipmentPort(uuid -> player);
    var handle = new EquipmentPort.PlayerHandle(player.getUniqueId());
    var view = port.viewOf(handle);
    var planned = new ArrayList<EquipmentPort.PlannedApply>();
    for (var slot : EquipmentSlotId.slotsFor(RepairMode.ALL)) {
      var iv = view.itemAt(slot);
      if (!iv.isEmpty() && iv.isDamageable() && iv.damage() > 0 && !iv.isUnbreakable()) {
        planned.add(new EquipmentPort.PlannedApply(slot, iv.snapshot()));
      }
    }
    assertEquals(2, planned.size());
    var outcome = port.applyRepair(handle, planned);
    assertInstanceOf(EquipmentPort.ApplyOutcome.Success.class, outcome);
    assertEquals(0, damageOf(inv.getItemInMainHand()));
    assertEquals(0, damageOf(inv.getHelmet()));
  }

  @Test
  void restore_fromSnapshot_restoresDamage() {
    var inv = inventoryWith(Map.of(EquipmentSlotId.MAIN_HAND, 12));
    var player = playerWith(inv);
    var port = new BukkitEquipmentPort(uuid -> player);
    var handle = new EquipmentPort.PlayerHandle(player.getUniqueId());
    var snapshot = new ItemSnapshot(false, true, 12, false);
    var planned = new EquipmentPort.PlannedApply(EquipmentSlotId.MAIN_HAND, snapshot);
    port.applyRepair(handle, List.of(planned));
    assertEquals(0, damageOf(inv.getItemInMainHand()));
    boolean ok = port.restore(handle, planned);
    assertTrue(ok);
    assertEquals(12, damageOf(inv.getItemInMainHand()));
  }

  @Test
  void applyFailure_restorationPreservesUntouchedSlots() {
    var inv = inventoryWith(Map.of(EquipmentSlotId.MAIN_HAND, 12, EquipmentSlotId.HELMET, 7));
    var player = playerWith(inv);
    var port = new BukkitEquipmentPort(uuid -> player);
    var handle = new EquipmentPort.PlayerHandle(player.getUniqueId());
    var view = port.viewOf(handle);
    var mainPlanned =
        new EquipmentPort.PlannedApply(
            EquipmentSlotId.MAIN_HAND, view.itemAt(EquipmentSlotId.MAIN_HAND).snapshot());
    port.applyRepair(handle, List.of(mainPlanned));
    assertEquals(7, damageOf(inv.getHelmet()));
    port.restore(handle, mainPlanned);
    assertEquals(12, damageOf(inv.getItemInMainHand()));
    assertEquals(7, damageOf(inv.getHelmet()));
  }

  // --- Bukkit fakes ---

  private static Server fakeServer() {
    ItemFactory factory = fakeItemFactory();
    return (Server)
        Proxy.newProxyInstance(
            Server.class.getClassLoader(),
            new Class<?>[] {Server.class},
            (proxy, method, args) -> {
              String n = method.getName();
              if (n.equals("getItemFactory")) return factory;
              if (n.equals("getServicesManager")) {
                return Proxy.newProxyInstance(
                    org.bukkit.plugin.ServicesManager.class.getClassLoader(),
                    new Class<?>[] {org.bukkit.plugin.ServicesManager.class},
                    (p2, m2, a2) -> {
                      if (m2.getName().equals("getRegistration")) return null;
                      Class<?> rt = m2.getReturnType();
                      if (rt == boolean.class) return false;
                      if (rt == int.class) return 0;
                      if (rt == long.class) return 0L;
                      if (rt == double.class) return 0.0;
                      if (rt == void.class) return null;
                      return null;
                    });
              }
              Class<?> rt = method.getReturnType();
              if (rt == boolean.class) return false;
              if (rt == int.class) return 0;
              if (rt == long.class) return 0L;
              if (rt == double.class) return 0.0;
              if (rt == void.class) return null;
              return null;
            });
  }

  private static ItemFactory fakeItemFactory() {
    return (ItemFactory)
        Proxy.newProxyInstance(
            ItemFactory.class.getClassLoader(),
            new Class<?>[] {ItemFactory.class},
            (proxy, method, args) -> {
              String n = method.getName();
              if (n.equals("getItemMeta") && args != null && args.length == 1) {
                return damageableMeta(0, false);
              }
              if (n.equals("isApplicable")) return true;
              if (n.equals("equals") && args != null && args.length == 2) {
                if (args[0] == null || args[1] == null) return args[0] == args[1];
                return args[0].equals(args[1]);
              }
              if (n.equals("asMetaFor")) return args[0];
              Class<?> rt = method.getReturnType();
              if (rt == boolean.class) return false;
              if (rt == int.class) return 0;
              if (rt == long.class) return 0L;
              if (rt == double.class) return 0.0;
              if (rt == void.class) return null;
              return null;
            });
  }

  private static ItemMeta damageableMeta(int damage, boolean unbreakable) {
    int[] dmg = new int[] {damage};
    boolean[] unb = new boolean[] {unbreakable};
    return (ItemMeta)
        Proxy.newProxyInstance(
            ItemMeta.class.getClassLoader(),
            new Class<?>[] {ItemMeta.class, Damageable.class},
            (proxy, method, args) -> {
              String m = method.getName();
              switch (m) {
                case "getDamage" -> {
                  return dmg[0];
                }
                case "setDamage" -> {
                  dmg[0] = (int) args[0];
                  return null;
                }
                case "hasDamage" -> {
                  return dmg[0] > 0;
                }
                case "getDamageValue" -> {
                  return dmg[0];
                }
                case "setDamageValue" -> {
                  dmg[0] = (int) args[0];
                  return null;
                }
                case "hasDamageValue" -> {
                  return dmg[0] > 0;
                }
                case "isUnbreakable" -> {
                  return unb[0];
                }
                case "setUnbreakable" -> {
                  unb[0] = (boolean) args[0];
                  return null;
                }
                case "clone" -> {
                  return damageableMeta(dmg[0], unb[0]);
                }
                case "serialize" -> {
                  return Map.of();
                }
                case "hashCode" -> {
                  return System.identityHashCode(proxy);
                }
                case "equals" -> {
                  return proxy == args[0];
                }
                case "toString" -> {
                  return "FakeMeta(damage=" + dmg[0] + ")";
                }
                default -> {
                  Class<?> rt = method.getReturnType();
                  if (rt == boolean.class) return false;
                  if (rt == int.class) return 0;
                  if (rt == long.class) return 0L;
                  if (rt == double.class) return 0.0;
                  if (rt == void.class) return null;
                  if (rt == String.class) return null;
                  return null;
                }
              }
            });
  }

  // --- inventory/player helpers (real ItemStack, fake inventory) ---

  private static int damageOf(ItemStack stack) {
    if (stack == null) return -1;
    ItemMeta m = stack.getItemMeta();
    if (m instanceof Damageable d) return d.getDamage();
    return -1;
  }

  private static Player playerWith(PlayerInventory inv) {
    UUID id = UUID.randomUUID();
    return (Player)
        Proxy.newProxyInstance(
            Player.class.getClassLoader(),
            new Class<?>[] {Player.class},
            (proxy, method, args) -> {
              String n = method.getName();
              if (n.equals("getUniqueId")) return id;
              if (n.equals("getInventory")) return inv;
              if (n.equals("hashCode")) return System.identityHashCode(proxy);
              if (n.equals("equals")) return proxy == args[0];
              if (n.equals("toString")) return "FakePlayer";
              Class<?> rt = method.getReturnType();
              if (rt == boolean.class) return false;
              if (rt == int.class) return 0;
              if (rt == long.class) return 0L;
              if (rt == double.class) return 0.0;
              if (rt == void.class) return null;
              return null;
            });
  }

  private static PlayerInventory inventoryWith(Map<EquipmentSlotId, Integer> damages) {
    Map<EquipmentSlotId, ItemStack> stacks = new EnumMap<>(EquipmentSlotId.class);
    for (var e : damages.entrySet()) {
      stacks.put(e.getKey(), damagedStack(e.getValue()));
    }
    return fakeInventory(stacks);
  }

  static final class FakeItemStack extends ItemStack {
    private Material type;
    private ItemMeta meta;

    FakeItemStack(Material type, ItemMeta meta) {
      super();
      this.type = type;
      this.meta = meta;
    }

    @Override
    public Material getType() {
      return type;
    }

    @Override
    public void setType(Material type) {
      this.type = type;
    }

    @Override
    public ItemMeta getItemMeta() {
      return meta;
    }

    @Override
    public boolean setItemMeta(ItemMeta meta) {
      this.meta = meta;
      return true;
    }

    @Override
    public int getAmount() {
      return 1;
    }

    public boolean isEmpty() {
      return false;
    }
  }

  private static ItemStack damagedStack(int damage) {
    return new FakeItemStack(Material.DIAMOND_CHESTPLATE, damageableMeta(damage, false));
  }

  private static PlayerInventory fakeInventory(Map<EquipmentSlotId, ItemStack> stacks) {
    Map<EquipmentSlotId, ItemStack> backing = new EnumMap<>(EquipmentSlotId.class);
    backing.putAll(stacks);
    return (PlayerInventory)
        Proxy.newProxyInstance(
            PlayerInventory.class.getClassLoader(),
            new Class<?>[] {PlayerInventory.class},
            (proxy, method, args) -> {
              String n = method.getName();
              switch (n) {
                case "getItemInMainHand" -> {
                  return backing.getOrDefault(EquipmentSlotId.MAIN_HAND, null);
                }
                case "setItemInMainHand" -> {
                  backing.put(EquipmentSlotId.MAIN_HAND, (ItemStack) args[0]);
                  return null;
                }
                case "getItemInOffHand" -> {
                  return backing.getOrDefault(EquipmentSlotId.OFF_HAND, null);
                }
                case "setItemInOffHand" -> {
                  backing.put(EquipmentSlotId.OFF_HAND, (ItemStack) args[0]);
                  return null;
                }
                case "getHelmet" -> {
                  return backing.get(EquipmentSlotId.HELMET);
                }
                case "setHelmet" -> {
                  backing.put(EquipmentSlotId.HELMET, (ItemStack) args[0]);
                  return null;
                }
                case "getChestplate" -> {
                  return backing.get(EquipmentSlotId.CHESTPLATE);
                }
                case "setChestplate" -> {
                  backing.put(EquipmentSlotId.CHESTPLATE, (ItemStack) args[0]);
                  return null;
                }
                case "getLeggings" -> {
                  return backing.get(EquipmentSlotId.LEGGINGS);
                }
                case "setLeggings" -> {
                  backing.put(EquipmentSlotId.LEGGINGS, (ItemStack) args[0]);
                  return null;
                }
                case "getBoots" -> {
                  return backing.get(EquipmentSlotId.BOOTS);
                }
                case "setBoots" -> {
                  backing.put(EquipmentSlotId.BOOTS, (ItemStack) args[0]);
                  return null;
                }
                case "getStorageContents" -> {
                  return new ItemStack[36];
                }
                case "getContents" -> {
                  return new ItemStack[41];
                }
                case "getHolder" -> {
                  return null;
                }
                default -> {
                  Class<?> rt = method.getReturnType();
                  if (rt == boolean.class) return false;
                  if (rt == int.class) return 0;
                  if (rt == long.class) return 0L;
                  if (rt == double.class) return 0.0;
                  if (rt == void.class) return null;
                  return null;
                }
              }
            });
  }
}
