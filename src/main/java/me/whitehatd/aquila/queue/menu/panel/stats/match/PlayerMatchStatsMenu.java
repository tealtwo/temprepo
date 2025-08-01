package me.whitehatd.aquila.queue.menu.panel.stats.match;

import gg.supervisor.menu.entities.InteractionModifier;
import gg.supervisor.menu.guis.builder.SchemaBuilder;
import gg.supervisor.menu.guis.impl.PersonalizedMenu;
import gg.supervisor.menu.builder.ItemBuilder;
import gg.supervisor.util.chat.Text;
import me.whitehatd.aquila.queue.bridge.match.ArmorSlot;
import me.whitehatd.aquila.queue.bridge.match.Match;
import me.whitehatd.aquila.queue.bridge.match.PlayerMatchStats;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerMatchStatsMenu extends PersonalizedMenu {

    private final Player player;
    private final Match match;
    private final UUID targetUuid;
    private final PlayerMatchStats stats;
    private final DecimalFormat df = new DecimalFormat("#.##");

    public PlayerMatchStatsMenu(Player player, Match match, UUID targetUuid) {
        super(player, 4, Text.translate("&5Player Match Stats"), InteractionModifier.VALUES);
        this.player = player;
        this.match = match;
        this.targetUuid = targetUuid;
        this.stats = match.getPlayerStats().get(targetUuid.toString());

        getDecorator().decorate(new SchemaBuilder()
                .add("p        ")
                .add(" h c b m ")
                .add(" i a d   ")
                .add("    r    ")
                .build());

        populateStats();
    }

    private void populateStats() {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(targetUuid);
        String playerName = offlinePlayer.getName() != null ? offlinePlayer.getName() : "Unknown";

        if (stats == null) {
            // No stats available
            getDecorator().add('p', ItemBuilder.from(Material.BARRIER)
                    .name("&cNo Stats Available")
                    .lore(List.of(Text.translate("&7No statistics available for this player")))
                    .build());

            // Back button
            getDecorator().add('r', ItemBuilder.from(Material.ARROW)
                    .name("&cBack")
                    .menuItem(event -> {
                        close(player);
                        new MatchPlayersMenu(player, match).open();
                    }));
            return;
        }

        // Player head with name and general info
        List<String> headerLore = new ArrayList<>();

        boolean userIsTeamA = match.getTeamA().contains(player.getUniqueId().toString());
        boolean isTeamA = match.getTeamA().contains(targetUuid.toString());

        String whichTeam = isTeamA ? (userIsTeamA ? "&aYour team" : "&cOpponents") : (userIsTeamA ? "&cOpponents" : "&aYour team");
        headerLore.add("&7Team: " + whichTeam);

        if (match.getDeadPlayers().contains(targetUuid.toString())) {
            headerLore.add("");
            headerLore.add("&cDied during match");
        } else if (match.getDisconnectedPlayers().contains(targetUuid.toString())) {
            headerLore.add("");
            headerLore.add("&cDisconnected during match");
        }

        if (match.isRanked() && stats.getEloChange() != 0) {
            headerLore.add("");
            String eloPrefix = stats.getEloChange() > 0 ? "&a+" : "&c";
            headerLore.add("&7ELO: " + stats.getPreviousElo() + " → " + stats.getNewElo() +
                    " (" + eloPrefix + stats.getEloChange() + ")");
        }

        getDecorator().add('p', ItemBuilder.from(Material.PLAYER_HEAD)
                .name("&e" + playerName)
                .lore(headerLore.stream().map(Text::translate).toList())
                .build());

        // Health stats
        List<String> healthLore = new ArrayList<>();
        if (stats.getFinalHealth() > 0) {
            healthLore.add("&7Final Health: &e" + df.format(stats.getFinalHealth()) + "&c❤");
        }
        healthLore.add("&7Max Health: &e" + df.format(stats.getMaxHealth()) + "&c❤");
        healthLore.add("&7Health Regenerated: &e" + df.format(stats.getHealthRegenerated()) + "&c❤");

        getDecorator().add('h', ItemBuilder.from(Material.GOLDEN_APPLE)
                .name("&aHealth Stats")
                .lore(healthLore.stream().map(Text::translate).toList())
                .build());

        // Combat stats
        List<String> combatLore = new ArrayList<>();
        combatLore.add("&7Damage Dealt: &e" + df.format(stats.getDamageDealt()) + " &c❤");
        combatLore.add("&7Damage Taken: &e" + df.format(stats.getDamageTaken()) + " &c❤");
        combatLore.add("&7Hits Landed: &e" + stats.getHitsLanded());
        combatLore.add("&7Hits Received: &e" + stats.getHitsReceived());
        combatLore.add("&7Critical Hits: &e" + stats.getCriticalHits());
        combatLore.add("&7Max Combo: &e" + stats.getMaxCombo());
        combatLore.add("&7Longest Hit Distance: &e" + df.format(stats.getLongestDistanceHit()) + " blocks");

        if (stats.getHitsLanded() > 0) {
            double damagePerHit = stats.getDamageDealt() / stats.getHitsLanded();
            combatLore.add("&7Avg. Damage/Hit: &e" + df.format(damagePerHit) + " &c❤");
        }
        if (stats.getHitsLanded() > 0) {
            double criticalPercentage = (double) stats.getCriticalHits() / stats.getHitsLanded() * 100;
            combatLore.add("&7Critical Hit Rate: &e" + df.format(criticalPercentage) + "%");
        }

        getDecorator().add('c', ItemBuilder.from(Material.IRON_SWORD)
                .name("&aCombat Stats")
                .lore(combatLore.stream().map(Text::translate).toList())
                .build());

        // Bow stats
        List<String> bowLore = new ArrayList<>();
        bowLore.add("&7Arrows Shot: &e" + stats.getArrowsShot());
        bowLore.add("&7Arrows Hit: &e" + stats.getArrowsHit());

        if (stats.getArrowsShot() > 0) {
            double accuracy = (double) stats.getArrowsHit() / stats.getArrowsShot() * 100;
            bowLore.add("&7Accuracy: &e" + df.format(accuracy) + "%");
        }

        getDecorator().add('b', ItemBuilder.from(Material.BOW)
                .name("&aArchery Stats")
                .lore(bowLore.stream().map(Text::translate).toList())
                .build());

        // Movement stats
        List<String> movementLore = new ArrayList<>();
        movementLore.add("&7Blocks Traversed: &e" + stats.getBlocksTraversed());
        movementLore.add("&7Jumps: &e" + stats.getJumps());
        movementLore.add("&7Sprint Distance: &e" + df.format(stats.getSprintDistance()) + " blocks");

        getDecorator().add('m', ItemBuilder.from(Material.LEATHER_BOOTS)
                .name("&aMovement Stats")
                .lore(movementLore.stream().map(Text::translate).toList())
                .build());

        // Inventory stats
        List<String> inventoryLore = new ArrayList<>();
        inventoryLore.add("&7Potions Used: &e" + stats.getPotionsUsed());
        inventoryLore.add("&7Food Consumed: &e" + stats.getFoodConsumed());

        // Add armor durability if available
        if (!stats.getArmorDurability().isEmpty()) {
            inventoryLore.add("");
            inventoryLore.add("&7Armor Durability:");
            for (ArmorSlot slot : stats.getArmorDurability().keySet()) {
                double durability = stats.getArmorDurability().get(slot);
                inventoryLore.add("  &7" + slot.name() + ": &e" + df.format(durability) + "%");
            }
        }

        inventoryLore.add("");
        inventoryLore.add("&eClick to view final inventory");

        getDecorator().add('i', ItemBuilder.from(Material.CHEST)
                .name("&aInventory Stats")
                .lore(inventoryLore.stream().map(Text::translate).toList())
                .menuItem(event -> {
                    new InventoryDisplayMenu(player, match, targetUuid).open();
                }));

        // Additional stats
        List<String> additionalLore = new ArrayList<>();
        additionalLore.add("&7Blocks Placed: &e" + stats.getBlocksPlaced());
        additionalLore.add("&7Blocks Broken: &e" + stats.getBlocksBroken());
        additionalLore.add("&7Items Picked Up: &e" + stats.getItemsPickedUp());
        additionalLore.add("&7Items Dropped: &e" + stats.getItemsDropped());

        getDecorator().add('a', ItemBuilder.from(Material.GRASS_BLOCK)
                .name("&aAdditional Stats")
                .lore(additionalLore.stream().map(Text::translate).toList())
                .build());

        // Detailed statistics
        List<String> detailedLore = new ArrayList<>();
        if (stats.getHitsReceived() > 0) {
            double damagePerHitReceived = stats.getDamageTaken() / stats.getHitsReceived();
            detailedLore.add("&7Avg. Damage Taken/Hit: &e" + df.format(damagePerHitReceived) + " &c❤");
        }
        detailedLore.add("&7Match Duration: &e" + formatDuration(match.getEndTime() - match.getStartTime()));

        getDecorator().add('d', ItemBuilder.from(Material.CLOCK)
                .name("&aOther Stats")
                .lore(detailedLore.stream().map(Text::translate).toList())
                .build());

        // Back button
        getDecorator().add('r', ItemBuilder.from(Material.ARROW)
                .name("&cBack")
                .menuItem(event -> {
                    close(player);
                    new MatchPlayersMenu(player, match).open();
                }));
    }

    private String formatDuration(long millis) {
        int seconds = (int) (millis / 1000) % 60;
        int minutes = (int) (millis / 60000);
        return minutes + "m " + seconds + "s";
    }
}