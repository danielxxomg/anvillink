// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class SignParserTest {

  @Test
  void parsesCaseInsensitiveRepairOnLine1AndHandOnLine2() {
    Optional<SignParser.ParseResult> result = new SignParser().parse("[RePaIr]", "hAnD");
    assertTrue(result.isPresent());
    assertEquals(RepairMode.HAND, result.get().mode());
  }

  @Test
  void parsesCaseInsensitiveRepairAndAll() {
    Optional<SignParser.ParseResult> result = new SignParser().parse("[repair]", "ALL");
    assertTrue(result.isPresent());
    assertEquals(RepairMode.ALL, result.get().mode());
  }

  @Test
  void parsesUppercaseRepairMixedCaseAll() {
    Optional<SignParser.ParseResult> result = new SignParser().parse("[REPAIR]", "all");
    assertTrue(result.isPresent());
    assertEquals(RepairMode.ALL, result.get().mode());
  }

  @Test
  void rejectsWrongLocationRepairOnLine2() {
    Optional<SignParser.ParseResult> result = new SignParser().parse("HAND", "[repair]");
    assertTrue(result.isEmpty());
  }

  @Test
  void rejectsEmptyOrInvalidLines() {
    SignParser parser = new SignParser();
    assertTrue(parser.parse(null, "HAND").isEmpty());
    assertTrue(parser.parse("[repair]", null).isEmpty());
    assertTrue(parser.parse("", "HAND").isEmpty());
    assertTrue(parser.parse("[repair]", "").isEmpty());
    assertTrue(parser.parse("[repair]", "repair").isEmpty());
    assertTrue(parser.parse("[repair]", "OFFHAND").isEmpty());
    assertTrue(parser.parse("[repairs]", "HAND").isEmpty());
  }

  @Test
  void trimsWhitespaceBeforeParsing() {
    Optional<SignParser.ParseResult> result = new SignParser().parse(" [repair] ", " HAND ");
    assertTrue(result.isPresent());
    assertEquals(RepairMode.HAND, result.get().mode());
  }
}
