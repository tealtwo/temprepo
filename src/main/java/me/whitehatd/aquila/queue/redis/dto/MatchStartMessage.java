package me.whitehatd.aquila.queue.redis.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class MatchStartMessage {
    private List<String> teamA;
    private List<String> teamB;
    private boolean ranked;
    private String matchType;
    private String tournamentId;
    private String arenaName;
    private String gamemodeId;
    private long matchScheduledTime;
    private Map<String, Map<Integer, String>> loadouts;
    private Map<String, Object> extraSettings;

    // Used in tournaments
    private boolean teleportPlayers = true;

    private String serverName;
}