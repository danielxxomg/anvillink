// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.adapter;

import static org.junit.jupiter.api.Assertions.*;

import io.github.danielxxomg.anvillink.domain.ports.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

class BukkitFeedbackAdapterTest {

  static final class StubConfig implements ConfigurationPort {
    ConfigSnapshot snap;

    StubConfig(boolean enabled) {
      snap =
          new ConfigSnapshot(
              new BigDecimal("12000.00"),
              new BigDecimal("25000.00"),
              Map.of(),
              8,
              Map.of("repair-success", "<green>{count} {price}</green>"),
              true,
              enabled,
              "BLOCK_ANVIL_USE",
              "CRIT");
    }

    @Override
    public ConfigSnapshot current() {
      return snap;
    }

    @Override
    public ReloadOutcome reload() {
      return new ReloadOutcome.Success(snap);
    }
  }

  static final class CountingMessage implements MessagePort {
    int calls;
    String lastKey;
    Map<String, String> lastPlaceholders;

    @Override
    public String render(String templateKey, Map<String, String> placeholders) {
      calls++;
      lastKey = templateKey;
      lastPlaceholders = placeholders != null ? new HashMap<>(placeholders) : null;
      return "rendered";
    }
  }

  static final class ThrowingMessage implements MessagePort {
    @Override
    public String render(String templateKey, Map<String, String> placeholders) {
      throw new RuntimeException("boom");
    }
  }

  static final class SyncScheduler implements SchedulerPort {
    int calls;

    @Override
    public void runOnServerThread(Runnable task) {
      calls++;
      task.run();
    }

    @Override
    public boolean isOnServerThread() {
      return true;
    }
  }

  private BukkitFeedbackAdapter adapter(
      ConfigurationPort cfg, MessagePort msg, SchedulerPort sch, Function<UUID, Player> lookup) {
    return new BukkitFeedbackAdapter(cfg, msg, sch, lookup);
  }

  @Test
  void disabled_noOpsNoRenderNoSchedulerDispatch() {
    var cfg = new StubConfig(false);
    var msg = new CountingMessage();
    var sch = new SyncScheduler();
    Function<UUID, Player> lookup =
        id -> {
          fail("player lookup must not be called when disabled");
          return null;
        };
    var a = adapter(cfg, msg, sch, lookup);
    a.play(new SignPort.PlayerId(UUID.randomUUID()), new BigDecimal("20000"), 4);
    assertEquals(0, msg.calls, "disabled must not render");
    assertEquals(0, sch.calls, "disabled must not dispatch to scheduler");
  }

  @Test
  void enabled_rendersWithCountAndPlainStringPrice_onServerThread() {
    var cfg = new StubConfig(true);
    var msg = new CountingMessage();
    var sch = new SyncScheduler();
    Function<UUID, Player> lookup = id -> null; // player may be null; adapter must not throw
    var a = adapter(cfg, msg, sch, lookup);
    a.play(new SignPort.PlayerId(UUID.randomUUID()), new BigDecimal("20000"), 4);
    assertEquals(1, sch.calls, "must dispatch via SchedulerPort");
    assertEquals(1, msg.calls);
    assertEquals("repair-success", msg.lastKey);
    assertNotNull(msg.lastPlaceholders);
    assertEquals("4", msg.lastPlaceholders.get("count"));
    assertEquals("20000", msg.lastPlaceholders.get("price"));
    // toPlainString contract: no scientific notation
    var msg2 = new CountingMessage();
    var sch2 = new SyncScheduler();
    var a2 = adapter(cfg, msg2, sch2, id -> null);
    a2.play(new SignPort.PlayerId(UUID.randomUUID()), new BigDecimal("12000.00"), 1);
    assertEquals("12000.00", msg2.lastPlaceholders.get("price"));
  }

  @Test
  void messageThrow_swallowedDoesNotPropagate() {
    var cfg = new StubConfig(true);
    var msg = new ThrowingMessage();
    var sch = new SyncScheduler();
    var a = adapter(cfg, msg, sch, id -> null);
    assertDoesNotThrow(
        () -> a.play(new SignPort.PlayerId(UUID.randomUUID()), new BigDecimal("20000"), 2));
    assertEquals(1, sch.calls);
  }

  @Test
  void enabled_usesToPlainString_notScientificNotation() {
    var cfg = new StubConfig(true);
    var msg = new CountingMessage();
    var sch = new SyncScheduler();
    var a = adapter(cfg, msg, sch, id -> null);
    // large value that would be scientific with toString for some scales
    a.play(new SignPort.PlayerId(UUID.randomUUID()), new BigDecimal("1E+5"), 1);
    // toPlainString of 1E+5 is 100000
    assertEquals("100000", msg.lastPlaceholders.get("price"));
  }
}
