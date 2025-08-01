package me.whitehatd.aquila.queue.menu.spectate;

import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
public class SpectateEntry {
    private final String playerName;
    private final String matchId;
    private final List<String> lore;
    private final UUID playerId;

    public SpectateEntry(UUID playerId, String playerName, String matchId, List<String> lore) {
        this.playerName = playerName;
        this.matchId = matchId;
        this.lore = lore;
        this.playerId = playerId;
    }
}
