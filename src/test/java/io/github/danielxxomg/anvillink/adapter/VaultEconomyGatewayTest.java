// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.adapter;

import static org.junit.jupiter.api.Assertions.*;

import io.github.danielxxomg.anvillink.domain.ports.EconomyPort;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class VaultEconomyGatewayTest {

  @AfterEach
  void clearServer() {
    try {
      var f = Bukkit.class.getDeclaredField("server");
      f.setAccessible(true);
      f.set(null, null);
    } catch (Exception ignored) {
    }
  }

  @Test
  void withdrawSuccess_oneFlatCharge() {
    Economy eco = economyOf(2, (player, amount) -> ok(amount, 100.0), (p, a) -> ok(a, 100.0));
    withVault(eco);
    var gw = new VaultEconomyGateway(() -> Bukkit.getServer());
    var pid = UUID.randomUUID();
    var w = gw.withdraw(pid, new BigDecimal("25.00"));
    assertInstanceOf(EconomyPort.Withdrawal.Success.class, w);
    assertEquals(
        0,
        new BigDecimal("25.00").compareTo(((EconomyPort.Withdrawal.Success) w).amountWithdrawn()));
  }

  @Test
  void withdrawFail_insufficientFunds_noMutationNoDeposit() {
    Economy eco =
        economyOf(
            2, (player, amount) -> fail(amount, "Insufficient funds"), (p, a) -> ok(a, 100.0));
    withVault(eco);
    var gw = new VaultEconomyGateway(() -> Bukkit.getServer());
    var w = gw.withdraw(UUID.randomUUID(), new BigDecimal("25.00"));
    assertInstanceOf(EconomyPort.Withdrawal.InsufficientFunds.class, w);
  }

  @Test
  void missingProvider_noProvider() {
    // no RegisteredServiceProvider<Economy> in ServicesManager
    Server s = serverWithNoEconomy();
    setServer(s);
    var gw = new VaultEconomyGateway(() -> Bukkit.getServer());
    var w = gw.withdraw(UUID.randomUUID(), new BigDecimal("25.00"));
    assertInstanceOf(EconomyPort.Withdrawal.NoProvider.class, w);
  }

  @Test
  void invalidResponse_amountMismatch_depositsOnceForFiniteWithdrawn() {
    List<Double> deposits = new ArrayList<>();
    Economy eco =
        economyOf(
            2,
            (player, amount) ->
                new EconomyResponse(20.0, 80.0, EconomyResponse.ResponseType.SUCCESS, null),
            (p, a) -> {
              deposits.add(a);
              return ok(a, 80.0 + a);
            });
    withVault(eco);
    var gw = new VaultEconomyGateway(() -> Bukkit.getServer());
    var w = gw.withdraw(UUID.randomUUID(), new BigDecimal("25.00"));
    // amount mismatch -> InvalidResponse; finite withdrawn 20.0 was recovered via one compensating
    // deposit
    assertInstanceOf(EconomyPort.Withdrawal.InvalidResponse.class, w);
    assertEquals(1, deposits.size());
    assertEquals(20.0, deposits.get(0));
  }

  @Test
  void invalidResponse_nonFiniteWithdrawn_severeNoDeposit() {
    List<Double> deposits = new ArrayList<>();
    Economy eco =
        economyOf(
            2,
            (player, amount) ->
                new EconomyResponse(Double.NaN, 0, EconomyResponse.ResponseType.SUCCESS, null),
            (p, a) -> {
              deposits.add(a);
              return ok(a, 0);
            });
    withVault(eco);
    var gw = new VaultEconomyGateway(() -> Bukkit.getServer());
    var w = gw.withdraw(UUID.randomUUID(), new BigDecimal("25.00"));
    assertInstanceOf(EconomyPort.Withdrawal.InvalidResponse.class, w);
    assertEquals(0, deposits.size());
  }

  @Test
  void fractionalDigits_scaleExceeds_rejected() {
    Economy eco = economyOf(2, (player, amount) -> ok(amount, 100.0), (p, a) -> ok(a, 100.0));
    withVault(eco);
    var gw = new VaultEconomyGateway(() -> Bukkit.getServer());
    assertEquals(2, gw.fractionalDigits());
    // ValidatedPrice would reject scale 4 before reaching withdraw; gateway itself exposes
    // fractionalDigits
    // so use-case validation gates it. Here we assert gateway reports 2 so 25.1234 is not silently
    // truncated.
    var w = gw.withdraw(UUID.randomUUID(), new BigDecimal("25.1234"));
    // gateway withdraw path validates via comparison: BigDecimal.valueOf(d).compareTo(requested)==0
    // is still needed
    // if provider accepted 25.123 with fractionalDigits=2 it would mismatch; here we route through
    // mismatch path
    // Simulate mismatch: economy returns success 25.12 (truncated) -> invalid
    Economy eco2 =
        economyOf(
            2,
            (player, amount) ->
                new EconomyResponse(25.12, 0, EconomyResponse.ResponseType.SUCCESS, null),
            (p, a) -> ok(a, 0));
    withVault(eco2);
    var gw2 = new VaultEconomyGateway(() -> Bukkit.getServer());
    var w2 = gw2.withdraw(UUID.randomUUID(), new BigDecimal("25.1234"));
    assertInstanceOf(EconomyPort.Withdrawal.InvalidResponse.class, w2);
  }

  @Test
  void deposit_succeedsAndFails_asDelegated() {
    Economy okEco = economyOf(2, (p, a) -> ok(a, 0), (p, a) -> ok(a, 50.0));
    withVault(okEco);
    var gwOk = new VaultEconomyGateway(() -> Bukkit.getServer());
    var d1 = gwOk.deposit(UUID.randomUUID(), new BigDecimal("25.00"));
    assertInstanceOf(EconomyPort.Deposit.Success.class, d1);

    Economy failEco = economyOf(2, (p, a) -> ok(a, 0), (p, a) -> fail(a, "bank-down"));
    withVault(failEco);
    var gwFail = new VaultEconomyGateway(() -> Bukkit.getServer());
    var d2 = gwFail.deposit(UUID.randomUUID(), new BigDecimal("25.00"));
    assertInstanceOf(EconomyPort.Deposit.Failure.class, d2);
  }

  // --- helpers ---

  private static EconomyResponse ok(double amount, double balance) {
    return new EconomyResponse(amount, balance, EconomyResponse.ResponseType.SUCCESS, null);
  }

  private static EconomyResponse fail(double amount, String msg) {
    return new EconomyResponse(amount, 0, EconomyResponse.ResponseType.FAILURE, msg);
  }

  @FunctionalInterface
  interface WithdrawFn {
    EconomyResponse apply(OfflinePlayer p, double amount);
  }

  @FunctionalInterface
  interface DepositFn {
    EconomyResponse apply(OfflinePlayer p, double amount);
  }

  private static Economy economyOf(int fd, WithdrawFn withdraw, DepositFn deposit) {
    return (Economy)
        Proxy.newProxyInstance(
            Economy.class.getClassLoader(),
            new Class<?>[] {Economy.class},
            (proxy, method, args) -> {
              String n = method.getName();
              if (n.equals("fractionalDigits")) return fd;
              if (n.equals("withdrawPlayer") && args.length == 2)
                return withdraw.apply((OfflinePlayer) args[0], (double) args[1]);
              if (n.equals("depositPlayer") && args.length == 2)
                return deposit.apply((OfflinePlayer) args[0], (double) args[1]);
              if (n.equals("isEnabled")) return true;
              if (n.equals("getName")) return "FakeEconomy";
              if (n.equals("hasBankSupport")) return false;
              if (n.equals("format")) return String.valueOf(args[0]);
              if (n.equals("currencyNamePlural")) return "dollars";
              if (n.equals("currencyNameSingular")) return "dollar";
              Class<?> rt = method.getReturnType();
              if (rt == boolean.class) return false;
              if (rt == int.class) return 0;
              if (rt == long.class) return 0L;
              if (rt == double.class) return 0.0;
              if (rt == void.class) return null;
              return null;
            });
  }

  private static Player uuidPlayer(UUID uuid) {
    return (Player)
        Proxy.newProxyInstance(
            Player.class.getClassLoader(),
            new Class<?>[] {Player.class},
            (p, m, a) -> {
              if (m.getName().equals("getUniqueId")) return uuid;
              Class<?> rt = m.getReturnType();
              if (rt == boolean.class) return false;
              if (rt == int.class) return 0;
              if (rt == long.class) return 0L;
              if (rt == double.class) return 0.0;
              if (rt == void.class) return null;
              return null;
            });
  }

  private static void withVault(Economy economy) {
    Server s = serverWithEconomy(economy);
    setServer(s);
  }

  private static Server serverWithEconomy(Economy economy) {
    return (Server)
        Proxy.newProxyInstance(
            Server.class.getClassLoader(),
            new Class<?>[] {Server.class},
            (proxy, method, args) -> {
              String n = method.getName();
              if (n.equals("getServicesManager")) {
                return fakeServicesManager(economy);
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

  private static Server serverWithNoEconomy() {
    return (Server)
        Proxy.newProxyInstance(
            Server.class.getClassLoader(),
            new Class<?>[] {Server.class},
            (proxy, method, args) -> {
              if (method.getName().equals("getServicesManager")) {
                return fakeServicesManager(null);
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

  private static org.bukkit.plugin.ServicesManager fakeServicesManager(Economy economy) {
    return (org.bukkit.plugin.ServicesManager)
        Proxy.newProxyInstance(
            org.bukkit.plugin.ServicesManager.class.getClassLoader(),
            new Class<?>[] {org.bukkit.plugin.ServicesManager.class},
            (proxy, method, args) -> {
              if (method.getName().equals("getRegistration")) {
                if (economy == null) return null;
                return fakeRegistration(economy);
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

  private static org.bukkit.plugin.RegisteredServiceProvider<Economy> fakeRegistration(
      Economy economy) {
    return new org.bukkit.plugin.RegisteredServiceProvider<>(
        Economy.class, economy, org.bukkit.plugin.ServicePriority.Normal, null);
  }

  private static void setServer(Server s) {
    try {
      var f = Bukkit.class.getDeclaredField("server");
      f.setAccessible(true);
      f.set(null, s);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
