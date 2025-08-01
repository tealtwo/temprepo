package me.whitehatd.aquila.queue.menu.panel.stats;

import gg.supervisor.core.loader.SupervisorLoader;
import gg.supervisor.core.util.Services;
import gg.supervisor.menu.entities.InteractionModifier;
import gg.supervisor.menu.guis.Pager;
import gg.supervisor.menu.guis.builder.SchemaBuilder;
import gg.supervisor.menu.guis.impl.PersonalizedMenu;
import gg.supervisor.menu.builder.ItemBuilder;
import gg.supervisor.util.chat.Text;
import gg.supervisor.util.prompt.ChatPromptService;
import me.whitehatd.aquila.queue.QueuePlugin;
import me.whitehatd.aquila.queue.bridge.match.Match;
import me.whitehatd.aquila.queue.bridge.match.MatchRepository;
import me.whitehatd.aquila.queue.bridge.match.MatchType;
import me.whitehatd.aquila.queue.menu.panel.stats.match.MatchPlayersMenu;
import me.whitehatd.aquila.queue.objects.report.Report;
import me.whitehatd.aquila.queue.objects.report.ReportRepository;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class MatchHistoryMenu extends PersonalizedMenu {

    private final Player player;
    private final MatchRepository matchRepository;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm");

    public MatchHistoryMenu(Player player) {
        super(player, 6, Text.translate("&5Match History"), InteractionModifier.VALUES);
        this.player = player;
        this.matchRepository = Services.loadIfPresent(MatchRepository.class);

        getDecorator().decorate(new SchemaBuilder()
                .add("         ")
                .add(" RRRRRRR ")
                .add(" RRRRRRR ")
                .add(" RRRRRRR ")
                .add("  < p >  ")
                .add("         ")
                .build());

        populateMenu();
    }

    private void populateMenu() {
        // Get all matches for this player
        List<Match> playerMatches = getPlayerMatches();

        // Set up pager
        Pager pager = new Pager(this, 'R').endless(Pager.EndlessType.SIMPLE);

        // Add matches to pager
        for (Match match : playerMatches) {
            String playerUuid = player.getUniqueId().toString();
            boolean isTeamA = match.getTeamA().contains(playerUuid);
            boolean isWinner = determineIfWinner(match, isTeamA);

            // Choose material based on match type
            Material material = getMaterialForMatchType(match.getMatchType());

            // Create match item
            pager.add(ItemBuilder.from(material)
                    .name((isWinner ? "&a" : "&c") + match.getMatchType().getName() + " Match")
                    .lore(generateMatchLore(match, playerUuid, isWinner))
                    .menuItem(event -> {
                        if (event.getClick() == ClickType.LEFT) {
                            new MatchPlayersMenu(player, match).open();
                        }

                        if (event.getClick() == ClickType.RIGHT) {

                            ChatPromptService promptService = Services.loadIfPresent(ChatPromptService.class);
                            player.sendMessage(Text.translate("&ePlease enter a reason for reporting this match: "));

                            promptService.create(player.getUniqueId(), 30, reason -> {
                                if (reason == null || reason.isEmpty()) {
                                    player.sendMessage(Text.translate("&cYou must provide a reason."));

                                    Bukkit.getScheduler().runTask(Services.loadIfPresent(QueuePlugin.class), () ->
                                            new MatchPlayersMenu(player, match).open());
                                    return;
                                }

                                ReportRepository reportRepo = Services.loadIfPresent(ReportRepository.class);
                                String reporter = player.getUniqueId().toString();

                                Report report = new Report();
                                report.setMatchId(match.getId());
                                report.setReporterId(reporter);
                                report.setReason(reason);

                                report.setMatchJson(SupervisorLoader.GSON.toJson(match));
                                report.setReportTime(System.currentTimeMillis());
                                reportRepo.save(match.getId() + "_" + reporter, report);

                                player.sendMessage(Text.translate("&aMatch reported."));

                                Bukkit.getScheduler().runTask(Services.loadIfPresent(QueuePlugin.class), () ->
                                        new MatchPlayersMenu(player, match).open());
                            });


                        }
                    }));
        }

        // Previous page button
        getDecorator().add('<', ItemBuilder.from(Material.ARROW)
                .name("&aPrevious Page")
                .menuItem(event -> pager.previous()));

        // Next page button
        getDecorator().add('>', ItemBuilder.from(Material.ARROW)
                .name("&aNext Page")
                .menuItem(event -> pager.next()));

        getDecorator().set(' ', ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).menuItem());

        // Back button
        getDecorator().add('p', ItemBuilder.from(Material.BARRIER)
                .name("&cBack")
                .menuItem(event -> {
                    close(player);
                    new StatisticsMenu(player).open();
                }));
    }

    private List<Match> getPlayerMatches() {
        if (matchRepository == null) return new ArrayList<>();

        Collection<Match> allMatches = matchRepository.values();
        String playerUuid = player.getUniqueId().toString();

        return allMatches.stream()
                .filter(match ->
                        match.getTeamA().contains(playerUuid) ||
                                match.getTeamB().contains(playerUuid))
                .sorted((m1, m2) -> Long.compare(m2.getEndTime(), m1.getEndTime())) // Most recent first
                .collect(Collectors.toList());
    }

    private boolean determineIfWinner(Match match, boolean isTeamA) {
        // Team A wins if all Team B players are dead/disconnected
        boolean teamBEliminated = match.getTeamB().stream()
                .allMatch(id -> match.getDeadPlayers().contains(id) || match.getDisconnectedPlayers().contains(id));

        // Team B wins if all Team A players are dead/disconnected
        boolean teamAEliminated = match.getTeamA().stream()
                .allMatch(id -> match.getDeadPlayers().contains(id) || match.getDisconnectedPlayers().contains(id));

        if (isTeamA) {
            return teamBEliminated && !teamAEliminated;
        } else {
            return teamAEliminated && !teamBEliminated;
        }
    }

    private Material getMaterialForMatchType(MatchType matchType) {
        return matchType.getMaterial();
    }

    private List<Component> generateMatchLore(Match match, String playerUuid, boolean isWinner) {
        List<String> lore = new ArrayList<>();

        // Format date
        String dateStr = dateFormat.format(new Date(match.getEndTime()));

        lore.add("&7Date: &e" + dateStr);
        lore.add("&7Type: &e" + match.getMatchType().getName());
        if(match.getTeamA().contains(playerUuid)) {
            lore.add("&7Team Distribution: &a" + match.getTeamA().size() + " &7against &c" + match.getTeamB().size());
        } else lore.add("&7Team Distribution: &a" + match.getTeamB().size() + " &7against &c" + match.getTeamA().size());
        lore.add("&7Ranked: &e" + (match.isRanked() ? "&aYes" : "&cNo"));

        // Add ELO change if ranked
        if (match.isRanked() && match.getPlayerStats().containsKey(playerUuid)) {
            int eloChange = match.getPlayerStats().get(playerUuid).getEloChange();
            String color = eloChange > 0 ? "&a+" : eloChange < 0 ? "&c" : "&e";
            lore.add("&7ELO Change: " + color + eloChange);
        }

        lore.add("&7Result: " + (isWinner ? "&aVictory" : "&cDefeat"));

        if (match.isInterrupted()) {
            lore.add("&cMatch was interrupted");
        }

        if(match.getRolledBackPlayers().contains(playerUuid)) {
            lore.add("&4Rolled back");
        }

        lore.add("");
        lore.add("&eLeft Click to view details");
        lore.add("&eRight Click to report");

        return lore.stream().map(Text::translate).toList();
    }
}