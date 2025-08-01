package me.whitehatd.aquila.queue.manager.tournament;

import gg.supervisor.core.annotation.Component;
import gg.supervisor.core.util.Services;
import me.whitehatd.aquila.queue.QueuePlugin;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Component
public class CountdownService {
    private final QueuePlugin plugin;
    private final Map<String, BukkitTask> tasks = new ConcurrentHashMap<>();
    private final Map<String, Runnable> cancelCallbacks = new ConcurrentHashMap<>();

    public CountdownService(QueuePlugin plugin) {
        this.plugin = plugin;
    }

    public void startCountdown(String tournamentId,
                               Consumer<Integer> onTick,
                               Runnable onComplete,
                               Runnable onCancel) {
        final int[] ticks = {100};
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (ticks[0] <= 0) {
                // Countdown finished
                Bukkit.getScheduler().runTask(plugin, onComplete);
                tasks.remove(tournamentId).cancel();
                cancelCallbacks.remove(tournamentId);
                return;
            }
            if (ticks[0] % 20 == 0) {
                onTick.accept(ticks[0] / 20);
            }
            ticks[0] -= 1;
        }, 0L, 1L);
        tasks.put(tournamentId, task);
        cancelCallbacks.put(tournamentId, onCancel);
    }

    public void cancelCountdown(String tournamentId) {
        BukkitTask t = tasks.remove(tournamentId);
        Runnable callback = cancelCallbacks.remove(tournamentId);
        if (t != null) {
            t.cancel();
            if (callback != null) {
                // Invoke cancel callback on main thread
                Bukkit.getScheduler().runTask(plugin, callback);
            }
        }
    }
}
