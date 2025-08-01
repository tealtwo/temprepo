package me.whitehatd.aquila.queue.manager.tournament;

import gg.supervisor.core.annotation.Component;
import me.whitehatd.aquila.queue.bridge.tournament.TournamentMatch;
import me.whitehatd.aquila.queue.bridge.tournament.TournamentRound;

import java.util.*;

/**
 * Produces a bracket where
 *   • Round-0 has no byes,
 *   • Later rounds may contain a one-sided “bye” match
 *     (teamB == null; winnerTeamIndex preset to 0).
 */
@Component
public class BracketGenerator {

    public List<TournamentRound> generateFullBracket(List<String> players,
                                                     int teamSize) {

        Collections.shuffle(players);

        /* 1. split players → teams --------------------------------------------------- */
        List<List<String>> pool = new ArrayList<>();
        for (int i = 0; i < players.size(); i += teamSize) {
            pool.add(new ArrayList<>(players.subList(
                    i, Math.min(players.size(), i + teamSize))));
        }

        /* 2. generate rounds until only ONE team remains ----------------------------- */
        List<TournamentRound> rounds = new ArrayList<>();
        int roundIdx = 0;

        while (pool.size() > 1) {

            TournamentRound round = new TournamentRound();
            round.setRoundIndex(roundIdx++);
            rounds.add(round);

            /* matches for this round */
            int numMatches = (pool.size() + 1) / 2;              // ceil
            List<List<String>> nextPool = new ArrayList<>(Collections.nCopies(numMatches, null));

            for (int m = 0, out = 0; m < pool.size(); m += 2, out++) {

                List<String> a = pool.get(m);
                List<String> b = (m + 1 < pool.size()) ? pool.get(m + 1) : null;

                TournamentMatch match = new TournamentMatch(a, b);

                if (b == null) {                // ── BYE, auto-advance A ──
                    match.setWinnerTeamIndex(0);
                    nextPool.set(out, a);       // A already in next pool
                } else {                        // ── real match ──
                    match.setWinnerTeamIndex(-1);
                    /* winner decided later – placeholder stays null */
                }
                round.getMatches().add(match);
            }

            pool = nextPool;                    // keep nulls for alignment
        }
        return rounds;
    }
}
