// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.domain;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class SignRecordTest {

  private static final UUID CREATOR = UUID.fromString("00000000-0000-0000-0000-000000000001");

  @Test
  void roundtripsThroughVersionedByteLayout() {
    // repair-signs Requirement: Permanent PDC identity (versioned byte array).
    SignRecord record = SignRecord.create(RepairMode.ALL, CREATOR);
    byte[] bytes = record.toBytes();
    SignRecord decoded = SignRecord.fromBytes(bytes);
    assertEquals(record.mode(), decoded.mode());
    assertEquals(record.creator(), decoded.creator());
    assertEquals(record.authorizedCreate(), decoded.authorizedCreate());
  }

  @Test
  void encodesExpectedMagicAndSchemaVersion() {
    SignRecord record = SignRecord.create(RepairMode.HAND, CREATOR);
    byte[] bytes = record.toBytes();
    // magic | schema=1 | mode | creator(16) | authorized-create
    assertArrayEquals(new byte[] {'A', 'L', 'R'}, new byte[] {bytes[0], bytes[1], bytes[2]});
    assertEquals(1, bytes[3]);
    assertEquals(RepairMode.HAND.ordinal(), bytes[4]);
  }

  @Test
  void rejectsMalformedByteArrays() {
    assertThrows(IllegalArgumentException.class, () -> SignRecord.fromBytes(new byte[0]));
    assertThrows(IllegalArgumentException.class, () -> SignRecord.fromBytes(new byte[] {'A'}));
    // wrong magic
    assertThrows(
        IllegalArgumentException.class,
        () ->
            SignRecord.fromBytes(
                new byte[] {
                  'X', 'Y', 'Z', 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1
                }));
  }

  @Test
  void rejectsUnsupportedSchemaVersion() {
    byte[] bytes = SignRecord.create(RepairMode.HAND, CREATOR).toBytes();
    bytes[3] = 99; // future schema
    assertThrows(IllegalArgumentException.class, () -> SignRecord.fromBytes(bytes));
  }

  @Test
  void rejectsMissingAuthorizationMarker() {
    byte[] bytes = SignRecord.create(RepairMode.HAND, CREATOR).toBytes();
    bytes[bytes.length - 1] = 0; // authorized-create = 0
    assertThrows(IllegalArgumentException.class, () -> SignRecord.fromBytes(bytes));
  }

  @Test
  void rejectsUnknownModeByte() {
    byte[] bytes = SignRecord.create(RepairMode.HAND, CREATOR).toBytes();
    bytes[4] = 99; // unknown mode
    assertThrows(IllegalArgumentException.class, () -> SignRecord.fromBytes(bytes));
  }
}
