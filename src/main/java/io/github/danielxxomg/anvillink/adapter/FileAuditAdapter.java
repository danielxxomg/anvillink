// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.adapter;

import io.github.danielxxomg.anvillink.domain.ports.AuditPort;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.format.DateTimeFormatter;

/** Fixed append-only audit adapter. Best-effort, never throws. */
public final class FileAuditAdapter implements AuditPort {

  private final File auditFile;

  public FileAuditAdapter(File auditFile) {
    this.auditFile = auditFile;
  }

  @Override
  public void record(AuditEntry entry) {
    try {
      File parent = auditFile.getParentFile();
      if (parent != null) {
        parent.mkdirs();
      }
      String timestamp =
          entry.timestamp() != null
              ? DateTimeFormatter.ISO_INSTANT.format(entry.timestamp())
              : DateTimeFormatter.ISO_INSTANT.format(java.time.Instant.now());
      String uuid = entry.playerUuid() != null ? entry.playerUuid().toString() : "";
      String name = entry.playerName() != null ? entry.playerName() : "";
      String mode = entry.mode() != null ? entry.mode().name() : "";
      String world = entry.worldName() != null ? entry.worldName() : "";
      String price =
          entry.price() != null
              ? entry.price().toPlainString()
              : java.math.BigDecimal.ZERO.toPlainString();
      String count = String.valueOf(entry.repairedCount());
      String result = entry.result() != null ? entry.result() : "SUCCESS";
      String line = String.join("|", timestamp, uuid, name, mode, world, price, count, result);
      Files.writeString(
          auditFile.toPath(), line + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    } catch (IOException | RuntimeException ignored) {
      // best-effort swallow — never affects transaction
    }
  }
}
