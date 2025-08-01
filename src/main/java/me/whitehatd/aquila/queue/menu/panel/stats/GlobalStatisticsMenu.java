package me.whitehatd.aquila.queue.menu.panel.stats;

import gg.supervisor.core.util.Services;
import gg.supervisor.menu.entities.InteractionModifier;
import gg.supervisor.menu.guis.builder.SchemaBuilder;
import gg.supervisor.menu.guis.impl.PersonalizedMenu;
import gg.supervisor.menu.builder.ItemBuilder;
import gg.supervisor.util.chat.Text;
import me.whitehatd.aquila.queue.bridge.player.PlayerData;
import me.whitehatd.aquila.queue.bridge.player.PlayerRepository;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Stream;

public class GlobalStatisticsMenu extends PersonalizedMenu {

    private final Player player;
    private final PlayerData playerData;

    public GlobalStatisticsMenu(Player player) {
        super(player, 5, Text.translate("&5Global Statistics"), InteractionModifier.VALUES);
        this.player = player;
        PlayerRepository playerRepository = Services.loadIfPresent(PlayerRepository.class);
        this.playerData = playerRepository.find(player.getUniqueId().toString());

        getDecorator().decorate(new SchemaBuilder()
                .add("         ")
                .add(" 1 2 3 4 ")
                .add(" 5 6 7 8 ")
                .add("         ")
                .add("    b    ")
                .build());

        populateMenu();
    }

    private void populateMenu() {
        getDecorator().set(' ', ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).menuItem());

        // Combat Statistics
        getDecorator().add('1', ItemBuilder.from(Material.IRON_SWORD)
                .name("&aCombat Statistics")
                .lore(getCombatStatisticsLore())
                .menuItem(event -> {}));

        // Bow Statistics
        getDecorator().add('2', ItemBuilder.from(Material.BOW)
                .name("&aBow Statistics")
                .lore(getBowStatisticsLore())
                .menuItem(event -> {}));

        // Movement Statistics
        getDecorator().add('3', ItemBuilder.from(Material.LEATHER_BOOTS)
                .name("&aMovement Statistics")
                .lore(getMovementStatisticsLore())
                .menuItem(event -> {}));

        // ELO Statistics
        getDecorator().add('4', ItemBuilder.from(Material.NETHER_STAR)
                .name("&aELO Statistics")
                .lore(getEloStatisticsLore())
                .menuItem(event -> {}));

        // Inventory Usage
        getDecorator().add('5', ItemBuilder.from(Material.CHEST)
                .name("&aInventory Usage")
                .lore(getInventoryUsageLore())
                .menuItem(event -> {}));

        // Block/Item Interaction
        getDecorator().add('6', ItemBuilder.from(Material.GRASS_BLOCK)
                .name("&aBlock/Item Interaction")
                .lore(getBlockItemInteractionLore())
                .menuItem(event -> {}));

        // Match Counts and Streaks
        getDecorator().add('7', ItemBuilder.from(Material.DIAMOND)
                .name("&aMatch Statistics")
                .lore(getMatchStatisticsLore())
                .menuItem(event -> {}));

        // Current ELO
        getDecorator().add('8', ItemBuilder.from(Material.GOLDEN_HELMET)
                .name("&aCurrent ELO Rating")
                .lore(List.of(
                        Text.translate("&7Your current ELO: &e" + playerData.getElo())
                ))
                .menuItem(event -> {}));

        // Back button
        getDecorator().add('b', ItemBuilder.from(Material.BARRIER)
                .name("&cBack")
                .menuItem(event -> {
                    close(player);
                    new StatisticsMenu(player).open();
                }));
    }

    private List<Component> getCombatStatisticsLore() {
        return Stream.of(
                "&7Total damage dealt: &e" + formatDouble(playerData.getTotalDamageDealt()),
                "&7Total damage taken: &e" + formatDouble(playerData.getTotalDamageTaken()),
                "&7Total hits landed: &e" + playerData.getTotalHitsLanded(),
                "&7Total hits received: &e" + playerData.getTotalHitsReceived(),
                "&7Total critical hits: &e" + playerData.getTotalCriticalHits(),
                "&7Highest combo achieved: &e" + playerData.getHighestComboAchieved(),
                "&7Longest distance hit: &e" + formatDouble(playerData.getLongestDistanceHit()) + " blocks",
                "&7Total health regenerated: &e" + formatDouble(playerData.getTotalHealthRegenerated())
        ).map(Text::translate).toList();
    }

    private List<Component> getBowStatisticsLore() {
        return Stream.of(
                "&7Total arrows shot: &e" + playerData.getTotalArrowsShot(),
                "&7Total arrows hit: &e" + playerData.getTotalArrowsHit(),
                "&7Accuracy rate: &e" + calculateAccuracy() + "%"
        ).map(Text::translate).toList();
    }

    private List<Component> getMovementStatisticsLore() {
        return Stream.of(
                "&7Total blocks traversed: &e" + playerData.getTotalBlocksTraversed(),
                "&7Total jumps: &e" + playerData.getTotalJumps(),
                "&7Total sprint distance: &e" + formatDouble(playerData.getTotalSprintDistance()) + " blocks"
        ).map(Text::translate).toList();
    }

    private List<Component> getEloStatisticsLore() {
        return Stream.of(
                "&7Total ELO gained: &a+" + playerData.getTotalEloGained(),
                "&7Total ELO lost: &c-" + playerData.getTotalEloLost(),
                "&7Net ELO change: &e" + (playerData.getTotalEloGained() - playerData.getTotalEloLost())
        ).map(Text::translate).toList();
    }

    private List<Component> getInventoryUsageLore() {
        return Stream.of(
                "&7Total potions used: &e" + playerData.getTotalPotionsUsed(),
                "&7Total food consumed: &e" + playerData.getTotalFoodConsumed()
        ).map(Text::translate).toList();
    }

    private List<Component> getBlockItemInteractionLore() {
        return Stream.of(
                "&7Total blocks placed: &e" + playerData.getTotalBlocksPlaced(),
                "&7Total blocks broken: &e" + playerData.getTotalBlocksBroken(),
                "&7Total items picked up: &e" + playerData.getTotalItemsPickedUp(),
                "&7Total items dropped: &e" + playerData.getTotalItemsDropped()
        ).map(Text::translate).toList();
    }

    private List<Component> getMatchStatisticsLore() {
        return Stream.of(
                "&7Matches played: &e" + playerData.getMatchesPlayed(),
                "&7Matches won: &a" + playerData.getMatchesWon(),
                "&7Matches lost: &c" + playerData.getMatchesLost(),
                "&7Win rate: &e" + calculateWinRate() + "%",
                "&7Current win streak: &a" + playerData.getCurrentWinStreak(),
                "&7Current loss streak: &c" + playerData.getCurrentLossStreak()
        ).map(Text::translate).toList();
    }

    private String formatDouble(double value) {
        return String.format("%.2f", value);
    }

    private String calculateAccuracy() {
        if (playerData.getTotalArrowsShot() == 0) {
            return "0.00";
        }
        double accuracy = (double) playerData.getTotalArrowsHit() / playerData.getTotalArrowsShot() * 100;
        return formatDouble(accuracy);
    }

    private String calculateWinRate() {
        if (playerData.getMatchesPlayed() == 0) {
            return "0.00";
        }
        double winRate = (double) playerData.getMatchesWon() / playerData.getMatchesPlayed() * 100;
        return formatDouble(winRate);
    }
}