package me.whitehatd.aquila.queue.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.PaperCommandManager;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import gg.supervisor.core.annotation.Component;
import gg.supervisor.core.util.Services;
import me.whitehatd.aquila.queue.QueuePlugin;
import me.whitehatd.aquila.queue.menu.spectate.SpectateMenu;
import org.bukkit.entity.Player;

@Component
@CommandAlias("spec|spectate")
public class SpectateCommand extends BaseCommand {

    public SpectateCommand(PaperCommandManager manager) {
        manager.registerCommand(this);
    }

    @Default
    public void onDefault(Player player) {
        new SpectateMenu(player, Services.loadIfPresent(QueuePlugin.class)).open();
    }
}
