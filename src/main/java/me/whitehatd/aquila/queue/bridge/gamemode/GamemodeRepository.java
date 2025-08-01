package me.whitehatd.aquila.queue.bridge.gamemode;

import gg.supervisor.core.annotation.Component;
import gg.supervisor.core.repository.sql.SQLRepository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for accessing and persisting Gamemode entities.
 */
@Component
public interface GamemodeRepository extends SQLRepository<Gamemode> {
}