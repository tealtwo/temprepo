package me.whitehatd.aquila.queue.util;

import gg.supervisor.core.annotation.Component;
import gg.supervisor.core.loader.SupervisorLoader;
import gg.supervisor.core.util.Services;
import gg.supervisor.util.chat.Text;
import me.whitehatd.aquila.queue.bridge.match.Match;
import me.whitehatd.aquila.queue.menu.spectate.SpectateEntry;
import me.whitehatd.aquila.queue.redis.RedisPublisher;
import me.whitehatd.aquila.queue.redis.SpectateDTO;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import redis.clients.jedis.Jedis;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class SpectateUtil {

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");



    /**
     * Reads the Redis hashes "activeMatches" (matchId → serialized Match)
     * and "playerMatchMap" (player UUID → matchId) (though here we only use activeMatches)
     * and builds a list of SpectateEntry objects for every active match.
     * For each match, all players (from both teams) are included in the entry.
     */
    public List<SpectateEntry> buildSpectateEntries() {
        List<SpectateEntry> entries = new ArrayList<>();
        RedisPublisher publisher = Services.loadIfPresent(RedisPublisher.class);
        try (Jedis jedis = publisher.getJedisPool().getResource()) {
            Map<String, String> activeMatches = jedis.hgetAll("activeMatches");
            Map<String, String> playerMatchMap = jedis.hgetAll("playerMatchMap");

            for (String playerId : playerMatchMap.keySet()) {
                String matchJson = activeMatches.get(playerMatchMap.get(playerId));

                if (matchJson == null || matchJson.isEmpty()) continue;
                // Deserialize into the proper Match object.
                Match match = SupervisorLoader.GSON.fromJson(matchJson, Match.class);
                if (match == null) continue;

                String displayName = match.getMatchType().getName();

                boolean isTeamA = match.getTeamA().contains(playerId);

                // Build lore based on match details.
                List<String> lore = new ArrayList<>();
                lore.add("&7Ranked: " + (match.isRanked() ? "&aYes" : "&cNo"));
                lore.add("&7Started: " + dateFormat.format(match.getStartTime()));
                lore.add("");

                if(isTeamA) {
                    lore.add("&aOwn Team:");
                    lore.addAll(match.getTeamA().stream().map(player -> "  &7- " +
                            Bukkit.getOfflinePlayer(UUID.fromString(player)).getName()).toList());
                    lore.add("");
                    lore.add("&cOpponents");
                    lore.addAll(match.getTeamB().stream().map(player -> "  &7- " +
                            Bukkit.getOfflinePlayer(UUID.fromString(player)).getName()).toList());
                } else {
                    lore.add("&aOwn Team:");
                    lore.addAll(match.getTeamB().stream().map(player -> "  &7- " +
                            Bukkit.getOfflinePlayer(UUID.fromString(player)).getName()).toList());
                    lore.add("");
                    lore.add("&cOpponents");
                    lore.addAll(match.getTeamA().stream().map(player -> "  &7- " +
                            Bukkit.getOfflinePlayer(UUID.fromString(player)).getName()).toList());
                }
                entries.add(
                        new SpectateEntry(
                                UUID.fromString(playerId),
                                Bukkit.getOfflinePlayer(UUID.fromString(playerId)).getName(),
                                playerMatchMap.get(playerId),
                                lore));
            }
        } catch (Exception e) {
            Bukkit.getLogger().severe("Error building spectate entries: " + e.getMessage());
            e.printStackTrace();
        }
        return entries;
    }

    /**
     * Publishes a spectate request for a spectator.
     * The request uses the existing RedisQueuePublisher.
     */
    public void sendSpectateRequest(Player spectator, String matchId, boolean hidden) {
        RedisPublisher publisher = Services.loadIfPresent(RedisPublisher.class);
        if (publisher != null) {
            SpectateDTO dto = new SpectateDTO();
            dto.setSpectatorUUID(spectator.getUniqueId().toString());
            dto.setMatchId(matchId);
            dto.setHidden(hidden);
            publisher.publishSpectateRequest(dto);
        } else {
            spectator.sendMessage(Text.translate("&cSpectate service unavailable."));
        }
    }
}
