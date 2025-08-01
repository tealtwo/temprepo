package me.whitehatd.aquila.queue.menu.tournament.admin;

import gg.supervisor.core.util.Services;
import gg.supervisor.menu.builder.ItemBuilder;
import gg.supervisor.menu.entities.InteractionModifier;
import gg.supervisor.menu.guis.builder.SchemaBuilder;
import gg.supervisor.menu.guis.impl.PersonalizedMenu;
import gg.supervisor.util.chat.Text;
import me.whitehatd.aquila.queue.bridge.gamemode.Gamemode;
import me.whitehatd.aquila.queue.bridge.match.MatchType;
import me.whitehatd.aquila.queue.bridge.tournament.Tournament;
import me.whitehatd.aquila.queue.manager.tournament.TournamentManager;
import me.whitehatd.aquila.queue.bridge.tournament.TournamentState;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class TournamentConfirmationMenu extends PersonalizedMenu {

    private final Player player;
    private final Gamemode gamemode;
    private final MatchType matchType;
    private final int numberOfTeams;

    public TournamentConfirmationMenu(Player player, Gamemode gamemode, MatchType matchType, int numberOfTeams) {
        super(player, 3, Text.translate("&6Confirm Tournament"), InteractionModifier.VALUES);
        this.player = player;
        this.gamemode = gamemode;
        this.matchType = matchType;
        this.numberOfTeams = numberOfTeams;

        setupMenu();
    }

    private void setupMenu() {
        getDecorator().decorate(new SchemaBuilder()
                .add("   I   ")
                .add("  <C>  ")
                .add("       ")
                .build());

        // Info item
        getDecorator().add('I', ItemBuilder.from(Material.PAPER)
                .name(Text.translate("&eTournament Info"))
                .lore(List.of(
                        Text.translate("&7Gamemode: &a" + gamemode.getName()),
                        Text.translate("&7MatchType: &a" + matchType.name()),
                        Text.translate("&7Team Size: &a" + matchType.getSize()),
                        Text.translate("&7Number of Teams: &a" + numberOfTeams),
                        Text.translate("&7Total Players Needed: &a" + (numberOfTeams * matchType.getSize()))
                ))
                .menuItem()
        );

        // Confirm
        getDecorator().add('C', ItemBuilder.from(Material.EMERALD_BLOCK)
                .name(Text.translate("&aConfirm"))
                .lore(List.of(Text.translate("&7Click to create this tournament")))
                .menuItem(e -> {
                    createTournament();
                    close(player);
                })
        );

        // Cancel
        getDecorator().add('<', ItemBuilder.from(Material.BARRIER)
                .name(Text.translate("&cCancel"))
                .lore(List.of(Text.translate("&7Click to cancel")))
                .menuItem(e -> {
                    close(player);
                    player.sendMessage(Text.translate("&cTournament creation cancelled."));
                })
        );
    }

    private void createTournament() {
        TournamentManager tm = Services.loadIfPresent(TournamentManager.class);

        Tournament t = new Tournament();
        t.setId(UUID.randomUUID().toString());
        t.setGamemodeId(gamemode.getId());
        t.setCreatorUuid(player.getUniqueId().toString());
        t.setNumberOfTeams(numberOfTeams);
        t.setTeamSize(matchType.getSize());
        t.setState(TournamentState.WAITING);
        t.setCreatedAt(System.currentTimeMillis());

        tm.saveTournament(t);
        player.sendMessage(Text.translate("&aTournament created with ID &e" + t.getId()));
    }
}
