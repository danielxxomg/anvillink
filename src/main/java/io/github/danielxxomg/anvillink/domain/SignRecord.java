// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.domain;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Versioned, permanent PDC identity record for an authorized repair sign (repair-signs Requirement:
 * Permanent PDC identity and creation authorization).
 *
 * <p>Byte layout persisted to a sign's PersistentDataContainer: {@code magic[3] | schema=1 | mode |
 * creator UUID (16 bytes) | authorized-create=1}.
 *
 * <p>The namespace/key/schema are brand-independent and permanent (platform Requirement: Stable
 * identity namespace). Malformed bytes, unknown schema, an unknown mode, or a missing
 * authorized-create marker fail closed.
 */
public record SignRecord(RepairMode mode, UUID creator, boolean authorizedCreate) {

  private static final byte[] MAGIC = {'A', 'L', 'R'};
  private static final byte SCHEMA_VERSION = 1;
  private static final byte AUTHORIZED = 1;

  private static final int PAYLOAD_LENGTH = 3 + 1 + 1 + 16 + 1; // 22

  /** Builds an authorized record for a sign created by {@code creator}. */
  public static SignRecord create(RepairMode mode, UUID creator) {
    if (creator == null) {
      throw new IllegalArgumentException("creator must not be null");
    }
    return new SignRecord(mode, creator, true);
  }

  /** Encodes this record to its PDC byte layout. */
  public byte[] toBytes() {
    ByteBuffer buf = ByteBuffer.allocate(PAYLOAD_LENGTH);
    buf.put(MAGIC);
    buf.put(SCHEMA_VERSION);
    buf.put((byte) mode.ordinal());
    buf.putLong(creator.getMostSignificantBits());
    buf.putLong(creator.getLeastSignificantBits());
    buf.put(authorizedCreate ? AUTHORIZED : 0);
    return buf.array();
  }

  /** Decodes and validates a PDC byte array; fails closed on any anomaly. */
  public static SignRecord fromBytes(byte[] bytes) {
    if (bytes == null) {
      throw new IllegalArgumentException("record bytes must not be null");
    }
    if (bytes.length != PAYLOAD_LENGTH) {
      throw new IllegalArgumentException(
          "record length mismatch: expected " + PAYLOAD_LENGTH + " got " + bytes.length);
    }
    ByteBuffer buf = ByteBuffer.wrap(bytes);
    byte[] magic = new byte[3];
    buf.get(magic);
    if (!java.util.Arrays.equals(magic, MAGIC)) {
      throw new IllegalArgumentException("invalid record magic");
    }
    byte schema = buf.get();
    if (schema != SCHEMA_VERSION) {
      throw new IllegalArgumentException("unsupported schema version: " + schema);
    }
    int modeOrdinal = Byte.toUnsignedInt(buf.get());
    if (modeOrdinal >= RepairMode.values().length) {
      throw new IllegalArgumentException("unknown repair mode: " + modeOrdinal);
    }
    RepairMode mode = RepairMode.values()[modeOrdinal];
    UUID creator = new UUID(buf.getLong(), buf.getLong());
    boolean authorized = buf.get() == AUTHORIZED;
    if (!authorized) {
      throw new IllegalArgumentException("record lacks authorized-create marker");
    }
    return new SignRecord(mode, creator, true);
  }
}
