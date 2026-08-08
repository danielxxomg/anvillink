// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.adapter;

import static org.junit.jupiter.api.Assertions.*;

import io.github.danielxxomg.anvillink.domain.RepairMode;
import io.github.danielxxomg.anvillink.domain.SignRecord;
import io.github.danielxxomg.anvillink.domain.ports.ConfigurationPort;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

class AdminCommandTest {

  private static class FakeState extends PdcSignIdentityTest.FakePdc implements Sign {
    DyeColor color;
    boolean updated;
    final String[] lines = new String[4];

    @Override
    public org.bukkit.persistence.PersistentDataContainer getPersistentDataContainer() {
      return this;
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

    @Override
    public org.bukkit.block.Block getBlock() {
      return null;
    }

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

    @Override
    public byte getRawData() {
      return 0;
    }

    @Override
    public void setRawData(byte d) {}

    @Override
    public org.bukkit.material.MaterialData getData() {
      return null;
    }

    @Override
    public void setData(org.bukkit.material.MaterialData d) {}

    @Override
    public org.bukkit.block.data.BlockData getBlockData() {
      return null;
    }

    @Override
    public void setBlockData(org.bukkit.block.data.BlockData d) {}

    @Override
    public boolean isPlaced() {
      return true;
    }

    @Override
    public boolean isCollidable() {
      return false;
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

    @Override
    public org.bukkit.Material getType() {
      return Material.OAK_SIGN;
    }

    @Override
    public void setType(org.bukkit.Material t) {}

    @Override
    public org.bukkit.Chunk getChunk() {
      return null;
    }

    @Override
    public byte getLightLevel() {
      return 0;
    }

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

  private ConfigurationPort configWith(int distance) {
    ConfigurationPort.ConfigSnapshot snap =
        new ConfigurationPort.ConfigSnapshot(
            new BigDecimal("12000.00"),
            new BigDecimal("25000.00"),
            Map.of(),
            distance,
            Map.of(),
            true,
            true,
            "BLOCK_ANVIL_USE",
            "CRIT");
    return new ConfigurationPort() {
      ConfigurationPort.ConfigSnapshot cur = snap;

      @Override
      public ConfigSnapshot current() {
        return cur;
      }

      @Override
      public ReloadOutcome reload() {
        return new ReloadOutcome.Success(cur);
      }
    };
  }

  private Player playerWithSign(FakeState sign, boolean canManage) {
    Block block =
        (Block)
            Proxy.newProxyInstance(
                Block.class.getClassLoader(),
                new Class<?>[] {Block.class},
                (proxy, method, args) -> {
                  if (method.getName().equals("getState")) return sign;
                  if (method.getName().equals("getType")) return Material.OAK_SIGN;
                  if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                  if (method.getName().equals("equals")) return proxy == args[0];
                  Class<?> rt = method.getReturnType();
                  if (rt == boolean.class) return false;
                  if (rt == int.class) return 0;
                  if (rt == long.class) return 0L;
                  if (rt == void.class) return null;
                  return null;
                });
    return playerForBlock(block, canManage);
  }

  private Player playerForBlock(Block block, boolean canManage) {
    return (Player)
        Proxy.newProxyInstance(
            Player.class.getClassLoader(),
            new Class<?>[] {Player.class},
            (proxy, method, args) -> {
              String n = method.getName();
              if (n.equals("hasPermission") && args != null && args.length == 1) {
                return "anvillink.manage".equals(args[0]) && canManage;
              }
              if (n.equals("getTargetBlock") && args != null) return block;
              if (n.equals("sendMessage") && args != null && args.length == 1) return null;
              if (n.equals("getUniqueId")) return UUID.randomUUID();
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

  private CommandSender senderProxy() {
    return (CommandSender)
        Proxy.newProxyInstance(
            CommandSender.class.getClassLoader(),
            new Class<?>[] {CommandSender.class},
            (proxy, method, args) -> {
              if (method.getName().equals("sendMessage") && args != null) return null;
              if (method.getName().equals("hasPermission")) return false;
              Class<?> rt = method.getReturnType();
              if (rt == boolean.class) return false;
              if (rt == void.class) return null;
              return null;
            });
  }

  @Test
  void inspect_valid_reportsValidity() {
    FakeState sign = new FakeState();
    PdcSignIdentity.write(sign, SignRecord.create(RepairMode.HAND, UUID.randomUUID()));
    sign.lines[0] = "[repair]";
    sign.lines[1] = "HAND";
    Player player = playerWithSign(sign, true);
    AdminCommandHandler handler = new AdminCommandHandler(configWith(8));
    boolean ok = handler.onCommand(player, null, "anvillink", new String[] {"inspect"});
    assertTrue(ok);
  }

  @Test
  void inspect_tampered_reportsTampered() {
    FakeState sign = new FakeState();
    PdcSignIdentity.write(sign, SignRecord.create(RepairMode.ALL, UUID.randomUUID()));
    sign.lines[0] = "[repair]";
    sign.lines[1] = "HAND";
    Player player = playerWithSign(sign, true);
    AdminCommandHandler handler = new AdminCommandHandler(configWith(8));
    boolean ok = handler.onCommand(player, null, "anvillink", new String[] {"inspect"});
    assertTrue(ok);
  }

  @Test
  void rerender_valid_restoresCanonical() {
    FakeState sign = new FakeState();
    PdcSignIdentity.write(sign, SignRecord.create(RepairMode.HAND, UUID.randomUUID()));
    sign.lines[0] = "[tampered]";
    sign.lines[1] = "hand";
    Player player = playerWithSign(sign, true);
    AdminCommandHandler handler = new AdminCommandHandler(configWith(8));
    boolean ok = handler.onCommand(player, null, "anvillink", new String[] {"rerender"});
    assertTrue(ok);
    assertEquals("[repair]", sign.lines[0]);
    assertEquals("HAND", sign.lines[1]);
    assertEquals(DyeColor.BLUE, sign.color);
    assertTrue(sign.updated);
  }

  @Test
  void rerender_invalidIdentity_rejects() {
    FakeState sign = new FakeState();
    sign.set(
        PdcSignIdentity.key(),
        org.bukkit.persistence.PersistentDataType.BYTE_ARRAY,
        new byte[] {1, 2, 3});
    sign.lines[0] = "[repair]";
    sign.lines[1] = "HAND";
    Player player = playerWithSign(sign, true);
    AdminCommandHandler handler = new AdminCommandHandler(configWith(8));
    boolean ok = handler.onCommand(player, null, "anvillink", new String[] {"rerender"});
    assertTrue(ok);
    assertEquals("[repair]", sign.lines[0]);
  }

  @Test
  void inspect_nonPlayer_rejects() {
    CommandSender sender = senderProxy();
    AdminCommandHandler handler = new AdminCommandHandler(configWith(8));
    boolean ok = handler.onCommand(sender, null, "anvillink", new String[] {"inspect"});
    assertTrue(ok);
  }
}
