// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.adapter;

import io.github.danielxxomg.anvillink.domain.ports.ConfigurationPort;
import io.github.danielxxomg.anvillink.domain.ports.MessagePort;
import java.util.Map;
import java.util.function.Supplier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * MiniMessage-backed MessagePort. Adventure is shaded/relocated (4.11.0) but port exposes String
 * only — no Component/TagResolver crosses the boundary. Signs use Bukkit strings, not MiniMessage.
 */
public final class MiniMessageMessagePort implements MessagePort {

  private final Supplier<ConfigurationPort.ConfigSnapshot> snapshotSupplier;

  public MiniMessageMessagePort(Supplier<ConfigurationPort.ConfigSnapshot> snapshotSupplier) {
    this.snapshotSupplier = snapshotSupplier;
  }

  public MiniMessageMessagePort(ConfigurationPort configPort) {
    this(configPort::current);
  }

  @Override
  public String render(String templateKey, Map<String, String> placeholders) {
    ConfigurationPort.ConfigSnapshot snap = snapshotSupplier.get();
    if (snap == null || snap.messages() == null) return templateKey;
    String template = snap.messages().get(templateKey);
    if (template == null) return templateKey;
    String withPlaceholders = template;
    if (placeholders != null) {
      for (Map.Entry<String, String> e : placeholders.entrySet()) {
        withPlaceholders = withPlaceholders.replace("{" + e.getKey() + "}", e.getValue());
      }
    }
    try {
      Component component = MiniMessage.miniMessage().deserialize(withPlaceholders);
      return LegacyComponentSerializer.legacySection().serialize(component);
    } catch (Exception e) {
      return withPlaceholders;
    }
  }
}
