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

class FileConfigurationPortIntegrationTest {

  @Test
  void reloadWithBadWorldRetainsPriorA(@TempDir Path temp) throws Exception {
    Path file = temp.resolve("config.yml");
    write(
        file,
        "price:\n  hand: 12000.00\n  all: 25000.00\nworlds:\n  a:\n    hand: 100\nadmin:\n  target-distance: 8\nmessages:\n  g: hi\n");
    FileConfigurationPort port = new FileConfigurationPort(file.toFile());
    assertTrue(port.current().activationEnabled());
    assertEquals(new BigDecimal("100"), port.current().worldPrices().get("a").hand());

    write(
        file,
        "price:\n  hand: 12000.00\n  all: 25000.00\nworlds:\n  b:\n    hand: -1\nadmin:\n  target-distance: 8\nmessages:\n  g: hi\n");
    var outcome = port.reload();
    assertInstanceOf(ConfigurationPort.ReloadOutcome.Failure.class, outcome);
    assertTrue(
        ((ConfigurationPort.ReloadOutcome.Failure) outcome).reason().contains("worlds.b.hand"));
    assertNotNull(port.current().worldPrices().get("a"), "must retain prior worlds.a");
    assertEquals(new BigDecimal("100"), port.current().worldPrices().get("a").hand());
  }

  @Test
  void validPartialWorldSurvivesReloadUnknownStillGlobal(@TempDir Path temp) throws Exception {
    Path file = temp.resolve("config.yml");
    write(
        file,
        "price:\n  hand: 12000.00\n  all: 25000.00\nworlds:\n  world:\n    hand: 5000\nadmin:\n  target-distance: 8\nmessages:\n  g: hi\n");
    FileConfigurationPort port = new FileConfigurationPort(file.toFile());
    assertTrue(port.current().activationEnabled());
    assertEquals(new BigDecimal("5000"), port.current().worldPrices().get("world").hand());
    assertNull(port.current().worldPrices().get("world").all());
    // unknown world still global after reload — no entry for world_the_end
    assertNull(port.current().worldPrices().get("world_the_end"));

    // reload with same valid partial still ok
    write(
        file,
        "price:\n  hand: 12000.00\n  all: 25000.00\nworlds:\n  world:\n    hand: 5000\nadmin:\n  target-distance: 8\nmessages:\n  g: hi\n");
    var outcome = port.reload();
    assertInstanceOf(ConfigurationPort.ReloadOutcome.Success.class, outcome);
    assertEquals(new BigDecimal("5000"), port.current().worldPrices().get("world").hand());
    assertNull(port.current().worldPrices().get("world_the_end"));
  }

  private static void write(Path file, String content) throws Exception {
    Files.writeString(file, content, StandardCharsets.UTF_8);
  }
}
