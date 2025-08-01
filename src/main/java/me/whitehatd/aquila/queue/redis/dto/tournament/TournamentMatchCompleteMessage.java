package me.whitehatd.aquila.queue.redis.dto.tournament;

import lombok.Data;

@Data
public class TournamentMatchCompleteMessage {
    private String tournamentId;
    private int roundIndex;
    private int matchIndex;
    private int winnerTeamIndex;
    private String previousArenaReference;
}