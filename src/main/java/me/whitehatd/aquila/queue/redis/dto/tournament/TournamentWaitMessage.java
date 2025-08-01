package me.whitehatd.aquila.queue.redis.dto.tournament;

import lombok.Data;
import java.util.List;

/** sent only when a team auto-advances because of a bye */
@Data
public class TournamentWaitMessage {
    private String tournamentId;
    private List<String> playerIds;   // UUIDs that must wait
    private int roundIdx;
    private int totalRounds;
}
