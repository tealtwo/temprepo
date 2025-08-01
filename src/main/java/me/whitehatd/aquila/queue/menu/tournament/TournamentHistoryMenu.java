package me.whitehatd.aquila.queue.menu.tournament;

import gg.supervisor.core.loader.SupervisorLoader;
import gg.supervisor.core.util.Services;
import gg.supervisor.menu.entities.InteractionModifier;
import gg.supervisor.menu.guis.Pager;
import gg.supervisor.menu.guis.builder.SchemaBuilder;
import gg.supervisor.menu.guis.impl.PersonalizedMenu;
import gg.supervisor.menu.builder.ItemBuilder;
import gg.supervisor.util.chat.Text;
import me.whitehatd.aquila.queue.bridge.gamemode.GamemodeRepository;
import me.whitehatd.aquila.queue.bridge.tournament.Tournament;
import me.whitehatd.aquila.queue.bridge.tournament.TournamentRepository;
import me.whitehatd.aquila.queue.bridge.tournament.TournamentRound;
import me.whitehatd.aquila.queue.bridge.tournament.TournamentState;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class TournamentHistoryMenu extends PersonalizedMenu {

    private final Player player;
    private final TournamentRepository tournamentRepository;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm");

    public TournamentHistoryMenu(Player player) {
        super(player, 6, Text.translate("&5Tournament History"),
                InteractionModifier.VALUES);
        this.player = player;
        this.tournamentRepository = Services.loadIfPresent(TournamentRepository.class);

        getDecorator().decorate(new SchemaBuilder()
                .add("x        ")
                .add(" RRRRRRR ")
                .add(" RRRRRRR ")
                .add(" RRRRRRR ")
                .add("  < B >  ")
                .add("         ")
                .build());

        populateMenu();
    }

    private void populateMenu() {
        Collection<Tournament> allTournaments = tournamentRepository != null ? tournamentRepository.values() : new ArrayList<>();
        // Filter for completed tournaments only
        List<Tournament> history = allTournaments.stream()
                .filter(t -> t.getState() == TournamentState.COMPLETED)
                .sorted(Comparator.comparingLong(Tournament::getEndTime).reversed())
                .toList();

        Pager pager = new Pager(this, 'R').endless(Pager.EndlessType.SIMPLE);
        if (history.isEmpty()) {
            getDecorator().set('x', ItemBuilder.from(Material.BARRIER)
                    .name(Text.translate("&cNo tournaments have been played."))
                    .menuItem());
        } else {
            GamemodeRepository gamemodeRepository = Services.loadIfPresent(GamemodeRepository.class);

            for (Tournament t : history) {
                // Determine the winner from the final round
                String winner = getWinnerString(t);
                String gamemodeName = gamemodeRepository.find(t.getGamemodeId()).getName();
                String ended = dateFormat.format(new Date(t.getEndTime()));
                List<String> lore = new ArrayList<>();
                lore.add("&7Game mode: &e" + gamemodeName);
                lore.add("&7Winner: &e" + winner);
                lore.add("&7Ended: &e" + ended);

                pager.add(ItemBuilder.from(Material.NETHER_STAR)
                        .name(Text.translate("&6Tournament: " + t.getId()))
                        .lore(lore.stream().map(Text::translate).collect(Collectors.toList()))
                        .menuItem(e -> {
                            player.sendMessage(Text.translate("&aTournament " + t.getId() + " details:"));
                            // Optionally open a detailed view menu here.
                        }));
            }
        }
        // Previous/Next page buttons
        getDecorator().add('<', ItemBuilder.from(Material.ARROW)
                .name(Text.translate("&aPrevious Page"))
                .menuItem(e -> pager.previous()));
        getDecorator().add('>', ItemBuilder.from(Material.ARROW)
                .name(Text.translate("&aNext Page"))
                .menuItem(e -> pager.next()));
        // Back button on slot 'B'
        getDecorator().add('B', ItemBuilder.from(Material.BARRIER)
                .name(Text.translate("&cBack"))
                .menuItem(e -> {
                    close(player);
                    new TournamentListMenu(player).open();
                }));
        getDecorator().set(' ', ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).menuItem());
        getDecorator().set('x', ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).menuItem());
        redraw();
    }

    /**
     * Helper method to determine the winning team from the tournament's final round.
     * This example assumes a single match in the final round.
     */
    private String getWinnerString(Tournament t) {
        if (t.getRounds() == null || t.getRounds().isEmpty()) {
            return "N/A";
        }
        TournamentRound lastRound = t.getRounds().getLast();
        if (lastRound.getMatches() == null || lastRound.getMatches().isEmpty()) {
            return "N/A";
        }
        // single match (the final)
        var finalMatch = lastRound.getMatches().getFirst();
        int winner = finalMatch.getWinnerTeamIndex();
        List<String> winnerTeam;
        if (winner == 0) {
            winnerTeam = finalMatch.getTeamA();
        } else if (winner == 1) {
            winnerTeam = finalMatch.getTeamB();
        } else {
            return "Undecided";
        }
        // Convert player UUID strings to names
        return winnerTeam.stream()
                .map(uuid -> Bukkit.getOfflinePlayer(java.util.UUID.fromString(uuid)).getName())
                .collect(Collectors.joining(", "));
    }
}
