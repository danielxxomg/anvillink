// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.adapter;

import io.github.danielxxomg.anvillink.domain.SignRecord;
import java.util.Optional;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;

/**
 * Adapter for the permanent PDC sign identity. Permanent namespace/key/schema, brand-independent.
 * See design.md: PDC BYTE_ARRAY magic|schema=1|mode|UUID|authorized.
 */
public final class PdcSignIdentity {

  public static final String NAMESPACE = "danielxxomg";
  public static final String KEY = "anvillink_repair_sign";
  private static final NamespacedKey PDC_KEY = new NamespacedKey(NAMESPACE, KEY);

  private PdcSignIdentity() {}

  public static NamespacedKey key() {
    return PDC_KEY;
  }

  public static void write(PersistentDataHolder holder, SignRecord record) {
    holder
        .getPersistentDataContainer()
        .set(PDC_KEY, PersistentDataType.BYTE_ARRAY, record.toBytes());
  }

  public static Optional<SignRecord> read(PersistentDataHolder holder) {
    PersistentDataContainer pdc = holder.getPersistentDataContainer();
    byte[] raw = pdc.get(PDC_KEY, PersistentDataType.BYTE_ARRAY);
    if (raw == null) return Optional.empty();
    try {
      return Optional.of(SignRecord.fromBytes(raw));
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }

  public static boolean has(PersistentDataHolder holder) {
    return holder.getPersistentDataContainer().has(PDC_KEY, PersistentDataType.BYTE_ARRAY);
  }

  public static void remove(PersistentDataHolder holder) {
    holder.getPersistentDataContainer().remove(PDC_KEY);
  }
}
