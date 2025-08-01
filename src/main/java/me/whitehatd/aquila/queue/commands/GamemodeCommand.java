package me.whitehatd.aquila.queue.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.PaperCommandManager;
import co.aikar.commands.annotation.*;
import gg.supervisor.core.annotation.Component;
import gg.supervisor.util.chat.Text;
import me.whitehatd.aquila.queue.manager.GamemodeManager;
import me.whitehatd.aquila.queue.menu.gamemode.GamemodeAdminMenu;
import org.bukkit.entity.Player;

@Component
@CommandAlias("gamemode|gm")
@CommandPermission("aquila.admin")
public class GamemodeCommand extends BaseCommand {

    private final GamemodeManager gamemodeManager;

    public GamemodeCommand(GamemodeManager gamemodeManager, PaperCommandManager paperCommandManager) {
        paperCommandManager.registerCommand(this);
        this.gamemodeManager = gamemodeManager;
    }

    @Default
    @CommandPermission("aquila.admin")
    @Description("Open the gamemode management menu")
    public void onDefault(Player player) {
        new GamemodeAdminMenu(player).open();
    }

    @Subcommand("create")
    @CommandPermission("aquila.admin")
    @Description("Create a new gamemode")
    public void create(Player player, String name) {
        boolean created = gamemodeManager.createGamemode(player, name);
        
        if (created) {
            player.sendMessage(Text.translate("&aGamemode '" + name + "' created successfully!"));
        } else {
            player.sendMessage(Text.translate("&cFailed to create gamemode '" + name + "'."));
        }
    }
}