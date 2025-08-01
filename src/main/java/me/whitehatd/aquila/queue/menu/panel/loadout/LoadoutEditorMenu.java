package me.whitehatd.aquila.queue.menu.panel.loadout;

import gg.supervisor.core.loader.SupervisorLoader;
import gg.supervisor.core.util.Services;
import gg.supervisor.menu.entities.InteractionModifier;
import gg.supervisor.menu.guis.impl.PersonalizedMenu;
import gg.supervisor.menu.builder.ItemBuilder;
import gg.supervisor.util.chat.Text;
import me.whitehatd.aquila.queue.bridge.gamemode.Gamemode;
import me.whitehatd.aquila.queue.bridge.player.PlayerData;
import me.whitehatd.aquila.queue.bridge.player.PlayerRepository;
import me.whitehatd.aquila.queue.manager.GamemodeManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class LoadoutEditorMenu extends PersonalizedMenu {

    private final GamemodeManager gamemodeManager;
    private final Gamemode gamemode;
    private final Player player;

    // Define which slots are editable - regular inventory (0-26) and armor (36-39)
    private final Set<Integer> editableSlots = new HashSet<>();
    // Define armor slot mappings (convert from UI to loadout data)
    private final int[] armorSlots = {1, 3, 5, 7}; // helmet, chestplate, leggings, boots

    public LoadoutEditorMenu(Player player, Gamemode gamemode) {
        super(player, 6, Text.translate("&5Edit Loadout: " + gamemode.getName()),
                Set.of(
                        InteractionModifier.PREVENT_ITEM_SWAP,
                        InteractionModifier.PREVENT_OTHER_ACTIONS));
        this.player = player;
        this.gamemode = gamemode;
        this.gamemodeManager = Services.loadIfPresent(GamemodeManager.class);

        for (int i = 9; i < 36; i++) {
            editableSlots.add(i);
        }

        for (int armorSlot : armorSlots) {
            editableSlots.add(armorSlot);
        }

        setupMenu();
    }

    private void setupMenu() {
        clearGui();

        // Fill all slots with glass panes initially
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 9; col++) {
                int slot = (row * 9) + col;
                if (!editableSlots.contains(slot)) {
                    setItem(slot, ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE)
                            .name("")
                            .menuItem(e -> e.setCancelled(true)));
                }
            }
        }

        // Load the player's personal loadout.
        PlayerRepository playerRepo = Services.loadIfPresent(PlayerRepository.class);
        Map<Integer, ItemStack> playerLoadout = null;
        if (playerRepo != null) {
            PlayerData data = playerRepo.find(player.getUniqueId().toString());
            if (data != null) {
                playerLoadout = data.getLoadout(gamemode.getId());
            }
        }
        // If no personal loadout is found, load the default (global) gamemode loadout.
        if (playerLoadout == null) {
            // Convert the global loadout data (map of slot -> JSON) into a map of ItemStacks.
            Map<Integer, String> defaultJson = gamemode.getLoadoutData();
            playerLoadout = new HashMap<>();
            if (defaultJson != null) {
                for (Map.Entry<Integer, String> entry : defaultJson.entrySet()) {
                    playerLoadout.put(entry.getKey(),
                            SupervisorLoader.GSON.fromJson(entry.getValue(), ItemStack.class));
                }
            }
            // Save the default as the player's personal loadout immediately.
            if (playerRepo != null) {
                PlayerData data = playerRepo.find(player.getUniqueId().toString());
                data.setLoadout(gamemode.getId(), playerLoadout);
                playerRepo.save(player.getUniqueId().toString(), data);
            }
        }

        for (int slot = 9; slot < 36; slot++) {
            int key = slot - 9;
            ItemStack item = playerLoadout.get(key);
            if (item != null) {
                setItem(slot, item);
            }
        }

        for (int i = 27; i < 31; i++) {
            ItemStack item = playerLoadout.get(i);
            if (item != null) {
                setItem(armorSlots[i - 27], item);
            }
        }

        // Add save button
        setItem(6, 3, ItemBuilder.from(Material.EMERALD)
                .name("&aSave Loadout")
                .lore(Collections.singletonList(
                        Text.translate("&7Save the current loadout configuration")
                ))
                .menuItem(event -> {
                    event.setCancelled(true);
                    saveLoadout();
                    player.sendMessage(Text.translate("&aLoadout saved successfully!"));
                    close(player);
                    new LoadoutGamemodeMenu(player).open();
                }));

        setItem(6, 5, ItemBuilder.from(Material.LIGHT_BLUE_DYE)
                .name("&cRestore Default Loadout")
                .lore(Collections.singletonList(Text.translate("&7Use the global loadout as your default")))
                .menuItem(event -> {
                    event.setCancelled(true);
                    restoreDefaultLoadout();
                    player.sendMessage(Text.translate("&aDefault loadout restored."));
                    setupMenu(); // Refresh the menu.
                }));

        // Add back button
        setItem(6, 8, ItemBuilder.from(Material.BARRIER)
                .name("&cBack")
                .lore(Collections.singletonList(
                        Text.translate("&7Return to the gamemodes loadouts menu")
                ))
                .menuItem(event -> {
                    event.setCancelled(true);
                    close(player);
                    new LoadoutGamemodeMenu(player).open();
                }));
    }

    /**
     * Saves the current configuration from the GUI into the player's personal loadout.
     */
    private void saveLoadout() {
        Map<Integer, ItemStack> playerLoadout = new HashMap<>();

        // Save regular inventory items (slots 9-35 map to indices 0-26).
        for (int slot = 9; slot < 36; slot++) {
            ItemStack item = getInventory().getItem(slot);
            if (item != null && item.getType() != Material.AIR &&
                    item.getType() != Material.BLACK_STAINED_GLASS_PANE) {
                playerLoadout.put(slot - 9, item);
            }
        }
        // Save armor items (indices 27-30).
        for (int i = 0; i < 4; i++) {
            ItemStack item = getInventory().getItem(armorSlots[i]);
            if (item != null && item.getType() != Material.AIR &&
                    item.getType() != Material.BLACK_STAINED_GLASS_PANE) {
                playerLoadout.put(27 + i, item);
            }
        }

        // Update the player's loadout using the helper method in PlayerData.
        PlayerRepository playerRepo = Services.loadIfPresent(PlayerRepository.class);
        if (playerRepo != null) {
            PlayerData data = playerRepo.find(player.getUniqueId().toString());
            data.setLoadout(gamemode.getId(), playerLoadout);

            playerRepo.save(player.getUniqueId().toString(), data);
        }
    }

    /**
     * Restores the default gamemode loadout (global loadout) into the player's personal loadout.
     */
    private void restoreDefaultLoadout() {
        // Use the gamemode's global loadout data as default.
        Map<Integer, String> defaultJson = gamemode.getLoadoutData();
        Map<Integer, ItemStack> defaultLoadout = new HashMap<>();
        if (defaultJson != null) {
            for (Map.Entry<Integer, String> entry : defaultJson.entrySet()) {
                defaultLoadout.put(entry.getKey(),
                        SupervisorLoader.GSON.fromJson(entry.getValue(), ItemStack.class));
            }
        }
        // Save this default loadout into the player's PlayerData.
        PlayerRepository playerRepo = Services.loadIfPresent(PlayerRepository.class);
        if (playerRepo != null) {
            PlayerData data = playerRepo.find(player.getUniqueId().toString());

            data.setLoadout(gamemode.getId(), defaultLoadout);
            playerRepo.save(player.getUniqueId().toString(), data);
        }
    }

    public Set<Integer> getEditableSlots() {
        return Collections.unmodifiableSet(editableSlots);
    }
}