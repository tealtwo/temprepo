package me.whitehatd.aquila.queue.manager;

import gg.supervisor.core.annotation.Component;
import gg.supervisor.core.util.Services;
import lombok.extern.java.Log;
import me.whitehatd.aquila.queue.bridge.gamemode.Gamemode;
import me.whitehatd.aquila.queue.bridge.gamemode.GamemodeRepository;
import me.whitehatd.aquila.queue.bridge.player.PlayerData;
import me.whitehatd.aquila.queue.bridge.player.PlayerRepository;
import me.whitehatd.aquila.queue.redis.RedisPublisher;
import org.bukkit.entity.Player;
import redis.clients.jedis.Jedis;

import java.util.*;
import java.util.concurrent.*;

@Log
@Component
public class GamemodeManager {

    private final GamemodeRepository gamemodeRepository;
    private final Map<String, Gamemode> gamemodeCache;
    private QueueManager queueManager;
    private final ExecutorService queueProcessorExecutor;
    private final ScheduledExecutorService scheduledExecutor;

    public GamemodeManager(GamemodeRepository gamemodeRepository) {
        this.gamemodeRepository = gamemodeRepository;

        this.gamemodeCache = new ConcurrentHashMap<>();

        this.queueProcessorExecutor = Executors.newCachedThreadPool();
        this.scheduledExecutor = Executors.newScheduledThreadPool(1);
    }

    private void refreshGamemodeArenas() {
        RedisPublisher publisher = Services.loadIfPresent(RedisPublisher.class);

        List<String> availableArenas;

        try (Jedis jedis = publisher.getJedisPool().getResource()) {
            availableArenas = jedis.lrange("availableArenas", 0, -1);
        } catch (Exception e) {
            availableArenas = new ArrayList<>();
        }
        for (Gamemode gamemode : gamemodeCache.values()) {
            List<String> purgedArenas =
                    gamemode.getArenas().stream()
                            .filter(availableArenas::contains)
                            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

            gamemode.setArenas(purgedArenas);

            if(gamemode.isEnabled() && gamemode.getArenas().isEmpty()) {
                log.warning("Gamemode " + gamemode.getName() + " doesn't have any arenas available. Disabling.");
                gamemode.setEnabled(false);
                updateGamemode(gamemode);
            }
        }
    }

    /**
     * Loads all gamemodes from the repository into the cache.
     */
    public void loadGamemodes(QueueManager queueManager) {
        this.queueManager = queueManager;
        if (gamemodeRepository == null) {
            log.warning("GamemodeRepository is null, cannot load gamemodes!");
            return;
        }

        try {
            CompletableFuture.supplyAsync(() -> {
                RedisPublisher publisher = Services.loadIfPresent(RedisPublisher.class);
                try (Jedis jedis = publisher.getJedisPool().getResource()) {
                    return jedis.lrange("availableArenas", 0, -1);
                } catch (Exception e) {
                    return new ArrayList<String>();
                }
            }).thenAccept(availableArenas -> {

                Collection<Gamemode> gamemodes = gamemodeRepository.values();
                for (Gamemode gamemode : gamemodes) {
                    gamemodeCache.put(gamemode.getId(), gamemode);

                    List<String> purgedArenas =
                            gamemode.getArenas().stream()
                                    .filter(availableArenas::contains)
                                            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

                    gamemode.setArenas(purgedArenas);

                    if(gamemode.isEnabled()) {
                        if(gamemode.getArenas().isEmpty()) {
                            gamemode.setEnabled(false);

                            gamemodeRepository.save(gamemode.getId(), gamemode);
                            gamemodeCache.put(gamemode.getId(), gamemode);

                            log.warning("Gamemode " + gamemode.getName() + " has no available arenas! Disabling.");
                            continue;
                        }
                        queueManager.addGamemodeQueues(gamemode);
                    }
                }
                log.info("Loaded " + gamemodes.size() + " gamemodes from repository.");
            });

        } catch (Exception e) {
            log.severe("Failed to load gamemodes: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        scheduledExecutor.scheduleAtFixedRate(this::refreshGamemodeArenas, 500, 500, TimeUnit.MILLISECONDS);
    }

    /**
     * Creates a new gamemode with the specified name and creator.
     *
     * @param creator The player creating the gamemode
     * @param name    The name of the gamemode
     * @return true if creation was successful, false otherwise
     */
    public boolean createGamemode(Player creator, String name) {
        Gamemode gamemode = new Gamemode(name, creator.getUniqueId());

        try {
            if (gamemodeRepository != null) {
                gamemodeRepository.save(gamemode.getId(), gamemode);
            } else {
                log.warning("GamemodeRepository is null, gamemode will not be persisted!");
            }

            gamemodeCache.put(gamemode.getId(), gamemode);
            log.info("Created new gamemode: " + name + " (ID: " + gamemode.getId() + ")");
            return true;
        } catch (Exception e) {
            log.severe("Failed to create gamemode: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Updates an existing gamemode.
     *
     * @param gamemode The gamemode to update
     * @return true if update was successful, false otherwise
     */
    public boolean updateGamemode(Gamemode gamemode) {
        gamemode.setLastUpdated(System.currentTimeMillis());

        try {
            if (gamemodeRepository != null) {
                gamemodeRepository.save(gamemode.getId(), gamemode);
            } else {
                log.warning("GamemodeRepository is null, gamemode will not be persisted!");
            }

            gamemodeCache.put(gamemode.getId(), gamemode);

            // Update any active queues if the gamemode was disabled

            queueManager.removeGamemodeQueues(gamemode.getId());
            if(gamemode.isEnabled())
                queueManager.addGamemodeQueues(gamemode);

            log.info("Updated gamemode: " + gamemode.getName() + " (ID: " + gamemode.getId() + ")");
            return true;
        } catch (Exception e) {
            log.severe("Failed to update gamemode: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Deletes a gamemode by ID.
     *
     * @param gamemodeId The ID of the gamemode to delete
     * @return true if deletion was successful, false otherwise
     */
    public boolean deleteGamemode(String gamemodeId) {
        try {
            if (gamemodeRepository != null) {
                gamemodeRepository.delete(gamemodeId);
            } else {
                log.warning("GamemodeRepository is null, gamemode will not be deleted from persistence!");
            }

            Gamemode removed = gamemodeCache.remove(gamemodeId);

            // Remove any active queues for this gamemode
            queueManager.removeGamemodeQueues(gamemodeId);

            CompletableFuture.runAsync(() -> {
                        PlayerRepository playerRepository = Services.loadIfPresent(PlayerRepository.class);
                        Collection<String> keys = playerRepository.keys();
                        for (String key : keys) {
                            PlayerData playerData = playerRepository.find(key);
                            playerData.removeLoadout(gamemodeId);
                            playerRepository.save(key, playerData);

                        }

                        log.info("Removed associated kits from all players.");
                    });


            log.info("Deleted gamemode: " + (removed != null ? removed.getName() : "unknown") +
                    " (ID: " + gamemodeId + ")");
            return true;
        } catch (Exception e) {
            log.severe("Failed to delete gamemode: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
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
     * Gets a gamemode by ID.
     *
     * @param gamemodeId The ID of the gamemode to retrieve
     * @return The gamemode, or null if not found
     */
    public Gamemode getGamemode(String gamemodeId) {
        if (gamemodeId == null) return null;
        return gamemodeCache.get(gamemodeId);
    }

    /**
     * Gets all gamemodes from the cache.
     *
     * @return A collection of all gamemodes
     */
    public Collection<Gamemode> getAllGamemodes() {
        return new ArrayList<>(gamemodeCache.values());
    }
}