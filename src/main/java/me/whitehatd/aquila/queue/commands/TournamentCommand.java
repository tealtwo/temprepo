package me.whitehatd.aquila.queue.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.PaperCommandManager;
import co.aikar.commands.annotation.*;
import gg.supervisor.core.annotation.Component;
import gg.supervisor.core.util.Services;
import gg.supervisor.util.chat.Text;
import me.whitehatd.aquila.queue.bridge.match.MatchType;
import me.whitehatd.aquila.queue.bridge.tournament.Tournament;
import me.whitehatd.aquila.queue.manager.tournament.TournamentManager;
import me.whitehatd.aquila.queue.bridge.tournament.TournamentState;
import me.whitehatd.aquila.queue.manager.tournament.TournamentService;
import me.whitehatd.aquila.queue.menu.tournament.TournamentListMenu;
import me.whitehatd.aquila.queue.menu.tournament.admin.TournamentGamemodeMenu;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@Component
@CommandAlias("tournament|tourneys")
public class TournamentCommand extends BaseCommand {

    public TournamentCommand(PaperCommandManager manager) {
        manager.registerCommand(this);
    }

    @Subcommand("help")
    @Description("Shows tournament help.")
    @CommandPermission("aquila.tournament.help")
    public void onHelp(CommandSender sender) {
        sender.sendMessage(Text.translate("&6Tournament Commands:"));
        sender.sendMessage(Text.translate("&e/tournament create <teams> <matchType>"));
        sender.sendMessage(Text.translate("&e/tournament list"));
        sender.sendMessage(Text.translate("&e/tournament join <id>"));
        sender.sendMessage(Text.translate("&e/tournament cancel <id>"));
    }

    @Default
    public void onDefault(Player player) {
        new TournamentListMenu(player).open();
    }

    @Subcommand("create")
    @CommandPermission("aquila.tournament.create")
    @CommandCompletion("@numbers @modes")
    @Description("Create a new tournament.")
    public void onCreate(Player player, int numberOfTeams, String matchTypeStr) {
        MatchType matchType;
        try {
            matchType = MatchType.valueOf(matchTypeStr);
        } catch (Exception e) {
            player.sendMessage(Text.translate("&cUnknown match type: " + matchTypeStr));
            return;
        }

        if (numberOfTeams <= 0 || numberOfTeams % 2 != 0) {
            player.sendMessage(Text.translate("&cNumber of teams must be a positive even number."));
            return;
        }

        new TournamentGamemodeMenu(player, numberOfTeams, matchType).open();
    }

    @Subcommand("leave")
    public void onLeave(Player p) {
        TournamentService service = Services.loadIfPresent(TournamentService.class);
        service.leaveTournament(p);
    }

    @Subcommand("join")
    public void onJoin(Player p, String id) {
        TournamentService service = Services.loadIfPresent(TournamentService.class);
        service.joinTournament(p, id);
    }

    @Subcommand("cancel")
    public void onCancel(Player p, String id) {
        TournamentService service = Services.loadIfPresent(TournamentService.class);
        service.cancelTournament(p, id);
    }

}
