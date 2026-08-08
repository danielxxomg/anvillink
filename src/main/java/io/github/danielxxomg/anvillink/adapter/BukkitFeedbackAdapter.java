// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.adapter;

import io.github.danielxxomg.anvillink.domain.ports.ConfigurationPort;
import io.github.danielxxomg.anvillink.domain.ports.FeedbackPort;
import io.github.danielxxomg.anvillink.domain.ports.MessagePort;
import io.github.danielxxomg.anvillink.domain.ports.SchedulerPort;
import io.github.danielxxomg.anvillink.domain.ports.SignPort;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import org.bukkit.entity.Player;

public final class BukkitFeedbackAdapter implements FeedbackPort {

  private final ConfigurationPort configPort;
  private final MessagePort messagePort;
  private final SchedulerPort scheduler;
  private final Function<UUID, Player> playerLookup;

  public BukkitFeedbackAdapter(
      ConfigurationPort configPort,
      MessagePort messagePort,
      SchedulerPort scheduler,
      Function<UUID, Player> playerLookup) {
    this.configPort = configPort;
    this.messagePort = messagePort;
    this.scheduler = scheduler;
    this.playerLookup = playerLookup;
  }

  @Override
  public void play(SignPort.PlayerId playerId, BigDecimal amount, int repairedCount) {
    ConfigurationPort.ConfigSnapshot snap = configPort.current();
    if (snap == null || !snap.feedbackEnabled()) {
      return;
    }
    scheduler.runOnServerThread(
        () -> {
          try {
            String rendered =
                messagePort.render(
                    "repair-success",
                    Map.of(
                        "count", String.valueOf(repairedCount), "price", amount.toPlainString()));
            Player player = playerLookup.apply(playerId.uuid());
            if (player != null) {
              if (rendered != null && !rendered.isEmpty()) {
                try {
                  player.sendMessage(rendered);
                } catch (Exception ignored) {
                  // swallow send failure
                }
              }
              String soundName =
                  snap.feedbackSound() != null && !snap.feedbackSound().isBlank()
                      ? snap.feedbackSound()
                      : "BLOCK_ANVIL_USE";
              String particleName =
                  snap.feedbackParticles() != null && !snap.feedbackParticles().isBlank()
                      ? snap.feedbackParticles()
                      : "CRIT";
              // Use string-based sound/particle resolution to stay compatible with both
              // Paper 1.18.2 (Sound is enum) and 1.21.x (Sound is interface) runtimes.
              try {
                player.playSound(player.getLocation(), soundName, 1.0f, 1.0f);
              } catch (Exception e) {
                try {
                  player.playSound(player.getLocation(), "BLOCK_ANVIL_USE", 1.0f, 1.0f);
                } catch (Exception ignored) {
                  // swallow
                }
              }
              try {
                org.bukkit.Particle particle = particleOf(particleName);
                player.spawnParticle(particle, player.getLocation(), 10);
              } catch (Exception e) {
                try {
                  player.spawnParticle(
                      org.bukkit.Particle.valueOf("CRIT"), player.getLocation(), 10);
                } catch (Exception ignored) {
                  // swallow
                }
              }
            }
          } catch (Exception ignored) {
            // feedback never affects transaction
          }
        });
  }

  private static org.bukkit.Particle particleOf(String name) {
    for (org.bukkit.Particle p : org.bukkit.Particle.values()) {
      if (p.name().equalsIgnoreCase(name)) return p;
    }
    return org.bukkit.Particle.valueOf(name);
  }
}
