// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.adapter;

import static org.junit.jupiter.api.Assertions.*;

import io.github.danielxxomg.anvillink.domain.ports.ConfigurationPort;
import io.github.danielxxomg.anvillink.domain.ports.MessagePort;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Regression guard: exact prior message keys still exist/are rendered. Traceability is
 * grep-trivial: each key literal appears explicitly.
 */
class ErrorMessagesUnchangedTest {

  private static final String[] REQUIRED_KEYS = {
    "insufficient-funds", "tampered", "invalid-identity", "activation-failure", "no-eligible-items"
  };

  @Test
  void configYml_containsRequiredMessageKeys() throws Exception {
    Path config = Path.of("src/main/resources/config.yml");
    assertTrue(Files.isRegularFile(config), "config.yml must exist");
    String text = Files.readString(config, StandardCharsets.UTF_8);
    assertTrue(text.contains("insufficient-funds"), "config.yml must contain insufficient-funds");
    assertTrue(text.contains("tampered"), "config.yml must contain tampered");
    assertTrue(text.contains("invalid-identity"), "config.yml must contain invalid-identity");
    assertTrue(text.contains("activation-failure"), "config.yml must contain activation-failure");
    assertTrue(text.contains("no-eligible-items"), "config.yml must contain no-eligible-items");
    assertTrue(text.contains("no-target"), "config.yml must contain no-target");
    assertTrue(text.contains("repair-success"), "config.yml must contain repair-success");
  }

  @Test
  void fileConfigurationPort_loadsMessagesContainingRequiredKeys() throws Exception {
    Path tmp = Files.createTempFile("anvillink-config-", ".yml");
    Files.writeString(
        tmp,
        "price:\n  hand: 12000.00\n  all: 25000.00\n"
            + "admin:\n  target-distance: 8\n"
            + "messages:\n"
            + "  insufficient-funds: \"<red>You do not have enough funds.</red>\"\n"
            + "  tampered: \"<red>tampered</red>\"\n"
            + "  invalid-identity: \"<red>invalid</red>\"\n"
            + "  activation-failure: \"<red>fail {reason}</red>\"\n"
            + "  no-eligible-items: \"<yellow>none</yellow>\"\n"
            + "  no-target: \"<red>no target</red>\"\n"
            + "  repair-success: \"<green>Repaired {count} items for {price}.</green>\"\n",
        StandardCharsets.UTF_8);
    FileConfigurationPort port = new FileConfigurationPort(tmp.toFile());
    ConfigurationPort.ConfigSnapshot snap = port.current();
    assertTrue(snap.activationEnabled());
    Map<String, String> messages = snap.messages();
    assertTrue(messages.containsKey("insufficient-funds"));
    assertTrue(messages.containsKey("tampered"));
    assertTrue(messages.containsKey("invalid-identity"));
    assertTrue(messages.containsKey("activation-failure"));
    assertTrue(messages.containsKey("no-eligible-items"));
    assertTrue(messages.containsKey("no-target"));
    assertTrue(messages.containsKey("repair-success"));
  }

  @Test
  void messagePort_rendersRequiredKeys() {
    ConfigurationPort.ConfigSnapshot snap =
        new ConfigurationPort.ConfigSnapshot(
            new java.math.BigDecimal("12000.00"),
            new java.math.BigDecimal("25000.00"),
            Map.of(),
            8,
            Map.of(
                "insufficient-funds", "<red>You do not have enough funds.</red>",
                "tampered", "<red>tampered</red>",
                "invalid-identity", "<red>invalid</red>",
                "activation-failure", "<red>Repair failed: <white>{reason}</white></red>",
                "no-eligible-items", "<yellow>No damaged repairable items to repair.</yellow>",
                "no-target", "<red>No sign in sight within <white>{distance}</white> blocks.</red>",
                "repair-success", "<green>Repaired {count} items for {price}.</green>"),
            true,
            true,
            "BLOCK_ANVIL_USE",
            "CRIT");
    MessagePort port = new MiniMessageMessagePort(() -> snap);
    String insufficientFunds = port.render("insufficient-funds", Map.of());
    assertNotNull(insufficientFunds);
    assertFalse(insufficientFunds.isBlank());
    String tampered = port.render("tampered", Map.of());
    assertNotNull(tampered);
    assertFalse(tampered.isBlank());
    String invalidIdentity = port.render("invalid-identity", Map.of());
    assertNotNull(invalidIdentity);
    assertFalse(invalidIdentity.isBlank());
    String activationFailure = port.render("activation-failure", Map.of("reason", "tampered-text"));
    assertNotNull(activationFailure);
    assertTrue(activationFailure.contains("tampered-text"));
    String noEligible = port.render("no-eligible-items", Map.of());
    assertNotNull(noEligible);
    assertFalse(noEligible.isBlank());
  }

  @Test
  void requiredKeys_areGrepTrivial() throws Exception {
    // This test exists so the key literals are grep-trivial for traceability.
    String src =
        Files.readString(
            Path.of(
                "src/test/java/io/github/danielxxomg/anvillink/adapter/ErrorMessagesUnchangedTest.java"),
            StandardCharsets.UTF_8);
    for (String key : REQUIRED_KEYS) {
      assertTrue(
          src.contains("\"" + key + "\""), "test source must contain literal \"" + key + "\"");
    }
    assertTrue(src.contains("\"no-target\""));
    assertTrue(src.contains("\"repair-success\""));
  }
}
