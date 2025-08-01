package me.whitehatd.aquila.queue.bridge.party;

import gg.supervisor.core.annotation.Component;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages whether a player is toggled into "party chat" mode.
 */
@Data
@Component
public class PartyChatManager {

    // Map from player UUID -> whether they're toggled into party chat.
    private final Map<UUID, Boolean> partyChatToggles = new HashMap<>();

    /**
     * Checks if the given player is in party chat mode.
     */
    public boolean isPartyChatToggled(UUID playerId) {
        return partyChatToggles.getOrDefault(playerId, false);
    }

    /**
     * Toggles party chat mode for this player. If ON, set OFF; if OFF, set ON.
     * Returns the new state after toggling.
     */
    public boolean togglePartyChat(UUID playerId) {
        boolean currently = isPartyChatToggled(playerId);
        boolean newState = !currently;
        partyChatToggles.put(playerId, newState);
        return newState;
    }

    /**
     * Forcefully disable party chat for a player (e.g. if they leave their party).
     */
    public void disablePartyChat(UUID playerId) {
        partyChatToggles.remove(playerId);
    }
}
