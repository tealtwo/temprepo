package me.whitehatd.aquila.queue.objects.report;

import gg.supervisor.core.annotation.Component;
import gg.supervisor.core.repository.sql.SQLRepository;

@Component
public interface ReportRepository extends SQLRepository<Report> {
}
