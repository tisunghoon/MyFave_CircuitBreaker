package com.myfave.api.domain.shipping.dto.response;

import com.myfave.api.domain.shipping.client.TrackerDeliveryClient;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Builder
public class TrackingResponse {

    private String trackingNumber;
    private String carrierId;
    private String statusCode;
    private String statusName;
    private OffsetDateTime time;
    private String location;
    private String description;
    private List<TrackingEventItem> events;

    @Getter
    @Builder
    public static class TrackingEventItem {
        private String statusCode;
        private String statusName;
        private OffsetDateTime time;
        private String location;
        private String description;
    }

    public static TrackingResponse from(String carrierId,
                                        TrackerDeliveryClient.TrackResult result) {
        TrackerDeliveryClient.EventData lastEvent = result.getLastEvent();

        String statusCode = null;
        String statusName = null;
        OffsetDateTime time = null;
        String location = null;
        String description = null;

        if (lastEvent != null) {
            if (lastEvent.getStatus() != null) {
                statusCode = lastEvent.getStatus().getCode();
                statusName = lastEvent.getStatus().getName();
            }
            time = lastEvent.getTime();
            location = lastEvent.getLocation() != null
                    ? lastEvent.getLocation().getName() : null;
            description = lastEvent.getDescription();
        }

        List<TrackingEventItem> events = List.of();
        if (result.getEvents() != null && result.getEvents().getEdges() != null) {
            events = result.getEvents().getEdges().stream()
                    .filter(e -> e.getNode() != null)
                    .map(e -> {
                        var node = e.getNode();
                        return TrackingEventItem.builder()
                                .statusCode(node.getStatus() != null ? node.getStatus().getCode() : null)
                                .statusName(node.getStatus() != null ? node.getStatus().getName() : null)
                                .time(node.getTime())
                                .location(node.getLocation() != null ? node.getLocation().getName() : null)
                                .description(node.getDescription())
                                .build();
                    })
                    .toList();
        }

        return TrackingResponse.builder()
                .trackingNumber(result.getTrackingNumber())
                .carrierId(carrierId)
                .statusCode(statusCode)
                .statusName(statusName)
                .time(time)
                .location(location)
                .description(description)
                .events(events)
                .build();
    }
}
