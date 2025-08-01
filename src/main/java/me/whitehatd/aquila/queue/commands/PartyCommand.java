package me.whitehatd.aquila.queue.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.PaperCommandManager;
import co.aikar.commands.annotation.*;
import gg.supervisor.core.annotation.Component;
import gg.supervisor.core.util.Services;
import gg.supervisor.util.chat.Text;
import me.whitehatd.aquila.queue.bridge.party.PartyChatManager;
import me.whitehatd.aquila.queue.manager.QueueManager;
import me.whitehatd.aquila.queue.menu.party.PartyDuelMenu;
import me.whitehatd.aquila.queue.bridge.party.Party;
import me.whitehatd.aquila.queue.bridge.party.PartyManager;
import me.whitehatd.aquila.queue.bridge.party.PartyManager.DuelInvite;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

@Component
@CommandAlias("party")
public class PartyCommand extends BaseCommand {

    private final PartyManager partyManager;

    public PartyCommand(PaperCommandManager manager, PartyManager partyManager) {
        manager.registerCommand(this);
        this.partyManager = partyManager;
    }

    @Default
    @Description("Shows party help")
    public void onDefault(Player player) {
        player.sendMessage(Text.translate("&6Party Commands:"));
        player.sendMessage(Text.translate("&e/party create"));
        player.sendMessage(Text.translate("&e/party chat"));
        player.sendMessage(Text.translate("&e/party info [optional: player]"));
        player.sendMessage(Text.translate("&e/party invite <player>"));
        player.sendMessage(Text.translate("&e/party acceptinvite <leader>"));
        player.sendMessage(Text.translate("&e/party leave"));
        player.sendMessage(Text.translate("&e/party disband"));
        player.sendMessage(Text.translate("&e/party duel <player>"));
        player.sendMessage(Text.translate("&e/party accept duel <gamemodeId>"));
    }

    @Subcommand("info")
    @Description("Displays party information")
    @CommandCompletion("@players")
    public void partyInfo(Player player, @Optional String target) {
        String id = (target != null ? target : player.getName());

        Player targetPlayer = player.getServer().getPlayerExact(id);
        if (targetPlayer == null) {
            player.sendMessage(Text.translate("&cPlayer not found: " + id));
            return;
        }

        Party party = partyManager.getParty(targetPlayer.getUniqueId().toString());
        if (party == null) {
            player.sendMessage(Text.translate("&cNo party found for " + id));
        } else {
            player.sendMessage(Text.translate("&aParty Leader: " + Bukkit.getOfflinePlayer(UUID.fromString(party.getLeader())).getName()));
            player.sendMessage(Text.translate("&aMembers: " + party.getMembers().stream()
                    .filter(member -> !member.equalsIgnoreCase(party.getLeader()))
                    .map(
                            member -> Bukkit.getOfflinePlayer(UUID.fromString(member)).getName()).toList()));
            if (!party.getPendingInvites().isEmpty()) {
                player.sendMessage(Text.translate("&aPending Invites: " + party.getPendingInvites().keySet().stream().map(
                        member -> Bukkit.getOfflinePlayer(UUID.fromString(member)).getName()).toList()));
            }
        }
    }

    @Subcommand("invite")
    @Description("Invites a player to your party (leader only)")
    @CommandCompletion("@players")
    public void invite(Player player, String targetName) {
        Player targetPlayer = player.getServer().getPlayerExact(targetName);
        if (targetPlayer == null) {
            player.sendMessage(Text.translate("&cPlayer not found: " + targetName));
            return;
        }

        if(player.getName().equalsIgnoreCase(targetName)) {
            player.sendMessage(Text.translate("&cYou cannot invite yourself."));
            return;
        }

        boolean success = partyManager.invite(player.getUniqueId().toString(), targetPlayer.getUniqueId().toString());
        if (success) {
            player.sendMessage(Text.translate("&aInvite sent to " + targetName));
            Player target = player.getServer().getPlayerExact(targetName);
            if (target != null) {
                TextComponent acceptButton = net.kyori.adventure.text.Component.text("[Accept]")
                        .color(NamedTextColor.GREEN)
                        .decorate(TextDecoration.BOLD)
                        .clickEvent(ClickEvent.runCommand("/party acceptinvite " + player.getName()));

                TextComponent message = net.kyori.adventure.text.Component.text(player.getName() + " has invited you to join their party. ")
                        .color(NamedTextColor.YELLOW)
                        .append(acceptButton);

                target.sendMessage(message);
            }
        } else {
            player.sendMessage(Text.translate("&cFailed to send invite. The target might already be in a party."));
        }
    }

    @Subcommand("create")
    @Description("Create a party")
    public void create(Player player) {
        if(partyManager.getParty(player.getUniqueId().toString()) != null) {
            player.sendMessage(Text.translate("&cYou are already in a party."));
            return;
        }

        Party party = partyManager.createParty(player.getUniqueId().toString());
        player.sendMessage(Text.translate("&aParty created!"));
    }

    @Subcommand("acceptinvite")
    @Description("Accept a party invite")
    public void acceptInvite(Player player, String leaderName) {
        Player targetPlayer = player.getServer().getPlayerExact(leaderName);
        if (targetPlayer == null) {
            player.sendMessage(Text.translate("&cPlayer not found: " + leaderName));
            return;
        }

        boolean success = partyManager.acceptInvite(player.getUniqueId().toString(),
                targetPlayer.getUniqueId().toString());
        if (success) {
            player.sendMessage(Text.translate("&aYou have joined the party."));
        } else {
            player.sendMessage(Text.translate("&cNo valid invite found."));
        }
    }

    @Subcommand("leave")
    @Description("Leave your party")
    public void leave(Player player) {
        boolean success = partyManager.leaveParty(player.getUniqueId().toString());
        if (success) {
            player.sendMessage(Text.translate("&aYou have left your party."));
        } else {
            player.sendMessage(Text.translate("&cYou are not in a party."));
        }
    }

    @Subcommand("disband")
    @Description("Disband your party (leader only)")
    public void disband(Player player) {
        Party party = partyManager.getParty(player.getUniqueId().toString());
        if (party == null) {
            player.sendMessage(Text.translate("&cYou are not in a party."));
            return;
        }
        if (!party.getLeader().equals(player.getUniqueId().toString())) {
            player.sendMessage(Text.translate("&cOnly the party leader can disband the party."));
            return;
        }
        partyManager.disbandParty(party.getLeader());
        player.sendMessage(Text.translate("&aYour party has been disbanded."));
    }

    @Subcommand("duel")
    @CommandCompletion("@players")
    @Description("Challenge another party to a duel (leader only)")
    public void duel(Player player, String targetPlayerName) {
        Party yourParty = partyManager.getParty(player.getUniqueId().toString());
        if (yourParty == null) {
            player.sendMessage(Text.translate("&cYou are not in a party."));
            return;
        }
        if (!yourParty.getLeader().equals(player.getUniqueId().toString())) {
            player.sendMessage(Text.translate("&cOnly your party leader can challenge another party."));
            return;
        }

        Player targetPlayer = player.getServer().getPlayerExact(targetPlayerName);
        if (targetPlayer == null) {
            player.sendMessage(Text.translate("&cPlayer not found: " + targetPlayerName));
            return;
        }

        Party targetParty = partyManager.getParty(targetPlayer.getUniqueId().toString());
        if (targetParty == null) {
            player.sendMessage(Text.translate("&c" + targetPlayerName + " is not in a party."));
            return;
        }
        if (yourParty.equals(targetParty)) {
            player.sendMessage(Text.translate("&cYou cannot duel your own party."));
            return;
        }
        // Open the duel menu so the leader can pick a gamemode.
        new PartyDuelMenu(player, yourParty, targetParty).open();
    }

    @Subcommand("accept")
    @Description("Accept a duel invitation (leader only)")
    public void acceptDuel(Player player, String gamemodeId) {
        // Only party leaders can accept duel invitations.
        Party party = partyManager.getParty(player.getUniqueId().toString());
        if (party == null || !party.getLeader().equals(player.getUniqueId().toString())) {
            player.sendMessage(Text.translate("&cOnly a party leader may accept a duel invitation."));
            return;
        }
        // Check if there's a duel invitation for this party leader.
        DuelInvite invite = partyManager.getDuelInvite(player.getUniqueId().toString());
        if (invite == null) {
            player.sendMessage(Text.translate("&cNo duel invitation found."));
            return;
        }
        // Optionally, if gamemodeId is provided, ensure it matches.
        if (!gamemodeId.replaceAll("[^A-Za-z0-9-]", "").equalsIgnoreCase(invite.getGamemodeId())) {
            player.sendMessage(Text.translate("&cInvalid gamemode id provided. Use the &e/party duel&c menu."));
            return;
        }
        // Retrieve the challenger party.
        Party challengerParty = partyManager.getParty(invite.getChallengerLeaderUUID());
        if (challengerParty == null) {
            player.sendMessage(Text.translate("&cThe challenging party is no longer available."));
            partyManager.removeDuelInvite(player.getUniqueId().toString());
            return;
        }

        List<UUID> teamA = challengerParty.getMembers().stream().map(java.util.UUID::fromString).toList();
        List<UUID> teamB = party.getMembers().stream().map(java.util.UUID::fromString).toList();
        // Create match via QueueManager.
        boolean created = Services.loadIfPresent(QueueManager.class)
                .createPartyMatch(teamA, teamB, invite.getGamemodeId(),
                        challengerParty.getLeader(), challengerParty.getMembers(),
                        party.getLeader(), party.getMembers());
        if (created) {
            player.sendMessage(Text.translate("&aDuel accepted! Match is being created."));
        } else {
            player.sendMessage(Text.translate("&cFailed to create duel match."));
        }
        // Remove the duel invitation.
        partyManager.removeDuelInvite(player.getUniqueId().toString());
    }

    @Subcommand("chat|c")
    @Description("Toggle party chat mode on/off")
    public void onPartyChatToggle(Player player) {
        if(partyManager.getParty(player.getUniqueId().toString()) == null) {
            player.sendMessage(Text.translate("&cYou are not in a party."));
            return;
        }
        PartyChatManager chatManager = Services.loadIfPresent(PartyChatManager.class);

        boolean newState = chatManager.togglePartyChat(player.getUniqueId());
        if (newState) {
            player.sendMessage(Text.translate("&aParty chat enabled. Your messages will go only to your party."));
        } else {
            player.sendMessage(Text.translate("&cParty chat disabled. Your messages will go to global chat."));
        }
    }
}
