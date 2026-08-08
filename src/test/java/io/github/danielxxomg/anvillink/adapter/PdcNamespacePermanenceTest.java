// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.adapter;

import static org.junit.jupiter.api.Assertions.*;

import io.github.danielxxomg.anvillink.domain.RepairMode;
import io.github.danielxxomg.anvillink.domain.SignRecord;
import java.util.UUID;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.Test;

class PdcNamespacePermanenceTest {

  @Test
  void displayBrandRename_preservesPdcIdentity() {
    // simulate existing sign before brand rename
    var pdc = new PdcSignIdentityTest.FakePdc();
    var holder = new PdcSignIdentityTest.FakeHolder(pdc);
    SignRecord before = SignRecord.create(RepairMode.ALL, UUID.randomUUID());
    PdcSignIdentity.write(holder, before);

    // "display brand rename" would affect presentation only — PDC must not move
    assertEquals("danielxxomg", PdcSignIdentity.NAMESPACE);
    assertEquals("anvillink_repair_sign", PdcSignIdentity.KEY);
    assertEquals("danielxxomg:anvillink_repair_sign", PdcSignIdentity.key().toString());
    // key object identity: namespace/key/schema are permanent, not derived from plugin.yml name
    assertEquals(before, PdcSignIdentity.read(holder).orElseThrow());
    // raw bytes still decode as schema 1
    byte[] raw = pdc.get(PdcSignIdentity.key(), PersistentDataType.BYTE_ARRAY);
    assertNotNull(raw);
    assertEquals(1, raw[3]); // schema
    // re-reading after imagined rename still valid
    assertTrue(PdcSignIdentity.has(holder));
  }

  @Test
  void existingSignsRemainValid_afterBrandChange() {
    var pdc = new PdcSignIdentityTest.FakePdc();
    var holder = new PdcSignIdentityTest.FakeHolder(pdc);
    UUID creator = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    PdcSignIdentity.write(holder, SignRecord.create(RepairMode.HAND, creator));
    // brand "AnvilLink" -> "SomeNewName" must not invalidate record
    var decoded = PdcSignIdentity.read(holder).orElseThrow();
    assertEquals(RepairMode.HAND, decoded.mode());
    assertEquals(creator, decoded.creator());
    assertTrue(decoded.authorizedCreate());
  }
}
