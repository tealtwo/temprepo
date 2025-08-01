package me.whitehatd.aquila.queue.menu.panel;

import gg.supervisor.menu.builder.SkullBuilder;
import gg.supervisor.menu.entities.InteractionModifier;
import gg.supervisor.menu.guis.builder.SchemaBuilder;
import gg.supervisor.menu.guis.impl.PersonalizedMenu;
import gg.supervisor.menu.builder.ItemBuilder;
import gg.supervisor.util.chat.Text;
import me.whitehatd.aquila.queue.menu.panel.loadout.LoadoutGamemodeMenu;
import me.whitehatd.aquila.queue.menu.panel.stats.StatisticsMenu;
import me.whitehatd.aquila.queue.menu.queue.GamemodeMenu;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

public class PanelMenu extends PersonalizedMenu {

    public PanelMenu(Player player) {
        super(player, 5, Text.translate("&5Player Panel"), InteractionModifier.VALUES);
        
        getDecorator().decorate(new SchemaBuilder()
                .add("bbbbbbbbb")
                .add("bRbGbSbQb")
                .add("bbbbbbbbb")
                .add("bbbbcbbbb")
                .add("bbbbbbbbb")
                .build());

        setupButtons();
    }

    private void setupButtons() {
        // Loadout customization button

        getDecorator().set('b', ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).menuItem());

        getDecorator().add('R', ItemBuilder.from(Material.IRON_SWORD)
                .name(Text.translate("&aLoadout Customization"))
                .lore(List.of(Text.translate("&7Click to customize your loadouts")))
                .menuItem(event -> new LoadoutGamemodeMenu(player).open()));

        // Game stats button
        getDecorator().add('G', ItemBuilder.from(Material.PLAYER_HEAD)
                .name(Text.translate("&aStatistics"))
                .lore(List.of(Text.translate("&7View your statistics")))
                .menuItem(event -> {
                    new StatisticsMenu(player).open();
                }));

        // Settings button
        getDecorator().add('S', ItemBuilder.from(Material.REDSTONE)
                .name(Text.translate("&aSettings"))
                .lore(List.of(Text.translate("&7Configure your settings")))
                .menuItem(event -> {
                    // Future implementation
                }));

        // Quick queue button
        getDecorator().add('Q', ItemBuilder.from(Material.COMPASS)
                .name(Text.translate("&aQuick Queue"))
                .lore(List.of(Text.translate("&7Join a queue quickly")))
                .menuItem(event -> new GamemodeMenu((Player) event.getWhoClicked()).open()));

        // Close button
        getDecorator().add('c', ItemBuilder.from(Material.BARRIER)
                .name(Text.translate("&cClose"))
                .menuItem(event -> close(player)));
    }
}