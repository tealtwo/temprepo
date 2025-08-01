package me.whitehatd.aquila.queue.menu.gamemode;

import gg.supervisor.core.util.Services;
import gg.supervisor.menu.entities.InteractionModifier;
import gg.supervisor.menu.guis.impl.PersonalizedMenu;
import gg.supervisor.menu.builder.ItemBuilder;
import gg.supervisor.util.chat.Text;
import me.whitehatd.aquila.queue.bridge.gamemode.Gamemode;
import me.whitehatd.aquila.queue.manager.GamemodeManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class GamemodeLoadoutMenu extends PersonalizedMenu {

    private final GamemodeManager gamemodeManager;
    private final Gamemode gamemode;
    private final Player player;

    // Define which slots are editable - regular inventory (0-26) and armor (36-39)
    private final Set<Integer> editableSlots = new HashSet<>();
    // Define armor slot mappings (convert from UI to loadout data)
    private final int[] armorSlots = {1, 3, 5, 7}; // helmet, chestplate, leggings, boots

    public GamemodeLoadoutMenu(Player player, Gamemode gamemode) {
        super(player, 6, Text.translate("&5Edit Loadout: " + gamemode.getName()),
                Set.of(
                        InteractionModifier.PREVENT_ITEM_SWAP,
                        InteractionModifier.PREVENT_OTHER_ACTIONS,
                        InteractionModifier.PREVENT_ITEM_DROP));
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

        // Load the current loadout items into the menu
        for (int slot = 9; slot < 36; slot++) {
            ItemStack item = gamemode.getLoadoutItem(slot-9);
            if (item != null) {
                setItem(slot, item);
            }
        }

        for (int i = 27; i < 31; i++) {
            ItemStack item = gamemode.getLoadoutItem(i);
            if (item != null) {
                setItem(armorSlots[i - 27], item);
            }
        }

        // Add save button
        setItem(6, 5, ItemBuilder.from(Material.EMERALD)
                .name("&aSave Loadout")
                .lore(Collections.singletonList(
                        Text.translate("&7Save the current loadout configuration")
                ))
                .menuItem(event -> {
                    event.setCancelled(true);
                    saveLoadout();
                    player.sendMessage(Text.translate("&aLoadout saved successfully!"));
                    close(player);
                    new GamemodeEditMenu(player, gamemode).open();
                }));

        // Add clear button
        setItem(6, 3, ItemBuilder.from(Material.REDSTONE)
                .name("&cClear Loadout")
                .lore(Collections.singletonList(
                        Text.translate("&7Remove all items from the loadout")
                ))
                .menuItem(event -> {
                    gamemode.clearLoadout();
                    gamemodeManager.updateGamemode(gamemode);
                    player.sendMessage(Text.translate("&cLoadout cleared."));
                    event.setCancelled(true);
                    setupMenu(); // Refresh the menu
                }));

        // Add back button
        setItem(6, 9, ItemBuilder.from(Material.ARROW)
                .name("&cBack")
                .lore(Collections.singletonList(
                        Text.translate("&7Return to gamemode edit menu")
                ))
                .menuItem(event -> {
                    event.setCancelled(true);
                    close(player);
                    new GamemodeEditMenu(player, gamemode).open();
                }));
    }

    private void saveLoadout() {
        // Clear existing loadout
        gamemode.clearLoadout();

        // Save regular inventory items (slots 0-26)
        for (int slot = 9; slot < 36; slot++) {
            if(getInventory().getItem(slot) == null) continue;

            ItemStack item = getInventory().getItem(slot);
            if (item != null && item.getType() != Material.AIR &&
                    item.getType() != Material.BLACK_STAINED_GLASS_PANE) {
                gamemode.addLoadoutItem(slot - 9, item);
            }
        }

        for (int i = 0; i < 4; i++) {
            ItemStack item = getInventory().getItem(armorSlots[i]);
            if (item != null && item.getType() != Material.AIR &&
                    item.getType() != Material.BLACK_STAINED_GLASS_PANE) {
                gamemode.addLoadoutItem(27 + i, item);
            }
        }

        // Save the gamemode with updated loadout
        gamemodeManager.updateGamemode(gamemode);
    }
}