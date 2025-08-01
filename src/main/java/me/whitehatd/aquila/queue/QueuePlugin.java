package me.whitehatd.aquila.queue;

import co.aikar.commands.PaperCommandManager;
import com.google.common.collect.ImmutableList;
import gg.supervisor.core.annotation.Component;
import gg.supervisor.core.loader.SupervisorLoader;
import gg.supervisor.core.repository.sql.DatabaseConfig;
import gg.supervisor.core.util.Services;
import me.whitehatd.aquila.queue.bridge.player.PlayerRepository;
import me.whitehatd.aquila.queue.bridge.tournament.TournamentRepository;
import me.whitehatd.aquila.queue.manager.tournament.BracketGenerator;
import me.whitehatd.aquila.queue.manager.tournament.CountdownService;
import me.whitehatd.aquila.queue.manager.tournament.TournamentManager;
import me.whitehatd.aquila.queue.manager.GamemodeManager;
import me.whitehatd.aquila.queue.manager.QueueManager;
import me.whitehatd.aquila.queue.manager.TrackingManager;
import me.whitehatd.aquila.queue.manager.tournament.TournamentService;
import me.whitehatd.aquila.queue.menu.report.ReportMenu;
import me.whitehatd.aquila.queue.redis.RedisPublisher;
import me.whitehatd.aquila.queue.redis.RedisSubscriberManager;
import me.whitehatd.aquila.queue.util.TitleUtils;
import org.bukkit.plugin.java.JavaPlugin;

@Component
public class QueuePlugin extends JavaPlugin {

    private PaperCommandManager commandManager;
    private RedisPublisher redisPublisher;
    private QueueManager queueManager;

    @Override
    public void onEnable() {
        commandManager = new PaperCommandManager(this);
        commandManager.enableUnstableAPI("help");

        commandManager.getCommandCompletions().registerCompletion("modes",
                c -> ImmutableList.of("ONE_VS_ONE", "TWO_VS_TWO", "THREE_VS_THREE", "FOUR_VS_FOUR"));
        commandManager.getCommandCompletions().registerCompletion("numbers",
                c -> {
                    ImmutableList.Builder<String> builder = ImmutableList.builder();
                    for (int i = 2; i <= 100; i += 2) {
                        builder.add(String.valueOf(i));
                    }
                    return builder.build();
                });

        DatabaseConfig config = new DatabaseConfig();
        config.setHost("172.18.0.1");
        config.setPort(3306);
        config.setDatabase("data");
        config.setUsername("plugin_user");
        config.setPassword("AVeryPowerfulPassword12345!@$0938");
        //config.setHost("localhost");
        //config.setUsername("root");
        //config.setPassword("Password123456");

        Services.register(DatabaseConfig.class, config);


        redisPublisher = new RedisPublisher("172.18.0.1", 6379, "AquilaRedisSomething1337%!)");
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");


        SupervisorLoader.register(this, commandManager, redisPublisher);

        Services.loadIfPresent(RedisSubscriberManager.class).start();

        queueManager = new QueueManager(
                Services.loadIfPresent(PlayerRepository.class), redisPublisher);
        Services.register(QueueManager.class, queueManager);

        Services.loadIfPresent(GamemodeManager.class).loadGamemodes(queueManager);
        Services.loadIfPresent(QueueManager.class).initialize(Services.loadIfPresent(GamemodeManager.class));

        TrackingManager trackingManager = new TrackingManager(redisPublisher);
        Services.register(TrackingManager.class, trackingManager);

        TournamentManager tournamentManager = new TournamentManager(redisPublisher,
                Services.loadIfPresent(TournamentRepository.class));

        Services.register(TournamentManager.class, tournamentManager);

        TournamentService service = new TournamentService(
                Services.loadIfPresent(CountdownService.class),
                Services.loadIfPresent(BracketGenerator.class),
                Services.loadIfPresent(TitleUtils.class),
                tournamentManager, redisPublisher);

        Services.register(TournamentService.class, service);


        getLogger().info("Queue Plugin enabled.");
    }

    @Override
    public void onDisable() {

        if (queueManager != null) {
            queueManager.shutdown();
        }

        Services.loadIfPresent(GamemodeManager.class).shutdown();
        Services.loadIfPresent(RedisSubscriberManager.class).shutdown();

        Services.loadIfPresent(ReportMenu.class).cancelTask();
        Services.loadIfPresent(TrackingManager.class).stop();

        Services.loadIfPresent(TournamentManager.class).shutdown();

        redisPublisher.shutdown();

        getLogger().info("Queue Plugin disabled.");
    }
}
