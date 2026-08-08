// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.domain;

import static org.junit.jupiter.api.Assertions.*;

import io.github.danielxxomg.anvillink.domain.ports.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;

class RepairActivationTest {
  @Test
  void emptyPlan_isFree_noVault() {
    var f = Fixture.with(Map.of());
    assertInstanceOf(TransactionResult.Success.class, f.act().activate(f.signId, f.pid));
    assertEquals(0, f.eco.withdrawCalls);
    assertEquals(0, f.eq.applyCalls);
  }

  @Test
  void noProvider_failClosed() {
    var f = Fixture.with(Map.of(EquipmentSlotId.MAIN_HAND, 10));
    f.eco.withdraw = new EconomyPort.Withdrawal.NoProvider();
    assertInstanceOf(TransactionResult.NoProvider.class, f.act().activate(f.signId, f.pid));
    assertEquals(0, f.eq.applyCalls);
  }

  @Test
  void insufficientFunds_noSecondWithdraw_noRepair() {
    var f = Fixture.with(Map.of(EquipmentSlotId.MAIN_HAND, 10));
    f.eco.withdraw = new EconomyPort.Withdrawal.InsufficientFunds();
    assertInstanceOf(TransactionResult.InsufficientFunds.class, f.act().activate(f.signId, f.pid));
    assertEquals(1, f.eco.withdrawCalls);
    assertEquals(0, f.eq.applyCalls);
  }

  @Test
  void textAlone_hasNoAuthority() {
    var f = Fixture.with(Map.of(EquipmentSlotId.MAIN_HAND, 10));
    f.sign.load = Optional.empty();
    f.sign.front = Optional.of(new SignPort.FrontText("[repair]", "HAND"));
    assertInstanceOf(TransactionResult.InvalidResponse.class, f.act().activate(f.signId, f.pid));
    assertEquals(0, f.eco.withdrawCalls);
  }

  @Test
  void handWithdrawsPriceHand() {
    var f = Fixture.with(Map.of(EquipmentSlotId.MAIN_HAND, 10));
    f.cfg.hand = new BigDecimal("12000.00");
    f.cfg.all = new BigDecimal("25000.00");
    var r = f.act().activate(f.signId, f.pid);
    assertInstanceOf(TransactionResult.Success.class, r);
    assertEquals(new BigDecimal("12000.00"), ((TransactionResult.Success) r).amount());
    assertEquals(1, f.eco.withdrawCalls);
    assertEquals(new BigDecimal("12000.00"), f.eco.lastWithdraw);
    assertEquals(1, ((TransactionResult.Success) r).repairedCount());
  }

  @Test
  void allWithdrawsPriceAll() {
    var f = Fixture.allSlots();
    f.cfg.hand = new BigDecimal("12000.00");
    f.cfg.all = new BigDecimal("25000.00");
    f.eco.withdraw = new EconomyPort.Withdrawal.Success(new BigDecimal("25000.00"));
    var r = f.act().activate(f.signId, f.pid);
    assertInstanceOf(TransactionResult.Success.class, r);
    assertEquals(new BigDecimal("25000.00"), ((TransactionResult.Success) r).amount());
    assertEquals(new BigDecimal("25000.00"), f.eco.lastWithdraw);
    assertEquals(6, ((TransactionResult.Success) r).repairedCount());
  }

  @Test
  void handPrecisionOverflow_failsClosedNoWithdrawal() {
    var f = Fixture.with(Map.of(EquipmentSlotId.MAIN_HAND, 10));
    f.cfg.hand = new BigDecimal("10000.001");
    f.cfg.all = new BigDecimal("25000.00");
    var r = f.act().activate(f.signId, f.pid);
    assertInstanceOf(TransactionResult.InvalidResponse.class, r);
    assertTrue(((TransactionResult.InvalidResponse) r).reason().contains("invalid-price"));
    assertEquals(0, f.eco.withdrawCalls);
  }

  @Test
  void allPrecisionOverflow_doesNotAffectHand() {
    var f = Fixture.with(Map.of(EquipmentSlotId.MAIN_HAND, 10));
    f.cfg.all = new BigDecimal("10000.001");
    f.cfg.hand = new BigDecimal("12000.00");
    // HAND mode should still succeed despite ALL being bad
    var r = f.act().activate(f.signId, f.pid);
    assertInstanceOf(TransactionResult.Success.class, r);
    assertEquals(new BigDecimal("12000.00"), f.eco.lastWithdraw);
  }

  @Test
  void emptyPlan_successZeroNoWithdrawal() {
    var f = Fixture.with(Map.of());
    f.cfg.hand = new BigDecimal("12000.00");
    f.cfg.all = new BigDecimal("25000.00");
    var r = f.act().activate(f.signId, f.pid);
    assertInstanceOf(TransactionResult.Success.class, r);
    assertEquals(0, ((TransactionResult.Success) r).amount().compareTo(BigDecimal.ZERO));
    assertEquals(0, ((TransactionResult.Success) r).repairedCount());
    assertEquals(0, f.eco.withdrawCalls);
  }

  @Test
  void allThreeSlots_successCountMatchesPlanned() {
    // Build a view with exactly 3 damaged slots for ALL (use fixture that returns 3)
    var f = new Fixture();
    f.sign.load = Optional.of(SignRecord.create(RepairMode.ALL, UUID.randomUUID()));
    f.sign.front = Optional.of(new SignPort.FrontText("[repair]", "ALL"));
    // main hand + helmet + chest damaged, rest empty
    f.eq.view =
        s -> {
          if (s == EquipmentSlotId.MAIN_HAND) return Fixture.item(10);
          if (s == EquipmentSlotId.HELMET) return Fixture.item(5);
          if (s == EquipmentSlotId.CHESTPLATE) return Fixture.item(6);
          return new StubItem(true, false, 0, false);
        };
    f.cfg.hand = new BigDecimal("12000.00");
    f.cfg.all = new BigDecimal("20000");
    f.eco.withdraw = new EconomyPort.Withdrawal.Success(new BigDecimal("20000"));
    var r = f.act().activate(f.signId, f.pid);
    assertInstanceOf(TransactionResult.Success.class, r);
    assertEquals(new BigDecimal("20000"), ((TransactionResult.Success) r).amount());
    assertEquals(3, ((TransactionResult.Success) r).repairedCount());
  }

  @Test
  void singleWithdrawal_enforced_perMode() {
    var f = Fixture.allSlots();
    f.cfg.hand = new BigDecimal("12000.00");
    f.cfg.all = new BigDecimal("25000.00");
    f.eco.withdraw = new EconomyPort.Withdrawal.Success(new BigDecimal("25000.00"));
    f.act().activate(f.signId, f.pid);
    assertEquals(1, f.eco.withdrawCalls);
  }

  @Test
  void paymentFailure_preservesEquipment() {
    var f = Fixture.with(Map.of(EquipmentSlotId.MAIN_HAND, 5));
    f.eco.withdraw = new EconomyPort.Withdrawal.InsufficientFunds();
    f.act().activate(f.signId, f.pid);
    assertEquals(0, f.eq.applyCalls);
    assertEquals(0, f.eco.depositCalls);
  }

  @Test
  void compensationSuccess_noNetCharge() {
    var f = Fixture.with(Map.of(EquipmentSlotId.MAIN_HAND, 10));
    f.eq.mut = List.of(planned(EquipmentSlotId.MAIN_HAND, 10));
    f.eq.fail = true;
    assertInstanceOf(
        TransactionResult.CompensationSuccess.class, f.act().activate(f.signId, f.pid));
    assertEquals(1, f.eco.depositCalls);
    assertEquals(0, f.rep.high);
  }

  @Test
  void compensationDepositFails_highSev() {
    var f = Fixture.with(Map.of(EquipmentSlotId.MAIN_HAND, 10));
    f.eq.mut = List.of(planned(EquipmentSlotId.MAIN_HAND, 10));
    f.eq.fail = true;
    f.eco.deposit = new EconomyPort.Deposit.Failure("bank-down");
    assertInstanceOf(TransactionResult.CompensationFailed.class, f.act().activate(f.signId, f.pid));
    assertEquals(1, f.rep.high);
  }

  @Test
  void restoreFails_terminal_highSev() {
    var f = Fixture.with(Map.of(EquipmentSlotId.MAIN_HAND, 10));
    f.eq.mut = List.of(planned(EquipmentSlotId.MAIN_HAND, 10));
    f.eq.fail = true;
    f.eq.restoreOk = false;
    assertInstanceOf(TransactionResult.RestorationFailed.class, f.act().activate(f.signId, f.pid));
    assertEquals(1, f.eco.depositCalls);
    assertEquals(1, f.rep.high);
  }

  private static EquipmentPort.PlannedApply planned(EquipmentSlotId s, int d) {
    return new EquipmentPort.PlannedApply(s, new ItemSnapshot(false, true, d, false));
  }

  static final class Fixture {
    final UUID pid = UUID.randomUUID();
    final SignPort.SignId signId = new SignPort.SignId("w:0,0,0");
    final StubSign sign = new StubSign();
    final StubEq eq = new StubEq();
    final StubEco eco = new StubEco();
    final StubCfg cfg = new StubCfg();
    final StubSch sch = new StubSch();
    final StubRep rep = new StubRep();

    static Fixture with(Map<EquipmentSlotId, Integer> d) {
      var f = new Fixture();
      f.sign.load = Optional.of(SignRecord.create(RepairMode.HAND, UUID.randomUUID()));
      f.sign.front = Optional.of(new SignPort.FrontText("[repair]", "HAND"));
      f.eq.view = s -> item(d.get(s));
      return f;
    }

    static Fixture allSlots() {
      var f = new Fixture();
      f.sign.load = Optional.of(SignRecord.create(RepairMode.ALL, UUID.randomUUID()));
      f.sign.front = Optional.of(new SignPort.FrontText("[repair]", "ALL"));
      f.eq.view = s -> new StubItem(false, true, 10, false);
      return f;
    }

    RepairActivation act() {
      return new RepairActivation(sign, eq, eco, sch, cfg, rep);
    }

    static ItemView item(Integer d) {
      if (d == null) return new StubItem(true, false, 0, false);
      return new StubItem(false, true, d, false);
    }
  }

  static final class StubSign implements SignPort {
    Optional<SignRecord> load = Optional.empty();
    Optional<FrontText> front = Optional.empty();

    public Optional<SignRecord> load(SignId id) {
      return load;
    }

    public boolean hasPermission(PlayerId p, String s) {
      return true;
    }

    public Optional<FrontText> frontText(SignId id) {
      return front;
    }
  }

  static final class StubEq implements EquipmentPort {
    EquipmentView view = s -> new StubItem(true, false, 0, false);
    int applyCalls;
    List<PlannedApply> mut;
    boolean restoreOk = true;
    boolean fail;

    public EquipmentView viewOf(PlayerHandle h) {
      return view;
    }

    public ApplyOutcome applyRepair(PlayerHandle h, List<PlannedApply> p) {
      applyCalls++;
      if (fail)
        return new ApplyOutcome.PartialFailure(mut != null ? mut : List.of(), List.of(), "fail");
      return new ApplyOutcome.Success(p);
    }

    public boolean restore(PlayerHandle h, PlannedApply s) {
      return restoreOk;
    }
  }

  static final class StubEco implements EconomyPort {
    int withdrawCalls, depositCalls;
    BigDecimal lastWithdraw;
    Withdrawal withdraw = new Withdrawal.Success(new BigDecimal("12000.00"));
    Deposit deposit = new Deposit.Success(new BigDecimal("12000.00"));

    public Withdrawal withdraw(UUID id, BigDecimal a) {
      withdrawCalls++;
      lastWithdraw = a;
      return withdraw;
    }

    public Deposit deposit(UUID id, BigDecimal a) {
      depositCalls++;
      return deposit;
    }

    public int fractionalDigits() {
      return 2;
    }
  }

  static final class StubCfg implements ConfigurationPort {
    BigDecimal hand = new BigDecimal("12000.00");
    BigDecimal all = new BigDecimal("25000.00");
    Map<String, WorldPrice> worldPrices = Map.of();

    public ConfigSnapshot current() {
      return new ConfigSnapshot(
          hand, all, worldPrices, 8, Map.of(), true, true, "BLOCK_ANVIL_USE", "CRIT");
    }

    public ReloadOutcome reload() {
      return new ReloadOutcome.Success(current());
    }
  }

  static final class StubSch implements SchedulerPort {
    public void runOnServerThread(Runnable t) {
      t.run();
    }

    public boolean isOnServerThread() {
      return true;
    }
  }

  static final class StubRep implements OperationalReporter {
    int high;

    public void report(Severity s, String c, EventContext e) {
      if (s == Severity.HIGH) high++;
    }
  }

  static final class StubItem implements ItemView {
    final boolean e, d, u;
    final int dmg;

    StubItem(boolean e, boolean d, int dmg, boolean u) {
      this.e = e;
      this.d = d;
      this.dmg = dmg;
      this.u = u;
    }

    public boolean isEmpty() {
      return e;
    }

    public boolean isDamageable() {
      return d;
    }

    public int damage() {
      return dmg;
    }

    public boolean isUnbreakable() {
      return u;
    }

    public ItemSnapshot snapshot() {
      return new ItemSnapshot(e, d, dmg, u);
    }
  }
}
