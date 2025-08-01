package me.whitehatd.aquila.queue.redis.subscriber;

import gg.supervisor.core.loader.SupervisorLoader;
import gg.supervisor.core.util.Services;
import gg.supervisor.util.chat.Text;
import lombok.extern.java.Log;
import me.whitehatd.aquila.queue.bridge.party.Party;
import me.whitehatd.aquila.queue.bridge.party.PartyManager;
import me.whitehatd.aquila.queue.redis.dto.RecreatePartyMessage;
import me.whitehatd.aquila.queue.util.ServerConfig;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import redis.clients.jedis.JedisPubSub;

import java.util.UUID;

@Log
public class RecreatePartySubscriber extends JedisPubSub {

    @Override
    public void onMessage(String channel, String message) {
        ServerConfig config     = Services.loadIfPresent(ServerConfig.class);
        String incomingChannel = "queue.party.recreate." + config.getServerName();

        if (!incomingChannel.equals(channel)) return;
        try {
            RecreatePartyMessage dto = SupervisorLoader.GSON.fromJson(message, RecreatePartyMessage.class);

            Player leader = Bukkit.getPlayer(UUID.fromString(dto.getLeaderId()));
            if(leader == null) return;

            PartyManager partyManager = Services.getService(PartyManager.class);
            Party newParty = partyManager.createParty(leader.getUniqueId().toString());

            for(String playerId : dto.getPartyMembers()) {
                Player player = Bukkit.getPlayer(UUID.fromString(playerId));
                if (player != null) {
                    partyManager.forceAddMember(newParty, player.getUniqueId().toString());
                }
            }

            for (String playerId : newParty.getMembers()) {
                Player player = Bukkit.getPlayer(UUID.fromString(playerId));
                if (player != null) {
                    player.sendMessage(Text.translate("&aYour party has been recreated!"));
                }
            }

        } catch (Exception e) {
            log.severe("Error processing recreate party response: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

