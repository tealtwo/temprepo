package me.whitehatd.aquila.queue.menu.report;

import gg.supervisor.core.annotation.Component;
import gg.supervisor.core.loader.SupervisorLoader;
import gg.supervisor.core.util.Services;
import gg.supervisor.menu.entities.InteractionModifier;
import gg.supervisor.menu.guis.Pager;
import gg.supervisor.menu.guis.builder.SchemaBuilder;
import gg.supervisor.menu.guis.impl.GlobalMenu;
import gg.supervisor.menu.builder.ItemBuilder;
import gg.supervisor.util.chat.Text;
import me.whitehatd.aquila.queue.QueuePlugin;
import me.whitehatd.aquila.queue.bridge.match.Match;
import me.whitehatd.aquila.queue.bridge.match.MatchRepository;
import me.whitehatd.aquila.queue.objects.report.Report;
import me.whitehatd.aquila.queue.objects.report.ReportRepository;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ReportMenu extends GlobalMenu {

    private final ReportRepository reportRepo = Services.loadIfPresent(ReportRepository.class);
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm");
    private BukkitTask refreshTask;

    public ReportMenu() {
        super(6, Text.translate("&6Match Reports"), InteractionModifier.VALUES);

        getDecorator().decorate(new SchemaBuilder()
                .add("         ")
                .add(" RRRRRRR ")
                .add(" RRRRRRR ")
                .add(" RRRRRRR ")
                .add("  <   >  ")
                .add("         ")
                .build());

        startRefreshTask();
    }


    private void startRefreshTask() {
        // Schedule an asynchronous refresh every 5 ticks
        refreshTask = Bukkit.getScheduler().runTaskTimerAsynchronously(Services.loadIfPresent(QueuePlugin.class),
                () -> {

                    List<Report> reports = reportRepo.values().stream()
                            .sorted((r1, r2) -> Long.compare(r2.getReportTime(), r1.getReportTime()))
                            .collect(Collectors.toList());
                    Bukkit.getScheduler().runTask(Services.loadIfPresent(QueuePlugin.class),
                            () -> updateMenu(reports));
                },
                2L, 20 * 5L);
    }

    private void updateMenu(List<Report> reports) {
        Pager pager = new Pager(this, 'R').endless(Pager.EndlessType.SIMPLE);
        clearGui();
        if (reports.isEmpty()) {
            getDecorator().set('x', ItemBuilder.from(Material.BARRIER)
                    .name(Text.translate("&cNo reports available"))
                    .menuItem());
        } else {
            for (Report report : reports) {
                String reportTimeStr = dateFormat.format(report.getReportTime());
                List<String> lore = new ArrayList<>(List.of(
                        "&7Reported at: &e" + reportTimeStr,
                        "&7Reporter: &e" + Bukkit.getOfflinePlayer(UUID.fromString(report.getReporterId())).getName(),
                        "&7Match ID: &e" + report.getMatchId(),
                        "",
                        "&7Reason: "

                ));

                lore.addAll(splitDescription(report.getReason(), 30));
                lore.add("");
                lore.add("&eClick to view details");

                pager.add(ItemBuilder.from(Material.WRITABLE_BOOK)
                        .name(Text.translate("&eReport"))
                        .lore(lore.stream().map(Text::translate).toList())
                        .menuItem(event -> {
                            new ReportMatchPlayersMenu(
                                    SupervisorLoader.GSON.fromJson(report.getMatchJson(), Match.class))
                                    .open(event.getWhoClicked());
                        }));
            }
        }
        getDecorator().add('<', ItemBuilder.from(Material.ARROW)
                .name(Text.translate("&aPrevious Page"))
                .menuItem(event -> pager.previous()));
        getDecorator().add('>', ItemBuilder.from(Material.ARROW)
                .name(Text.translate("&aNext Page"))
                .menuItem(event -> pager.next()));
        getDecorator().set(' ', ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).menuItem());

    }

    private List<String> splitDescription(String description, int maxCharsPerLine) {
        List<String> lines = new ArrayList<>();

        if (description == null || description.isEmpty()) {
            lines.add("No reason provided");
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

    public void cancelTask() {
        if (refreshTask != null && !refreshTask.isCancelled()) {
            refreshTask.cancel();
        }
    }
}
