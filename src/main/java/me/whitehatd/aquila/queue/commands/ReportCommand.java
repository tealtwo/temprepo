package me.whitehatd.aquila.queue.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.PaperCommandManager;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import gg.supervisor.core.annotation.Component;
import gg.supervisor.core.util.Services;
import gg.supervisor.util.chat.Text;
import me.whitehatd.aquila.queue.menu.report.ReportMenu;
import org.bukkit.entity.Player;

@Component
@CommandAlias("reports")
@CommandPermission("aquila.reports")
public class ReportCommand extends BaseCommand {

    public ReportCommand(PaperCommandManager commandManager) {
        commandManager.registerCommand(this);
    }

    @Default
    public void onDefault(Player player) {
        Services.loadIfPresent(ReportMenu.class).open(player);
    }
}
