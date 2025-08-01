package me.whitehatd.aquila.queue.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.PaperCommandManager;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import gg.supervisor.core.annotation.Component;
import me.whitehatd.aquila.queue.menu.panel.PanelMenu;
import org.bukkit.entity.Player;

@Component
@CommandAlias("panel")
public class PanelCommand extends BaseCommand {

    public PanelCommand(PaperCommandManager commandManager) {
        commandManager.registerCommand(this);
    }

    @Default
    public void openPanel(Player player) {
        new PanelMenu(player).open();
    }
}