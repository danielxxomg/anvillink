// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Slice 1 shipped-config contract — real File I/O on src/main/resources/config.yml.
 *
 * <p>RED 2.1: copy shipped config via @TempDir File then FileConfigurationPort(File) — synthetic
 * strings alone must not satisfy. RED 2.3: quote-aware stripInlineComment.
 */
class ShippedConfigRoundTripTest {

  @Test
  void shippedConfigViaRealFileParsesInlineCommentsAndDefaults(@TempDir Path temp)
      throws Exception {
    Path shipped = Path.of("src/main/resources/config.yml");
    assertTrue(Files.isRegularFile(shipped), "shipped config.yml must exist at " + shipped);
    String content = Files.readString(shipped, StandardCharsets.UTF_8);
    Path copy = temp.resolve("config.yml");
    Files.writeString(copy, content, StandardCharsets.UTF_8);

    FileConfigurationPort port = new FileConfigurationPort(copy.toFile());

    assertTrue(
        port.current().activationEnabled(),
        "shipped config.yml with inline comments must be valid (regression bug #1)");
    assertEquals(new BigDecimal("12000.00"), port.current().priceHand(), "price.hand");
    assertEquals(new BigDecimal("25000.00"), port.current().priceAll(), "price.all");
    assertEquals(8, port.current().targetDistance(), "admin.target-distance default 8");
    // worlds: is commented in shipped file — absent from parsed worlds
    assertTrue(
        port.current().worldPrices().isEmpty(), "commented worlds: must not be parsed as entries");
  }

  @Test
  void quotedHashPreserved_unquotedHashStripped(@TempDir Path temp) throws Exception {
    Path file = temp.resolve("config.yml");
    Files.writeString(
        file,
        "price:\n  hand: 12000.00 # trailing comment\n  all: 25000.00\n"
            + "admin:\n  target-distance: 8\n"
            + "messages:\n  g: \"a # b\"\n",
        StandardCharsets.UTF_8);
    FileConfigurationPort port = new FileConfigurationPort(file.toFile());
    assertTrue(port.current().activationEnabled());
    assertEquals(new BigDecimal("12000.00"), port.current().priceHand());
    assertEquals("a # b", port.current().messages().get("g"), "quoted # must be preserved");
  }

  @Test
  void singleQuotedHashPreserved(@TempDir Path temp) throws Exception {
    Path file = temp.resolve("config.yml");
    Files.writeString(
        file,
        "price:\n  hand: 12000.00\n  all: 25000.00\n"
            + "admin:\n  target-distance: 8\n"
            + "messages:\n  g: 'a # b'\n",
        StandardCharsets.UTF_8);
    FileConfigurationPort port = new FileConfigurationPort(file.toFile());
    assertTrue(port.current().activationEnabled());
    assertEquals("a # b", port.current().messages().get("g"));
  }

  @Test
  void barePriceScalarFailsClosedPriceWithHashStillParsed(@TempDir Path temp) throws Exception {
    Path file = temp.resolve("config.yml");
    // bare scalar is invalid — must fail closed
    Files.writeString(
        file,
        "price: 25.00\nadmin:\n  target-distance: 8\nmessages:\n  g: \"hi\"\n",
        StandardCharsets.UTF_8);
    FileConfigurationPort port = new FileConfigurationPort(file.toFile());
    assertFalse(port.current().activationEnabled(), "bare price scalar must fail closed");
    // hash with preceding space stripped
    Files.writeString(
        file,
        "price:\n  hand: 123 # comment\n  all: 456\n"
            + "admin:\n  target-distance: 8\nmessages:\n  g: \"hi\"\n",
        StandardCharsets.UTF_8);
    FileConfigurationPort port2 = new FileConfigurationPort(file.toFile());
    assertTrue(port2.current().activationEnabled());
    assertEquals(new BigDecimal("123"), port2.current().priceHand());
  }
}
