package me.whitehatd.aquila.queue.menu.panel.loadout;

import gg.supervisor.core.loader.SupervisorLoader;
import gg.supervisor.core.util.Services;
import gg.supervisor.menu.entities.InteractionModifier;
import gg.supervisor.menu.guis.Pager;
import gg.supervisor.menu.guis.builder.SchemaBuilder;
import gg.supervisor.menu.guis.impl.PersonalizedMenu;
import gg.supervisor.menu.builder.ItemBuilder;
import gg.supervisor.util.chat.Text;
import me.whitehatd.aquila.queue.bridge.gamemode.Gamemode;
import me.whitehatd.aquila.queue.manager.GamemodeManager;
import me.whitehatd.aquila.queue.menu.panel.PanelMenu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class LoadoutGamemodeMenu extends PersonalizedMenu {

    private final GamemodeManager gamemodeManager;
    private final Player player;

    public LoadoutGamemodeMenu(Player player) {
        super(player, 6, Text.translate("&5Select Gamemode for Loadout"), InteractionModifier.VALUES);
        this.player = player;
        this.gamemodeManager = Services.loadIfPresent(GamemodeManager.class);

        getDecorator().decorate(new SchemaBuilder()
                .add("         ")
                .add(" RRRRRRR ")
                .add(" RRRRRRR ")
                .add(" RRRRRRR ")
                .add("  a b c  ")
                .add("         ")
                .build());

        populateGamemodes();
    }

    private void populateGamemodes() {
        List<Gamemode> gamemodes = gamemodeManager.getAllGamemodes().stream()
                .filter(Gamemode::isEnabled)
                .sorted(Comparator.comparing(Gamemode::getName))
                .toList();

        Pager pager = new Pager(this, 'R').endless(Pager.EndlessType.SIMPLE);
        
        for (Gamemode gamemode : gamemodes) {
            ItemBuilder item;
            try {
                ItemStack iconItem = SupervisorLoader.GSON.fromJson(
                        gamemode.getIconJson(),
                        ItemStack.class
                );
                if (iconItem != null) {
                    item = ItemBuilder.from(iconItem);
                } else {
                    item = ItemBuilder.from(Material.BOOK);
                }
            } catch (Exception e) {
                item = ItemBuilder.from(Material.BOOK);
            }


            List<String> lore = new ArrayList<>();
            // Add description if available
            String description = gamemode.getDescription();
            if (description != null && !description.isEmpty()) {
                lore.add("");
                // Split description into lines of max 30 chars
                List<String> descLines = splitDescription(description, 30);
                for (String line : descLines) {
                    lore.add("&f" + line);
                }
            }

            lore.add("");
            lore.add("&eClick to edit your loadout!");

            item.name(Text.translate("&a" + gamemode.getName()))
                    .lore(lore.stream().map(Text::translate).toList());
            
            pager.add(item.menuItem(event -> {
                new LoadoutEditorMenu(player, gamemode).open();
            }));
        }

        getDecorator().set(' ', ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).menuItem());

        // Previous page button
        getDecorator().add('a', ItemBuilder.from(Material.ARROW)
                .name(Text.translate("&aPrevious Page"))
                .menuItem(event -> pager.previous()));

        // Next page button
        getDecorator().add('c', ItemBuilder.from(Material.ARROW)
                .name(Text.translate("&aNext Page"))
                .menuItem(event -> pager.next()));

        // Back button
        getDecorator().add('b', ItemBuilder.from(Material.BARRIER)
                .name(Text.translate("&cBack"))
                .menuItem(event -> new PanelMenu(player).open()));
    }

    private List<String> splitDescription(String description, int maxCharsPerLine) {
        List<String> lines = new ArrayList<>();

        if (description == null || description.isEmpty()) {
            lines.add("No description provided");
            return lines;
        }

        String[] words = description.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            if (currentLine.length() + word.length() + 1 <= maxCharsPerLine) {
                if (currentLine.length() > 0) {
                    currentLine.append(" ");
                }
                currentLine.append(word);
            } else {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder(word);
            }
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        return lines;
    }
}