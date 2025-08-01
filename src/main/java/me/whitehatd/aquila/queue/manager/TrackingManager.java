package me.whitehatd.aquila.queue.manager;

import gg.supervisor.core.util.Services;
import lombok.extern.java.Log;
import me.whitehatd.aquila.queue.util.SpectateUtil;
import me.whitehatd.aquila.queue.redis.RedisPublisher;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import redis.clients.jedis.Jedis;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Repeatedly checks all trackers every 500ms,
 * if the tracked user is in a match, we send a spectate request for the tracker.
 */
@Log
public class TrackingManager {

    private final ScheduledExecutorService scheduledExecutor;
    private final RedisPublisher publisher;

    public TrackingManager(RedisPublisher publisher) {
        this.publisher = publisher;
        this.scheduledExecutor = Executors.newScheduledThreadPool(1);

        startTrackingScheduler();
    }

    /**
     * Starts the background task that runs every 500ms
     * and automatically tries to spectate for all tracker → tracked pairs.
     */
    public void startTrackingScheduler() {
        scheduledExecutor.scheduleAtFixedRate(() -> {
            try {
                checkAndSpectateAllTrackers();
            } catch (Exception e) {
                log.warning("Error in tracking manager: " + e.getMessage());
            }
        }, 500, 500, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        scheduledExecutor.shutdownNow();
    }

    private void checkAndSpectateAllTrackers() {
        // In a separate thread
        try (Jedis jedis = publisher.getJedisPool().getResource()) {

            Map<String, String> trackersMap = jedis.hgetAll("trackers");
            if (trackersMap.isEmpty()) {
                return;
            }

            Map<String, String> playerMatchMap = jedis.hgetAll("playerMatchMap");

            for (Map.Entry<String, String> entry : trackersMap.entrySet()) {
                String trackerStr = entry.getKey();
                String trackedStr = entry.getValue();

                // Is the tracker online?
                Player trackerPlayer = Bukkit.getPlayer(UUID.fromString(trackerStr));
                if (trackerPlayer == null || !trackerPlayer.isOnline()) {
                    // no reason to spectate
                    continue;
                }

                String matchId = playerMatchMap.get(trackedStr);
                if (matchId == null || matchId.isEmpty()) {
                    continue;
                }

                Services.loadIfPresent(SpectateUtil.class).sendSpectateRequest(trackerPlayer, matchId, true);
            }
        }
    }
}
