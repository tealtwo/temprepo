package me.whitehatd.aquila.queue.redis;

import gg.supervisor.core.annotation.Component;
import gg.supervisor.core.loader.SupervisorLoader;
import gg.supervisor.core.util.Services;
import lombok.Getter;
import lombok.extern.java.Log;
import me.whitehatd.aquila.queue.redis.dto.ArenaReadyMessage;
import me.whitehatd.aquila.queue.redis.subscriber.RecreatePartySubscriber;
import me.whitehatd.aquila.queue.redis.subscriber.RedisArenaReadySubscriber;
import me.whitehatd.aquila.queue.redis.subscriber.RedisSpectateResponseSubscriber;
import me.whitehatd.aquila.queue.redis.subscriber.RedisTournamentMatchCompleteSubscriber;
import me.whitehatd.aquila.queue.util.ServerConfig;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Log
@Component
public class RedisSubscriberManager {

    private final JedisPool jedisPool;
    private final ExecutorService executorService;
    private final ServerConfig config;


    @Getter
    private final List<ArenaReadyMessage> pendingData;

    public RedisSubscriberManager(RedisPublisher publisher, ServerConfig config) {
        // Reuse the publisher's JedisPool and ExecutorService.
        this.jedisPool = publisher.getJedisPool();
        this.executorService = publisher.getExecutorService();
        this.pendingData = new ArrayList<>();
        this.config = config;
    }

    public void subscribeToArenaReady(ServerConfig config) {
        String channel = "queue.arena.ready." + config.getServerName();

        try (Jedis jedis = jedisPool.getResource()) {
            // Use the arena-ready subscriber from the queue side.
            RedisArenaReadySubscriber subscriber = new RedisArenaReadySubscriber();
            log.info("Subscribing to Redis channel: " + channel);
            jedis.subscribe(subscriber, channel);
        } catch (Exception e) {
            log.severe("Redis subscription to " + channel + " failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void subscribeToSpectateResponse(ServerConfig config) {
        String channel = "spectate.response." + config.getServerName();

        try (Jedis jedis = jedisPool.getResource()) {
            // Use the spectate-response subscriber from the queue side.
            RedisSpectateResponseSubscriber subscriber = new RedisSpectateResponseSubscriber();
            log.info("Subscribing to Redis channel: " + channel);
            jedis.subscribe(subscriber, channel);
        } catch (Exception e) {
            log.severe("Redis subscription to " + channel + " failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void subscribeToTournamentMatchComplete(ServerConfig config) {
        String channel = "tournament.match.complete." + config.getServerName();

        try (Jedis jedis = jedisPool.getResource()) {
            // Use the spectate-response subscriber from the queue side.
            RedisTournamentMatchCompleteSubscriber subscriber = new RedisTournamentMatchCompleteSubscriber();
            log.info("Subscribing to Redis channel: " + channel);
            jedis.subscribe(subscriber, channel);
        } catch (Exception e) {
            log.severe("Redis subscription to " + channel + " failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void subscribeToRecreateParty(ServerConfig config) {
        String channel = "queue.party.recreate." + config.getServerName();

        try (Jedis jedis = jedisPool.getResource()) {
            RecreatePartySubscriber subscriber = new RecreatePartySubscriber();
            log.info("Subscribing to Redis channel: " + channel);
            jedis.subscribe(subscriber, channel);
        } catch (Exception e) {
            log.severe("Redis subscription to " + channel + " failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Starts both subscriptions in parallel using the shared executor.
     */
    public void start() {
        executorService.submit(() -> subscribeToArenaReady(config));
        executorService.submit(() -> subscribeToSpectateResponse(config));
        executorService.submit(() -> subscribeToRecreateParty(config));
        executorService.submit(() -> subscribeToTournamentMatchComplete(config));
    }

    public void shutdown() {
        executorService.shutdownNow();
    }
}
