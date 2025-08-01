package me.whitehatd.aquila.queue.manager;

import gg.supervisor.core.annotation.Component;
import gg.supervisor.core.util.Services;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.whitehatd.aquila.queue.bridge.gamemode.Gamemode;
import me.whitehatd.aquila.queue.bridge.match.MatchType;
import me.whitehatd.aquila.queue.bridge.player.PlayerData;
import me.whitehatd.aquila.queue.bridge.player.PlayerRepository;
import me.whitehatd.aquila.queue.redis.RedisPublisher;
import me.whitehatd.aquila.queue.redis.dto.MatchStartMessage;
import me.whitehatd.aquila.queue.util.ServerConfig;
import org.bukkit.Bukkit;

import java.util.*;
import java.util.concurrent.*;

@Slf4j
@Component
public class QueueManager {

    private final PlayerRepository playerRepository;
    private final RedisPublisher redisPublisher;
    private final ExecutorService queueProcessorExecutor;
    private final ScheduledExecutorService scheduledExecutor;

    @Getter
    private final Map<QueueKey, Queue<QueueEntry>> queues = new ConcurrentHashMap<>();
    private final Map<UUID, QueueKey> playerQueue = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> playerEloCache = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> playerEloRangeCache = new ConcurrentHashMap<>();
    private final int initialEloRange = 20;
    private final int maxEloRange = 400;
    private final int eloRangeStep = 30;
    private final long eloRangeIncrementMs = 10000; // 10 seconds
    private GamemodeManager gamemodeManager;

    public QueueManager(PlayerRepository playerRepository,
                        RedisPublisher redisPublisher) {
        this.playerRepository = playerRepository;
        this.redisPublisher = redisPublisher;
        this.queueProcessorExecutor = Executors.newCachedThreadPool();
        this.scheduledExecutor = Executors.newScheduledThreadPool(1);


        // Start periodic queue processing
        startQueueProcessor();
    }

    public void initialize(GamemodeManager gamemodeManager) {
        this.gamemodeManager = gamemodeManager;
    }

    /**
     * Start periodic queue processing
     */
    private void startQueueProcessor() {
        scheduledExecutor.scheduleAtFixedRate(this::processAllQueues, 500, 500, TimeUnit.MILLISECONDS);
    }

    /**
     * Process all active queues
     */
    private void processAllQueues() {
        for (QueueKey key : new HashSet<>(queues.keySet())) {
            queueProcessorExecutor.submit(() -> processQueue(key));
        }
    }


    /**
     * Creates queues for a newly added gamemode
     */
    public void addGamemodeQueues(Gamemode gamemode) {
        if (!gamemode.isEnabled()) return;

        for (MatchType matchType : gamemode.getAvailableMatchTypes()) {
            QueueKey rankedKey = new QueueKey(matchType, true, gamemode.getId());
            QueueKey unrankedKey = new QueueKey(matchType, false, gamemode.getId());

            queues.putIfAbsent(rankedKey, new LinkedBlockingQueue<>());
            queues.putIfAbsent(unrankedKey, new LinkedBlockingQueue<>());
        }

        log.info("Added queues for gamemode: {}", gamemode.getName());
    }

    /**
     * Removes queues for a deleted gamemode
     */
    public void removeGamemodeQueues(String gamemodeId) {
        // Create a list of keys to remove
        List<QueueKey> keysToRemove = new ArrayList<>();

        for (QueueKey key : queues.keySet()) {
            if (key.getGamemodeId().equals(gamemodeId)) {
                keysToRemove.add(key);
            }
        }

        // Remove all players from the queues first
        for (QueueKey key : keysToRemove) {
            Queue<QueueEntry> queue = queues.get(key);
            if (queue != null) {
                for (QueueEntry entry : queue) {
                    playerQueue.remove(entry.getPlayerId());
                }
                queue.clear();
                queues.remove(key);
            }
        }

        log.info("Removed {} queues for gamemode: {}", keysToRemove.size(), gamemodeManager.getGamemode(gamemodeId).getName());
    }

    /**
     * Adds a player to a queue with a specific gamemode
     */
    public boolean addToQueue(UUID playerId, MatchType matchType, boolean ranked, String gamemodeId, int maxPingDifference) {
        if (playerQueue.containsKey(playerId)) {
            return false;
        }

        Gamemode gamemode = gamemodeManager.getGamemode(gamemodeId);
        if (gamemode == null || !gamemode.isEnabled() || !gamemode.getAvailableMatchTypes().contains(matchType)) {
            log.warn("Invalid gamemode or match type for queueing: {} / {}", gamemodeId, matchType);
            return false;
        }

        QueueKey key = new QueueKey(matchType, ranked, gamemodeId);
        Queue<QueueEntry> queue = queues.computeIfAbsent(key, k -> new LinkedBlockingQueue<>());

        QueueEntry entry = new QueueEntry(playerId, System.currentTimeMillis(), maxPingDifference);
        queue.add(entry);
        playerQueue.put(playerId, key);

        if (ranked) {
            playerEloRangeCache.put(playerId, initialEloRange);
        }

        return true;
    }

    /**
     * Removes a player from their current queue
     */
    public boolean removeFromQueue(UUID playerId) {
        QueueKey key = playerQueue.get(playerId);
        if (key == null) {
            return false;
        }

        Queue<QueueEntry> queue = queues.get(key);
        if (queue == null) {
            playerQueue.remove(playerId);
            return false;
        }

        // Remove player from queue
        boolean removed = queue.removeIf(entry -> entry.getPlayerId().equals(playerId));
        playerQueue.remove(playerId);
        playerEloRangeCache.remove(playerId);

        return removed;
    }

    /**
     * Process a queue to find potential matches
     */
    private void processQueue(QueueKey key) {
        Queue<QueueEntry> queue = queues.get(key);
        if (queue == null || queue.size() < 2) {
            return;
        }

        // Match logic depends on queue type
        if (key.isRanked()) {
            processRankedQueue(key, queue);
        } else {
            processUnrankedQueue(key, queue);
        }
    }

    /**
     * Process a ranked queue using ELO-based matching
     */
    private void processRankedQueue(QueueKey key, Queue<QueueEntry> queue) {
        List<QueueEntry> entries = new ArrayList<>(queue);

        for (int i = 0; i < entries.size(); i++) {
            QueueEntry entry1 = entries.get(i);
            if (!queue.contains(entry1)) continue; // Entry might have been removed

            UUID player1Id = entry1.getPlayerId();
            int player1Elo = getPlayerElo(player1Id);
            int eloRange = getCurrentEloRange(player1Id);

            for (int j = i + 1; j < entries.size(); j++) {
                QueueEntry entry2 = entries.get(j);
                if (!queue.contains(entry2)) continue;

                UUID player2Id = entry2.getPlayerId();
                int player2Elo = getPlayerElo(player2Id);

                // Check if players are within ELO range
                if (Math.abs(player1Elo - player2Elo) <= eloRange) {
                    // Found a match. Check for ping difference

                    int ping1 = Objects.requireNonNull(Bukkit.getPlayer(entry1.getPlayerId())).getPing();
                    int ping2 = Objects.requireNonNull(Bukkit.getPlayer(entry2.getPlayerId())).getPing();

                    int allowedDiff = Math.min(entry1.getMaxPingDifference(), entry2.getMaxPingDifference());

                    // Found actual match
                    if (Math.abs(ping1 - ping2) <= allowedDiff) {
                        if (queue.remove(entry1) && queue.remove(entry2)) {
                            playerQueue.remove(player1Id);
                            playerQueue.remove(player2Id);
                            playerEloRangeCache.remove(player1Id);
                            playerEloRangeCache.remove(player2Id);

                            // Create teams for the match
                            createMatch(key, List.of(player1Id), List.of(player2Id));
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * Process an unranked queue using first-come-first-served matching
     */
    private void processUnrankedQueue(QueueKey key, Queue<QueueEntry> queue) {
        while (queue.size() >= 2) {
            QueueEntry entry1 = queue.poll();
            QueueEntry entry2 = queue.poll();

            if (entry1 != null && entry2 != null) {
                UUID player1Id = entry1.getPlayerId();
                UUID player2Id = entry2.getPlayerId();

                playerQueue.remove(player1Id);
                playerQueue.remove(player2Id);

                int ping1 = Objects.requireNonNull(Bukkit.getPlayer(entry1.getPlayerId())).getPing();
                int ping2 = Objects.requireNonNull(Bukkit.getPlayer(entry2.getPlayerId())).getPing();

                int allowedDiff = Math.min(entry1.getMaxPingDifference(), entry2.getMaxPingDifference());

                // Found actual match
                if (Math.abs(ping1 - ping2) <= allowedDiff) {
                    // Create teams for the match
                    createMatch(key, List.of(player1Id), List.of(player2Id));
                }


            } else {
                break;
            }
        }
    }

    /**
     * Create a match between two teams
     */
    private void createMatch(QueueKey key, List<UUID> teamA, List<UUID> teamB) {
        queueProcessorExecutor.submit(() -> {
            MatchStartMessage message = new MatchStartMessage();

            // Convert UUID to string
            List<String> teamAStr = teamA.stream().map(UUID::toString).toList();
            List<String> teamBStr = teamB.stream().map(UUID::toString).toList();

            message.setTeamA(teamAStr);
            message.setTeamB(teamBStr);
            message.setRanked(key.isRanked());
            message.setMatchType(key.getMatchType().toString());
            message.setMatchScheduledTime(System.currentTimeMillis());

            ServerConfig config = Services.loadIfPresent(ServerConfig.class);
            message.setServerName(config.getServerName());

            // Add gamemode settings
            Gamemode gamemode = gamemodeManager.getGamemode(key.getGamemodeId());

            List<String> allPlayers = new ArrayList<>();
            allPlayers.addAll(teamAStr);
            allPlayers.addAll(teamBStr);

            Map<String, Map<Integer, String>> loadouts = new HashMap<>();

            allPlayers.forEach(playerId -> {
                Map<Integer, String> playerLoadout = playerRepository.find(playerId).getLoadoutRaw(gamemode.getId());
                if(playerLoadout != null) {
                    loadouts.put(playerId, playerLoadout);
                } else {
                    loadouts.put(playerId, gamemode.getLoadoutData());
                }
            });
            message.setLoadouts(loadouts);

            message.setArenaName(getRandomString(gamemode.getArenas()));
            message.setGamemodeId(gamemode.getId());

            //space for extra settings

            // Publish match start message
            redisPublisher.sendMatchStart(message);
            log.info("Created match between teams: {} vs {} (Gamemode: {})",
                    teamAStr, teamBStr, gamemode.getName());
        });
    }

    public boolean createPartyMatch(List<UUID> teamA, List<UUID> teamB, String gamemodeId,
                                    String partyALeader, Set<String> partyAMembers, String partyBLeader, Set<String> partyBMembers) {
        queueProcessorExecutor.submit(() -> {
            MatchStartMessage message = new MatchStartMessage();
            List<String> teamAStr = teamA.stream().map(UUID::toString).toList();
            List<String> teamBStr = teamB.stream().map(UUID::toString).toList();
            message.setTeamA(teamAStr);
            message.setTeamB(teamBStr);
            message.setRanked(false); // Party duels are unranked.
            message.setMatchType(MatchType.PARTY_VS_PARTY.toString());
            message.setMatchScheduledTime(System.currentTimeMillis());
            message.setGamemodeId(gamemodeId);

            ServerConfig config = Services.loadIfPresent(ServerConfig.class);
            message.setServerName(config.getServerName());

            List<String> allPlayers = new ArrayList<>();
            allPlayers.addAll(teamAStr);
            allPlayers.addAll(teamBStr);

            Map<String, Map<Integer, String>> loadouts = new HashMap<>();

            // Add gamemode settings
            Gamemode gamemode = gamemodeManager.getGamemode(gamemodeId);

            allPlayers.forEach(playerId -> {
                Map<Integer, String> playerLoadout = playerRepository.find(playerId).getLoadoutRaw(gamemode.getId());
                if(playerLoadout != null) {
                    loadouts.put(playerId, playerLoadout);
                } else {
                    loadouts.put(playerId, gamemode.getLoadoutData());
                }
            });
            message.setLoadouts(loadouts);

            message.setArenaName(getRandomString(gamemode.getArenas()));
            message.setGamemodeId(gamemode.getId());

            Map<String, Object> extraSettings = new HashMap<>();
            extraSettings.put("partyALeader", partyALeader);
            extraSettings.put("partyAMembers", partyAMembers);
            extraSettings.put("partyBLeader", partyBLeader);
            extraSettings.put("partyBMembers", partyBMembers);
            message.setExtraSettings(extraSettings);

            redisPublisher.sendMatchStart(message);
            log.info("Created party duel match between {} and {} (Gamemode: {})", teamAStr, teamBStr, gamemodeManager.getGamemode(gamemodeId).getName());
        });
        return true;
    }


    public String getRandomString(List<String> list) {
        if (list == null || list.isEmpty()) {
            throw new IllegalArgumentException("List must not be null or empty");
        }
        int randomIndex = ThreadLocalRandom.current().nextInt(list.size());
        return list.get(randomIndex);
    }

    /**
     * Get a player's ELO rating
     */
    public int getPlayerElo(UUID playerId) {
        // Check cache first
        if (playerEloCache.containsKey(playerId)) {
            return playerEloCache.get(playerId);
        }

        // Fetch from repository
        PlayerData data = playerRepository.find(playerId.toString());
        if(data == null) {
            data = new PlayerData(playerId);

        }
        int elo = data.getElo();
        data.setOnArena(false);

        // Cache the value
        playerRepository.save(playerId.toString(), data);
        playerEloCache.put(playerId, elo);
        return elo;
    }

    /**
     * Gets the join time for a player in queue
     * @return join time in milliseconds, or null if player is not in queue
     */
    public Long getPlayerJoinTime(UUID playerId) {
        QueueKey key = playerQueue.get(playerId);
        if (key == null) return null;

        Queue<QueueEntry> queue = queues.get(key);
        if (queue == null) return null;

        for (QueueEntry entry : queue) {
            if (entry.getPlayerId().equals(playerId)) {
                return entry.getJoinTime();
            }
        }

        return null;
    }

    /**
     * Get a player's current ELO matching range
     */
    public int getCurrentEloRange(UUID playerId) {
        if (!playerEloRangeCache.containsKey(playerId)) {
            return initialEloRange;
        }

        int currentRange = playerEloRangeCache.get(playerId);
        QueueKey key = playerQueue.get(playerId);

        if (key == null) {
            return currentRange;
        }

        // Calculate time in queue
        Queue<QueueEntry> queue = queues.get(key);
        if (queue == null) return currentRange;

        for (QueueEntry entry : queue) {
            if (entry.getPlayerId().equals(playerId)) {
                long timeInQueue = System.currentTimeMillis() - entry.getJoinTime();

                // Increase range based on time in queue
                int steps = (int) (timeInQueue / eloRangeIncrementMs);
                int newRange = Math.min(initialEloRange + (steps * eloRangeStep), maxEloRange);

                if (newRange != currentRange) {
                    playerEloRangeCache.put(playerId, newRange);
                    return newRange;
                }

                return currentRange;
            }
        }

        return currentRange;
    }

    /**
     * Clear a player's cached ELO value
     */
    public void clearPlayerEloCache(UUID playerId) {
        playerEloCache.remove(playerId);
        playerEloRangeCache.remove(playerId);
    }

    /**
     * Check if a player is in a queue
     */
    public boolean isInQueue(UUID playerId) {
        return playerQueue.containsKey(playerId);
    }

    /**
     * Get the queue a player is in
     */
    public QueueKey getPlayerQueue(UUID playerId) {
        return playerQueue.get(playerId);
    }

    /**
     * Get the number of players in a queue
     */
    public int getQueueSize(MatchType matchType, boolean ranked) {
        int count = 0;
        for (Map.Entry<QueueKey, Queue<QueueEntry>> entry : queues.entrySet()) {
            QueueKey key = entry.getKey();
            if (key.getMatchType() == matchType && key.isRanked() == ranked) {
                count += entry.getValue().size();
            }
        }
        return count;
    }

    /**
     * Get the number of players in a specific gamemode queue
     */
    public int getQueueSize(MatchType matchType, boolean ranked, String gamemodeId) {
        QueueKey key = new QueueKey(matchType, ranked, gamemodeId);
        Queue<QueueEntry> queue = queues.get(key);
        return queue != null ? queue.size() : 0;
    }

    /**
     * Shuts down the executor services
     */
    public void shutdown() {
        scheduledExecutor.shutdown();
        queueProcessorExecutor.shutdown();

        try {
            if (!scheduledExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduledExecutor.shutdownNow();
            }
            if (!queueProcessorExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                queueProcessorExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            scheduledExecutor.shutdownNow();
            queueProcessorExecutor.shutdownNow();
        }
    }

    /**
     * Represents a unique queue identifier
     */
    @Data
    public static class QueueKey {
        private final MatchType matchType;
        private final boolean ranked;
        private final String gamemodeId;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            QueueKey queueKey = (QueueKey) o;
            return ranked == queueKey.ranked &&
                    matchType == queueKey.matchType &&
                    Objects.equals(gamemodeId, queueKey.gamemodeId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(matchType, ranked, gamemodeId);
        }
    }

    /**
     * Represents a player entry in a queue
     */
    @Data
    public static class QueueEntry {
        private final UUID playerId;
        private final long joinTime;
        private final int maxPingDifference;
    }
}