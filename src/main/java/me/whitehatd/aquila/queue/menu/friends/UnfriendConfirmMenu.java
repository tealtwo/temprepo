package me.whitehatd.aquila.queue.menu.friends;

import gg.supervisor.core.util.Services;
import gg.supervisor.menu.entities.InteractionModifier;
import gg.supervisor.menu.guis.builder.SchemaBuilder;
import gg.supervisor.menu.guis.impl.PersonalizedMenu;
import gg.supervisor.menu.builder.ItemBuilder;
import gg.supervisor.util.chat.Text;
import me.whitehatd.aquila.queue.bridge.player.Friend;
import me.whitehatd.aquila.queue.bridge.player.PlayerData;
import me.whitehatd.aquila.queue.bridge.player.PlayerRepository;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class UnfriendConfirmMenu extends PersonalizedMenu {

    private final Player player;
    private final PlayerData playerData, friendData;
    private final Friend friend;

    public UnfriendConfirmMenu(Player player, PlayerData playerData, PlayerData friendData, Friend friend) {
        super(player, 3, Text.translate("&5Confirm Unfriend"), InteractionModifier.VALUES);
        this.player = player;
        this.playerData = playerData;
        this.friendData = friendData;
        this.friend = friend;

        getDecorator().decorate(new SchemaBuilder()
                .add("    h    ")
                .add("  c   x  ")
                .add("         ")
                .build());

        populateMenu();
    }

    private void populateMenu() {
        OfflinePlayer offlineFriend = Bukkit.getOfflinePlayer(UUID.fromString(friend.getFriendId()));
        String friendName = offlineFriend.getName() != null ? offlineFriend.getName() : "Unknown Player";

        // Player head with confirmation message
        getDecorator().add('h', ItemBuilder.from(Material.PLAYER_HEAD)
                .name("&eUnfriend " + friendName + "?")
                .lore(List.of(
                        Text.translate("&7Are you sure you want to remove"),
                        Text.translate("&7this player from your friends list?"),
                        Text.translate(""),
                        Text.translate("&cThis action cannot be undone.")
                ))
                .build());

        // Confirm button
        getDecorator().add('c', ItemBuilder.from(Material.LIME_WOOL)
                .name("&aConfirm")
                .lore(List.of(Text.translate("&7Click to remove this friend")))
                .menuItem(event -> {
                    // Remove from friend list
                    playerData.getFriends().removeIf(f -> f.getFriendId().equals(friend.getFriendId()));
                    friendData.getFriends().removeIf(f -> f.getFriendId().equals(player.getUniqueId().toString()));


                    // Save to database
                    PlayerRepository playerRepository = Services.loadIfPresent(PlayerRepository.class);
                    playerRepository.save(player.getUniqueId().toString(), playerData);
                    playerRepository.save(friend.getFriendId(), friendData);

                    // Notify player
                    player.sendMessage(Text.translate("&c" + friendName + " has been removed from your friends list."));

                    // Go back to the friends menu
                    new FriendsMenu(player).open();
                }));

        // Cancel button
        getDecorator().add('x', ItemBuilder.from(Material.RED_WOOL)
                .name("&cCancel")
                .lore(List.of(Text.translate("&7Click to cancel")))
                .menuItem(event -> {
                    // Just go back to the friends menu
                    new FriendsMenu(player).open();
                }));
    }
}