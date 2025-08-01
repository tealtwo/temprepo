package me.whitehatd.aquila.queue.redis;

import gg.supervisor.core.loader.SupervisorLoader;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.whitehatd.aquila.queue.redis.dto.MatchStartMessage;
import me.whitehatd.aquila.queue.redis.dto.tournament.TournamentWaitMessage;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Central Redis publisher for queue<->arena communication.
 */
@Slf4j
@Getter
public class RedisPublisher {

    private final JedisPool jedisPool;
    private final ExecutorService executorService;

    public RedisPublisher(String host, int port, String password) {
        this.executorService = Executors.newCachedThreadPool();
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(25);
        this.jedisPool = new JedisPool(poolConfig, host, port, 2000, password);
    }

    /**
     * Sends a standard match start: teleports players to arena server.
     */
    public void sendMatchStart(MatchStartMessage message) {
        publishAsync("arena.match.start", message);
    }

    public void sendTournamentWait(TournamentWaitMessage message) {
        publishAsync("arena.match.wait", message);
    }

    /**
     * Publishes a spectate request.
     */
    public void publishSpectateRequest(SpectateDTO dto) {
        publishAsync("arena.match.spectate", dto);
    }

    /**
     * Generic async publish helper.
     */
    private <T> void publishAsync(String channel, T dto) {
        String json = SupervisorLoader.GSON.toJson(dto);
        executorService.submit(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.publish(channel, json);
            } catch (Exception e) {
                log.warn("Failed to publish to {}: {}", channel, e.getMessage(), e);
            }
        });
    }

    /**
     * Shuts down Redis connections and executor.
     */
    public void shutdown() {
        executorService.shutdownNow();
        jedisPool.close();
    }
}
