package me.whitehatd.aquila.queue.redis;

import lombok.Data;

@Data
public class SpectateDTO {
    private String spectatorUUID;
    private String matchId;
    private boolean hidden;
}
