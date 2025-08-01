package me.whitehatd.aquila.queue.menu.friends;

import gg.supervisor.core.util.Services;
import gg.supervisor.menu.entities.InteractionModifier;
import gg.supervisor.menu.guis.Pager;
import gg.supervisor.menu.guis.builder.SchemaBuilder;
import gg.supervisor.menu.guis.impl.PersonalizedMenu;
import gg.supervisor.menu.builder.ItemBuilder;
import gg.supervisor.menu.item.MenuItem;
import gg.supervisor.util.chat.Text;
import gg.supervisor.util.prompt.ChatPromptService;
import me.whitehatd.aquila.queue.QueuePlugin;
import me.whitehatd.aquila.queue.bridge.player.Friend;
import me.whitehatd.aquila.queue.bridge.player.PlayerData;
import me.whitehatd.aquila.queue.bridge.player.PlayerRepository;
import me.whitehatd.aquila.queue.commands.PartyCommand;
import me.whitehatd.aquila.queue.util.SpectateUtil;
import me.whitehatd.aquila.queue.redis.RedisPublisher;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import redis.clients.jedis.Jedis;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FriendsMenu extends PersonalizedMenu {

    private final Player player;
    private BukkitTask refreshTask;

    public FriendsMenu(Player player) {
        super(player, 6, Text.translate("&5Your Friends"), InteractionModifier.VALUES);
        this.player = player;

        getDecorator().decorate(new SchemaBuilder()
                .add("x        ")
                .add(" RRRRRRR ")
                .add(" RRRRRRR ")
                .add(" RRRRRRR ")
                .add("  <   >  ")
                .add("    a    ")
                .build());

        startRefreshTask();
    }

    private void startRefreshTask() {

        refreshTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
                Services.loadIfPresent(QueuePlugin.class),
                () -> {

                    PlayerRepository repository = Services.loadIfPresent(PlayerRepository.class);
                    PlayerData playerData = repository.find(player.getUniqueId().toString());

                    List<MenuItem> entries = new ArrayList<>();

                    if(playerData.getFriends() == null) {
                        playerData.setFriends(new ArrayList<>());
                        repository.save(player.getUniqueId().toString(), playerData);
                    }


                    for (Friend friend : playerData.getFriends()) {
                        PlayerData friendData = repository.find(friend.getFriendId());

                        OfflinePlayer offlineFriend = Bukkit.getOfflinePlayer(UUID.fromString(friend.getFriendId()));
                        String friendName = offlineFriend.getName() != null ? offlineFriend.getName() : "Unknown Player";

                        List<String> lore = new ArrayList<>();
                        lore.add("&7Friends since: &e" + formatTimestamp(friend.getFriendsSince()));
                        lore.add("");

                        ItemStack head;

                        if (offlineFriend.isOnline()) {
                            head = new ItemStack(Material.PLAYER_HEAD);
                            lore.add("&aAvailable");
                            lore.add("");
                            lore.add("&eLeft Click &7to invite to party");
                        } else {
                            if (friendData.isOnArena()) {
                                head = new ItemStack(Material.CREEPER_HEAD);
                                lore.add("&6In a match");
                                lore.add("");
                                lore.add("&eLeft Click &7to spectate");
                            } else {
                                head = new ItemStack(Material.SKELETON_SKULL);
                                lore.add("&cOffline");
                                lore.add("");
                                lore.add("&7Last seen: &e" + formatTimestamp(friendData.getLastSeen()));
                            }
                        }

                        lore.add("&eRight Click &7to unfriend");

                        entries.add(ItemBuilder.from(head)
                                .name("&b" + friendName)
                                .lore(lore.stream().map(Text::translate).toList())
                                .menuItem(event -> {
                                    if (event.getClick() == ClickType.LEFT) {

                                        if (offlineFriend.isOnline()) {
                                            PartyCommand partyCommand = Services.loadIfPresent(PartyCommand.class);
                                            partyCommand.invite(player, offlineFriend.getName());
                                            player.sendMessage(Text.translate("&aInvited " + friendName + " to your party!"));
                                        } else {
                                            if (friendData.isOnArena()) {
                                                SpectateUtil spectateUtil = Services.loadIfPresent(SpectateUtil.class);
                                                close(player);

                                                RedisPublisher publisher = Services.loadIfPresent(RedisPublisher.class);
                                                publisher.getExecutorService().submit(() -> {
                                                    try (Jedis jedis = publisher.getJedisPool().getResource()) {
                                                        String matchId = jedis.hget("playerMatchMap", friend.getFriendId());
                                                        if (matchId != null) {
                                                            spectateUtil.sendSpectateRequest(player, matchId, false);
                                                        } else {
                                                            player.sendMessage(Text.translate("&cThis player is not in a match."));
                                                        }
                                                    }
                                                });
                                            }
                                        }
                                    } else if (event.getClick() == ClickType.RIGHT) {
                                        // Right click - Open unfriend confirmation
                                        new UnfriendConfirmMenu(player, playerData, friendData, friend).open();
                                    }
                                }));
                    }

                    Bukkit.getScheduler().runTask(Services.loadIfPresent(QueuePlugin.class), () -> updateMenu(entries));
                },
                0L,
                5L
        );
    }

    private void updateMenu(List<MenuItem> entries) {
        Pager pager = new Pager(this, 'R').endless(Pager.EndlessType.SIMPLE);
        clearGui();

        getDecorator().set('a', ItemBuilder.from(Material.EMERALD)
                .name("&aAdd Friend")
                .lore(Stream.of(
                                "&7Click to add a friend",
                                "&7to your friends list")
                        .map(Text::translate).collect(Collectors.toList()))
                .menuItem(event -> {
                    close(player);
                    player.sendMessage(Text.translate("&ePlease type the name of the player you want to add as a friend."));

                    ChatPromptService chatPromptService = Services.loadIfPresent(ChatPromptService.class);
                    chatPromptService.create(player.getUniqueId(), 30.0, targetName -> {
                        Player target = Bukkit.getPlayerExact(targetName);
                        if (target == null) {
                            player.sendMessage(Text.translate("&cPlayer not found. They might be in a game."));
                            return;
                        }

                        if(target.getUniqueId().equals(player.getUniqueId())) {
                            player.sendMessage(Text.translate("&cYou cannot add yourself as a friend."));
                            return;
                        }

                        PlayerRepository repository = Services.loadIfPresent(PlayerRepository.class);
                        PlayerData playerData = repository.find(player.getUniqueId().toString());
                        PlayerData targetData = repository.find(target.getUniqueId().toString());

                        if(targetData.getFriendIds().contains(player.getUniqueId().toString()) ||
                                playerData.getFriendIds().contains(target.getUniqueId().toString())) {
                            player.sendMessage(Text.translate("&cYou are already friends with " + targetName));
                            return;
                        }

                        targetData.getFriends().add(
                                new Friend(player.getUniqueId().toString(), System.currentTimeMillis()));
                        playerData.getFriends().add(
                                new Friend(target.getUniqueId().toString(), System.currentTimeMillis()));

                        repository.save(player.getUniqueId().toString(), playerData);
                        repository.save(target.getUniqueId().toString(), targetData);

                        player.sendMessage(Text.translate("&aYou are now friends with &e" + targetName + "&a."));
                        target.sendMessage(Text.translate("&aYou are now friends with &e" + player.getName() + "&a."));

                        Bukkit.getScheduler().runTask(Services.loadIfPresent(QueuePlugin.class), () -> {
                            new FriendsMenu(player).open();
                        });
                    });
                }));

        if (entries.isEmpty()) {
            getDecorator().set('x', ItemBuilder.from(Material.BARRIER)
                    .name(Text.translate("&cYou have no friends!"))
                    .menuItem());
            return;
        }

        pager.add(entries);

        getDecorator().add('<', ItemBuilder.from(Material.ARROW)
                .name("&aPrevious Page")
                .menuItem(event -> {
                    pager.previous();
                }));

        getDecorator().add('>', ItemBuilder.from(Material.ARROW)
                .name("&aNext Page")
                .menuItem(event -> {
                    pager.next();
                }));

        getDecorator().set(' ', ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).menuItem());
        getDecorator().set('x', ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).menuItem());

        redraw();

    }

    private void cancelRefreshTask() {
        if (refreshTask != null && !refreshTask.isCancelled()) {
            refreshTask.cancel();
        }
    }

    @Override
    public void onClose() {
        cancelRefreshTask();
        super.onClose();
    }

    private String formatTimestamp(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a MMM dd yyyy");
        return sdf.format(new Date(timestamp));
    }
}