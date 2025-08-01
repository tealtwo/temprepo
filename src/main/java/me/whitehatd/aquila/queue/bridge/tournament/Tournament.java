package me.whitehatd.aquila.queue.bridge.tournament;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Tournament {
    private String id;
    private String gamemodeId;
    private String creatorUuid;
    private int numberOfTeams;
    private int teamSize;  // from matchType
    private long createdAt;

    private TournamentState state;
    private List<String> playerIds = new ArrayList<>();

    private List<TournamentRound> rounds = new ArrayList<>();

    // Timestamps for final
    private long startTime;
    private long endTime;
}
