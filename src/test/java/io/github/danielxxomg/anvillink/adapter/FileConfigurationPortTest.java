// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.adapter;

import static org.junit.jupiter.api.Assertions.*;

import io.github.danielxxomg.anvillink.domain.ports.ConfigurationPort;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileConfigurationPortTest {

  @Test
  void validReload_atomicallySwaps(@TempDir Path temp) throws Exception {
    Path file = temp.resolve("config.yml");
    write(
        file,
        "price:\n  hand: 12000.00\n  all: 25000.00\n"
            + "admin:\n"
            + "  target-distance: 8\n"
            + "messages:\n"
            + "  greeting: \"<green>hi</green>\"\n");
    FileConfigurationPort port = new FileConfigurationPort(file.toFile());
    assertEquals(new BigDecimal("12000.00"), port.current().priceHand());
    assertEquals(new BigDecimal("25000.00"), port.current().priceAll());
    assertEquals(8, port.current().targetDistance());
    assertTrue(port.current().activationEnabled());

    write(
        file,
        "price:\n  hand: 15000.00\n  all: 30000.00\n"
            + "admin:\n"
            + "  target-distance: 12\n"
            + "messages:\n"
            + "  greeting: \"<red>bye</red>\"\n");
    ConfigurationPort.ReloadOutcome outcome = port.reload();
    assertInstanceOf(ConfigurationPort.ReloadOutcome.Success.class, outcome);
    assertEquals(new BigDecimal("15000.00"), port.current().priceHand());
    assertEquals(new BigDecimal("30000.00"), port.current().priceAll());
    assertEquals(12, port.current().targetDistance());
  }

  @Test
  void invalidReload_retainsPriorAndReportsFailure(@TempDir Path temp) throws Exception {
    Path file = temp.resolve("config.yml");
    write(
        file,
        "price:\n  hand: 12000.00\n  all: 25000.00\n"
            + "admin:\n"
            + "  target-distance: 8\n"
            + "messages:\n"
            + "  greeting: \"<green>hi</green>\"\n");
    FileConfigurationPort port = new FileConfigurationPort(file.toFile());
    BigDecimal priorHand = port.current().priceHand();
    BigDecimal priorAll = port.current().priceAll();

    write(
        file,
        "price:\n  hand: 9999.99\n  all: 25000.00\n"
            + "admin:\n"
            + "  target-distance: 8\n"
            + "messages:\n"
            + "  greeting: \"bad\"\n");
    ConfigurationPort.ReloadOutcome outcome = port.reload();
    assertInstanceOf(ConfigurationPort.ReloadOutcome.Failure.class, outcome);
    ConfigurationPort.ReloadOutcome.Failure failure =
        (ConfigurationPort.ReloadOutcome.Failure) outcome;
    assertNotNull(failure.reason());
    assertEquals(priorHand, port.current().priceHand());
    assertEquals(priorAll, port.current().priceAll());
    assertEquals(priorHand, failure.retained().priceHand());
  }

  @Test
  void invalidStartup_disablesActivation(@TempDir Path temp) throws Exception {
    Path file = temp.resolve("config.yml");
    write(
        file,
        "price:\n  hand: 9999.99\n  all: 25000.00\n"
            + "admin:\n"
            + "  target-distance: 8\n"
            + "messages:\n"
            + "  greeting: \"hi\"\n");
    FileConfigurationPort port = new FileConfigurationPort(file.toFile());
    assertFalse(port.current().activationEnabled());
  }

  @Test
  void invalidTargetDistance_rejectedOnReload(@TempDir Path temp) throws Exception {
    Path file = temp.resolve("config.yml");
    write(
        file,
        "price:\n  hand: 12000.00\n  all: 25000.00\n"
            + "admin:\n"
            + "  target-distance: 8\n"
            + "messages:\n"
            + "  g: \"hi\"\n");
    FileConfigurationPort port = new FileConfigurationPort(file.toFile());
    write(
        file,
        "price:\n  hand: 12000.00\n  all: 25000.00\n"
            + "admin:\n"
            + "  target-distance: 99\n"
            + "messages:\n"
            + "  g: \"hi\"\n");
    ConfigurationPort.ReloadOutcome outcome = port.reload();
    assertInstanceOf(ConfigurationPort.ReloadOutcome.Failure.class, outcome);
    assertEquals(8, port.current().targetDistance());
  }

  @Test
  void bareScalarPrice_rejectedWithMissingHand(@TempDir Path temp) throws Exception {
    Path file = temp.resolve("config.yml");
    write(
        file,
        "price: 25.00\n" + "admin:\n" + "  target-distance: 8\n" + "messages:\n" + "  g: \"hi\"\n");
    FileConfigurationPort port = new FileConfigurationPort(file.toFile());
    assertFalse(port.current().activationEnabled());
    // reload from valid should also fail
    write(
        file,
        "price:\n  hand: 12000.00\n  all: 25000.00\n"
            + "admin:\n"
            + "  target-distance: 8\n"
            + "messages:\n"
            + "  g: \"hi\"\n");
    port = new FileConfigurationPort(file.toFile());
    assertTrue(port.current().activationEnabled());
    write(file, "price: 25.00\nadmin:\n  target-distance: 8\nmessages:\n  g: \"hi\"\n");
    ConfigurationPort.ReloadOutcome outcome = port.reload();
    assertInstanceOf(ConfigurationPort.ReloadOutcome.Failure.class, outcome);
    assertTrue(
        ((ConfigurationPort.ReloadOutcome.Failure) outcome)
            .reason()
            .contains("missing price.hand"));
    assertEquals(new BigDecimal("12000.00"), port.current().priceHand());
  }

  @Test
  void missingHand_rejected(@TempDir Path temp) throws Exception {
    Path file = temp.resolve("config.yml");
    write(file, "price:\n  all: 25000.00\nadmin:\n  target-distance: 8\nmessages:\n  g: \"hi\"\n");
    FileConfigurationPort port = new FileConfigurationPort(file.toFile());
    assertFalse(port.current().activationEnabled());
  }

  @Test
  void missingAll_rejected(@TempDir Path temp) throws Exception {
    Path file = temp.resolve("config.yml");
    write(file, "price:\n  hand: 12000.00\nadmin:\n  target-distance: 8\nmessages:\n  g: \"hi\"\n");
    FileConfigurationPort port = new FileConfigurationPort(file.toFile());
    assertFalse(port.current().activationEnabled());
  }

  @Test
  void belowFloorHand_rejected(@TempDir Path temp) throws Exception {
    Path file = temp.resolve("config.yml");
    write(
        file,
        "price:\n  hand: 9999.99\n  all: 25000.00\n"
            + "admin:\n  target-distance: 8\nmessages:\n  g: \"hi\"\n");
    FileConfigurationPort port = new FileConfigurationPort(file.toFile());
    assertFalse(port.current().activationEnabled());
  }

  @Test
  void belowFloorAll_rejected(@TempDir Path temp) throws Exception {
    Path file = temp.resolve("config.yml");
    write(
        file,
        "price:\n  hand: 12000.00\n  all: 5000\n"
            + "admin:\n  target-distance: 8\nmessages:\n  g: \"hi\"\n");
    FileConfigurationPort port = new FileConfigurationPort(file.toFile());
    assertFalse(port.current().activationEnabled());
  }

  @Test
  void emptyPriceBlock_rejected(@TempDir Path temp) throws Exception {
    Path file = temp.resolve("config.yml");
    write(file, "price:\nadmin:\n  target-distance: 8\nmessages:\n  g: \"hi\"\n");
    FileConfigurationPort port = new FileConfigurationPort(file.toFile());
    assertFalse(port.current().activationEnabled());
  }

  @Test
  void feedbackDefaults_whenAbsent(@TempDir Path temp) throws Exception {
    Path file = temp.resolve("config.yml");
    write(
        file,
        "price:\n  hand: 12000.00\n  all: 25000.00\n"
            + "admin:\n  target-distance: 8\nmessages:\n  g: \"hi\"\n");
    FileConfigurationPort port = new FileConfigurationPort(file.toFile());
    assertTrue(port.current().feedbackEnabled());
    assertEquals("BLOCK_ANVIL_USE", port.current().feedbackSound());
    assertEquals("CRIT", port.current().feedbackParticles());
  }

  private static void write(Path file, String content) throws Exception {
    Files.writeString(file, content, StandardCharsets.UTF_8);
  }
}
