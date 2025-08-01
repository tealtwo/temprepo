package me.whitehatd.aquila.queue.menu.panel.stats.match;

import gg.supervisor.core.util.Services;
import gg.supervisor.menu.entities.InteractionModifier;
import gg.supervisor.menu.guis.Pager;
import gg.supervisor.menu.guis.builder.SchemaBuilder;
import gg.supervisor.menu.guis.impl.PersonalizedMenu;
import gg.supervisor.menu.builder.ItemBuilder;
import gg.supervisor.util.chat.Text;
import me.whitehatd.aquila.queue.bridge.match.Match;
import me.whitehatd.aquila.queue.bridge.match.PlayerMatchStats;
import me.whitehatd.aquila.queue.menu.panel.stats.MatchHistoryMenu;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MatchPlayersMenu extends PersonalizedMenu {

    private final Player player;
    private final Match match;
    private final String playerUuid;

    public MatchPlayersMenu(Player player, Match match) {
        super(player, 5, Text.translate("&5Match Players"), InteractionModifier.VALUES);
        this.player = player;
        this.match = match;
        this.playerUuid = player.getUniqueId().toString();
        
        getDecorator().decorate(new SchemaBuilder()
                .add("RRRRRRRRR")
                .add("RRRRRRRRR")
                .add("RRRRRRRRR")
                .add("         ")
                .add("   a d  p")
                .build());

        populatePlayers();
    }

    private void populatePlayers() {
        List<String> allPlayers = new ArrayList<>();
        boolean isPlayerInTeamA = match.getTeamA().contains(playerUuid);
        
        // Add current player first
        allPlayers.add(playerUuid);
        
        // Add teammates next
        List<String> teammates = isPlayerInTeamA ? match.getTeamA() : match.getTeamB();
        for (String teammate : teammates) {
            if (!teammate.equals(playerUuid)) {
                allPlayers.add(teammate);
            }
        }
        
        // Add opponents last
        List<String> opponents = isPlayerInTeamA ? match.getTeamB() : match.getTeamA();
        allPlayers.addAll(opponents);
        
        Pager pager = new Pager(this, 'R').endless(Pager.EndlessType.SIMPLE);

        for (String uuid : allPlayers) {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(uuid));
            PlayerMatchStats stats = match.getPlayerStats().get(uuid);
            
            if (stats == null) continue;
            
            boolean isTeamA = match.getTeamA().contains(uuid);
            boolean userIsTeamA = match.getTeamA().contains(playerUuid);

            String whichTeam = isTeamA ? (userIsTeamA ? "&aYour team" : "&cOpponents") : (userIsTeamA ? "&cOpponents" : "&aYour team");

            boolean isViewer = uuid.equals(playerUuid);
            
            Material icon = isViewer ? Material.PLAYER_HEAD : Material.SKELETON_SKULL;
            String teamColor = isTeamA ? "&a" : "&c";
            String namePrefix = isViewer ? "&e" : teamColor;
            
            List<String> lore = new ArrayList<>();
            lore.add("&7Team: " + whichTeam);
            
            if (stats.getEloChange() != 0) {
                String eloPrefix = stats.getEloChange() > 0 ? "&a+" : "&c";
                lore.add("&7ELO Change: " + eloPrefix + stats.getEloChange());
            }
            
            if (match.getDeadPlayers().contains(uuid)) {
                lore.add("");
                lore.add("&cDied during match");
            } else if (match.getDisconnectedPlayers().contains(uuid)) {
                lore.add("");
                lore.add("&cDisconnected during match");
            }
            
            lore.add("");
            lore.add("&eClick to view detailed stats");
            
            pager.add(ItemBuilder.from(icon)
                    .name(namePrefix + offlinePlayer.getName())
                    .lore(lore.stream().map(Text::translate).toList())
                    .menuItem(event -> new PlayerMatchStatsMenu(player, match, UUID.fromString(uuid)).open()));
        }

                
        // Previous page button
        getDecorator().add('a', ItemBuilder.from(Material.ARROW)
                .name("&aPrevious Page")
                .menuItem(event -> pager.previous()));

        // Next page button
        getDecorator().add('d', ItemBuilder.from(Material.ARROW)
                .name("&aNext Page")
                .menuItem(event -> pager.next()));

        // Back button
        getDecorator().add('p', ItemBuilder.from(Material.BARRIER)
                .name("&cBack")
                .menuItem(event -> {
                    close(player);
                    new MatchHistoryMenu(player).open();
                }));
    }
}