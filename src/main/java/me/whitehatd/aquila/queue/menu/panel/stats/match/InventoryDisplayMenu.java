package me.whitehatd.aquila.queue.menu.panel.stats.match;

import gg.supervisor.core.loader.SupervisorLoader;
import gg.supervisor.menu.entities.InteractionModifier;
import gg.supervisor.menu.guis.Pager;
import gg.supervisor.menu.guis.builder.SchemaBuilder;
import gg.supervisor.menu.guis.impl.PersonalizedMenu;
import gg.supervisor.menu.builder.ItemBuilder;
import gg.supervisor.util.chat.Text;
import me.whitehatd.aquila.queue.bridge.CapturedInventory;
import me.whitehatd.aquila.queue.bridge.match.Match;
import me.whitehatd.aquila.queue.bridge.match.PlayerMatchStats;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public class InventoryDisplayMenu extends PersonalizedMenu {

    private final Player player;
    private final Match match;
    private final UUID targetUuid;

    public InventoryDisplayMenu(Player player, Match match, UUID targetUuid) {
        super(player, 6, Text.translate("&5Final Inventory"), InteractionModifier.VALUES);
        this.player = player;
        this.match = match;
        this.targetUuid = targetUuid;

        // Set up menu layout using decorator
        getDecorator().decorate(new SchemaBuilder()
                .add(" clho f  ")
                .add("         ")
                .add("111111111")
                .add("111111111")
                .add("111111111")
                .add("    r    ")
                .build());

        populateInventory();
    }

    private void populateInventory() {
        // Get player stats from match
        PlayerMatchStats stats = match.getPlayerStats().get(targetUuid.toString());

        if (stats == null || stats.getFinalInventoryJson() == null || stats.getFinalInventoryJson().isEmpty()) {
            // No inventory data available
            getDecorator().add('o', createErrorItem("No inventory data available").menuItem());

            // Back button
            getDecorator().add('r', ItemBuilder.from(Material.ARROW)
                    .name("&cBack")
                    .menuItem(event -> {
                        new PlayerMatchStatsMenu(player, match, targetUuid).open();
                    }));
            return;
        }

        try {
            // Parse inventory data
            CapturedInventory inventory = CapturedInventory.fromJson(stats.getFinalInventoryJson());

            // Display armor pieces
            if (inventory.getHelmet() != null) {
                ItemStack helmet = SupervisorLoader.GSON.fromJson(inventory.getHelmet(), ItemStack.class);
                getDecorator().add('c', ItemBuilder.from(helmet).build());
            } else {
                getDecorator().add('c', ItemBuilder.from(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
                        .name("&7No Helmet")
                        .build());
            }

            if (inventory.getChestplate() != null) {
                ItemStack chestplate = SupervisorLoader.GSON.fromJson(inventory.getChestplate(), ItemStack.class);
                getDecorator().add('l', ItemBuilder.from(chestplate).build());
            } else {
                getDecorator().add('l', ItemBuilder.from(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
                        .name("&7No Chestplate")
                        .build());
            }

            if (inventory.getLeggings() != null) {
                ItemStack leggings = SupervisorLoader.GSON.fromJson(inventory.getLeggings(), ItemStack.class);
                getDecorator().add('h', ItemBuilder.from(leggings).build());
            } else {
                getDecorator().add('h', ItemBuilder.from(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
                        .name("&7No Leggings")
                        .build());
            }

            if (inventory.getBoots() != null) {
                ItemStack boots = SupervisorLoader.GSON.fromJson(inventory.getBoots(), ItemStack.class);
                getDecorator().add('o', ItemBuilder.from(boots).build());
            } else {
                getDecorator().add('o', ItemBuilder.from(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
                        .name("&7No Boots")
                        .build());
            }

            if (inventory.getOffhand() != null) {
                ItemStack offhand = SupervisorLoader.GSON.fromJson(inventory.getOffhand(), ItemStack.class);
                getDecorator().add('f', ItemBuilder.from(offhand).build());
            } else {
                getDecorator().add('f', ItemBuilder.from(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
                        .name("&7No Offhand Item")
                        .build());
            }

            Pager pager = new Pager(this, '1').endless(Pager.EndlessType.NONE);


            for (String itemJson : inventory.getItems()) {
                ItemStack item = SupervisorLoader.GSON.fromJson(itemJson, ItemStack.class);

                pager.add(ItemBuilder.from(item).menuItem());
            }

        } catch (Exception e) {
            // Display error
            getDecorator().add('o', createErrorItem("Failed to load inventory data: " + e.getMessage()).menuItem());
        }

        // Back button
        getDecorator().add('r', ItemBuilder.from(Material.ARROW)
                .name("&cBack")
                .menuItem(event -> {
                    close(player);
                    new PlayerMatchStatsMenu(player, match, targetUuid).open();
                }));
    }

    private ItemBuilder createErrorItem(String errorMessage) {
        return ItemBuilder.from(Material.BARRIER)
                .name("&cError")
                .lore(List.of(Text.translate("&7" + errorMessage)));
    }
}