package pl.zai.airquality.web.dto;

import java.time.Instant;

public record SensorResponse(
        Long id,
        String name,
        Long seriesId,
        Instant createdAt,
        Instant lastMeasurementAt
) {
}
