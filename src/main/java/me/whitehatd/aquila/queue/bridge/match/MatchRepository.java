package me.whitehatd.aquila.queue.bridge.match;

import gg.supervisor.core.annotation.Component;
import gg.supervisor.core.repository.sql.SQLRepository;

/**
 * Repository interface for persisting Match entities.
 * Supervisor will generate the implementation using your SQLStore.
 */
@Component
public interface MatchRepository extends SQLRepository<Match> {
}
