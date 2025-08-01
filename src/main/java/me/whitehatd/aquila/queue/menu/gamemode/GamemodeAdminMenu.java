package me.whitehatd.aquila.queue.menu.gamemode;

import gg.supervisor.core.util.Services;
import gg.supervisor.menu.entities.InteractionModifier;
import gg.supervisor.menu.guis.Pager;
import gg.supervisor.menu.guis.builder.SchemaBuilder;
import gg.supervisor.menu.guis.impl.PersonalizedMenu;
import gg.supervisor.menu.builder.ItemBuilder;
import gg.supervisor.util.chat.Text;
import gg.supervisor.util.prompt.ChatPromptService;
import me.whitehatd.aquila.queue.QueuePlugin;
import me.whitehatd.aquila.queue.bridge.gamemode.Gamemode;
import me.whitehatd.aquila.queue.commands.GamemodeCommand;
import me.whitehatd.aquila.queue.manager.GamemodeManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;

public class GamemodeAdminMenu extends PersonalizedMenu {

    private final GamemodeManager gamemodeManager;
    private final Player player;

    public GamemodeAdminMenu(Player player) {
        super(player, 4, Text.translate("&5Gamemode Administration"), InteractionModifier.VALUES);
        this.player = player;
        this.gamemodeManager = Services.loadIfPresent(GamemodeManager.class);
        
        getDecorator().decorate(new SchemaBuilder()
                .add("RRRRRRRRR")
                .add("RRRRRRRRR")
                .add("RRRRRRRRR")
                .add("bbbabdbnc")
                .build());

        populateGamemodes();
    }

    private void populateGamemodes() {
        List<Gamemode> gamemodes = gamemodeManager.getAllGamemodes().stream()
                .sorted(Comparator.comparing(Gamemode::getName))
                .toList();

        Pager pager = new Pager(this, 'R').endless(Pager.EndlessType.SIMPLE);
        
        for (Gamemode gamemode : gamemodes) {
            Material material = gamemode.isEnabled() ? Material.LIME_WOOL : Material.RED_WOOL;
            
            pager.add(ItemBuilder.from(material)
                    .name("&a" + gamemode.getName())
                    .lore(List.of(
                        Text.translate("&7Status: " + (gamemode.isEnabled() ? "&aEnabled" : "&cDisabled")),
                        Text.translate("&7Match Types: &e" + gamemode.getAvailableMatchTypes().size()),
                        Text.translate(""),
                        Text.translate("&eClick to edit this gamemode")
                    ))
                    .menuItem(event -> {
                        new GamemodeEditMenu(player, gamemode).open();
                    }));
        }

        getDecorator().add('a', ItemBuilder.from(Material.ARROW)
                .name("&aPrevious Page")
                .menuItem(event -> {
                    if(pager.hasPreviousPage())pager.previous();
                }));

        getDecorator().add('d', ItemBuilder.from(Material.ARROW)
                .name("&aNext Page")
                .menuItem(event -> {
                    if(pager.hasNextPage())pager.next();
                }));

        getDecorator().add('n', ItemBuilder.from(Material.EMERALD)
                .name("&aCreate New Gamemode")
                .lore(List.of(
                    Text.translate("&7Click to create a new gamemode"),
                    Text.translate("&7You will be prompted to enter a name")
                ))
                .menuItem(event -> {
                    close(player);
                    player.sendMessage(Text.translate("&7Please enter the name of the new gamemode:"));

                    ChatPromptService chatPromptService = Services.loadIfPresent(ChatPromptService.class);
                    chatPromptService.create(player.getUniqueId(), 30.0, rankName -> {
                        GamemodeCommand command = Services.loadIfPresent(GamemodeCommand.class);
                        command.create(player, rankName);

                        Bukkit.getScheduler().runTask(Services.loadIfPresent(QueuePlugin.class), () -> {
                            new GamemodeAdminMenu(player).open();
                        });
                    });
                }));

        getDecorator().add('c', ItemBuilder.from(Material.BARRIER)
                .name("&cClose")
                .menuItem(event -> close(player)));
    }
}