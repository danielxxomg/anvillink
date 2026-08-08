// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.domain;

import java.math.BigDecimal;

/** Pure domain per-world price override. Nullable per field for partial overrides. */
public record WorldPrice(BigDecimal hand, BigDecimal all) {}
