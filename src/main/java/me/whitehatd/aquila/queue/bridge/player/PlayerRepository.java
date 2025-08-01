package me.whitehatd.aquila.queue.bridge.player;

import gg.supervisor.core.annotation.Component;
import gg.supervisor.core.repository.sql.SQLRepository;

/**
 * This interface marks the PlayerRepository as a SQL-backed repository.
 */
@Component
public interface PlayerRepository extends SQLRepository<PlayerData> { }
