package me.whitehatd.aquila.queue.menu.gamemode;

import gg.supervisor.core.loader.SupervisorLoader;
import gg.supervisor.core.util.Services;
import gg.supervisor.menu.entities.InteractionModifier;
import gg.supervisor.menu.guis.impl.PersonalizedMenu;
import gg.supervisor.menu.builder.ItemBuilder;
import gg.supervisor.util.chat.Text;
import gg.supervisor.util.prompt.ChatPromptService;
import me.whitehatd.aquila.queue.QueuePlugin;
import me.whitehatd.aquila.queue.bridge.gamemode.Gamemode;
import me.whitehatd.aquila.queue.bridge.match.MatchType;
import me.whitehatd.aquila.queue.manager.GamemodeManager;
import me.whitehatd.aquila.queue.menu.queue.GamemodeMenu;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GamemodeEditMenu extends PersonalizedMenu {

    private final GamemodeManager gamemodeManager;
    private final Gamemode gamemode;
    private final Player player;

    public GamemodeEditMenu(Player player, Gamemode gamemode) {
        super(player, 3, Text.translate("&5Edit: " + gamemode.getName()), InteractionModifier.VALUES);

        this.player = player;
        this.gamemode = gamemode;
        this.gamemodeManager = Services.loadIfPresent(GamemodeManager.class);

        setupMenu();
    }

    private void setupMenu() {
        clearGui();
        // Toggle enabled/disabled
        setItem(1, 3, ItemBuilder.from(gamemode.isEnabled() ? Material.LIME_DYE : Material.GRAY_DYE)
                .name(gamemode.isEnabled() ? "&aEnabled" : "&cDisabled")
                .lore(Arrays.asList(
                        Text.translate("&7This gamemode is currently"),
                        Text.translate(gamemode.isEnabled() ? "&aEnabled" : "&cDisabled"),
                        Text.translate("&7Click to toggle")))
                .menuItem(event -> {
                    if(!gamemode.isEnabled())
                        if(gamemode.getArenas().isEmpty()) {
                            player.sendMessage(Text.translate("&cYou must select at least one available arena before enabling this game mode."));
                            return;
                        }

                    gamemode.setEnabled(!gamemode.isEnabled());
                    gamemodeManager.updateGamemode(gamemode);
                    setupMenu(); // Refresh the menu
                }));

        // Edit loadout
        setItem(1, 5, ItemBuilder.from(Material.CHEST)
                .name("&6Edit Loadout")
                .lore(Arrays.asList(
                        Text.translate("&7Configure the items that"),
                        Text.translate("&7players will receive")))
                .menuItem(event -> {
                    close(player);
                    new GamemodeLoadoutMenu(player, gamemode).open();
                }));

        // Match types
        setItem(1, 7, ItemBuilder.from(Material.DIAMOND_SWORD)
                .name("&6Available Match Types")
                .lore(getMatchTypeLore())
                .menuItem(event -> {
                    toggleNextMatchType();
                    gamemodeManager.updateGamemode(gamemode);
                    setupMenu(); // Refresh the menu
                }));

        ItemStack iconItem;
            try {
                if(gamemode.getIconJson() != null)
                    iconItem = SupervisorLoader.GSON.fromJson(
                            gamemode.getIconJson(),
                            ItemStack.class);
                else iconItem = new ItemStack(Material.BOOK);
            } catch (Exception e) {
                iconItem = new ItemStack(Material.BOOK);
            }

        setItem(2, 3, ItemBuilder.from(iconItem)
                .name("&6Set Gamemode Icon")
                .lore(Arrays.asList(
                        Text.translate("&7Current icon: &e" + iconItem.getType().name()),
                        Text.translate("&7Click to set the item in your"),
                        Text.translate("&7main hand as the gamemode icon")))
                .menuItem(event -> {
                    ItemStack handItem = player.getInventory().getItemInMainHand();
                    if (handItem.getType() != Material.AIR) {

                        String iconJson = SupervisorLoader.GSON.toJson(handItem, ItemStack.class);
                        gamemode.setIconJson(iconJson);

                        gamemodeManager.updateGamemode(gamemode);
                        player.sendMessage(Text.translate("&aGamemode icon updated!"));
                        setupMenu(); // Refresh the menu
                    } else {
                        player.sendMessage(Text.translate("&cPlease hold an item in your main hand!"));
                    }
                }));

        String currentDesc = gamemode.getDescription();
        setItem(2, 5, ItemBuilder.from(Material.WRITABLE_BOOK)
                .name("&6Set Description")
                .lore(getDescriptionLore(currentDesc))
                .menuItem(event -> {
                    // Close menu first
                    close(player);

                    // Use ChatPromptService to get input
                    player.sendMessage(Text.translate("&aEnter a description for this gamemode (or type 'cancel' to abort):"));
                    ChatPromptService chatPromptService = Services.loadIfPresent(ChatPromptService.class);

                    chatPromptService.create(player.getUniqueId(), 30,response -> {
                        if (!response.equalsIgnoreCase("cancel")) {
                            gamemode.setDescription(response);
                            gamemodeManager.updateGamemode(gamemode);
                            player.sendMessage(Text.translate("&aDescription updated!"));
                        } else {
                            player.sendMessage(Text.translate("&cDescription update cancelled."));
                        }
                        // Reopen the menu after input
                        Bukkit.getScheduler().runTask(Services.loadIfPresent(QueuePlugin.class),
                                () -> new GamemodeEditMenu(player, gamemode).open());
                    });
                }));

        setItem(3, 5, ItemBuilder.from(Material.MAP)
                .name(Text.translate("&aManage Arenas"))
                .lore(List.of(
                        Text.translate("&7Click to select which arenas"),
                        Text.translate("&7this gamemode can be played on"),
                        Text.translate(""),
                        Text.translate("&7Current arenas: &e" + gamemode.getArenas().size())
                ))
                .menuItem(event -> new GamemodeArenaMenu(player, gamemode).open()));

        // Delete gamemode
        setItem(3, 3, ItemBuilder.from(Material.REDSTONE_BLOCK)
                .name("&cDelete Gamemode")
                .lore(Arrays.asList(
                        Text.translate("&7Permanently remove this gamemode"),
                        Text.translate("&cThis action cannot be undone!")))
                .menuItem(event -> {
                    gamemodeManager.deleteGamemode(gamemode.getId());
                    close(player);
                    player.sendMessage(Text.translate("&cGamemode deleted successfully."));
                }));

        // Back button
        setItem(3, 9, ItemBuilder.from(Material.BARRIER)
                .name("&cBack")
                .lore(Collections.singletonList(
                        Text.translate("&7Return to the previous menu")))
                .menuItem(event -> {
                    new GamemodeAdminMenu(player).open();
                }));
    }

    private List<Component> getMatchTypeLore() {
        List<String> lore = new ArrayList<>();
        lore.add("&7Available match types:");

        for (MatchType type : MatchType.values()) {
            boolean enabled = gamemode.getAvailableMatchTypes().contains(type);
            lore.add((enabled ? "&a✓ " : "&c✗ ") + formatMatchTypeName(type));
        }

        lore.add("");
        lore.add("&eClick to toggle match types");

        return lore.stream().map(Text::translate).toList();
    }

    private List<Component> getDescriptionLore(String description) {
        List<String> lore = new ArrayList<>();
        lore.add("&7Current description:");

        // Split description into lines of max 30 chars
        List<String> descLines = splitDescription(description, 30);
        for (String line : descLines) {
            lore.add("&f" + line);
        }

        lore.add("");
        lore.add("&eClick to change description");

        return lore.stream().map(Text::translate).toList();
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

    private String formatMatchTypeName(MatchType matchType) {
        return matchType.name().replace("_", " ");
    }

    private void toggleNextMatchType() {
        // Simple toggle - if all are enabled, disable all except 1v1
        // If only some are enabled, enable all
        boolean allEnabled = true;

        for (MatchType type : MatchType.values()) {
            if (!gamemode.getAvailableMatchTypes().contains(type)) {
                allEnabled = false;
                break;
            }
        }

        if (allEnabled) {
            // Disable all except 1v1
            gamemode.getAvailableMatchTypes().clear();
            gamemode.getAvailableMatchTypes().add(MatchType.ONE_VS_ONE);
        } else {
            // Enable all match types
            gamemode.getAvailableMatchTypes().clear();
            gamemode.getAvailableMatchTypes().addAll(Arrays.asList(MatchType.values()));
        }
    }
}