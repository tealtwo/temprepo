package me.whitehatd.aquila.queue.redis.subscriber;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import gg.supervisor.core.loader.SupervisorLoader;
import gg.supervisor.core.util.Services;
import lombok.extern.java.Log;
import me.whitehatd.aquila.queue.QueuePlugin;
import me.whitehatd.aquila.queue.redis.dto.ArenaReadyMessage;
import me.whitehatd.aquila.queue.util.ServerConfig;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import redis.clients.jedis.JedisPubSub;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Log
public class RedisArenaReadySubscriber extends JedisPubSub {

    @Override
    public void onMessage(String channel, String message) {
        ServerConfig config     = Services.loadIfPresent(ServerConfig.class);
        String incomingChannel = "queue.arena.ready." + config.getServerName();

        log.info("Incoming channel: " + incomingChannel);
        log.info("Message: " + message);

        if (!incomingChannel.equals(channel)) return;
        try {
            ArenaReadyMessage readyMsg = SupervisorLoader.GSON.fromJson(message, ArenaReadyMessage.class);

            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Connect");
            out.writeUTF("arena");

            List<String> all = new ArrayList<>(readyMsg.getTeamA());
            all.addAll(readyMsg.getTeamB());

            for (String uuid : all) {
                Player player = Bukkit.getPlayer(UUID.fromString(uuid));
                if (player == null) {
                    log.severe("Someone disconnected while waiting for the arena to be ready. Stopped processing.");
                    return;
                }
            }

            Bukkit.getScheduler().runTask(Services.loadIfPresent(QueuePlugin.class), () -> {
                for (String uuid : all) {
                    Player player = Bukkit.getPlayer(UUID.fromString(uuid));
                    if (player != null) {
                        player.sendPluginMessage(
                                Services.loadIfPresent(QueuePlugin.class), "BungeeCord", out.toByteArray());
                    }
                }
            });

        } catch (Exception e) {
            log.severe("Error processing queue.arena.ready message in queue plugin: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
