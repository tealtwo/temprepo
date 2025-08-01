package me.whitehatd.aquila.queue.menu.spectate;

import gg.supervisor.core.util.Services;
import gg.supervisor.menu.entities.InteractionModifier;
import gg.supervisor.menu.guis.Pager;
import gg.supervisor.menu.guis.builder.SchemaBuilder;
import gg.supervisor.menu.guis.impl.PersonalizedMenu;
import gg.supervisor.menu.builder.ItemBuilder;
import gg.supervisor.util.chat.Text;
import me.whitehatd.aquila.queue.QueuePlugin;
import me.whitehatd.aquila.queue.util.SpectateUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.stream.Collectors;

public class SpectateMenu extends PersonalizedMenu {

    private BukkitTask refreshTask;
    private QueuePlugin plugin;

    public SpectateMenu(Player player, QueuePlugin plugin) {
        super(player, 6, Text.translate("&5Spectate Matches"), InteractionModifier.VALUES);
        this.plugin = plugin;


        getDecorator().decorate(new SchemaBuilder()
                .add("x        ")
                .add(" RRRRRRR ")
                .add(" RRRRRRR ")
                .add(" RRRRRRR ")
                .add("  <   >  ")
                .add("         ")
                .build());
        populateMenu();
        startRefreshTask();
    }

    private void startRefreshTask() {

        refreshTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
                plugin,
                () -> {
                    List<SpectateEntry> entries = Services.loadIfPresent(SpectateUtil.class).buildSpectateEntries();

                    Bukkit.getScheduler().runTask(plugin, () -> updateMenu(entries));
                },
                5L,
                5L
        );
    }

    private void updateMenu(List<SpectateEntry> entries) {
        Pager pager = new Pager(this, 'R').endless(Pager.EndlessType.SIMPLE);
        clearGui();
        if (entries.isEmpty()) {
            getDecorator().set('x', ItemBuilder.from(Material.BARRIER)
                    .name(Text.translate("&cNo players available for spectating"))
                    .menuItem());
            return;
        }

        for (SpectateEntry entry : entries) {
            pager.add(ItemBuilder.from(Material.PLAYER_HEAD)
                    .name("&a" + entry.getPlayerName())
                    .lore(entry.getLore().stream().map(Text::translate).collect(Collectors.toList()))
                    .menuItem(event -> {
                        close(player);
                        Services.loadIfPresent(SpectateUtil.class).sendSpectateRequest(player, entry.getMatchId(), false);

                        player.sendMessage(Text.translate("&aAttempting to spectate &e" + entry.getPlayerName() + "'s&a match..."));
                    }));
        }

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

    private void populateMenu() {
        List<SpectateEntry> entries = Services.loadIfPresent(SpectateUtil.class).buildSpectateEntries();
        updateMenu(entries);
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
}
