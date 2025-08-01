package me.whitehatd.aquila.queue.menu.queue;

import gg.supervisor.menu.entities.InteractionModifier;
import gg.supervisor.menu.guis.builder.SchemaBuilder;
import gg.supervisor.menu.guis.impl.PersonalizedMenu;
import gg.supervisor.menu.builder.ItemBuilder;
import gg.supervisor.util.chat.Text;
import me.whitehatd.aquila.queue.QueuePlugin;
import me.whitehatd.aquila.queue.bridge.gamemode.Gamemode;
import me.whitehatd.aquila.queue.manager.GamemodeManager;
import me.whitehatd.aquila.queue.manager.QueueManager;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class QueueInfoMenu extends PersonalizedMenu {

    private final QueueManager queueManager;
    private final GamemodeManager gamemodeManager;
    private final QueuePlugin plugin;
    private BukkitTask updateTask;

    public QueueInfoMenu(Player player, QueueManager queueManager, GamemodeManager gamemodeManager, QueuePlugin plugin) {
        super(player, 5, Text.translate("&5Queue Information"), InteractionModifier.VALUES);
        this.queueManager = queueManager;
        this.gamemodeManager = gamemodeManager;
        this.plugin = plugin;

        getDecorator().decorate(new SchemaBuilder()
                .add("         ")
                .add("    a    ")
                .add("         ")
                .add("    b    ")
                .add("         ")
                .build());

        redraw();
        startUpdateTask();
    }

    private void startUpdateTask() {
        updateTask = plugin.getServer().getScheduler().runTaskTimer(
            plugin,
            this::redraw,
            5L, 5L  // Update every 5 ticks (0.25 seconds)
        );
    }

    @Override
    public void redraw() {
        clearGui();

        UUID playerId = getPlayer().getUniqueId();

        getDecorator().set(' ', ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).menuItem());

        if (!queueManager.isInQueue(playerId)) {
            // Player is not in queue anymore
            getDecorator().set('a', ItemBuilder.from(Material.BARRIER)
                .name("&cNot in Queue")
                .lore(List.of(Text.translate("&7You are not currently in a queue")))
                .build());
            close(getPlayer());
            return;
        }

        QueueManager.QueueKey queueKey = queueManager.getPlayerQueue(playerId);
        Gamemode gamemode = gamemodeManager.getGamemode(queueKey.getGamemodeId());

        // Format time in queue
        Long joinTime = queueManager.getPlayerJoinTime(playerId);

        long timeInQueue = System.currentTimeMillis() - joinTime;
        int seconds = (int) (timeInQueue / 1000) % 60;
        int minutes = (int) (timeInQueue / 60000);

        // Queue info
        List<String> lore = new ArrayList<>();
        lore.add("&7Queue Type: &e" + (queueKey.isRanked() ? "Ranked" : "Unranked"));
        lore.add("&7Match Type: &e" + queueKey.getMatchType().getName());
        lore.add("&7Gamemode: &e" + (gamemode != null ? gamemode.getName() : "Unknown"));
        lore.add("");
        lore.add("&7Time in Queue: &e" + minutes + "m " + seconds + "s");
        lore.add("&7Queue Size: &e" + queueManager.getQueueSize(queueKey.getMatchType(), queueKey.isRanked(), queueKey.getGamemodeId()));

        if (queueKey.isRanked()) {
            int playerElo = queueManager.getPlayerElo(playerId);
            int eloRange = queueManager.getCurrentEloRange(playerId);
            lore.add("");
            lore.add("&7Your Rating: &e" + playerElo);
            lore.add("&7Current Range: &e" + playerElo + " ±" + eloRange);
            lore.add("&7Matching: &e" + (playerElo - eloRange) + " - " + (playerElo + eloRange));
            lore.add("");
            lore.add("&7The matching range will");
            lore.add("&7increase the longer you wait");
        }

        getDecorator().set('a', ItemBuilder.from(Material.COMPASS)
            .name("&6Searching for Match")
            .lore(lore.stream().map(Text::translate).toList())
            .build());

        // Leave queue button
        getDecorator().set('b', ItemBuilder.from(Material.BARRIER)
                .name("&cLeave Queue")
                .lore(List.of(Text.translate("&7Click to leave the current queue")))
                .menuItem(event -> {
                    boolean removed = queueManager.removeFromQueue(playerId);
                    if (removed) {
                        // Player left queue, open GamemodeMenu
                        close(getPlayer());
                        new GamemodeMenu(getPlayer()).open();
                    } else {
                        getPlayer().sendMessage(Text.translate("&cFailed to leave queue."));
                    }
                }));
    }

    @Override
    public void onClose() {
        if (updateTask != null) {
            updateTask.cancel();
        }
        super.onClose();
    }
}