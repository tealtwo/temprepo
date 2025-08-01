package me.whitehatd.aquila.queue.objects.report;

import gg.supervisor.core.annotation.Component;
import lombok.Data;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Component
public class ReportManager {
    @Getter
    private final List<Report> reports = new ArrayList<>();

    /**
     * Adds a new report.
     */
    public void addReport(Report report) {
        reports.add(report);
    }

    /**
     * Checks if the given player has already reported the given match.
     */
    public boolean hasReport(String matchId, String reporterId) {
        return reports.stream()
                .anyMatch(r -> r.getMatchId().equals(matchId) && r.getReporterId().equals(reporterId));
    }

    /**
     * Retrieves all reports sorted by latest reportTime first.
     */
    public List<Report> getReportsSortedLatestFirst() {
        return reports.stream()
                .sorted(Comparator.comparingLong(Report::getReportTime).reversed())
                .collect(Collectors.toList());
    }
}
