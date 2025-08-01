package me.whitehatd.aquila.queue.redis.dto;

import lombok.Data;

import java.util.List;

@Data
public class RecreatePartyMessage {

    private String leaderId;
    private List<String> partyMembers;
}
