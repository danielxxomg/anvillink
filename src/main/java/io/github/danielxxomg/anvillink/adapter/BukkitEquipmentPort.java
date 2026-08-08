// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.adapter;

import io.github.danielxxomg.anvillink.domain.EquipmentSlotId;
import io.github.danielxxomg.anvillink.domain.EquipmentView;
import io.github.danielxxomg.anvillink.domain.ItemSnapshot;
import io.github.danielxxomg.anvillink.domain.ItemView;
import io.github.danielxxomg.anvillink.domain.ports.EquipmentPort;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Bukkit adapter for EquipmentPort. Owns snapshot restoration. Reads PlayerInventory, setDamage(0),
 * snapshot restore.
 */
public final class BukkitEquipmentPort implements EquipmentPort {

  private final Function<java.util.UUID, Player> playerLookup;

  public BukkitEquipmentPort(Function<java.util.UUID, Player> playerLookup) {
    this.playerLookup = playerLookup;
  }

  @Override
  public EquipmentView viewOf(PlayerHandle handle) {
    Player player = playerLookup.apply(handle.uuid());
    if (player == null) return null;
    PlayerInventory inv = player.getInventory();
    return slot -> viewOfStack(stackFor(inv, slot));
  }

  @Override
  public ApplyOutcome applyRepair(PlayerHandle handle, List<PlannedApply> planned) {
    Player player = playerLookup.apply(handle.uuid());
    if (player == null) return new ApplyOutcome.Success(List.of());
    PlayerInventory inv = player.getInventory();
    List<PlannedApply> mutated = new ArrayList<>();
    List<PlannedApply> failed = new ArrayList<>();
    String reason = null;
    for (PlannedApply p : planned) {
      ItemStack stack = stackFor(inv, p.slot());
      if (stack == null || stack.getType().isAir()) {
        failed.add(p);
        reason = "empty-slot:" + p.slot();
        break;
      }
      ItemMeta meta = stack.getItemMeta();
      if (!(meta instanceof Damageable d)) {
        failed.add(p);
        reason = "not-damageable:" + p.slot();
        break;
      }
      if (meta.isUnbreakable()) {
        failed.add(p);
        reason = "unbreakable:" + p.slot();
        break;
      }
      if (d.getDamage() <= 0) {
        failed.add(p);
        reason = "undamaged:" + p.slot();
        break;
      }
      d.setDamage(0);
      stack.setItemMeta(meta);
      writeStack(inv, p.slot(), stack);
      mutated.add(p);
    }
    if (failed.isEmpty()) return new ApplyOutcome.Success(List.copyOf(mutated));
    return new ApplyOutcome.PartialFailure(List.copyOf(mutated), List.copyOf(failed), reason);
  }

  @Override
  public boolean restore(PlayerHandle handle, PlannedApply slot) {
    Player player = playerLookup.apply(handle.uuid());
    if (player == null) return false;
    PlayerInventory inv = player.getInventory();
    ItemSnapshot snap = slot.snapshot();
    try {
      ItemStack current = stackFor(inv, slot.slot());
      if (current == null) {
        // empty snapshot -> clear slot
        if (snap.empty()) {
          writeStack(inv, slot.slot(), null);
          return true;
        }
        return false;
      }
      ItemMeta meta = current.getItemMeta();
      if (meta == null) return false;
      // restore damage + unbreakable; for empty/non-damageable we still restore damage field
      if (meta instanceof Damageable d) {
        d.setDamage(snap.damage());
      }
      meta.setUnbreakable(snap.unbreakable());
      current.setItemMeta(meta);
      writeStack(inv, slot.slot(), current);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  private static ItemView viewOfStack(ItemStack stack) {
    if (stack == null || stack.getType() == null || stack.getType().isAir()) {
      return new ItemView() {
        public boolean isEmpty() {
          return true;
        }

        public boolean isDamageable() {
          return false;
        }

        public int damage() {
          return 0;
        }

        public boolean isUnbreakable() {
          return false;
        }

        public ItemSnapshot snapshot() {
          return new ItemSnapshot(true, false, 0, false);
        }
      };
    }
    ItemMeta meta = stack.getItemMeta();
    boolean damageable = meta instanceof Damageable;
    int dmg = damageable ? ((Damageable) meta).getDamage() : 0;
    boolean unbreak = meta != null && meta.isUnbreakable();
    return new ItemView() {
      public boolean isEmpty() {
        return false;
      }

      public boolean isDamageable() {
        return damageable;
      }

      public int damage() {
        return dmg;
      }

      public boolean isUnbreakable() {
        return unbreak;
      }

      public ItemSnapshot snapshot() {
        return new ItemSnapshot(false, damageable, dmg, unbreak);
      }
    };
  }

  private static ItemStack stackFor(PlayerInventory inv, EquipmentSlotId slot) {
    return switch (slot) {
      case MAIN_HAND -> inv.getItemInMainHand();
      case OFF_HAND -> inv.getItemInOffHand();
      case HELMET -> inv.getHelmet();
      case CHESTPLATE -> inv.getChestplate();
      case LEGGINGS -> inv.getLeggings();
      case BOOTS -> inv.getBoots();
      case STORAGE -> null;
    };
  }

  private static void writeStack(PlayerInventory inv, EquipmentSlotId slot, ItemStack stack) {
    switch (slot) {
      case MAIN_HAND -> inv.setItemInMainHand(stack);
      case OFF_HAND -> inv.setItemInOffHand(stack);
      case HELMET -> inv.setHelmet(stack);
      case CHESTPLATE -> inv.setChestplate(stack);
      case LEGGINGS -> inv.setLeggings(stack);
      case BOOTS -> inv.setBoots(stack);
      case STORAGE -> {}
    }
  }
}
