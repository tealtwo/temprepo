package me.whitehatd.aquila.queue.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.PaperCommandManager;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Subcommand;
import gg.supervisor.core.annotation.Component;
import gg.supervisor.core.util.Services;
import gg.supervisor.util.chat.Text;
import me.whitehatd.aquila.queue.QueuePlugin;
import me.whitehatd.aquila.queue.manager.GamemodeManager;
import me.whitehatd.aquila.queue.manager.QueueManager;
import me.whitehatd.aquila.queue.menu.queue.GamemodeMenu;
import me.whitehatd.aquila.queue.menu.queue.QueueInfoMenu;
import org.bukkit.entity.Player;

@Component
@CommandAlias("queue")
public class QueueCommand extends BaseCommand {

    public QueueCommand(PaperCommandManager commandManager) {
        commandManager.registerCommand(this);
    }

    @Default
    public void onDefault(Player player) {
        QueueManager queueManager = Services.loadIfPresent(QueueManager.class);

        // Check if player is in queue first
        if (queueManager.isInQueue(player.getUniqueId())) {
            // Open queue info menu directly if player is in queue
            new QueueInfoMenu(player,
                    queueManager,
                    Services.loadIfPresent(GamemodeManager.class),
                    Services.loadIfPresent(QueuePlugin.class))
                    .open();
        } else {
            // Otherwise show gamemode selection menu
            new GamemodeMenu(player).open();
        }
    }

    @Subcommand("leave")
    public void onLeave(Player player) {
        QueueManager queueManager = Services.loadIfPresent(QueueManager.class);

        if (!queueManager.isInQueue(player.getUniqueId())) {
            player.sendMessage(Text.translate("&cYou are not in any queue."));
            return;
        }

        boolean removed = queueManager.removeFromQueue(player.getUniqueId());
        if (removed) {
            player.sendMessage(Text.translate("&aYou have left the queue."));
        } else {
            player.sendMessage(Text.translate("&cFailed to leave queue."));
        }
    }
}