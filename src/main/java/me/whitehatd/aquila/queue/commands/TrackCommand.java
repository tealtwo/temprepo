package me.whitehatd.aquila.queue.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.PaperCommandManager;
import co.aikar.commands.annotation.*;
import gg.supervisor.core.annotation.Component;
import gg.supervisor.core.util.Services;
import gg.supervisor.util.chat.Text;
import me.whitehatd.aquila.queue.redis.RedisPublisher;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import redis.clients.jedis.Jedis;

import java.util.UUID;

@Component
@CommandAlias("track")
@CommandPermission("aquila.track")
public class TrackCommand extends BaseCommand {

    public TrackCommand(PaperCommandManager manager) {
        manager.registerCommand(this);
    }

    @Subcommand("cancel")
    @Description("Stops tracking any player")
    public void onCancel(Player sender) {
        UUID trackerId = sender.getUniqueId();

        RedisPublisher publisher = Services.loadIfPresent(RedisPublisher.class);
        publisher.getExecutorService().submit(() -> {
            try(Jedis jedis = publisher.getJedisPool().getResource()) {
                boolean removed = jedis.hdel("trackers", trackerId.toString()) > 0;

                if (removed) {
                    sender.sendMessage(Text.translate("&aStopped tracking."));
                } else {
                    sender.sendMessage(Text.translate("&cYou weren't tracking anyone."));
                }
            }
        });


    }

    @Default
    @CommandCompletion("@players")
    @Description("Begins tracking another player, automatically spectating their matches.")
    public void onTrack(Player sender, String targetName) {
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            sender.sendMessage(Text.translate("&cPlayer not found: " + targetName + ". " +
                    "They might be in a game. Wait until they return to the lobby and try again."));
            return;
        }
        if (sender.getUniqueId().equals(target.getUniqueId())) {
            sender.sendMessage(Text.translate("&cYou cannot track yourself."));
            return;
        }

        UUID trackerId = sender.getUniqueId();
        UUID targetId = target.getUniqueId();

        // Put in map
        RedisPublisher publisher = Services.loadIfPresent(RedisPublisher.class);
        publisher.getExecutorService().submit(() -> {
            try(Jedis jedis = publisher.getJedisPool().getResource()) {
                jedis.hset("trackers", trackerId.toString(), targetId.toString());

                sender.sendMessage(Text.translate("&eYou are now tracking &f" + target.getName()));
            }
        });
    }
}
