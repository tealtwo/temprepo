package me.whitehatd.aquila.queue.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.PaperCommandManager;
import co.aikar.commands.annotation.*;
import gg.supervisor.core.annotation.Component;
import gg.supervisor.core.util.Services;
import gg.supervisor.util.chat.Text;
import me.whitehatd.aquila.queue.bridge.match.Match;
import me.whitehatd.aquila.queue.bridge.match.MatchRepository;
import me.whitehatd.aquila.queue.bridge.match.PlayerMatchStats;
import me.whitehatd.aquila.queue.bridge.player.PlayerData;
import me.whitehatd.aquila.queue.bridge.player.PlayerRepository;
import me.whitehatd.aquila.queue.redis.RedisPublisher;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@CommandAlias("rollbackstats")
@CommandPermission("aquila.rollback")
@Component
public class RollbackStatsCommand extends BaseCommand {

    private final PlayerRepository playerRepository;
    private final MatchRepository matchRepository;

    public RollbackStatsCommand(PlayerRepository playerRepository,
                                MatchRepository matchRepository, PaperCommandManager manager) {
        this.playerRepository = playerRepository;
        this.matchRepository = matchRepository;
        manager.registerCommand(this);
    }

    @Default
    @Syntax("<player> <time>")
    @CommandCompletion("@players [duration]")
    @Description("Rollback stats for a player for matches that ended in the last [time]. E.g. 30m, 2h, 1d, etc.")
    public void onRollbackStats(Player sender, String targetPlayerName, String timeArg) {
        // Attempt to find that player
        Player target = Bukkit.getPlayerExact(targetPlayerName);
        if (target == null) {
            sender.sendMessage(Text.translate("&cCannot find online player named " + targetPlayerName));
            return;
        }

        // parse timeArg into milliseconds offset
        long offsetMillis = parseTimeArg(timeArg);
        if (offsetMillis <= 0) {
            sender.sendMessage(Text.translate("&cInvalid time argument: " + timeArg));
            sender.sendMessage(Text.translate("&cValid formats examples: 30m, 2h, 1d"));
            return;
        }

        String playerId = target.getUniqueId().toString();
        long earliestEndTime = System.currentTimeMillis() - offsetMillis;

        sender.sendMessage(Text.translate("&eStarting rollback for &f" + target.getName() +
                "&e for matches ended within the last &f" + timeArg + "&e..."));

        // Run the rollback logic asynchronously
        CompletableFuture.supplyAsync(() -> doStatsRollback(playerId, earliestEndTime),
                        Services.loadIfPresent(RedisPublisher.class).getExecutorService())
                .thenAccept(amount -> {
                    sender.sendMessage(Text.translate("&aRollback complete. " +
                            "Rolled back &f" + amount + " &a matches for " + target.getName()));
                });
    }

    /**
     * Actually do the rollback logic:
     *  1) load all matches from matchRepository
     *  2) filter matches that ended after earliestEndTime
     *  3) check if the player is in the match and hasn't been rolled back
     *  4) revert stats from the match
     *  5) mark the match as rolled back for that player
     *  6) persist changes
     *
     * @return how many matches we rolled back for that player
     */
    private int doStatsRollback(String playerId, long earliestEndTime) {
        int rollbackCount = 0;

        Collection<Match> allMatches = matchRepository.values();
        for (Match match : allMatches) {
            // only handle matches that ended in the last timeframe
            if (match.getEndTime() < earliestEndTime) {
                continue;
            }
            // check if this player participated
            boolean participated = match.getTeamA().contains(playerId) || match.getTeamB().contains(playerId);
            if (!participated) continue;

            // check if we already rolled back this player
            if (match.getRolledBackPlayers().contains(playerId)) {
                continue;
            }

            // let's revert
            PlayerMatchStats stats = match.getPlayerStats().get(playerId);
            if (stats == null) continue; // no stats => skip

            // revert from PlayerData
            PlayerData data = playerRepository.find(playerId);
            if (data != null) {
                revertStatsFromMatch(data, stats);
                // save
                playerRepository.save(playerId, data);
            }

            // add them to the rolledBackPlayers
            match.getRolledBackPlayers().add(playerId);
            matchRepository.save(match.getId(), match);

            rollbackCount++;
        }
        return rollbackCount;
    }

    /**
     * Revert the stats from the match. This is the inverse of your updatePlayerData(...) logic in MatchManager.
     */
    private void revertStatsFromMatch(PlayerData data, PlayerMatchStats stats) {
        // If was ranked, revert ELO
        int eloChange = stats.getEloChange();
        data.setElo(data.getElo() - eloChange);

        // If eloChange > 0 => revert from totalEloGained; if < 0 => revert from totalEloLost
        if (eloChange > 0) {
            data.setTotalEloGained(data.getTotalEloGained() - eloChange);
        } else if (eloChange < 0) {
            data.setTotalEloLost(data.getTotalEloLost() - Math.abs(eloChange));
        }

        // Revert matches
        data.setMatchesPlayed(data.getMatchesPlayed() - 1);

        if (stats.isWinner()) {
            data.setMatchesWon(data.getMatchesWon() - 1);
        } else {
            data.setMatchesLost(data.getMatchesLost() - 1);
        }
        data.setCurrentWinStreak(0);
        data.setCurrentLossStreak(0);

        // Revert all cumulative statistics
        data.setTotalHealthRegenerated(data.getTotalHealthRegenerated() - stats.getHealthRegenerated());
        data.setTotalDamageDealt(data.getTotalDamageDealt() - stats.getDamageDealt());
        data.setTotalDamageTaken(data.getTotalDamageTaken() - stats.getDamageTaken());
        data.setTotalHitsLanded(data.getTotalHitsLanded() - stats.getHitsLanded());
        data.setTotalHitsReceived(data.getTotalHitsReceived() - stats.getHitsReceived());
        data.setTotalCriticalHits(data.getTotalCriticalHits() - stats.getCriticalHits());

        data.setTotalArrowsShot(data.getTotalArrowsShot() - stats.getArrowsShot());
        data.setTotalArrowsHit(data.getTotalArrowsHit() - stats.getArrowsHit());
        data.setTotalBlocksTraversed(data.getTotalBlocksTraversed() - stats.getBlocksTraversed());
        data.setTotalJumps(data.getTotalJumps() - stats.getJumps());
        data.setTotalSprintDistance(data.getTotalSprintDistance() - stats.getSprintDistance());
        data.setTotalPotionsUsed(data.getTotalPotionsUsed() - stats.getPotionsUsed());
        data.setTotalFoodConsumed(data.getTotalFoodConsumed() - stats.getFoodConsumed());
        data.setTotalBlocksPlaced(data.getTotalBlocksPlaced() - stats.getBlocksPlaced());
        data.setTotalBlocksBroken(data.getTotalBlocksBroken() - stats.getBlocksBroken());
        data.setTotalItemsPickedUp(data.getTotalItemsPickedUp() - stats.getItemsPickedUp());
        data.setTotalItemsDropped(data.getTotalItemsDropped() - stats.getItemsDropped());
    }

    /**
     * Parse a time argument like "30m", "2h", "1d" into milliseconds.
     * Returns -1 if invalid.
     */
    private long parseTimeArg(String arg) {
        // Examples: 30m => 30 * 60 * 1000
        // 2h => 2 * 60 * 60 * 1000
        // 1d => 1 * 24 * 60 * 60 * 1000
        Pattern pattern = Pattern.compile("(\\d+)([dhm])", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(arg);
        if (!matcher.matches()) {
            return -1;
        }
        long value = Long.parseLong(matcher.group(1));
        String unit = matcher.group(2).toLowerCase();

        return switch (unit) {
            case "d" -> value * 24L * 60L * 60L * 1000L;
            case "h" -> value * 60L * 60L * 1000L;
            case "m" -> value * 60L * 1000L;
            default -> -1;
        };
    }
}
