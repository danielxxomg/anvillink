// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.domain;

import io.github.danielxxomg.anvillink.domain.ports.*;
import java.math.BigDecimal;
import java.util.*;

public final class RepairActivation {
  private final SignPort signs;
  private final EquipmentPort equipment;
  private final EconomyPort economy;
  private final SchedulerPort scheduler;
  private final ConfigurationPort config;
  private final OperationalReporter reporter;
  private final RepairPlanner planner = new RepairPlanner();

  public RepairActivation(
      SignPort signs,
      EquipmentPort equipment,
      EconomyPort economy,
      SchedulerPort scheduler,
      ConfigurationPort config,
      OperationalReporter reporter) {
    this.signs = signs;
    this.equipment = equipment;
    this.economy = economy;
    this.scheduler = scheduler;
    this.config = config;
    this.reporter = reporter;
  }

  public TransactionResult activate(SignPort.SignId id, UUID player) {
    return activate(id, player, null);
  }

  public TransactionResult activate(SignPort.SignId id, UUID player, String worldName) {
    var rec = signs.load(id);
    if (rec.isEmpty()) return new TransactionResult.InvalidResponse("missing-pdc");
    if (!signs.hasPermission(new SignPort.PlayerId(player), "anvillink.use")) {
      return new TransactionResult.InvalidResponse("missing-permission");
    }
    var ft = signs.frontText(id);
    if (ft.isPresent()) {
      var pr = new SignParser().parse(ft.get().line1(), ft.get().line2());
      if (pr.isEmpty() || pr.get().mode() != rec.get().mode()) {
        return new TransactionResult.InvalidResponse("tampered-text");
      }
    }
    var cfg = config.current();
    if (cfg == null || !cfg.activationEnabled())
      return new TransactionResult.InvalidResponse("activation-disabled");
    BigDecimal selected = resolveEffectivePrice(cfg, rec.get().mode(), worldName);
    ValidatedPrice price;
    try {
      price = ValidatedPrice.of(selected, economy.fractionalDigits());
    } catch (IllegalArgumentException e) {
      return new TransactionResult.InvalidResponse("invalid-price:" + e.getMessage());
    }
    var handle = new EquipmentPort.PlayerHandle(player);
    var view = equipment.viewOf(handle);
    if (view == null) return new TransactionResult.Success(BigDecimal.ZERO, 0);
    var plan = planner.plan(rec.get().mode(), view);
    if (plan.isEmpty()) return new TransactionResult.Success(BigDecimal.ZERO, 0);
    var planned =
        plan.slots().stream()
            .map(s -> new EquipmentPort.PlannedApply(s.slot(), s.snapshot()))
            .toList();
    var amount = price.value();
    var w = economy.withdraw(player, amount);
    if (w instanceof EconomyPort.Withdrawal.NoProvider) return new TransactionResult.NoProvider();
    if (w instanceof EconomyPort.Withdrawal.InsufficientFunds)
      return new TransactionResult.InsufficientFunds();
    if (w instanceof EconomyPort.Withdrawal.InvalidResponse ir)
      return new TransactionResult.InvalidResponse(ir.reason());
    var withdrawn = ((EconomyPort.Withdrawal.Success) w).amountWithdrawn();
    var out = new TransactionResult[1];
    scheduler.runOnServerThread(
        () -> apply(handle, planned, id, player, amount, withdrawn, out, planned.size()));
    return out[0] != null ? out[0] : new TransactionResult.Success(withdrawn, planned.size());
  }

  private BigDecimal resolveEffectivePrice(
      ConfigurationPort.ConfigSnapshot cfg, RepairMode mode, String worldName) {
    if (worldName != null && !worldName.isEmpty()) {
      var wp = cfg.worldPrices().get(worldName);
      if (wp != null) {
        BigDecimal perWorld = mode == RepairMode.HAND ? wp.hand() : wp.all();
        if (perWorld != null) return perWorld;
      }
    }
    return mode == RepairMode.HAND ? cfg.priceHand() : cfg.priceAll();
  }

  private void apply(
      EquipmentPort.PlayerHandle h,
      List<EquipmentPort.PlannedApply> planned,
      SignPort.SignId id,
      UUID player,
      BigDecimal amount,
      BigDecimal withdrawn,
      TransactionResult[] out,
      int repairedCount) {
    var o = equipment.applyRepair(h, planned);
    if (o instanceof EquipmentPort.ApplyOutcome.Success) {
      out[0] = new TransactionResult.Success(withdrawn, repairedCount);
      return;
    }
    var pf = (EquipmentPort.ApplyOutcome.PartialFailure) o;
    List<String> mut = new ArrayList<>(), res = new ArrayList<>(), unr = new ArrayList<>();
    for (var m : pf.mutated()) {
      mut.add(m.slot().name());
      if (equipment.restore(h, m)) res.add(m.slot().name());
      else unr.add(m.slot().name());
    }
    var d = economy.deposit(player, withdrawn);
    boolean ok = d instanceof EconomyPort.Deposit.Success;
    String outcome = ok ? "deposited" : "failed:" + ((EconomyPort.Deposit.Failure) d).reason();
    if (!unr.isEmpty()) {
      reporter.report(
          OperationalReporter.Severity.HIGH,
          "restoration-failed",
          new OperationalReporter.EventContext(
              id.value(),
              player.toString(),
              amount,
              withdrawn,
              List.copyOf(mut),
              List.copyOf(res),
              List.copyOf(unr),
              outcome,
              pf.reason()));
      out[0] = new TransactionResult.RestorationFailed(withdrawn, pf.reason());
      return;
    }
    if (!ok) {
      reporter.report(
          OperationalReporter.Severity.HIGH,
          "compensation-failed",
          new OperationalReporter.EventContext(
              id.value(),
              player.toString(),
              amount,
              withdrawn,
              List.copyOf(mut),
              List.copyOf(res),
              List.of(),
              outcome,
              ((EconomyPort.Deposit.Failure) d).reason()));
      out[0] =
          new TransactionResult.CompensationFailed(
              withdrawn, ((EconomyPort.Deposit.Failure) d).reason());
      return;
    }
    out[0] = new TransactionResult.CompensationSuccess(withdrawn);
  }
}
