// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.domain.ports;

import java.util.Map;

/** Pure-domain message port. Exposes String only — no Adventure types. */
public interface MessagePort {
  String render(String templateKey, Map<String, String> placeholders);
}
