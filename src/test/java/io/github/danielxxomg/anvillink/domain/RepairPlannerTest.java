// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RepairPlannerTest {

  private static EquipmentView viewOf(Map<EquipmentSlotId, ItemView> map) {
    return slot -> map.get(slot);
  }

  private static ItemView eligible(int damage) {
    return new StubItem(false, true, damage, false);
  }

  private static ItemView ineligibleEmpty() {
    return new StubItem(true, false, 0, false);
  }

  private static ItemView ineligibleUndamaged() {
    return new StubItem(false, true, 0, false);
  }

  private static ItemView ineligibleUnbreakable() {
    return new StubItem(false, true, 10, true);
  }

  private static ItemView ineligibleNonDamageable() {
    return new StubItem(false, false, 10, false);
  }

  @Test
  void handModePlansOnlyMainHand() {
    Map<EquipmentSlotId, ItemView> map = new EnumMap<>(EquipmentSlotId.class);
    map.put(EquipmentSlotId.MAIN_HAND, eligible(10));
    map.put(EquipmentSlotId.OFF_HAND, eligible(10));
    map.put(EquipmentSlotId.HELMET, eligible(10));
    RepairPlan plan = new RepairPlanner().plan(RepairMode.HAND, viewOf(map));
    assertEquals(1, plan.slots().size());
    assertEquals(EquipmentSlotId.MAIN_HAND, plan.slots().get(0).slot());
  }

  @Test
  void allModePlansSixSlotsAndExcludesStorage() {
    Map<EquipmentSlotId, ItemView> map = new EnumMap<>(EquipmentSlotId.class);
    for (EquipmentSlotId slot : EquipmentSlotId.slotsFor(RepairMode.ALL)) {
      map.put(slot, eligible(5));
    }
    map.put(EquipmentSlotId.STORAGE, eligible(5));
    RepairPlan plan = new RepairPlanner().plan(RepairMode.ALL, viewOf(map));
    assertEquals(6, plan.slots().size());
    assertTrue(plan.slots().stream().noneMatch(s -> s.slot() == EquipmentSlotId.STORAGE));
  }

  @Test
  void skipsIneligibleItemsAndSelectsOnlyEligible() {
    Map<EquipmentSlotId, ItemView> map = new EnumMap<>(EquipmentSlotId.class);
    map.put(EquipmentSlotId.MAIN_HAND, eligible(7));
    map.put(EquipmentSlotId.OFF_HAND, ineligibleEmpty());
    map.put(EquipmentSlotId.HELMET, ineligibleUndamaged());
    map.put(EquipmentSlotId.CHESTPLATE, ineligibleUnbreakable());
    map.put(EquipmentSlotId.LEGGINGS, ineligibleNonDamageable());
    map.put(EquipmentSlotId.BOOTS, eligible(3));
    RepairPlan plan = new RepairPlanner().plan(RepairMode.ALL, viewOf(map));
    assertEquals(2, plan.slots().size());
    assertEquals(EquipmentSlotId.MAIN_HAND, plan.slots().get(0).slot());
    assertEquals(EquipmentSlotId.BOOTS, plan.slots().get(1).slot());
  }

  @Test
  void emptyPlanWhenNoEligibleTargets() {
    Map<EquipmentSlotId, ItemView> map = new EnumMap<>(EquipmentSlotId.class);
    map.put(EquipmentSlotId.MAIN_HAND, ineligibleEmpty());
    map.put(EquipmentSlotId.OFF_HAND, ineligibleUndamaged());
    RepairPlan plan = new RepairPlanner().plan(RepairMode.ALL, viewOf(map));
    assertTrue(plan.isEmpty());
    assertEquals(0, plan.slots().size());
  }

  @Test
  void repeatedPlanningIsStableAndSnapshotsEquivalent() {
    Map<EquipmentSlotId, ItemView> map = new EnumMap<>(EquipmentSlotId.class);
    map.put(EquipmentSlotId.MAIN_HAND, eligible(4));
    map.put(EquipmentSlotId.OFF_HAND, eligible(9));
    EquipmentView view = viewOf(map);
    RepairPlan first = new RepairPlanner().plan(RepairMode.ALL, view);
    RepairPlan second = new RepairPlanner().plan(RepairMode.ALL, view);
    assertEquals(first.slots().size(), second.slots().size());
    for (int i = 0; i < first.slots().size(); i++) {
      assertEquals(first.slots().get(i).slot(), second.slots().get(i).slot());
      assertEquals(first.slots().get(i).snapshot(), second.slots().get(i).snapshot());
    }
  }

  @Test
  void planningDoesNotMutateItems() {
    StubItem item = new StubItem(false, true, 12, false);
    Map<EquipmentSlotId, ItemView> map = new EnumMap<>(EquipmentSlotId.class);
    map.put(EquipmentSlotId.MAIN_HAND, item);
    new RepairPlanner().plan(RepairMode.HAND, viewOf(map));
    assertEquals(12, item.damage());
  }

  private static final class StubItem implements ItemView {
    private final boolean empty;
    private final boolean damageable;
    private final int damage;
    private final boolean unbreakable;

    StubItem(boolean empty, boolean damageable, int damage, boolean unbreakable) {
      this.empty = empty;
      this.damageable = damageable;
      this.damage = damage;
      this.unbreakable = unbreakable;
    }

    @Override
    public boolean isEmpty() {
      return empty;
    }

    @Override
    public boolean isDamageable() {
      return damageable;
    }

    @Override
    public int damage() {
      return damage;
    }

    @Override
    public boolean isUnbreakable() {
      return unbreakable;
    }

    @Override
    public ItemSnapshot snapshot() {
      return new ItemSnapshot(empty, damageable, damage, unbreakable);
    }
  }
}
