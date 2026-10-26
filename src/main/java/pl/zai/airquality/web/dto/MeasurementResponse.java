package pl.zai.airquality.web.dto;

import java.time.Instant;
import pl.zai.airquality.domain.Measurement;

public record MeasurementResponse(
        Long id,
        Long seriesId,
        Long sensorId,
        double value,
        Instant timestamp
) {
    public static MeasurementResponse from(Measurement measurement) {
        Long sensorId = measurement.getSensor() == null ? null : measurement.getSensor().getId();
        return new MeasurementResponse(
                measurement.getId(),
                measurement.getSeries().getId(),
                sensorId,
                measurement.getValue(),
                measurement.getTimestamp()
        );
    }
}
