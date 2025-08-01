package me.whitehatd.aquila.queue.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.PaperCommandManager;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import gg.supervisor.core.annotation.Component;
import me.whitehatd.aquila.queue.menu.friends.FriendsMenu;
import org.bukkit.entity.Player;

@Component
public class FriendsCommand extends BaseCommand {

    public FriendsCommand(PaperCommandManager commandManager) {
        commandManager.registerCommand(this);
    }

    @Default
    @CommandAlias("friends|friend")
    @Description("Opens your friends menu")
    public void onFriends(Player player) {
        new FriendsMenu(player).open();
    }
}