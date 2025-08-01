package me.whitehatd.aquila.queue.bridge.gamemode;

import gg.supervisor.core.loader.SupervisorLoader;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.whitehatd.aquila.queue.bridge.match.MatchType;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Represents a game mode configuration
 */
@Data
@NoArgsConstructor
public class Gamemode {

    private String id;
    private String name;
    private UUID creatorId;
    private long createdAt;
    private long lastUpdated;
    private boolean enabled = false;
    private String iconJson;
    private String description = "No description provided";
    private List<String> arenas = new ArrayList<>();


    // Match types this gamemode supports
    private Set<MatchType> availableMatchTypes = new HashSet<>();

    // Game rules as key-value pairs
    private Map<String, Object> gameRules = new HashMap<>();

    // Loadout data - maps inventory slot positions to serialized ItemStack JSON
    private Map<Integer, String> loadoutData = new HashMap<>();

    public Gamemode(String name, UUID creatorId) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.creatorId = creatorId;
        this.createdAt = System.currentTimeMillis();
        this.lastUpdated = System.currentTimeMillis();

        // Default to supporting all match types
        this.availableMatchTypes.add(MatchType.ONE_VS_ONE);
        this.availableMatchTypes.add(MatchType.TWO_VS_TWO);
        this.availableMatchTypes.add(MatchType.THREE_VS_THREE);
        this.availableMatchTypes.add(MatchType.FOUR_VS_FOUR);
    }

    /**
     * Adds an item to the loadout at the specified inventory position
     *
     * @param slot The inventory slot position (0-35)
     * @param item The ItemStack to add
     */
    public void addLoadoutItem(int slot, ItemStack item) {
        loadoutData.put(slot, SupervisorLoader.GSON.toJson(item, ItemStack.class));
    }

    /**
     * Removes an item from the loadout at the specified position
     *
     * @param slot The inventory slot to clear
     */
    public void removeLoadoutItem(int slot) {
        loadoutData.remove(slot);
    }

    /**
     * Gets the item at the specified loadout position
     *
     * @param slot The inventory slot
     * @return The serialized ItemStack JSON, or null if no item exists
     */
    public ItemStack getLoadoutItem(int slot) {
        if(!loadoutData.containsKey(slot)) {
            return null;
        }
        return SupervisorLoader.GSON.fromJson(loadoutData.get(slot), ItemStack.class);
    }

    /**
     * Clears all loadout items
     */
    public void clearLoadout() {
        loadoutData.clear();
    }
}