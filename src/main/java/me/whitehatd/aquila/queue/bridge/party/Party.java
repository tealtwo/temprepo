package me.whitehatd.aquila.queue.bridge.party;

import lombok.Data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Data
public class Party {
    // The party leader (their UUID string)
    private final String leader;
    // All party members (including leader)
    private final Set<String> members = new HashSet<>();
    // Pending invites: key = invited player UUID, value = expiry timestamp (ms)
    private final Map<String, Long> pendingInvites = new HashMap<>();

    public Party(String leader) {
        this.leader = leader;
        members.add(leader);
    }

    /**
     * Adds an invite for the given player with a 30-second expiry.
     */
    public void addInvite(String playerUUID) {
        pendingInvites.put(playerUUID, System.currentTimeMillis() + 30000L);
    }

    /**
     * Checks if there is a valid (not expired) invite for the given player.
     */
    public boolean hasInvite(String playerUUID) {
        Long expiry = pendingInvites.get(playerUUID);
        return expiry != null && System.currentTimeMillis() <= expiry;
    }

    /**
     * Removes the invite for the given player.
     */
    public void removeInvite(String playerUUID) {
        pendingInvites.remove(playerUUID);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Party party)) return false;

        if (!leader.equals(party.leader)) return false;
        if (!members.equals(party.members)) return false;
        return pendingInvites.equals(party.pendingInvites);
    }
}
