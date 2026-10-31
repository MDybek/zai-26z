package pl.zai.airquality.web.dto;

import java.time.Instant;

public record SensorCreatedResponse(
        Long id,
        String name,
        Long seriesId,
        Instant createdAt,
        Instant lastMeasurementAt,
        String apiKey
) {
}
