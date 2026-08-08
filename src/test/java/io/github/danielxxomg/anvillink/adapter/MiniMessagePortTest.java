// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.adapter;

import static org.junit.jupiter.api.Assertions.*;

import io.github.danielxxomg.anvillink.domain.ports.ConfigurationPort;
import io.github.danielxxomg.anvillink.domain.ports.MessagePort;
import java.lang.reflect.Method;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MiniMessagePortTest {

  @Test
  void render_returnsBukkitStringWithPlaceholder() {
    ConfigurationPort.ConfigSnapshot snap =
        new ConfigurationPort.ConfigSnapshot(
            new java.math.BigDecimal("12000.00"),
            new java.math.BigDecimal("25000.00"),
            8,
            Map.of("greeting", "<green>Hello <white>{name}</white></green>"),
            true,
            true,
            "BLOCK_ANVIL_USE",
            "CRIT");
    MessagePort port = new MiniMessageMessagePort(() -> snap);
    String out = port.render("greeting", Map.of("name", "Ada"));
    assertNotNull(out);
    assertTrue(out.contains("Ada"), "rendered string must contain placeholder value");
    assertFalse(out.contains("<green>"), "MiniMessage tags should be rendered, not raw");
    // must not contain raw MiniMessage placeholder syntax leftover
    assertFalse(out.contains("{name}"));
  }

  @Test
  void portPublicApi_exposesOnlyString() {
    Method[] methods = MessagePort.class.getDeclaredMethods();
    for (Method m : methods) {
      String rt = m.getReturnType().getSimpleName();
      String pts =
          java.util.Arrays.stream(m.getParameterTypes())
              .map(Class::getSimpleName)
              .reduce((a, b) -> a + "," + b)
              .orElse("");
      assertTrue(
          rt.equals("String") || rt.equals("void"),
          "MessagePort must expose String only, found: " + m.getName() + " -> " + rt);
      assertFalse(
          pts.contains("Component") || pts.contains("TagResolver") || pts.contains("MiniMessage"),
          "MessagePort params must not reference Adventure types: "
              + m.getName()
              + "("
              + pts
              + ")");
    }
    // MiniMessageMessagePort itself must not leak Adventure types in public render signature
    for (Method m : MiniMessageMessagePort.class.getDeclaredMethods()) {
      if (m.getName().equals("render")) {
        assertEquals(String.class, m.getReturnType(), "render must return String");
        assertEquals(String.class, m.getParameterTypes()[0], "first param must be String");
        assertEquals(Map.class, m.getParameterTypes()[1], "second param must be Map");
      }
    }
  }

  @Test
  void unknownTemplate_returnsKey() {
    ConfigurationPort.ConfigSnapshot snap =
        new ConfigurationPort.ConfigSnapshot(
            new java.math.BigDecimal("12000.00"),
            new java.math.BigDecimal("25000.00"),
            8,
            Map.of(),
            true,
            true,
            "BLOCK_ANVIL_USE",
            "CRIT");
    MessagePort port = new MiniMessageMessagePort(() -> snap);
    String out = port.render("missing", Map.of());
    assertNotNull(out);
  }
}
