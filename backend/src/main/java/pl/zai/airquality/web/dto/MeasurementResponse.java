package pl.zai.airquality.web.dto;

import java.time.Instant;
import pl.zai.airquality.domain.Measurement;

public record MeasurementResponse(
        String id,
        Long seriesId,
        Long sensorId,
        double value,
        Instant timestamp
) {
    public static MeasurementResponse from(Measurement measurement) {
        return new MeasurementResponse(
                measurement.getId(),
                measurement.getSeriesId(),
                measurement.getSensorId(),
                measurement.getValue(),
                measurement.getTimestamp()
        );
    }
}
