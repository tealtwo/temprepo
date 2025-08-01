package me.whitehatd.aquila.queue.objects.report;

import lombok.Data;

@Data
public class Report {

    private String matchId;

    private String reporterId;

    private String matchJson;

    private long reportTime;

    private String reason;
}
