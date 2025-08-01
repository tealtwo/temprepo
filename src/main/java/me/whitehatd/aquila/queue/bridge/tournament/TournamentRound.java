package me.whitehatd.aquila.queue.bridge.tournament;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single round in the tournament bracket.
 */
@Data
public class TournamentRound {
    private int roundIndex;                      // e.g. 0 for first round, 1 for second, etc.
    private List<TournamentMatch> matches = new ArrayList<>();
}


