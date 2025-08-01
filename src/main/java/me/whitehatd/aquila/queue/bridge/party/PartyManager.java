package me.whitehatd.aquila.queue.bridge.party;

import gg.supervisor.core.annotation.Component;
import gg.supervisor.core.util.Services;
import gg.supervisor.util.chat.Text;
import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

@Data
@Component
public class PartyManager {
    // Map of party leader UUID to Party
    private final Map<String, Party> parties = new HashMap<>();
    // Map of challenged party leader UUID to DuelInvite
    private final Map<String, DuelInvite> duelInvites = new HashMap<>();

    /**
     * Returns the party a player is in (searching by membership).
     */
    public Party getParty(String playerUUID) {
        for (Party party : parties.values()) {
            if (party.getMembers().contains(playerUUID)) {
                return party;
            }
        }
        return null;
    }

    /**
     * Creates a party with the given leader.
     */
    public Party createParty(String leaderUUID) {
        Party party = new Party(leaderUUID);
        parties.put(leaderUUID, party);
        return party;
    }

    /**
     * Invites a target player to the leader's party. (Only the leader may invite.)
     */
    public boolean invite(String leaderUUID, String targetUUID) {
        // Ensure the invoker is the leader.
        Party party = parties.get(leaderUUID);
        if (party == null) {
            // If the leader is not in a party, create one.
            party = createParty(leaderUUID);
        } else if (!party.getLeader().equals(leaderUUID)) {
            return false;
        }
        // Do not invite if the target is already in a party.
        if (getParty(targetUUID) != null) {
            return false;
        }
        party.addInvite(targetUUID);
        parties.put(leaderUUID, party);
        return true;
    }

    /**
     * Accepts an invite from a party leader.
     */
    public boolean acceptInvite(String playerUUID, String leaderUUID) {
        Party party = parties.get(leaderUUID);
        if (party == null || !party.hasInvite(playerUUID)) {
            return false;
        }
        party.removeInvite(playerUUID);
        party.getMembers().add(playerUUID);

        broadcast(party,
                "&a" + Bukkit.getPlayer(UUID.fromString(playerUUID)).getName() + " has joined the party!",
                true);

        return true;
    }

    /**
     * Removes a player from their party. If the leader leaves, disband the party.
     */
    public boolean leaveParty(String playerUUID) {
        Party party = getParty(playerUUID);
        if (party == null) return false;

        Services.loadIfPresent(PartyChatManager.class).disablePartyChat(UUID.fromString(playerUUID));

        if (party.getLeader().equals(playerUUID)) {
            disbandParty(playerUUID);
        } else {
            party.getMembers().remove(playerUUID);
            parties.put(party.getLeader(), party);

            broadcast(party,
                    "&c" + Bukkit.getOfflinePlayer(UUID.fromString(playerUUID)).getName() + " has left the party!",
                    true);
        }

        return true;
    }

    public void forceAddMember(Party party, String memberUUID) {
        if(getParty(memberUUID) != null) return;

        party.getMembers().add(memberUUID);
        broadcast(party,
                "&a" + Bukkit.getPlayer(UUID.fromString(memberUUID)).getName() + " has joined the party!",
                true);

    }

    public void broadcast(Party party, String message, boolean includeLeader, String... membersToExclude) {
        Set<String> excludedMembers = new HashSet<>(List.of(membersToExclude));
        excludedMembers.add(party.getLeader());

        List<String> membersToBroadcast = new ArrayList<>(party.getMembers());
        membersToBroadcast.removeIf(excludedMembers::contains);

        if (includeLeader) {
            membersToBroadcast.add(party.getLeader());
        }

        for (String memberUUID : membersToBroadcast) {
            Player player = Bukkit.getPlayer(UUID.fromString(memberUUID));
            if (player != null && player.isOnline()) {
                player.sendMessage(Text.translate(message));
            }
        }
    }

    /**
     * Disbands the party led by leaderUUID.
     */
    public boolean disbandParty(String leaderUUID) {
        Party party = parties.get(leaderUUID);
        if (party != null) {
            PartyChatManager cm = Services.loadIfPresent(PartyChatManager.class);
            for (String mem : party.getMembers()) {
                cm.disablePartyChat(UUID.fromString(mem));
            }

            broadcast(party,
                    "&4The party has been disbanded!",
                    false);
        }

        return parties.remove(leaderUUID) != null;
    }

    /**
     * Stores a duel invitation from the challenger (leader) for the challenged (leader).
     */
    public void setDuelInvite(String challengedLeaderUUID, DuelInvite invite) {
        duelInvites.put(challengedLeaderUUID, invite);
    }

    /**
     * Retrieves a duel invitation for the challenged leader.
     */
    public DuelInvite getDuelInvite(String challengedLeaderUUID) {
        return duelInvites.get(challengedLeaderUUID);
    }

    /**
     * Clears a duel invitation.
     */
    public void removeDuelInvite(String challengedLeaderUUID) {
        duelInvites.remove(challengedLeaderUUID);
    }

    /**
     * Returns all current parties.
     */
    public Collection<Party> getAllParties() {
        return parties.values();
    }

    @Data
    public static class DuelInvite {
        private final String challengerLeaderUUID;
        private final String gamemodeId;
    }
}
