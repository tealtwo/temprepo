package me.whitehatd.aquila.queue.menu.panel.stats;

import gg.supervisor.menu.entities.InteractionModifier;
import gg.supervisor.menu.guis.builder.SchemaBuilder;
import gg.supervisor.menu.guis.impl.PersonalizedMenu;
import gg.supervisor.menu.builder.ItemBuilder;
import gg.supervisor.util.chat.Text;
import me.whitehatd.aquila.queue.menu.panel.PanelMenu;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

public class StatisticsMenu extends PersonalizedMenu {

    private final Player player;

    public StatisticsMenu(Player player) {
        super(player, 5, Text.translate("&5Player Statistics"), InteractionModifier.VALUES);
        this.player = player;

        getDecorator().decorate(new SchemaBuilder()
                .add("         ")
                .add("   1 2   ")
                .add("         ")
                .add("    c    ")
                .add("         ")
                .build());

        populateMenu();
    }

    private void populateMenu() {
        getDecorator().set(' ', ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).menuItem());

        getDecorator().add('1', ItemBuilder.from(Material.ARMOR_STAND)
                .name("&aGlobal Statistics")
                .lore(List.of(
                        Text.translate("&7View your overall statistics"),
                        Text.translate("&7across all gamemodes")
                ))
                .menuItem(event -> {
                    close(player);
                    new GlobalStatisticsMenu(player).open();
                }));

        getDecorator().add('2', ItemBuilder.from(Material.BOOK)
                .name("&aMatch History")
                .lore(List.of(
                        Text.translate("&7View your past matches"),
                        Text.translate("&7and their outcomes")
                ))
                .menuItem(event -> {
                    close(player);
                    new MatchHistoryMenu(player).open();
                }));

        // Close button
        getDecorator().add('c', ItemBuilder.from(Material.BARRIER)
                .name("&cBack")
                .menuItem(event -> new PanelMenu(player).open()));
    }
}