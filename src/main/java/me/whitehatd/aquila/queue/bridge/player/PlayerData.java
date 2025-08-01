package me.whitehatd.aquila.queue.bridge.player;

import gg.supervisor.core.loader.SupervisorLoader;
import lombok.Data;
import me.whitehatd.aquila.queue.bridge.match.ArmorSlot;
import org.bukkit.inventory.ItemStack;

import java.util.*;

@Data
public class PlayerData {

    // Identification
    private final UUID uuid;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
    }

    // Current ELO rating (for ranked matches)
    private int elo = 1000;

    private List<Friend> friends = new ArrayList<>();

    private long lastSeen;

    private boolean isOnArena;

    // Cumulative Health/Recovery Data
    private double totalHealthRegenerated = 0;

    // Cumulative Combat Statistics
    private double totalDamageDealt = 0;
    private double totalDamageTaken = 0;
    private int totalHitsLanded = 0;
    private int totalHitsReceived = 0;
    private int totalCriticalHits = 0;
    private int highestComboAchieved = 0;
    private double longestDistanceHit = 0;

    // Cumulative Bow Statistics
    private int totalArrowsShot = 0;
    private int totalArrowsHit = 0;

    // Cumulative Movement Statistics
    private int totalBlocksTraversed = 0;
    private int totalJumps = 0;
    private double totalSprintDistance = 0;

    // ELO accumulators
    private int totalEloGained = 0;
    private int totalEloLost = 0;

    // Cumulative Inventory Usage
    private int totalPotionsUsed = 0;
    private int totalFoodConsumed = 0;
    private Map<ArmorSlot, Double> averageArmorDurability = new HashMap<>();

    // Cumulative Block/Item Interaction Statistics
    private int totalBlocksPlaced = 0;
    private int totalBlocksBroken = 0;
    private int totalItemsPickedUp = 0;
    private int totalItemsDropped = 0;

    // Match counts and streaks
    private int matchesPlayed = 0;
    private int matchesWon = 0;
    private int matchesLost = 0;
    private int currentWinStreak = 0;
    private int currentLossStreak = 0;

    private Map<String, Map<Integer, String>> gamemodeLoadouts = new HashMap<>();

    /**
     * Returns the loadout for the specified gamemode as a mapping from slot to ItemStack.
     * If no loadout is found, returns null.
     */
    public Map<Integer, ItemStack> getLoadout(String gamemodeId) {
        Map<Integer, String> jsonLoadout = gamemodeLoadouts.get(gamemodeId);
        if (jsonLoadout == null) return null;
        Map<Integer, ItemStack> loadout = new HashMap<>();
        for (Map.Entry<Integer, String> entry : jsonLoadout.entrySet()) {
            loadout.put(entry.getKey(), SupervisorLoader.GSON.fromJson(entry.getValue(), ItemStack.class));
        }
        return loadout;
    }

    public Map<Integer, String> getLoadoutRaw(String gamemodeId) {
        return gamemodeLoadouts.get(gamemodeId);
    }

    /**
     * Sets the loadout for the specified gamemode from a mapping of slot to ItemStack.
     * All ItemStacks are serialized internally as JSON.
     */
    public void setLoadout(String gamemodeId, Map<Integer, ItemStack> loadout) {
        Map<Integer, String> jsonLoadout = new HashMap<>();
        for (Map.Entry<Integer, ItemStack> entry : loadout.entrySet()) {
            jsonLoadout.put(entry.getKey(), SupervisorLoader.GSON.toJson(entry.getValue(), ItemStack.class));
        }
        gamemodeLoadouts.put(gamemodeId, jsonLoadout);
    }

    /**
     * Adds or updates a single loadout item for the specified gamemode.
     */
    public void addLoadoutItem(String gamemodeId, int slot, ItemStack item) {
        Map<Integer, String> jsonLoadout = gamemodeLoadouts.computeIfAbsent(gamemodeId, k -> new HashMap<>());
        jsonLoadout.put(slot, SupervisorLoader.GSON.toJson(item, ItemStack.class));
    }

    /**
     * Retrieves a single loadout item for the given gamemode and slot.
     */
    public ItemStack getLoadoutItem(String gamemodeId, int slot) {
        Map<Integer, String> jsonLoadout = gamemodeLoadouts.get(gamemodeId);
        if (jsonLoadout == null || !jsonLoadout.containsKey(slot))
            return null;
        return SupervisorLoader.GSON.fromJson(jsonLoadout.get(slot), ItemStack.class);
    }

    /**
     * Removes the loadout for the given gamemode.
     */
    public void removeLoadout(String gamemodeId) {
        gamemodeLoadouts.remove(gamemodeId);
    }

    public List<String> getFriendIds() {
        List<String> friendIds = new ArrayList<>();
        if(friends == null) {
            friends = new ArrayList<>();
            return friendIds;
        }
        for (Friend friend : friends) {
            friendIds.add(friend.getFriendId());
        }
        return friendIds;
    }
}
