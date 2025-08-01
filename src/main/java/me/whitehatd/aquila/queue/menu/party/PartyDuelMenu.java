package me.whitehatd.aquila.queue.menu.party;

import gg.supervisor.core.loader.SupervisorLoader;
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
import me.whitehatd.aquila.queue.bridge.party.Party;
import me.whitehatd.aquila.queue.bridge.party.PartyManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class PartyDuelMenu extends PersonalizedMenu {

    private final Player player;
    private final Party challengerParty;
    private final Party challengedParty;
    private final PartyManager partyManager;
    private final GamemodeManager gamemodeManager;
    private final QueuePlugin plugin;

    public PartyDuelMenu(Player player, Party challengerParty, Party challengedParty) {
        super(player, 6, Text.translate("&5Select Gamemode for Party Duel"), InteractionModifier.VALUES);
        this.player = player;
        this.challengerParty = challengerParty;
        this.challengedParty = challengedParty;
        this.partyManager = Services.loadIfPresent(PartyManager.class);
        this.gamemodeManager = Services.loadIfPresent(GamemodeManager.class);
        this.plugin = Services.loadIfPresent(QueuePlugin.class);

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
                .collect(Collectors.toList());

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
            lore.add("");
            lore.add("&eClick to select!");

            item.name(Text.translate("&a" + gamemode.getName()))
                    .lore(lore.stream().map(Text::translate).toList());

            pager.add(item.menuItem(event -> {
                // Create a duel invitation.
                partyManager.setDuelInvite(challengedParty.getLeader(),
                        new PartyManager.DuelInvite(challengerParty.getLeader(), gamemode.getId()));
                // Send a clickable message to the challenged party leader.
                Player challengedLeader = Bukkit.getPlayer(UUID.fromString(challengedParty.getLeader()));
                if (challengedLeader != null) {
                    String cmd = "/party accept " + gamemode.getId();

                    TextComponent acceptButton = Component.text(" [Accept]")
                            .color(NamedTextColor.GREEN)
                            .decorate(TextDecoration.BOLD)
                            .clickEvent(ClickEvent.runCommand(cmd));

                    TextComponent message = Component.text(player.getName() + " " +
                                    "has challenged your party to a duel on " + gamemode.getName() + "!")
                            .color(NamedTextColor.YELLOW)
                            .append(acceptButton);

                    challengedLeader.sendMessage(message);

                    close(player);
                    player.sendMessage(Text.translate("&aDuel invitation sent to " + challengedLeader.getName()));
                }
            }));
        }

        getDecorator().set(' ', ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).menuItem());

        // Previous page button
        getDecorator().add('<', ItemBuilder.from(Material.ARROW)
                .name(Text.translate("&aPrevious Page"))
                .menuItem(event -> pager.previous()));

        // Next page button
        getDecorator().add('>', ItemBuilder.from(Material.ARROW)
                .name(Text.translate("&aNext Page"))
                .menuItem(event -> pager.next()));

        // Close button
        getDecorator().add('p', ItemBuilder.from(Material.BARRIER)
                .name(Text.translate("&cClose"))
                .menuItem(event -> close(player)));
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
