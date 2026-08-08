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
        "price: 25.00\n"
            + "admin:\n"
            + "  target-distance: 8\n"
            + "messages:\n"
            + "  greeting: \"<green>hi</green>\"\n");
    FileConfigurationPort port = new FileConfigurationPort(file.toFile());
    assertEquals(new BigDecimal("25.00"), port.current().price());
    assertEquals(8, port.current().targetDistance());
    assertTrue(port.current().activationEnabled());

    write(
        file,
        "price: 50.00\n"
            + "admin:\n"
            + "  target-distance: 12\n"
            + "messages:\n"
            + "  greeting: \"<red>bye</red>\"\n");
    ConfigurationPort.ReloadOutcome outcome = port.reload();
    assertInstanceOf(ConfigurationPort.ReloadOutcome.Success.class, outcome);
    assertEquals(new BigDecimal("50.00"), port.current().price());
    assertEquals(12, port.current().targetDistance());
  }

  @Test
  void invalidReload_retainsPriorAndReportsFailure(@TempDir Path temp) throws Exception {
    Path file = temp.resolve("config.yml");
    write(
        file,
        "price: 25.00\n"
            + "admin:\n"
            + "  target-distance: 8\n"
            + "messages:\n"
            + "  greeting: \"<green>hi</green>\"\n");
    FileConfigurationPort port = new FileConfigurationPort(file.toFile());
    BigDecimal priorPrice = port.current().price();

    write(
        file,
        "price: -5.00\n"
            + "admin:\n"
            + "  target-distance: 99\n"
            + "messages:\n"
            + "  greeting: \"bad\"\n");
    ConfigurationPort.ReloadOutcome outcome = port.reload();
    assertInstanceOf(ConfigurationPort.ReloadOutcome.Failure.class, outcome);
    ConfigurationPort.ReloadOutcome.Failure failure =
        (ConfigurationPort.ReloadOutcome.Failure) outcome;
    assertNotNull(failure.reason());
    assertEquals(priorPrice, port.current().price());
    assertEquals(priorPrice, failure.retained().price());
  }

  @Test
  void invalidStartup_disablesActivation(@TempDir Path temp) throws Exception {
    Path file = temp.resolve("config.yml");
    write(
        file,
        "price: -10.00\n"
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
        "price: 25.00\n" + "admin:\n" + "  target-distance: 8\n" + "messages:\n" + "  g: \"hi\"\n");
    FileConfigurationPort port = new FileConfigurationPort(file.toFile());
    write(
        file,
        "price: 25.00\n"
            + "admin:\n"
            + "  target-distance: 99\n"
            + "messages:\n"
            + "  g: \"hi\"\n");
    ConfigurationPort.ReloadOutcome outcome = port.reload();
    assertInstanceOf(ConfigurationPort.ReloadOutcome.Failure.class, outcome);
    assertEquals(8, port.current().targetDistance());
  }

  private static void write(Path file, String content) throws Exception {
    Files.writeString(file, content, StandardCharsets.UTF_8);
  }
}
