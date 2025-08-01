package me.whitehatd.aquila.queue.menu.queue;

import gg.supervisor.core.util.Services;
import gg.supervisor.menu.entities.InteractionModifier;
import gg.supervisor.menu.guis.builder.SchemaBuilder;
import gg.supervisor.menu.guis.impl.PersonalizedMenu;
import gg.supervisor.menu.builder.ItemBuilder;
import gg.supervisor.menu.item.MenuItem;
import gg.supervisor.util.chat.Text;
import me.whitehatd.aquila.queue.QueuePlugin;
import me.whitehatd.aquila.queue.bridge.gamemode.Gamemode;
import me.whitehatd.aquila.queue.bridge.match.MatchType;
import me.whitehatd.aquila.queue.manager.GamemodeManager;
import me.whitehatd.aquila.queue.manager.QueueManager;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class MatchTypeQueueMenu extends PersonalizedMenu {

    private final QueuePlugin plugin;
    private final GamemodeManager gamemodeManager;
    private final QueueManager queueManager;
    private boolean ranked = false; // Default to unranked
    private int pingDifference = 0; // Default to infinite ping difference
    private BukkitTask refreshTask;
    private final Gamemode selectedGamemode;

    public MatchTypeQueueMenu(Player player, QueuePlugin plugin, Gamemode selectedGamemode) {
        super(player, 6, Text.translate("&5Select Match Type"), InteractionModifier.VALUES);
        this.plugin = plugin;
        this.gamemodeManager = Services.loadIfPresent(GamemodeManager.class);
        this.queueManager = Services.loadIfPresent(QueueManager.class);
        this.selectedGamemode = selectedGamemode;

        // Set up menu layout
        getDecorator().decorate(new SchemaBuilder()
                .add("         ")
                .add(" 1 2 3 4 ")
                .add("         ")
                .add("    r    ")
                .add("    p    ")
                .add("        b")
                .build());

        redraw();
        startRefreshTask();
    }

    private void startRefreshTask() {
        // Refresh the menu every 2 seconds to update queue counts
        refreshTask = plugin.getServer().getScheduler().runTaskTimer(
                plugin,
                this::redraw,
                5L, 5L
        );
    }

    @Override
    public void redraw() {
        // Add match type options
        getDecorator().add('1', createMatchTypeItem(MatchType.ONE_VS_ONE));
        getDecorator().add('2', createMatchTypeItem(MatchType.TWO_VS_TWO));
        getDecorator().add('3', createMatchTypeItem(MatchType.THREE_VS_THREE));
        getDecorator().add('4', createMatchTypeItem(MatchType.FOUR_VS_FOUR));

        getDecorator().set(' ', ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).menuItem());

        // Add ranked toggle button
        getDecorator().add('r', ItemBuilder.from(ranked ? Material.GOLDEN_HELMET : Material.LEATHER_HELMET)
                .name(ranked ? "&6Ranked Queue" : "&7Unranked Queue")
                .lore(List.of(
                        Text.translate("&eCurrently: " + (ranked ? "&6Ranked" : "&7Unranked")),
                        Text.translate(""),
                        Text.translate("&7Click to toggle ranked status")
                ))
                .menuItem(event -> {
                    ranked = !ranked;
                    redraw();
                }));

        // Add ping difference button
        getDecorator().add('p', ItemBuilder.from(Material.CLOCK)
                .name("&bPing Difference: &e" + (pingDifference == 0 ? "∞" : pingDifference))
                .lore(List.of(
                        Text.translate("&7Current ping difference: &e" + (pingDifference == 0 ? "∞" : pingDifference + "ms")),
                        Text.translate(""),
                        Text.translate("&7Left-click to &aincrease &7by 10ms"),
                        Text.translate("&7Right-click to &cdecrease &7by 10ms"),
                        Text.translate(""),
                        Text.translate("&7When set to &e∞&7, there's no ping restriction")
                ))
                .menuItem(event -> {
                    if (event.getClick() == ClickType.LEFT) {
                        // Left-click: increase by 10
                        pingDifference += 10;
                    } else if (event.getClick() == ClickType.RIGHT) {
                        // Right-click: decrease by 10, but not below 0
                        pingDifference = Math.max(0, pingDifference - 10);
                    }
                    redraw();
                }));

        // Add back button
        getDecorator().add('b', ItemBuilder.from(Material.BARRIER)
                .name("&cBack")
                .lore(List.of(Text.translate("&7Return to gamemode selection")))
                .menuItem(event -> {
                    cancelRefreshTask();
                    close(getPlayer());
                    new GamemodeMenu(getPlayer()).open();
                }));

    }

    private MenuItem createMatchTypeItem(MatchType matchType) {
        List<String> lore = new ArrayList<>();
        lore.add("&7Queue Type: " + (ranked ? "&6Ranked" : "&7Unranked"));
        lore.add("");

        // Add queue size info
            int queueSize = queueManager.getQueueSize(
                    matchType,
                    ranked,
                    selectedGamemode.getId());
            lore.add("&7Players in queue: &e" + queueSize);

        // Add ping difference info
        lore.add("&7Ping difference: &e" + (pingDifference == 0 ? "∞" : pingDifference + "ms"));
        lore.add("");
        lore.add("&eClick to join queue");

        return ItemBuilder.from(matchType.getMaterial())
                .name("&a" + matchType.getName())
                .lore(lore.stream().map(Text::translate).toList())
                .menuItem(event -> {

                    // Check if matchType is supported by the gamemode
                    if (!selectedGamemode.getAvailableMatchTypes().contains(matchType)) {
                        getPlayer().sendMessage(Text.translate("&cThis gamemode does not support " +
                                matchType.getName()));
                        return;
                    }

                    // Join the queue with the current ping difference setting
                    // If pingDifference is 0, we pass 1000 to represent "infinite" ping difference
                    int maxPingDiff = pingDifference == 0 ? 1000 : pingDifference;
                    boolean joined = queueManager.addToQueue(
                            getPlayer().getUniqueId(),
                            matchType,
                            ranked,
                            selectedGamemode.getId(),
                            maxPingDiff);

                    if (joined) {
                        getPlayer().sendMessage(Text.translate("&aJoined queue for " +
                                matchType.getName() +
                                (ranked ? " (Ranked)" : " (Unranked)")));

                        cancelRefreshTask();
                        // Open queue info menu
                        new QueueInfoMenu(getPlayer(), queueManager, gamemodeManager, plugin).open();
                    } else {
                        getPlayer().sendMessage(Text.translate("&cFailed to join queue"));
                    }
                });
    }

    private void cancelRefreshTask() {
        if (refreshTask != null && !refreshTask.isCancelled()) {
            refreshTask.cancel();
            refreshTask = null;
        }
    }

    @Override
    public void onClose() {
        if (refreshTask != null) {
            refreshTask.cancel();
        }
        super.onClose();
    }
}