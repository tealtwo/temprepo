package me.whitehatd.aquila.queue.manager.tournament;

import com.google.common.collect.ImmutableMap;
import gg.supervisor.core.loader.SupervisorLoader;
import gg.supervisor.core.util.Services;
import me.whitehatd.aquila.queue.bridge.tournament.Tournament;
import me.whitehatd.aquila.queue.bridge.tournament.TournamentRepository;
import me.whitehatd.aquila.queue.bridge.tournament.TournamentState;
import me.whitehatd.aquila.queue.redis.RedisPublisher;
import redis.clients.jedis.Jedis;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class TournamentManager {
    private final RedisPublisher redis;
    private final TournamentRepository repo;
    private final Map<String, Tournament> cache;
    private final Map<String, String> playerToTournament;

    public TournamentManager(RedisPublisher redis, TournamentRepository repo) {
        this.redis = redis;
        this.repo = repo;
        this.cache = new ConcurrentHashMap<>();
        this.playerToTournament = new ConcurrentHashMap<>();
    }

    public Map<String, String> getPlayerToTournament() {
        return ImmutableMap.copyOf(playerToTournament);
    }

    public void saveTournament(Tournament t) {
        cache.put(t.getId(), t);
        // Cache in Redis
        CompletableFuture.runAsync(() -> {
            try (Jedis j = redis.getJedisPool().getResource()) {
                j.hset("tournaments", t.getId(), SupervisorLoader.GSON.toJson(t));
            }
        }, redis.getExecutorService());
    }

    public Tournament getTournament(String id) {
        return cache.get(id);
    }

    public Map<String, Tournament> getInMemoryTournaments() {
        return cache;
    }

    /**
     * Returns true if join succeeded; false if already in another tournament,
     * full, or not in WAITING state.
     */
    public synchronized boolean addPlayer(String tournamentId, String playerUuid) {
        if (playerToTournament.containsKey(playerUuid)) return false;

        Tournament t = cache.get(tournamentId);
        if (t == null || t.getState() != TournamentState.WAITING) return false;

        int needed = t.getNumberOfTeams() * t.getTeamSize();
        if (t.getPlayerIds().size() >= needed) return false;

        t.getPlayerIds().add(playerUuid);
        playerToTournament.put(playerUuid, tournamentId);
        saveTournament(t);
        return true;
    }

    /**
     * Removes a disconnected player from any WAITING or COUNTDOWN tournament,
     * aborting countdown if necessary.
     */
    public void removePlayerFromAllTournaments(String playerUuid) {
        String tid = playerToTournament.remove(playerUuid);
        if (tid == null) return;

        Tournament t = cache.get(tid);
        if (t == null) return;

        if (t.getState() == TournamentState.COUNTDOWN) {
            Services.loadIfPresent(CountdownService.class).cancelCountdown(tid);
            t.setState(TournamentState.WAITING);
        }
        if (t.getState() == TournamentState.WAITING) {
            t.getPlayerIds().remove(playerUuid);
            saveTournament(t);
        }
    }

    /**
     * Mark tournament completed and clean up cache & Redis.
     */
    public void completeTournament(Tournament t) {
        t.setState(TournamentState.COMPLETED);
        t.setEndTime(System.currentTimeMillis());
        repo.save(t.getId(), t);
        removeTournament(t.getId());
        t.getPlayerIds().forEach(playerToTournament::remove);
    }

    /**
     * Cancel tournament (owner or admin), abort countdown, eject all players.
     */
    public void cancelTournament(String tournamentId) {
        Services.loadIfPresent(CountdownService.class)
                .cancelCountdown(tournamentId);

        Tournament t = cache.remove(tournamentId);
        if (t == null) return;
        t.setState(TournamentState.CANCELLED);
        repo.save(tournamentId, t);

        // cleanup Redis
        CompletableFuture.runAsync(() -> {
            try (Jedis j = redis.getJedisPool().getResource()) {
                j.hdel("tournaments", tournamentId);
            }
        });

        // eject all players
        t.getPlayerIds().forEach(playerToTournament::remove);
    }

    /**
     * Removes tournament from in-memory cache and Redis cache-hashmap
     */
    private void removeTournament(String tournamentId) {
        cache.remove(tournamentId);
        CompletableFuture.runAsync(() -> {
            try (Jedis j = redis.getJedisPool().getResource()) {
                j.hdel("tournaments", tournamentId);
            }
        });
    }

    public void shutdown() {
        for (String tid : cache.keySet()) {
            Tournament t = cache.get(tid);
            if (t.getState() != TournamentState.COMPLETED && t.getState() != TournamentState.CANCELLED) {
                cancelTournament(tid);
            }
        }
    }
}