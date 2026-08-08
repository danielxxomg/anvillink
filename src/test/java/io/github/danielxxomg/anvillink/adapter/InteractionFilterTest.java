// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.adapter;

import static org.junit.jupiter.api.Assertions.*;

import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;
import org.junit.jupiter.api.Test;

class InteractionFilterTest {

  @Test
  void mainHandRightClick_proceeds() {
    var e = FakeInteractEvent.of(EquipmentSlot.HAND, Action.RIGHT_CLICK_BLOCK);
    assertTrue(InteractionFilter.shouldProceed(e));
  }

  @Test
  void offHandRightClick_ignored() {
    var e = FakeInteractEvent.of(EquipmentSlot.OFF_HAND, Action.RIGHT_CLICK_BLOCK);
    assertFalse(InteractionFilter.shouldProceed(e));
  }

  @Test
  void nonRightClick_ignored() {
    var e = FakeInteractEvent.of(EquipmentSlot.HAND, Action.LEFT_CLICK_BLOCK);
    assertFalse(InteractionFilter.shouldProceed(e));
    var e2 = FakeInteractEvent.of(EquipmentSlot.HAND, Action.PHYSICAL);
    assertFalse(InteractionFilter.shouldProceed(e2));
  }

  @Test
  void rightClickAir_mainHand_proceeds() {
    var e = FakeInteractEvent.of(EquipmentSlot.HAND, Action.RIGHT_CLICK_AIR);
    assertTrue(InteractionFilter.shouldProceed(e));
  }

  // minimal fake to avoid needing MockBukkit for PlayerInteractEvent
  static final class FakeInteractEvent extends org.bukkit.event.player.PlayerInteractEvent {
    private final EquipmentSlot hand;
    private final Action action;

    FakeInteractEvent(EquipmentSlot hand, Action action) {
      super(fakePlayer(), action, null, null, org.bukkit.block.BlockFace.SELF, hand);
      this.hand = hand;
      this.action = action;
    }

    static FakeInteractEvent of(EquipmentSlot h, Action a) {
      return new FakeInteractEvent(h, a);
    }

    @Override
    public EquipmentSlot getHand() {
      return hand;
    }

    @Override
    public Action getAction() {
      return action;
    }

    private static org.bukkit.entity.Player fakePlayer() {
      return (org.bukkit.entity.Player)
          java.lang.reflect.Proxy.newProxyInstance(
              org.bukkit.entity.Player.class.getClassLoader(),
              new Class<?>[] {org.bukkit.entity.Player.class},
              (p, m, a) -> {
                Class<?> rt = m.getReturnType();
                if (rt == boolean.class) return false;
                if (rt == int.class) return 0;
                if (rt == long.class) return 0L;
                if (rt == void.class) return null;
                return null;
              });
    }
  }
}
