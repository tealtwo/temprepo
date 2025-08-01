package me.whitehatd.aquila.queue.bridge.party;

import gg.supervisor.core.annotation.Component;
import gg.supervisor.core.util.Services;
import gg.supervisor.util.chat.Text;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.UUID;

@Component
public class PartyChatListener implements Listener {

    private final PartyChatManager chatManager;
    private final PartyManager partyManager;

    public PartyChatListener(PartyChatManager chatManager, PartyManager partyManager) {
        this.chatManager = chatManager;
        this.partyManager = partyManager;

    }

    @EventHandler
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (!chatManager.isPartyChatToggled(playerId)) {
            return;
        }

        Party party = partyManager.getParty(playerId.toString());
        if (party == null) {
            chatManager.disablePartyChat(playerId);
            player.sendMessage(Text.translate("&cYou are no longer in a party, party chat disabled."));
            return;
        }

        // They are in a party and toggled => broadcast to all online members of that party
        event.setCancelled(true);

        String message = Text.translateToMiniMessage(event.message());

        // Format how you want. Example:
        String format = "&d[Party] &f%p: &6%s"
                .replace("%p", player.getName())
                .replace("%s", message);

        // Send to every online member
        for (String memberStr : party.getMembers()) {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(UUID.fromString(memberStr));
            if (offline.isOnline() && offline.getPlayer() != null) {
                offline.getPlayer().sendMessage(Text.translate(format));
            }
        }
    }
}
