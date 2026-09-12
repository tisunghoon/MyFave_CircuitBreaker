package com.myfave.api.domain.shipping.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.myfave.api.global.error.CustomException;
import com.myfave.api.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class TrackerDeliveryClient {

    private final WebClient trackerWebClient;

    private static final String TRACK_QUERY = """
            query Track($carrierId: ID!, $trackingNumber: String!) {
              track(carrierId: $carrierId, trackingNumber: $trackingNumber) {
                trackingNumber
                lastEvent {
                  status { code name }
                  time
                  location { name }
                  description
                }
                events(first: 20) {
                  edges {
                    node {
                      status { code name }
                      time
                      location { name }
                      description
                    }
                  }
                }
              }
            }
            """;

    public TrackResult track(String carrierId, String trackingNumber) {
        Map<String, Object> body = Map.of(
                "query", TRACK_QUERY,
                "variables", Map.of("carrierId", carrierId, "trackingNumber", trackingNumber)
        );

        try {
            GraphQLResponse response = trackerWebClient.post()
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(GraphQLResponse.class)
                    .block(Duration.ofSeconds(15));

            if (response == null || response.getData() == null
                    || response.getData().getTrack() == null) {
                throw new CustomException(ErrorCode.TRACKING_API_ERROR);
            }

            return response.getData().getTrack();

        } catch (CustomException e) {
            throw e;
        } catch (WebClientResponseException e) {
            throw new CustomException(ErrorCode.TRACKING_API_ERROR);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.TRACKING_API_ERROR);
        }
    }

    // ── GraphQL 응답 역직렬화용 inner class ────────────────────────

    @Getter @Setter @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GraphQLResponse {
        private DataBody data;
    }

    @Getter @Setter @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DataBody {
        private TrackResult track;
    }

    @Getter @Setter @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TrackResult {
        private String trackingNumber;
        private EventData lastEvent;
        private EventConnection events;
    }

    @Getter @Setter @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EventConnection {
        private List<EventEdge> edges;
    }

    @Getter @Setter @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EventEdge {
        private EventData node;
    }

    @Getter @Setter @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EventData {
        private StatusData status;
        private OffsetDateTime time;
        private LocationData location;
        private String description;
    }

    @Getter @Setter @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StatusData {
        private String code;
        private String name;
    }

    @Getter @Setter @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LocationData {
        private String name;
    }
}
