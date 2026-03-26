package com.playtab.cloudgateservice.websocket;

import com.playtab.cloudgateservice.dto.OccupancyResponse;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class OccupancyBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    public OccupancyBroadcaster(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void broadcast(OccupancyResponse response) {
        messagingTemplate.convertAndSend(
                "/topic/occupancy/" + response.stageId(), response);
        messagingTemplate.convertAndSend(
                "/topic/occupancy", response);
    }
}
