package me.whitehatd.aquila.queue.redis.subscriber;

import gg.supervisor.core.loader.SupervisorLoader;
import gg.supervisor.core.util.Services;
import lombok.extern.java.Log;
import me.whitehatd.aquila.queue.redis.dto.tournament.TournamentMatchCompleteMessage;
import me.whitehatd.aquila.queue.manager.tournament.TournamentService;
import me.whitehatd.aquila.queue.util.ServerConfig;
import redis.clients.jedis.JedisPubSub;

@Log
public class RedisTournamentMatchCompleteSubscriber extends JedisPubSub {

    @Override
    public void onMessage(String channel, String message) {
        ServerConfig config     = Services.loadIfPresent(ServerConfig.class);
        String incomingChannel = "tournament.match.complete." + config.getServerName();

        if (!incomingChannel.equals(channel)) return;
        try {
            TournamentMatchCompleteMessage dto =
                SupervisorLoader.GSON.fromJson(message, TournamentMatchCompleteMessage.class);
            Services.loadIfPresent(TournamentService.class).onMatchComplete(dto);
        } catch (Exception e) {
            log.severe("Error handling tournament.match.complete: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
