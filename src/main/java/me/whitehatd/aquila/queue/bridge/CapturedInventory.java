package me.whitehatd.aquila.queue.bridge;

import gg.supervisor.core.loader.SupervisorLoader;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class CapturedInventory {
    private List<String> items = new ArrayList<>();
    private String helmet;
    private String chestplate;
    private String leggings;
    private String boots;
    private String offhand;

    /**
     * Captures a player's inventory state
     */
    public static CapturedInventory fromPlayer(Player player) {
        CapturedInventory inventory = new CapturedInventory();

        // Main inventory
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                inventory.items.add(SupervisorLoader.GSON.toJson(item, ItemStack.class));
            }
        }

        // Armor and offhand

            if (player.getEquipment().getHelmet() != null) {
                inventory.helmet = SupervisorLoader.GSON.toJson(player.getEquipment().getHelmet(), ItemStack.class);
            }
            if (player.getEquipment().getChestplate() != null) {
                inventory.chestplate = SupervisorLoader.GSON.toJson(player.getEquipment().getChestplate(), ItemStack.class);
            }
            if (player.getEquipment().getLeggings() != null) {
                inventory.leggings = SupervisorLoader.GSON.toJson(player.getEquipment().getLeggings(), ItemStack.class);
            }
            if (player.getEquipment().getBoots() != null) {
                inventory.boots = SupervisorLoader.GSON.toJson(player.getEquipment().getBoots(), ItemStack.class);
            }
            
            ItemStack offhand = player.getEquipment().getItemInOffHand();
            if (offhand.getType() != Material.AIR) {
                inventory.offhand = SupervisorLoader.GSON.toJson(offhand, ItemStack.class);
            }

        return inventory;
    }

    /**
     * Serialize to JSON
     */
    public String toJson() {
        return SupervisorLoader.GSON.toJson(this);
    }

    /**
     * Deserialize from JSON
     */
    public static CapturedInventory fromJson(String json) {
        return SupervisorLoader.GSON.fromJson(json, CapturedInventory.class);
    }
}