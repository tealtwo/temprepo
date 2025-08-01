package me.whitehatd.aquila.queue.listener;

import gg.supervisor.core.annotation.Component;
import gg.supervisor.core.util.Services;
import lombok.extern.slf4j.Slf4j;
import me.whitehatd.aquila.queue.bridge.player.PlayerData;
import me.whitehatd.aquila.queue.bridge.player.PlayerRepository;
import me.whitehatd.aquila.queue.manager.QueueManager;
import me.whitehatd.aquila.queue.manager.tournament.TournamentManager;
import me.whitehatd.aquila.queue.bridge.party.PartyManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import java.util.UUID;

@Slf4j
@Component
public class PlayerHandlingListener implements Listener {

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(AsyncPlayerPreLoginEvent event) {
        QueueManager queueManager = Services.loadIfPresent(QueueManager.class);
        queueManager.getPlayerElo(event.getUniqueId());
        log.debug("Cached player {}", event.getName());
    }

    @EventHandler
    public void onPlayerActualJoin(PlayerJoinEvent event) {
        event.joinMessage(net.kyori.adventure.text.Component.text(""));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerQuit(PlayerQuitEvent event) {
        event.quitMessage(net.kyori.adventure.text.Component.text(""));
        handlePlayerDisconnect(event.getPlayer().getUniqueId(), event.getPlayer().getName());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerKick(PlayerKickEvent event) {
        event.leaveMessage(net.kyori.adventure.text.Component.text(""));
        handlePlayerDisconnect(event.getPlayer().getUniqueId(), event.getPlayer().getName());
    }

    private void handlePlayerDisconnect(UUID playerId, String playerName) {
        QueueManager queueManager = Services.loadIfPresent(QueueManager.class);
        if (queueManager.isInQueue(playerId)) {
            if (queueManager.removeFromQueue(playerId)) {
                log.debug("Removed {} from queue", playerName);
            } else {
                log.warn("Failed to remove {} from queue on disconnect", playerName);
            }
        }
        queueManager.clearPlayerEloCache(playerId);

        PartyManager partyManager = Services.loadIfPresent(PartyManager.class);
        partyManager.leaveParty(playerId.toString());

        // Remove from any waiting/countdown/in-progress tournaments
        TournamentManager tMgr = Services.loadIfPresent(TournamentManager.class);
        tMgr.removePlayerFromAllTournaments(playerId.toString());

        PlayerRepository repo = Services.loadIfPresent(PlayerRepository.class);
        PlayerData data = repo.find(playerId.toString());
        data.setOnArena(false);
        data.setLastSeen(System.currentTimeMillis());
        repo.save(playerId.toString(), data);
    }
}

