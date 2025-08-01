package me.whitehatd.aquila.queue.menu.tournament.admin;

import gg.supervisor.core.loader.SupervisorLoader;
import gg.supervisor.core.util.Services;
import gg.supervisor.menu.builder.ItemBuilder;
import gg.supervisor.menu.entities.InteractionModifier;
import gg.supervisor.menu.guis.Pager;
import gg.supervisor.menu.guis.builder.SchemaBuilder;
import gg.supervisor.menu.guis.impl.PersonalizedMenu;
import gg.supervisor.util.chat.Text;
import me.whitehatd.aquila.queue.bridge.gamemode.Gamemode;
import me.whitehatd.aquila.queue.bridge.match.MatchType;
import me.whitehatd.aquila.queue.manager.GamemodeManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TournamentGamemodeMenu extends PersonalizedMenu {

    private final Player player;
    private final int numberOfTeams;
    private final MatchType matchType;
    private final GamemodeManager gamemodeManager;

    public TournamentGamemodeMenu(Player player, int numberOfTeams, MatchType matchType) {
        super(player, 6, Text.translate("&5Select Gamemode for Tournament"), InteractionModifier.VALUES);
        this.player = player;
        this.numberOfTeams = numberOfTeams;
        this.matchType = matchType;
        this.gamemodeManager = Services.loadIfPresent(GamemodeManager.class);

        getDecorator().decorate(new SchemaBuilder()
                .add("         ")
                .add(" RRRRRRR ")
                .add(" RRRRRRR ")
                .add(" RRRRRRR ")
                .add("  < c >  ")
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
            // Build icon
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

            // Build lore
            List<String> lore = new ArrayList<>();
            String description = gamemode.getDescription();
            if (description != null && !description.isEmpty()) {
                lore.addAll(splitDescription(description, 30));
            }
            lore.add("");
            lore.add("&7Teams: &e" + numberOfTeams);
            lore.add("&7Match Type: &e" + matchType);
            lore.add("&7Team Size: &e" + matchType.getSize());
            lore.add("&eClick to select!");

            item.name(Text.translate("&a" + gamemode.getName()))
                .lore(lore.stream().map(Text::translate).toList());

            pager.add(item.menuItem(event -> {
                // Once they click this gamemode, open a confirmation menu:
                new TournamentConfirmationMenu(
                        player, gamemode, matchType, numberOfTeams
                ).open();
            }));
        }

        // Pager controls
        getDecorator().add('<', ItemBuilder.from(Material.ARROW)
                .name(Text.translate("&aPrevious Page"))
                .menuItem(e -> pager.previous()));
        getDecorator().add('>', ItemBuilder.from(Material.ARROW)
                .name(Text.translate("&aNext Page"))
                .menuItem(e -> pager.next()));
        getDecorator().add('c', ItemBuilder.from(Material.BARRIER)
                .name(Text.translate("&cClose"))
                .menuItem(e -> close(player)));

        getDecorator().set(' ', ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).menuItem());
    }

    private List<String> splitDescription(String text, int maxChars) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder sb = new StringBuilder();

        for (String word : words) {
            if (sb.length() + word.length() + 1 > maxChars) {
                lines.add("&f" + sb.toString());
                sb = new StringBuilder(word);
            } else {
                if (sb.length() > 0) sb.append(" ");
                sb.append(word);
            }
        }
        if (sb.length() > 0) {
            lines.add("&f" + sb.toString());
        }
        return lines;
    }
}
