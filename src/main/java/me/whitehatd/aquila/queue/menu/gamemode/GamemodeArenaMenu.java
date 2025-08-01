package me.whitehatd.aquila.queue.menu.gamemode;

import gg.supervisor.core.util.Services;
import gg.supervisor.menu.entities.InteractionModifier;
import gg.supervisor.menu.guis.Pager;
import gg.supervisor.menu.guis.builder.SchemaBuilder;
import gg.supervisor.menu.guis.impl.PersonalizedMenu;
import gg.supervisor.menu.builder.ItemBuilder;
import gg.supervisor.util.chat.Text;
import me.whitehatd.aquila.queue.QueuePlugin;
import me.whitehatd.aquila.queue.bridge.gamemode.Gamemode;
import me.whitehatd.aquila.queue.manager.GamemodeManager;
import me.whitehatd.aquila.queue.redis.RedisPublisher;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class GamemodeArenaMenu extends PersonalizedMenu {

    private final Gamemode gamemode;
    private final GamemodeManager gamemodeManager;
    private final Player player;
    private List<String> availableArenas;

    public GamemodeArenaMenu(Player player, Gamemode gamemode) {
        super(player, 6, Text.translate("&5Arena Selection for " + gamemode.getName()), InteractionModifier.VALUES);
        this.player = player;
        this.gamemode = gamemode;
        this.gamemodeManager = Services.loadIfPresent(GamemodeManager.class);

        // Get available arenas from Redis
        CompletableFuture.supplyAsync(() -> {
            RedisPublisher publisher = Services.loadIfPresent(RedisPublisher.class);
            try (Jedis jedis = publisher.getJedisPool().getResource()) {
                return jedis.lrange("availableArenas", 0, -1);
            } catch (Exception e) {
                return new ArrayList<String>();
            }
        }).thenAccept(availableArenas -> {
            Bukkit.getScheduler().runTask(Services.loadIfPresent(QueuePlugin.class), () -> {
                this.availableArenas = availableArenas;
                populateArenas();
            });
        });

        getDecorator().decorate(new SchemaBuilder()
                .add("x        ")
                .add(" RRRRRRR ")
                .add(" RRRRRRR ")
                .add(" RRRRRRR ")
                .add("  a b c  ")
                .add("         ")
                .build());
    }

    private void populateArenas() {
        Pager pager = new Pager(this, 'R').endless(Pager.EndlessType.SIMPLE);

        clearGui();

        if(availableArenas.isEmpty()) {
            getDecorator().set('x', ItemBuilder.from(Material.BARRIER)
                    .name(Text.translate("&cNo arenas available"))
                    .lore(List.of(
                        Text.translate("&7There are no arenas available for selection.")
                    ))
                    .menuItem());

            return;
        }

        for (String arenaName : availableArenas) {
            boolean isSelected = gamemode.getArenas().contains(arenaName);

            Material material = isSelected ? Material.LIME_CONCRETE : Material.RED_CONCRETE;
            String status = isSelected ? "&aEnabled" : "&cDisabled";

            pager.add(ItemBuilder.from(material)
                    .name(Text.translate("&b" + arenaName))
                    .lore(List.of(
                        Text.translate("&7Status: " + status),
                        Text.translate(""),
                        Text.translate("&eClick to " + (isSelected ? "disable" : "enable") + " this arena")
                    ))
                    .menuItem(event -> {
                        if (isSelected) {
                            gamemode.getArenas().remove(arenaName);
                        } else {
                            gamemode.getArenas().add(arenaName);
                        }

                        gamemodeManager.updateGamemode(gamemode);

                        // Refresh menu
                        populateArenas();
                    }));
        }

        // Navigation and control buttons
        getDecorator().set(' ', ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).menuItem());
        getDecorator().set('x', ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).menuItem());

        // Previous page button
        getDecorator().add('a', ItemBuilder.from(Material.ARROW)
                .name(Text.translate("&aPrevious Page"))
                .menuItem(event -> pager.previous()));

        // Back button
        getDecorator().add('b', ItemBuilder.from(Material.BARRIER)
                .name(Text.translate("&cBack"))
                .menuItem(event -> new GamemodeEditMenu(player, gamemode).open()));

        // Next page button
        getDecorator().add('c', ItemBuilder.from(Material.ARROW)
                .name(Text.translate("&aNext Page"))
                .menuItem(event -> pager.next()));
    }
}