// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.adapter;

import static org.junit.jupiter.api.Assertions.*;

import io.github.danielxxomg.anvillink.domain.WorldPrice;
import io.github.danielxxomg.anvillink.domain.ports.ConfigurationPort;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileConfigurationPortWorldsTest {

  @Test
  void handOnlyWorldValid(@TempDir Path temp) throws Exception {
    Path file = temp.resolve("config.yml");
    write(
        file,
        "price:\n  hand: 12000.00\n  all: 25000.00\nworlds:\n  world:\n    hand: 5000\n"
            + "admin:\n  target-distance: 8\nmessages:\n  g: hi\n");
    FileConfigurationPort port = new FileConfigurationPort(file.toFile());
    assertTrue(port.current().activationEnabled());
    WorldPrice wp = port.current().worldPrices().get("world");
    assertNotNull(wp);
    assertEquals(new BigDecimal("5000"), wp.hand());
    assertNull(wp.all());
  }

  @Test
  void allOnlyWorldValid(@TempDir Path temp) throws Exception {
    Path file = temp.resolve("config.yml");
    write(
        file,
        "price:\n  hand: 12000.00\n  all: 25000.00\nworlds:\n  world_nether:\n    all: 1000\n"
            + "admin:\n  target-distance: 8\nmessages:\n  g: hi\n");
    FileConfigurationPort port = new FileConfigurationPort(file.toFile());
    assertTrue(port.current().activationEnabled());
    WorldPrice wp = port.current().worldPrices().get("world_nether");
    assertNotNull(wp);
    assertNull(wp.hand());
    assertEquals(new BigDecimal("1000"), wp.all());
  }

  @Test
  void bothValid(@TempDir Path temp) throws Exception {
    Path file = temp.resolve("config.yml");
    write(
        file,
        "price:\n  hand: 12000.00\n  all: 25000.00\nworlds:\n  world:\n    hand: 5000\n    all: 8000\n"
            + "admin:\n  target-distance: 8\nmessages:\n  g: hi\n");
    FileConfigurationPort port = new FileConfigurationPort(file.toFile());
    assertTrue(port.current().activationEnabled());
    WorldPrice wp = port.current().worldPrices().get("world");
    assertEquals(new BigDecimal("5000"), wp.hand());
    assertEquals(new BigDecimal("8000"), wp.all());
  }

  @Test
  void emptyWorldsValid(@TempDir Path temp) throws Exception {
    Path file = temp.resolve("config.yml");
    write(
        file,
        "price:\n  hand: 12000.00\n  all: 25000.00\nworlds:\n"
            + "admin:\n  target-distance: 8\nmessages:\n  g: hi\n");
    FileConfigurationPort port = new FileConfigurationPort(file.toFile());
    assertTrue(port.current().activationEnabled());
    assertTrue(port.current().worldPrices().isEmpty());
  }

  @Test
  void unknownSubkeyWarnsNotInvalid(@TempDir Path temp) throws Exception {
    Path file = temp.resolve("config.yml");
    write(
        file,
        "price:\n  hand: 12000.00\n  all: 25000.00\nworlds:\n  world:\n    hand: 5000\n    foo: 999\n"
            + "admin:\n  target-distance: 8\nmessages:\n  g: hi\n");
    FileConfigurationPort port = new FileConfigurationPort(file.toFile());
    assertTrue(port.current().activationEnabled());
    assertEquals(new BigDecimal("5000"), port.current().worldPrices().get("world").hand());
  }

  @Test
  void negativePerWorldHandFailsWholeFile(@TempDir Path temp) throws Exception {
    Path file = temp.resolve("config.yml");
    write(
        file,
        "price:\n  hand: 12000.00\n  all: 25000.00\nworlds:\n  world:\n    hand: -1\n"
            + "admin:\n  target-distance: 8\nmessages:\n  g: hi\n");
    FileConfigurationPort port = new FileConfigurationPort(file.toFile());
    assertFalse(port.current().activationEnabled());
  }

  @Test
  void unparseablePerWorldHandFailsWholeFile(@TempDir Path temp) throws Exception {
    Path file = temp.resolve("config.yml");
    write(
        file,
        "price:\n  hand: 12000.00\n  all: 25000.00\nworlds:\n  world:\n    hand: abc\n"
            + "admin:\n  target-distance: 8\nmessages:\n  g: hi\n");
    FileConfigurationPort port = new FileConfigurationPort(file.toFile());
    assertFalse(port.current().activationEnabled());
  }

  @Test
  void nonFinitePerWorldHandFailsWholeFile(@TempDir Path temp) throws Exception {
    Path file = temp.resolve("config.yml");
    write(
        file,
        "price:\n  hand: 12000.00\n  all: 25000.00\nworlds:\n  world:\n    hand: Infinity\n"
            + "admin:\n  target-distance: 8\nmessages:\n  g: hi\n");
    // Infinity -> BigDecimal throws, mapped to worlds.world.hand: Infinity -> fail whole file
    FileConfigurationPort port = new FileConfigurationPort(file.toFile());
    assertFalse(port.current().activationEnabled());
  }

  @Test
  void worldsHandReloadFailsWholeFileRetainsPrior(@TempDir Path temp) throws Exception {
    Path file = temp.resolve("config.yml");
    write(
        file,
        "price:\n  hand: 12000.00\n  all: 25000.00\nworlds:\n  world:\n    hand: 5000\n"
            + "admin:\n  target-distance: 8\nmessages:\n  g: hi\n");
    FileConfigurationPort port = new FileConfigurationPort(file.toFile());
    assertTrue(port.current().activationEnabled());
    assertEquals(new BigDecimal("5000"), port.current().worldPrices().get("world").hand());
    write(
        file,
        "price:\n  hand: 12000.00\n  all: 25000.00\nworlds:\n  world:\n    hand: -5\n"
            + "admin:\n  target-distance: 8\nmessages:\n  g: hi\n");
    var outcome = port.reload();
    assertInstanceOf(ConfigurationPort.ReloadOutcome.Failure.class, outcome);
    assertTrue(
        ((ConfigurationPort.ReloadOutcome.Failure) outcome).reason().contains("worlds.world.hand"));
    assertEquals(new BigDecimal("5000"), port.current().worldPrices().get("world").hand());
  }

  @Test
  void dupWorldLastWinsWarns(@TempDir Path temp) throws Exception {
    Path file = temp.resolve("config.yml");
    write(
        file,
        "price:\n  hand: 12000.00\n  all: 25000.00\nworlds:\n  world:\n    hand: 5000\n"
            + "  world:\n    hand: 9000\n"
            + "admin:\n  target-distance: 8\nmessages:\n  g: hi\n");
    FileConfigurationPort port = new FileConfigurationPort(file.toFile());
    assertTrue(port.current().activationEnabled());
    assertEquals(new BigDecimal("9000"), port.current().worldPrices().get("world").hand());
  }

  @Test
  void quotedWorldNetherParses(@TempDir Path temp) throws Exception {
    Path file = temp.resolve("config.yml");
    write(
        file,
        "price:\n  hand: 12000.00\n  all: 25000.00\nworlds:\n  \"world nether\":\n    hand: 1234\n"
            + "admin:\n  target-distance: 8\nmessages:\n  g: hi\n");
    FileConfigurationPort port = new FileConfigurationPort(file.toFile());
    assertTrue(port.current().activationEnabled());
    WorldPrice wp = port.current().worldPrices().get("world nether");
    assertNotNull(wp);
    assertEquals(new BigDecimal("1234"), wp.hand());
  }

  @Test
  void missingSubkeyLenientFallback(@TempDir Path temp) throws Exception {
    Path file = temp.resolve("config.yml");
    write(
        file,
        "price:\n  hand: 12000.00\n  all: 25000.00\nworlds:\n  world:\n    hand: 5000\n"
            + "admin:\n  target-distance: 8\nmessages:\n  g: hi\n");
    FileConfigurationPort port = new FileConfigurationPort(file.toFile());
    assertTrue(port.current().activationEnabled());
    WorldPrice wp = port.current().worldPrices().get("world");
    assertNull(wp.all());
  }

  @Test
  void reloadRetainsWorldPricesOnMalformedPerWorld(@TempDir Path temp) throws Exception {
    Path file = temp.resolve("config.yml");
    write(
        file,
        "price:\n  hand: 12000.00\n  all: 25000.00\nworlds:\n  a:\n    hand: 100\n"
            + "admin:\n  target-distance: 8\nmessages:\n  g: hi\n");
    FileConfigurationPort port = new FileConfigurationPort(file.toFile());
    assertTrue(port.current().activationEnabled());
    write(
        file,
        "price:\n  hand: 12000.00\n  all: 25000.00\nworlds:\n  b:\n    hand: -1\n"
            + "admin:\n  target-distance: 8\nmessages:\n  g: hi\n");
    var outcome = port.reload();
    assertInstanceOf(ConfigurationPort.ReloadOutcome.Failure.class, outcome);
    assertNotNull(port.current().worldPrices().get("a"));
    assertEquals(new BigDecimal("100"), port.current().worldPrices().get("a").hand());
  }

  private static void write(Path file, String content) throws Exception {
    Files.writeString(file, content, StandardCharsets.UTF_8);
  }
}
