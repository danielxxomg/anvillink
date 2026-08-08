// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.domain.ports;

import io.github.danielxxomg.anvillink.domain.EquipmentView;
import io.github.danielxxomg.anvillink.domain.ItemSnapshot;
import java.util.List;
import java.util.UUID;

/** Pure-domain equipment port. No Bukkit types. */
public interface EquipmentPort {
  record PlayerHandle(UUID uuid) {
    public PlayerHandle {
      if (uuid == null) {
        throw new IllegalArgumentException("player handle must not be null");
      }
    }
  }

  EquipmentView viewOf(PlayerHandle handle);

  ApplyOutcome applyRepair(PlayerHandle handle, List<PlannedApply> planned);

  record PlannedApply(
      io.github.danielxxomg.anvillink.domain.EquipmentSlotId slot, ItemSnapshot snapshot) {}

  sealed interface ApplyOutcome permits ApplyOutcome.Success, ApplyOutcome.PartialFailure {
    record Success(List<PlannedApply> mutated) implements ApplyOutcome {}

    record PartialFailure(List<PlannedApply> mutated, List<PlannedApply> failed, String reason)
        implements ApplyOutcome {}
  }

  boolean restore(PlayerHandle handle, PlannedApply slot);
}
