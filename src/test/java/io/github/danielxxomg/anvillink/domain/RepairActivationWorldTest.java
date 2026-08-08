// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.domain;

import static org.junit.jupiter.api.Assertions.*;

import io.github.danielxxomg.anvillink.domain.ports.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;

class RepairActivationWorldTest {

  @Test
  void worldHandOverridesHand() {
    var f = RepairActivationTest.Fixture.with(Map.of(EquipmentSlotId.MAIN_HAND, 10));
    f.cfg.worldPrices = Map.of("world", new WorldPrice(new BigDecimal("5000"), null));
    f.cfg.hand = new BigDecimal("12000.00");
    f.cfg.all = new BigDecimal("25000.00");
    var r = f.act().activate(f.signId, f.pid, "world");
    assertInstanceOf(TransactionResult.Success.class, r);
    assertEquals(new BigDecimal("5000"), f.eco.lastWithdraw);
  }

  @Test
  void worldHandOnlyAllFallsBackToGlobal() {
    var f = RepairActivationTest.Fixture.allSlots();
    f.cfg.worldPrices = Map.of("world", new WorldPrice(new BigDecimal("5000"), null));
    f.cfg.hand = new BigDecimal("12000.00");
    f.cfg.all = new BigDecimal("25000.00");
    f.eco.withdraw = new EconomyPort.Withdrawal.Success(new BigDecimal("25000.00"));
    var r = f.act().activate(f.signId, f.pid, "world");
    assertInstanceOf(TransactionResult.Success.class, r);
    assertEquals(new BigDecimal("25000.00"), f.eco.lastWithdraw);
  }

  @Test
  void worldAllOnlyOverridesAll() {
    var f = RepairActivationTest.Fixture.allSlots();
    f.cfg.worldPrices = Map.of("world_nether", new WorldPrice(null, new BigDecimal("1000")));
    f.cfg.hand = new BigDecimal("12000.00");
    f.cfg.all = new BigDecimal("25000.00");
    f.eco.withdraw = new EconomyPort.Withdrawal.Success(new BigDecimal("1000"));
    var r = f.act().activate(f.signId, f.pid, "world_nether");
    assertInstanceOf(TransactionResult.Success.class, r);
    assertEquals(new BigDecimal("1000"), f.eco.lastWithdraw);
  }

  @Test
  void unknownWorldFallsBackToGlobal() {
    var f = RepairActivationTest.Fixture.with(Map.of(EquipmentSlotId.MAIN_HAND, 10));
    f.cfg.worldPrices = Map.of("world", new WorldPrice(new BigDecimal("5000"), null));
    f.cfg.hand = new BigDecimal("12000.00");
    f.cfg.all = new BigDecimal("25000.00");
    var r = f.act().activate(f.signId, f.pid, "world_the_end");
    assertInstanceOf(TransactionResult.Success.class, r);
    assertEquals(new BigDecimal("12000.00"), f.eco.lastWithdraw);
  }

  @Test
  void caseMismatchFallsBackToGlobal() {
    var f = RepairActivationTest.Fixture.with(Map.of(EquipmentSlotId.MAIN_HAND, 10));
    f.cfg.worldPrices = Map.of("world", new WorldPrice(new BigDecimal("5000"), null));
    f.cfg.hand = new BigDecimal("12000.00");
    var r = f.act().activate(f.signId, f.pid, "World");
    assertInstanceOf(TransactionResult.Success.class, r);
    assertEquals(new BigDecimal("12000.00"), f.eco.lastWithdraw);
  }

  @Test
  void nullWorldNameFallsBackToGlobal() {
    var f = RepairActivationTest.Fixture.with(Map.of(EquipmentSlotId.MAIN_HAND, 10));
    f.cfg.worldPrices = Map.of("world", new WorldPrice(new BigDecimal("5000"), null));
    f.cfg.hand = new BigDecimal("12000.00");
    var r = f.act().activate(f.signId, f.pid, (String) null);
    assertInstanceOf(TransactionResult.Success.class, r);
    assertEquals(new BigDecimal("12000.00"), f.eco.lastWithdraw);
  }

  @Test
  void emptyWorldNameFallsBackToGlobal() {
    var f = RepairActivationTest.Fixture.with(Map.of(EquipmentSlotId.MAIN_HAND, 10));
    f.cfg.worldPrices = Map.of("world", new WorldPrice(new BigDecimal("5000"), null));
    f.cfg.hand = new BigDecimal("12000.00");
    var r = f.act().activate(f.signId, f.pid, "");
    assertInstanceOf(TransactionResult.Success.class, r);
    assertEquals(new BigDecimal("12000.00"), f.eco.lastWithdraw);
  }

  @Test
  void worldPriceScaleInvalidFailsClosedNoWithdrawal() {
    var f = RepairActivationTest.Fixture.with(Map.of(EquipmentSlotId.MAIN_HAND, 10));
    f.cfg.worldPrices = Map.of("world", new WorldPrice(new BigDecimal("100.001"), null));
    f.cfg.hand = new BigDecimal("12000.00");
    var r = f.act().activate(f.signId, f.pid, "world");
    assertInstanceOf(TransactionResult.InvalidResponse.class, r);
    assertTrue(((TransactionResult.InvalidResponse) r).reason().contains("invalid-price"));
    assertEquals(0, f.eco.withdrawCalls);
  }

  @Test
  void worldHandZeroWithNonEmptyPlanRequestsWithdrawalZero() {
    var f = RepairActivationTest.Fixture.with(Map.of(EquipmentSlotId.MAIN_HAND, 10));
    f.cfg.worldPrices = Map.of("world", new WorldPrice(BigDecimal.ZERO, null));
    f.cfg.hand = new BigDecimal("12000.00");
    f.eco.withdraw = new EconomyPort.Withdrawal.Success(BigDecimal.ZERO);
    var r = f.act().activate(f.signId, f.pid, "world");
    assertInstanceOf(TransactionResult.Success.class, r);
    assertEquals(1, f.eco.withdrawCalls);
    assertEquals(0, BigDecimal.ZERO.compareTo(f.eco.lastWithdraw));
  }

  @Test
  void otherModeValidNotBypass() {
    // world.hand is bad scale, but ALL mode should use global all and succeed
    var f = RepairActivationTest.Fixture.allSlots();
    f.cfg.worldPrices = Map.of("world", new WorldPrice(new BigDecimal("100.001"), null));
    f.cfg.hand = new BigDecimal("12000.00");
    f.cfg.all = new BigDecimal("25000.00");
    f.eco.withdraw = new EconomyPort.Withdrawal.Success(new BigDecimal("25000.00"));
    var r = f.act().activate(f.signId, f.pid, "world");
    // Fixture allSlots uses ALL mode, so it should use global all=25000 not world.hand
    assertInstanceOf(TransactionResult.Success.class, r);
    assertEquals(new BigDecimal("25000.00"), f.eco.lastWithdraw);
  }
}
