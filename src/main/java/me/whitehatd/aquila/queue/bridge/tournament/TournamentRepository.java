package me.whitehatd.aquila.queue.bridge.tournament;

import gg.supervisor.core.annotation.Component;
import gg.supervisor.core.repository.sql.SQLRepository;

@Component
public interface TournamentRepository extends SQLRepository<Tournament> {
}
