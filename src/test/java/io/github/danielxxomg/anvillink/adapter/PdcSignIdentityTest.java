// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.adapter;

import static org.junit.jupiter.api.Assertions.*;

import io.github.danielxxomg.anvillink.domain.RepairMode;
import io.github.danielxxomg.anvillink.domain.SignRecord;
import java.util.*;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.Test;

class PdcSignIdentityTest {

  @Test
  void roundtripThroughTileStateMock() {
    FakePdc pdc = new FakePdc();
    FakeHolder holder = new FakeHolder(pdc);
    SignRecord rec = SignRecord.create(RepairMode.HAND, UUID.randomUUID());
    PdcSignIdentity.write(holder, rec);
    Optional<SignRecord> decoded = PdcSignIdentity.read(holder);
    assertTrue(decoded.isPresent());
    assertEquals(rec.mode(), decoded.get().mode());
    assertEquals(rec.creator(), decoded.get().creator());
    // stable namespace: display-brand change preserves identity
    assertEquals("danielxxomg", PdcSignIdentity.NAMESPACE);
    assertEquals("anvillink_repair_sign", PdcSignIdentity.KEY);
    assertEquals("danielxxomg:anvillink_repair_sign", PdcSignIdentity.key().toString());
  }

  @Test
  void malformedBytesFailClosed() {
    FakePdc pdc = new FakePdc();
    FakeHolder holder = new FakeHolder(pdc);
    // malformed: wrong length
    pdc.set(PdcSignIdentity.key(), PersistentDataType.BYTE_ARRAY, new byte[] {1, 2, 3});
    assertTrue(PdcSignIdentity.read(holder).isEmpty());
    // malformed: wrong magic
    pdc.set(PdcSignIdentity.key(), PersistentDataType.BYTE_ARRAY, new byte[22]);
    assertTrue(PdcSignIdentity.read(holder).isEmpty());
    // valid then corrupt marker
    SignRecord rec = SignRecord.create(RepairMode.ALL, UUID.randomUUID());
    byte[] good = rec.toBytes();
    good[good.length - 1] = 0;
    pdc.set(PdcSignIdentity.key(), PersistentDataType.BYTE_ARRAY, good);
    assertTrue(PdcSignIdentity.read(holder).isEmpty());
  }

  @Test
  void missingCreatorOrMarkerFailsClosed() {
    FakePdc pdc = new FakePdc();
    FakeHolder holder = new FakeHolder(pdc);
    assertTrue(PdcSignIdentity.read(holder).isEmpty());
    assertFalse(PdcSignIdentity.has(holder));
    SignRecord rec = SignRecord.create(RepairMode.HAND, UUID.randomUUID());
    PdcSignIdentity.write(holder, rec);
    assertTrue(PdcSignIdentity.has(holder));
    PdcSignIdentity.remove(holder);
    assertTrue(PdcSignIdentity.read(holder).isEmpty());
  }

  static final class FakePdc implements PersistentDataContainer {
    private final Map<NamespacedKey, Object> map = new HashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public <T, Z> void set(NamespacedKey k, PersistentDataType<T, Z> t, Z v) {
      map.put(k, v);
    }

    @Override
    public <T, Z> boolean has(NamespacedKey k, PersistentDataType<T, Z> t) {
      return map.containsKey(k);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T, Z> Z get(NamespacedKey k, PersistentDataType<T, Z> t) {
      Object v = map.get(k);
      if (v == null) return null;
      return (Z) v;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T, Z> Z getOrDefault(NamespacedKey k, PersistentDataType<T, Z> t, Z d) {
      Z v = get(k, t);
      return v != null ? v : d;
    }

    @Override
    public Set<NamespacedKey> getKeys() {
      return Set.copyOf(map.keySet());
    }

    @Override
    public void remove(NamespacedKey k) {
      map.remove(k);
    }

    @Override
    public boolean isEmpty() {
      return map.isEmpty();
    }

    @Override
    public org.bukkit.persistence.PersistentDataAdapterContext getAdapterContext() {
      return null;
    }

    @Override
    public boolean has(NamespacedKey k) {
      return map.containsKey(k);
    }
  }

  static final class FakeHolder implements PersistentDataHolder {
    private final PersistentDataContainer pdc;

    FakeHolder(PersistentDataContainer pdc) {
      this.pdc = pdc;
    }

    @Override
    public PersistentDataContainer getPersistentDataContainer() {
      return pdc;
    }
  }
}
