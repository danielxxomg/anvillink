// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.adapter;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Proxy;
import java.util.UUID;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.junit.jupiter.api.Test;

class SignLifecycleListenerTest {

  @Test
  void createAuthorized_writesBlueTextAndPdc() {
    FakeSign signState = new FakeSign();
    Block block = blockWithState(signState);
    Player player = playerWith(UUID.randomUUID(), true, false);
    SignChangeEvent event =
        new SignChangeEvent(block, player, new String[] {"[repair]", "HAND", "", ""});
    SignLifecycleListener listener = new SignLifecycleListener();
    listener.onSignChange(event);
    assertFalse(event.isCancelled());
    assertEquals("[repair]", event.getLine(0));
    assertEquals("HAND", event.getLine(1));
    assertEquals(DyeColor.BLUE, signState.color);
    assertTrue(PdcSignIdentity.has(signState));
    assertTrue(signState.updated);
  }

  @Test
  void createUnauthorized_cancelledNoPdc() {
    FakeSign signState = new FakeSign();
    Block block = blockWithState(signState);
    Player player = playerWith(UUID.randomUUID(), false, false);
    SignChangeEvent event =
        new SignChangeEvent(block, player, new String[] {"[repair]", "HAND", "", ""});
    new SignLifecycleListener().onSignChange(event);
    assertTrue(event.isCancelled());
    assertFalse(PdcSignIdentity.has(signState));
  }

  @Test
  void breakUnauthorized_cancelledPdcUnchanged() {
    FakeSign signState = registeredSign();
    Block block = blockWithState(signState);
    Player player = playerWith(UUID.randomUUID(), false, false);
    BlockBreakEvent event = new BlockBreakEvent(block, player);
    new SignLifecycleListener().onBlockBreak(event);
    assertTrue(event.isCancelled());
    assertTrue(PdcSignIdentity.has(signState));
  }

  @Test
  void editByManager_proceedsButTextRemainsTamperedUntilRerender() {
    FakeSign signState = registeredSign();
    // simulate existing registered sign with canonical mode HAND
    Block block = blockWithState(signState);
    Player player = playerWith(UUID.randomUUID(), false, true);
    // manager edits line 1 to tampered value; event carries tampered lines
    SignChangeEvent event =
        new SignChangeEvent(block, player, new String[] {"[tampered]", "HAND", "", ""});
    new SignLifecycleListener().onSignChange(event);
    // edit of registered sign with manage permission proceeds (not cancelled)
    assertFalse(event.isCancelled());
    // PDC unchanged (still valid original record)
    assertTrue(PdcSignIdentity.has(signState));
    // text remains tampered until rerender: listener does not overwrite tampered text on edit path
    assertEquals("[tampered]", event.getLine(0));
  }

  // --- helpers ---

  private static FakeSign registeredSign() {
    FakeSign s = new FakeSign();
    // pre-register via PDC
    PdcSignIdentity.write(
        s,
        io.github.danielxxomg.anvillink.domain.SignRecord.create(
            io.github.danielxxomg.anvillink.domain.RepairMode.HAND, UUID.randomUUID()));
    s.updated = false;
    s.color = null;
    return s;
  }

  private static Block blockWithState(FakeSign state) {
    return (Block)
        Proxy.newProxyInstance(
            Block.class.getClassLoader(),
            new Class<?>[] {Block.class},
            (proxy, method, args) -> {
              String n = method.getName();
              if (n.equals("getState")) return state;
              if (n.equals("getType")) return Material.OAK_SIGN;
              if (n.equals("hashCode")) return System.identityHashCode(proxy);
              if (n.equals("equals")) return proxy == args[0];
              if (n.equals("toString")) return "FakeBlock";
              // default primitive / void handling
              Class<?> rt = method.getReturnType();
              if (rt == boolean.class) return false;
              if (rt == int.class) return 0;
              if (rt == long.class) return 0L;
              if (rt == void.class) return null;
              return null;
            });
  }

  private static Player playerWith(UUID uuid, boolean canCreate, boolean canManage) {
    return (Player)
        Proxy.newProxyInstance(
            Player.class.getClassLoader(),
            new Class<?>[] {Player.class},
            (proxy, method, args) -> {
              String n = method.getName();
              if (n.equals("hasPermission") && args != null && args.length == 1) {
                String perm = (String) args[0];
                if ("anvillink.create".equals(perm)) return canCreate;
                if ("anvillink.manage".equals(perm)) return canManage;
                return false;
              }
              if (n.equals("getUniqueId")) return uuid;
              if (n.equals("hashCode")) return System.identityHashCode(proxy);
              if (n.equals("equals")) return proxy == args[0];
              if (n.equals("toString")) return "FakePlayer";
              Class<?> rt = method.getReturnType();
              if (rt == boolean.class) return false;
              if (rt == int.class) return 0;
              if (rt == long.class) return 0L;
              if (rt == void.class) return null;
              return null;
            });
  }

  static final class FakeSign implements Sign {
    DyeColor color;
    boolean updated;
    final PdcSignIdentityTest.FakePdc pdc = new PdcSignIdentityTest.FakePdc();
    final String[] lines = new String[4];

    @Override
    public PersistentDataContainer getPersistentDataContainer() {
      return pdc;
    }

    @Override
    public DyeColor getColor() {
      return color;
    }

    @Override
    public void setColor(DyeColor c) {
      this.color = c;
    }

    @Override
    public String getLine(int i) {
      return lines[i];
    }

    @Override
    public void setLine(int i, String s) {
      lines[i] = s;
    }

    @Override
    public String[] getLines() {
      return lines.clone();
    }

    @Override
    public boolean update() {
      updated = true;
      return true;
    }

    @Override
    public boolean update(boolean f) {
      updated = true;
      return true;
    }

    @Override
    public boolean update(boolean f, boolean a) {
      updated = true;
      return true;
    }

    @Override
    public boolean isSnapshot() {
      return false;
    }

    // unused BlockState / TileState / Sign contract stubs
    @Override
    public org.bukkit.block.Block getBlock() {
      return null;
    }

    @Override
    public org.bukkit.Material getType() {
      return Material.OAK_SIGN;
    }

    @Override
    public void setType(org.bukkit.Material t) {}

    @Override
    public org.bukkit.World getWorld() {
      return null;
    }

    @Override
    public int getX() {
      return 0;
    }

    @Override
    public int getY() {
      return 0;
    }

    @Override
    public int getZ() {
      return 0;
    }

    @Override
    public org.bukkit.Location getLocation() {
      return null;
    }

    @Override
    public org.bukkit.Location getLocation(org.bukkit.Location l) {
      return null;
    }

    @Override
    public org.bukkit.Chunk getChunk() {
      return null;
    }

    @Override
    public org.bukkit.block.data.BlockData getBlockData() {
      return null;
    }

    @Override
    public void setBlockData(org.bukkit.block.data.BlockData d) {}

    @Override
    public byte getLightLevel() {
      return 0;
    }

    @Override
    public org.bukkit.material.MaterialData getData() {
      return null;
    }

    @Override
    public void setData(org.bukkit.material.MaterialData d) {}

    @Override
    public byte getRawData() {
      return 0;
    }

    @Override
    public void setRawData(byte b) {}

    @Override
    public boolean isPlaced() {
      return true;
    }

    @Override
    public boolean isCollidable() {
      return true;
    }

    @Override
    public java.util.Collection<org.bukkit.inventory.ItemStack> getDrops(
        org.bukkit.inventory.ItemStack tool, org.bukkit.entity.Entity entity) {
      return java.util.List.of();
    }

    @Override
    public org.bukkit.block.BlockState copy() {
      return this;
    }

    @Override
    public org.bukkit.block.BlockState copy(org.bukkit.Location loc) {
      return this;
    }

    @Override
    public boolean isSuffocating() {
      return false;
    }

    // Metadatable
    @Override
    public void setMetadata(String k, org.bukkit.metadata.MetadataValue v) {}

    @Override
    public java.util.List<org.bukkit.metadata.MetadataValue> getMetadata(String k) {
      return java.util.List.of();
    }

    @Override
    public boolean hasMetadata(String k) {
      return false;
    }

    @Override
    public void removeMetadata(String k, org.bukkit.plugin.Plugin p) {}

    // Sign
    @Override
    public java.util.List<net.kyori.adventure.text.Component> lines() {
      return java.util.List.of();
    }

    @Override
    public net.kyori.adventure.text.Component line(int i) {
      return net.kyori.adventure.text.Component.empty();
    }

    @Override
    public void line(int i, net.kyori.adventure.text.Component c) {}

    @Override
    public boolean isEditable() {
      return true;
    }

    @Override
    public void setEditable(boolean b) {}

    @Override
    public boolean isWaxed() {
      return false;
    }

    @Override
    public void setWaxed(boolean b) {}

    @Override
    public boolean isGlowingText() {
      return false;
    }

    @Override
    public void setGlowingText(boolean b) {}

    @Override
    public org.bukkit.block.sign.SignSide getSide(org.bukkit.block.sign.Side s) {
      return null;
    }

    @Override
    public org.bukkit.block.sign.SignSide getTargetSide(org.bukkit.entity.Player p) {
      return null;
    }

    @Override
    public org.bukkit.entity.Player getAllowedEditor() {
      return null;
    }

    @Override
    public java.util.UUID getAllowedEditorUniqueId() {
      return null;
    }

    @Override
    public void setAllowedEditorUniqueId(java.util.UUID u) {}

    @Override
    public org.bukkit.block.sign.Side getInteractableSideFor(double x, double z) {
      return org.bukkit.block.sign.Side.FRONT;
    }
  }
}
