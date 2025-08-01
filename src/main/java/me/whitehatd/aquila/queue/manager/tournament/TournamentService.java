package me.whitehatd.aquila.queue.manager.tournament;

import gg.supervisor.core.util.Services;
import gg.supervisor.util.chat.Text;
import lombok.extern.slf4j.Slf4j;
import me.whitehatd.aquila.queue.bridge.gamemode.GamemodeRepository;
import me.whitehatd.aquila.queue.bridge.match.MatchType;
import me.whitehatd.aquila.queue.bridge.party.Party;
import me.whitehatd.aquila.queue.bridge.party.PartyManager;
import me.whitehatd.aquila.queue.bridge.player.PlayerRepository;
import me.whitehatd.aquila.queue.bridge.tournament.Tournament;
import me.whitehatd.aquila.queue.bridge.tournament.TournamentMatch;
import me.whitehatd.aquila.queue.bridge.tournament.TournamentRound;
import me.whitehatd.aquila.queue.bridge.tournament.TournamentState;
import me.whitehatd.aquila.queue.redis.RedisPublisher;
import me.whitehatd.aquila.queue.redis.dto.MatchStartMessage;
import me.whitehatd.aquila.queue.redis.dto.tournament.TournamentMatchCompleteMessage;
import me.whitehatd.aquila.queue.redis.dto.tournament.TournamentWaitMessage;
import me.whitehatd.aquila.queue.util.TitleUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Orchestrates tournament flow: countdown, bracket creation,
 * unified match dispatch, and match-completion handling.
 */

@Slf4j
public class TournamentService {

    private final CountdownService countdown;
    private final TitleUtils titles;
    private final BracketGenerator bracketGen;
    private final TournamentManager manager;
    private final RedisPublisher redis;

    public TournamentService(CountdownService countdown,
                             BracketGenerator bracketGen,
                             TitleUtils titles,
                             TournamentManager manager,
                             RedisPublisher redis) {
        this.countdown = countdown;
        this.bracketGen = bracketGen;
        this.titles = titles;
        this.manager = manager;
        this.redis = redis;
    }

    public void joinTournament(Player leader, String tournamentId) {
        String leaderUuid = leader.getUniqueId().toString();
        Tournament t = manager.getTournament(tournamentId);
        PartyManager partyMgr = Services.loadIfPresent(PartyManager.class);

        // 1) Party logic
        Party party = partyMgr.getParty(leaderUuid);
        if (party != null) {
            // Only the leader can join-for-all
            if (!party.getLeader().equals(leaderUuid)) {
                leader.sendMessage(Text.translate("&cOnly the party leader can join the tournament for the whole party."));
                return;
            }

            // Must be open
            if (t == null || t.getState() != TournamentState.WAITING) {
                leader.sendMessage(Text.translate("&cThat tournament is not open for joining."));
                return;
            }

            // Check capacity
            int maxPlayers = t.getNumberOfTeams() * t.getTeamSize();
            int current  = t.getPlayerIds().size();
            int partySize = party.getMembers().size();
            int freeSlots = maxPlayers - current;
            if (partySize > freeSlots) {
                leader.sendMessage(Text.translate(
                        "&cNot enough space: your party of &e" + partySize +
                                " &cwon’t fit (&e" + freeSlots + " &cslots left)."
                ));
                return;
            }

            // Check none are already in another tournament
            for (String memberUuid : party.getMembers()) {
                if (manager.getPlayerToTournament().containsKey(memberUuid)) {
                    String name = Bukkit.getOfflinePlayer(UUID.fromString(memberUuid)).getName();
                    leader.sendMessage(Text.translate(
                            "&cCannot add party: &e" + name + " &cis already in a tournament."
                    ));
                    return;
                }
            }

            // All clear → add each member
            for (String memberUuid : party.getMembers()) {
                // This will update cache, playerToTournament, and persist
                manager.addPlayer(tournamentId, memberUuid);

                Player member = Bukkit.getPlayer(UUID.fromString(memberUuid));
                if (member != null && member.isOnline()) {
                    member.sendMessage(Text.translate(
                            "&aYou’ve been added to a tournament by your party leader."
                    ));
                }
            }

            // Re-evaluate countdown/start
            onTournamentUpdated(t);
            return;
        }

        // 2) Solo join (exactly as before)
        String soloUuid = leaderUuid;
        boolean success = manager.addPlayer(tournamentId, soloUuid);
        if (!success) {
            leader.sendMessage(Text.translate(
                    "&cUnable to join: you might already be in a tournament, it's full, or not joinable."
            ));
            return;
        }

        leader.sendMessage(Text.translate("&aJoined tournament &e" + tournamentId));
        onTournamentUpdated(t);
    }

    public void leaveTournament(Player player) {
        String playerUuid = player.getUniqueId().toString();

        String tournamentId = manager.getPlayerToTournament().get(playerUuid);
        if (tournamentId == null) {
            player.sendMessage(Text.translate("&cYou are not in any tournament."));
            return;
        }

        manager.removePlayerFromAllTournaments(playerUuid);

        player.sendMessage(Text.translate("&aYou have left the tournament."));
    }


    public void cancelTournament(Player p, String tournamentId) {
        Tournament t = manager.getTournament(tournamentId);
        if (t == null || t.getState() != TournamentState.WAITING) {
            p.sendMessage(Text.translate("&cCannot cancel now."));
            return;
        }
        // check ownership/perm
        if (!t.getCreatorUuid().equals(p.getUniqueId().toString())
                && !p.hasPermission("aquila.tournaments.cancel.any")) {
            p.sendMessage(Text.translate("&cNo permission."));
            return;
        }

        manager.cancelTournament(tournamentId);
        p.sendMessage(Text.translate("&cTournament cancelled."));
    }

    /** Called whenever players join/leave to trigger countdown if full. */
    public void onTournamentUpdated(Tournament t) {
        int needed = t.getNumberOfTeams() * t.getTeamSize();
        if (t.getState() == TournamentState.WAITING
                && t.getPlayerIds().size() == needed) {
            t.setState(TournamentState.COUNTDOWN);
            manager.saveTournament(t);

            countdown.startCountdown(
                    t.getId(),
                    sec -> broadcastTitle(t, "&eTournament starts in", String.format("&6%ds", sec)),
                    () -> CompletableFuture.runAsync(() -> startTournament(t), redis.getExecutorService()),
                    () -> {
                        t.setState(TournamentState.WAITING);
                        manager.saveTournament(t);
                        broadcastTitle(t, "&cCountdown aborted", "&eWaiting for players");
                    }
            );
        }
    }

    /** Initiates the bracket and dispatches first-round matches. */
    private void startTournament(Tournament t) {
        t.setState(TournamentState.IN_PROGRESS);
        t.setStartTime(System.currentTimeMillis());

        CompletableFuture.supplyAsync(() ->
                        bracketGen.generateFullBracket(new ArrayList<>(t.getPlayerIds()), t.getTeamSize()),
                redis.getExecutorService()
        ).thenAccept(rounds -> {
            t.setRounds(rounds);
            manager.saveTournament(t);
            spawnMatches(t, true, 0);
            broadcastTitle(t, "&aTournament started!", "&eGood luck");
        });
    }

    /**
     * Sends matches to Redis:
     *   • firstRound  → teleportPlayers = true  (queue → arena hop)
     *   • laterRounds → teleportPlayers = false (already on arena server)
     *
     * Bye-matches (one team missing) are **not** published – the winner is
     * already known, so we simply let the round-completion logic advance
     * automatically.
     */
    private void spawnMatches(Tournament t, boolean firstRound, int roundIdx) {

        TournamentRound round = t.getRounds().get(roundIdx);

        /* -------- previous arenas (needed for clean-up on arena node) ------- */
        Set<String> prevArenas = new HashSet<>();
        if (!firstRound) {
            t.getRounds().get(roundIdx - 1).getMatches().stream()
                    .map(TournamentMatch::getArenaReference)
                    .filter(Objects::nonNull)
                    .forEach(prevArenas::add);
        }

        for (int i = 0; i < round.getMatches().size(); i++) {
            TournamentMatch m = round.getMatches().get(i);

            List<String> a = m.getTeamA();
            List<String> b = m.getTeamB();

            /* ───── BYE  → auto-advance & notify team ───── */
            if (a == null || a.isEmpty() || b == null || b.isEmpty()) {

                m.setWinnerTeamIndex(a == null || a.isEmpty() ? 1 : 0);   // mark winner
                // ⇢ inform the client arena so players get the title
                TournamentWaitMessage wait = new TournamentWaitMessage();
                wait.setTournamentId(t.getId());
                wait.setPlayerIds(a == null || a.isEmpty() ? b : a);
                wait.setRoundIdx(roundIdx);
                wait.setTotalRounds(t.getRounds().size());

                redis.sendTournamentWait(wait);
                continue;
            }

            MatchStartMessage msg = new MatchStartMessage();
            msg.setTeamA(a);
            msg.setTeamB(b);
            msg.setTeleportPlayers(firstRound);
            msg.setTournamentId(t.getId());

            MatchType mt = Arrays.stream(MatchType.values())
                    .filter(x -> x.getSize() == t.getTeamSize())
                    .findFirst().orElseThrow();
            msg.setMatchType(mt.name());

            msg.setGamemodeId(t.getGamemodeId());
            msg.setMatchScheduledTime(System.currentTimeMillis());
            msg.setLoadouts(buildLoadouts(a, b, t.getGamemodeId()));

            Map<String,Object> extra = new HashMap<>();
            extra.put("roundIdx",     roundIdx);
            extra.put("matchIdx",     i);
            extra.put("totalRounds",  t.getRounds().size());
            extra.put("isFinal",      roundIdx == t.getRounds().size() - 1);
            extra.put("prevArenas",  new ArrayList<>(prevArenas));   // may be []

            msg.setExtraSettings(extra);

            redis.sendMatchStart(msg);
        }

        manager.saveTournament(t);
    }

    /* small helper to keep original loadout-building logic in one place */
    private Map<String, Map<Integer,String>> buildLoadouts(
            List<String> a, List<String> b, String gmId) {

        var playerRepo   = Services.loadIfPresent(PlayerRepository.class);
        var gamemodeRepo = Services.loadIfPresent(GamemodeRepository.class);

        Map<String, Map<Integer,String>> out = new HashMap<>();
        for (String pid : concat(a,b)) {
            Map<Integer,String> lo =
                    playerRepo.find(pid).getLoadoutRaw(gmId);
            if (lo == null) lo = gamemodeRepo.find(gmId).getLoadoutData();
            out.put(pid, lo);
        }
        return out;
    }
    private List<String> concat(List<String> a, List<String> b) {
        List<String> all = new ArrayList<>(a == null ? List.of() : a);
        if (b != null) all.addAll(b);
        return all;
    }

    /** Processes a match-completion coming from the arena side. */
    public void onMatchComplete(TournamentMatchCompleteMessage dto) {

        Tournament t = manager.getTournament(dto.getTournamentId());
        if (t == null) return;

        TournamentRound round = t.getRounds().get(dto.getRoundIndex());
        TournamentMatch tm    = round.getMatches().get(dto.getMatchIndex());

        tm.setWinnerTeamIndex(dto.getWinnerTeamIndex());
        tm.setArenaReference  (dto.getPreviousArenaReference());
        manager.saveTournament(t);

        /* wait until every match in this round has a winner */
        boolean allDone = round.getMatches().stream()
                .allMatch(m -> m.getWinnerTeamIndex() >= 0);
        if (!allDone) return;

        /* ===== round completed – decide what’s next ===== */
        int nextIdx = dto.getRoundIndex() + 1;

        /* no more rounds → tournament over */
        if (nextIdx >= t.getRounds().size()) {
            manager.completeTournament(t);
            return;
        }

        TournamentRound next = t.getRounds().get(nextIdx);

        /* collect winners from the round we just finished */
        List<List<String>> winners = round.getMatches().stream()
                .map(m -> m.getWinnerTeamIndex() == 0 ? m.getTeamA() : m.getTeamB())
                .toList();

        /* wire winners into the next-round bracket */
        for (int i = 0; i < winners.size(); i += 2) {
            int slot      = i / 2;
            TournamentMatch target = next.getMatches().get(slot);

            TournamentMatch src    = round.getMatches().get(i);   // ← winner’s match

            target.setTeamA(winners.get(i));

            if (i + 1 < winners.size()) {               // real match next round
                target.setTeamB(winners.get(i + 1));
                target.setWinnerTeamIndex(-1);
            } else {                                    // bye → auto-advance
                target.setTeamB(null);
                target.setWinnerTeamIndex(0);

                /* ---------- NEW ---------- */
                target.setArenaReference(src.getArenaReference());
            }
        }

        manager.saveTournament(t);

        /* finally, publish the next real matches (BYEs skipped) */
        spawnMatches(t, /* firstRound = */ false, /* roundIdx = */ nextIdx);

        log.info("Tournament {} → round {} dispatched", t.getId(), nextIdx);
    }

    /** Broadcasts a title to all players in the tournament. */
    private void broadcastTitle(Tournament t, String title, String subtitle) {
        t.getPlayerIds().stream()
                .map(UUID::fromString)
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .forEach(p -> titles.showTitle(p, title, subtitle, 10, 40, 10));
    }
}
