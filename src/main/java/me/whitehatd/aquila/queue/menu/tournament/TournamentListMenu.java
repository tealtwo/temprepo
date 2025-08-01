package me.whitehatd.aquila.queue.menu.tournament;

import gg.supervisor.core.util.Services;
import gg.supervisor.menu.entities.InteractionModifier;
import gg.supervisor.menu.guis.Pager;
import gg.supervisor.menu.guis.builder.SchemaBuilder;
import gg.supervisor.menu.guis.impl.PersonalizedMenu;
import gg.supervisor.menu.builder.ItemBuilder;
import gg.supervisor.util.chat.Text;
import me.whitehatd.aquila.queue.QueuePlugin;
import me.whitehatd.aquila.queue.bridge.gamemode.GamemodeRepository;
import me.whitehatd.aquila.queue.bridge.tournament.Tournament;
import me.whitehatd.aquila.queue.bridge.tournament.TournamentState;
import me.whitehatd.aquila.queue.manager.tournament.TournamentManager;
import me.whitehatd.aquila.queue.manager.tournament.TournamentService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TournamentListMenu extends PersonalizedMenu {

    private BukkitTask refreshTask;
    private final TournamentManager tournamentManager;
    private final TournamentService tournamentService;


    public TournamentListMenu(Player player) {
        super(player, 6, Text.translate("&5Ongoing Tournaments"), InteractionModifier.VALUES);
        this.tournamentManager = Services.loadIfPresent(TournamentManager.class);
        this.tournamentService = Services.loadIfPresent(TournamentService.class);

        getDecorator().decorate(new SchemaBuilder()
                .add("x        ")
                .add(" RRRRRRR ")
                .add(" RRRRRRR ")
                .add(" RRRRRRR ")
                .add("  <   >  ")
                .add("    H    ")
                .build());
        populateMenu();
        startRefreshTask();
    }

    private void startRefreshTask() {
        refreshTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
                Services.loadIfPresent(QueuePlugin.class),
                () -> {
                    List<Tournament> tournaments = tournamentManager.getInMemoryTournaments().values().stream()
                            .filter(t -> t.getState() != TournamentState.COMPLETED
                                    && t.getState() != TournamentState.CANCELLED)
                            .toList();
                    Bukkit.getScheduler().runTask(
                            Services.loadIfPresent(QueuePlugin.class),
                            () -> updateMenu(tournaments)
                    );
                },
                5L,
                5L
        );
    }

    private void updateMenu(List<Tournament> tournaments) {
        Pager pager = new Pager(this, 'R').endless(Pager.EndlessType.SIMPLE);
        clearGui();
        if (tournaments.isEmpty()) {
            getDecorator().set('x', ItemBuilder.from(Material.BARRIER)
                    .name(Text.translate("&cNo ongoing tournaments"))
                    .menuItem());
        } else {
            GamemodeRepository gamemodeRepository = Services.loadIfPresent(GamemodeRepository.class);

            for (Tournament t : tournaments) {

                int needed = t.getNumberOfTeams() * t.getTeamSize();
                int joined = t.getPlayerIds().size();

                List<String> lore = new ArrayList<>();
                lore.add("&7State: &e" + t.getState().toString().toLowerCase());
                lore.add("&7Players: &e" + joined + "/" + needed);
                lore.add("&7Game mode: &e" + gamemodeRepository.find(t.getGamemodeId()).getName());

                lore.add("");

                switch (t.getState()) {
                    case WAITING -> {
                        lore.add("&eClick to join the tournament");
                    }
                    case COUNTDOWN -> {
                        lore.add("&6Counting down...");
                    }
                    case IN_PROGRESS -> {
                        lore.add("&aIn Progress");
                    }
                    default -> {
                        lore.add("&cTournament is not joinable");
                    }
                }

                if (t.getCreatorUuid().equals(player.getUniqueId().toString()) ||
                        player.hasPermission("aquila.tournaments.cancel.any")) {
                    lore.add("");
                    lore.add("&cRight-click&7 to cancel this tournament");
                }

                pager.add(ItemBuilder.from(Material.NETHER_STAR)
                        .name(Text.translate("&6Tournament"))
                        .lore(lore.stream().map(Text::translate).collect(Collectors.toList()))
                        .menuItem(e -> {
                            if (e.getClick() == ClickType.RIGHT) {
                                if (t.getCreatorUuid().equals(player.getUniqueId().toString()) ||
                                        player.hasPermission("aquila.tournaments.cancel.any")) {

                                    tournamentService.cancelTournament(player, t.getId());
                                    close(player);
                                } else {
                                    tournamentService.joinTournament(player, t.getId());
                                    close(player);

                                }
                            } else if (e.getClick() == ClickType.LEFT) {
                                tournamentService.joinTournament(player, t.getId());
                                close(player);
                            }

                        }));
            }
        }
        // Add navigation controls
        getDecorator().add('<', ItemBuilder.from(Material.ARROW)
                .name(Text.translate("&aPrevious Page"))
                .menuItem(e -> pager.previous()));
        getDecorator().add('>', ItemBuilder.from(Material.ARROW)
                .name(Text.translate("&aNext Page"))
                .menuItem(e -> pager.next()));
        // History button (slot 'H')
        getDecorator().add('H', ItemBuilder.from(Material.BOOK)
                .name(Text.translate("&eHistory"))
                        .lore(Stream.of(
                                "&7View all the tournaments",
                                "&7that ever took place")
                        .map(Text::translate).toList())
                .menuItem(e -> {
                    new TournamentHistoryMenu(player).open();
                }));


        getDecorator().set(' ', ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).menuItem());
        getDecorator().set('x', ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).menuItem());
    }

    private void populateMenu() {
        List<Tournament> tournaments = tournamentManager.getInMemoryTournaments().values().stream()
                .filter(t -> t.getState() != TournamentState.COMPLETED 
                        && t.getState() != TournamentState.CANCELLED)
                .collect(Collectors.toList());
        updateMenu(tournaments);
    }

    private void cancelRefreshTask() {
        if (refreshTask != null && !refreshTask.isCancelled()) {
            refreshTask.cancel();
        }
    }

    @Override
    public void onClose() {
        cancelRefreshTask();
        super.onClose();
    }
}
