package me.whitehatd.aquila.queue.redis.subscriber;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import gg.supervisor.core.loader.SupervisorLoader;
import gg.supervisor.core.util.Services;
import gg.supervisor.util.chat.Text;
import lombok.extern.java.Log;
import me.whitehatd.aquila.queue.QueuePlugin;
import me.whitehatd.aquila.queue.redis.SpectateDTO;
import me.whitehatd.aquila.queue.util.ServerConfig;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import java.util.UUID;

@Log
public class RedisSpectateResponseSubscriber extends JedisPubSub {

    @Override
    public void onMessage(String channel, String message) {
        ServerConfig config     = Services.loadIfPresent(ServerConfig.class);
        String incomingChannel = "spectate.response." + config.getServerName();

        if (!incomingChannel.equals(channel)) return;
        try {
            SpectateDTO dto = SupervisorLoader.GSON.fromJson(message, SpectateDTO.class);

            if(dto.getMatchId().equals("invalid")) {
                log.info("Spectate response indicated an invalid match. No action taken.");
                Player player = Bukkit.getPlayer(UUID.fromString(dto.getSpectatorUUID()));

                if(player != null)
                    player.sendMessage(Text.translate("&cThe match you are trying to spectate is no longer active."));

                return;
            }

            String spectatorUUID = dto.getSpectatorUUID();
            Player spectator = Bukkit.getPlayer(UUID.fromString(spectatorUUID));
            if (spectator == null) {
                log.info("Spectator " + spectatorUUID + " is not online.");
                return;
            }

            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Connect");
            out.writeUTF("arena");

            Bukkit.getScheduler().runTask(Services.loadIfPresent(QueuePlugin.class), () -> {
                spectator.sendMessage(Text.translate("&aSpectate validated! Connecting to arena..."));
                spectator.sendPluginMessage(Services.loadIfPresent(QueuePlugin.class), "BungeeCord", out.toByteArray());
            });
        } catch (Exception e) {
            log.severe("Error processing spectate response: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
