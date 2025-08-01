package me.whitehatd.aquila.queue.bridge.tournament;

import lombok.Data;

import java.util.List;

/**
 * Represents a single match within a TournamentRound:
 * - Two teams (each a list of player UUID strings),
 * - Which team eventually won, etc.
 */
@Data
public class TournamentMatch {
    private List<String> teamA;
    private List<String> teamB;  // if it's a bye, can be null or empty
    private int winnerTeamIndex; // 0 = A, 1 = B, -1 = not decided
    private String arenaReference;

    public TournamentMatch(List<String> teamA, List<String> teamB) {
        this.teamA = teamA;
        this.teamB = teamB;
        this.winnerTeamIndex = -1; // means not decided yet
        this.arenaReference = null; // means not assigned yet
    }
}