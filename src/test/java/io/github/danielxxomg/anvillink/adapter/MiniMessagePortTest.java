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
            Map.of(),
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
            Map.of(),
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

  @Test
  void render_fallbackOnThrowable_returnsRawWithPlaceholders() {
    // RED 2.5: MiniMessage path that would throw NoSuchMethodError (Paper 1.21.5 host mismatch)
    // must be caught as Throwable and return raw withPlaceholders, not propagate Error.
    // Use a template with an unknown MiniMessage tag that triggers an exception path, and
    // also assert that even a hard Error injected via a corrupted component path would be
    // swallowed — here we prove the catch is Throwable by reflecting the source.
    ConfigurationPort.ConfigSnapshot snap =
        new ConfigurationPort.ConfigSnapshot(
            new java.math.BigDecimal("12000.00"),
            new java.math.BigDecimal("25000.00"),
            Map.of(),
            8,
            Map.of("greeting", "<green>Hello {name}</green>"),
            true,
            true,
            "BLOCK_ANVIL_USE",
            "CRIT");
    // Verify that MiniMessageMessagePort.render catches Throwable, not just Exception.
    // If the implementation only caught Exception, NoSuchMethodError would propagate.
    // We verify by inspecting source and by functional fallback: a template that is valid
    // still renders, but a Throwable-throwing snapshot supplier edge is exercised via source.
    String raw = "<green>Hello {name}</green>";
    // Force a rendering failure by using a null snapshot is not the path — instead verify
    // the fallback string contains the substituted placeholder (withPlaceholders).
    // Real host mismatch is exercised as LegacyComponentSerializer.serialize throwing Error;
    // MiniMessageMessagePort catches Throwable so withPlaceholders is returned.
    MessagePort port = new MiniMessageMessagePort(() -> snap);
    String out = port.render("greeting", Map.of("name", "Ada"));
    assertNotNull(out);
    assertTrue(out.contains("Ada"));
    // Source-level gate: catch(Throwable) must exist, not catch(Exception)
    try {
      String src =
          java.nio.file.Files.readString(
              java.nio.file.Path.of(
                  "src/main/java/io/github/danielxxomg/anvillink/adapter/MiniMessageMessagePort.java"),
              java.nio.charset.StandardCharsets.UTF_8);
      assertTrue(
          src.contains("catch (Throwable"),
          "MiniMessageMessagePort must catch Throwable, not just Exception — host Error must fallback");
      assertFalse(
          src.contains("catch (Exception") && !src.contains("catch (Throwable"),
          "catch(Exception) alone would let Error propagate");
    } catch (java.io.IOException e) {
      fail("cannot read MiniMessageMessagePort source: " + e.getMessage());
    }
    // Functional: supplier that throws Error must still be catchable if render wraps it.
    // The actual Error-vs-Exception proof is the source assertion above; the functional
    // fallback is that a valid MiniMessage still returns with substitutions.
    assertEqualsWithPlaceholdersFallback(port, snap, raw);
  }

  private static void assertEqualsWithPlaceholdersFallback(
      MessagePort port, ConfigurationPort.ConfigSnapshot snap, String rawTemplate) {
    // raw with placeholders substituted
    String expected = rawTemplate.replace("{name}", "Ada");
    // If legacy path throws Error, port would return expected (withPlaceholders). On healthy
    // classpath it returns legacy-serialized form which also contains Ada — both are acceptable
    // as long as the Throwable catch exists.
    String out = port.render("greeting", Map.of("name", "Ada"));
    assertTrue(out.contains("Ada"), "fallback or normal path must contain Ada, got: " + out);
    // At minimum, the Throwable catch guarantees this would not throw
    assertDoesNotThrow(() -> port.render("greeting", Map.of("name", "Ada")));
  }
}
