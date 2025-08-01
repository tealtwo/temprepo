// Match.java
package me.whitehatd.aquila.queue.bridge.match;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class Match {
    private String id;
    private boolean ranked, interrupted;
    private long startTime;
    private long endTime;
    private MatchType matchType;

    private List<String> teamA, disconnectedPlayers;
    private List<String> teamB, deadPlayers;

    private String tournamentId;
    private String arenaId;
    private String slimeArenaWorld;

    private Map<String, Object> extraSettings = new HashMap<>();

    private List<String> spectators = new ArrayList<>();

    private Map<String, PlayerMatchStats> playerStats = new HashMap<>();

    private List<String> rolledBackPlayers = new ArrayList<>();

    private String incomingServerName;
}
