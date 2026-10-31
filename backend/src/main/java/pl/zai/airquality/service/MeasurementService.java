package pl.zai.airquality.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.zai.airquality.domain.Measurement;
import pl.zai.airquality.domain.Sensor;
import pl.zai.airquality.domain.Series;
import pl.zai.airquality.repo.MeasurementRepository;
import pl.zai.airquality.web.ApiException;
import pl.zai.airquality.web.ProblemBody;
import pl.zai.airquality.web.dto.MeasurementInput;
import pl.zai.airquality.web.dto.MeasurementResponse;

@Service
public class MeasurementService {

    private static final Duration FUTURE_TOLERANCE = Duration.ofMinutes(5);

    private final MeasurementRepository measurementRepository;
    private final SensorService sensorService;
    private final ErrorLogService errorLogService;

    public MeasurementService(
            MeasurementRepository measurementRepository,
            SensorService sensorService,
            ErrorLogService errorLogService
    ) {
        this.measurementRepository = measurementRepository;
        this.sensorService = sensorService;
        this.errorLogService = errorLogService;
    }

    public MeasurementResponse create(String apiKey, MeasurementInput input) {
        Sensor sensor = sensorService.requireByApiKey(apiKey);
        Series series = sensor.getSeries();
        Instant timestamp = input.timestamp() == null ? Instant.now() : input.timestamp();
        if (timestamp.isAfter(Instant.now().plus(FUTURE_TOLERANCE))) {
            String message = "Rejected measurement: timestamp is more than 5 minutes in the future";
            errorLogService.record(message);
            throw ApiException.unprocessable(message, List.of(new ProblemBody.FieldError("timestamp", message)));
        }
        double value = input.value();
        if (value < series.getMinValue() || value > series.getMaxValue()) {
            String message = "Rejected measurement: value " + value + " is outside series range ["
                    + series.getMinValue() + ", " + series.getMaxValue() + "]";
            errorLogService.record(message);
            throw ApiException.unprocessable(message, List.of(new ProblemBody.FieldError("value", message)));
        }
        return saveMeasurement(series, sensor, value, timestamp);
    }

    @Transactional
    public MeasurementResponse saveMeasurement(Series series, Sensor sensor, double value, Instant timestamp) {
        Measurement measurement = new Measurement();
        measurement.setSeries(series);
        measurement.setSensor(sensor);
        measurement.setValue(value);
        measurement.setTimestamp(timestamp);
        return MeasurementResponse.from(measurementRepository.save(measurement));
    }

    @Transactional(readOnly = true)
    public MeasurementResponse get(Long id) {
        Measurement measurement = measurementRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Measurement not found"));
        return MeasurementResponse.from(measurement);
    }

    @Transactional(readOnly = true)
    public List<MeasurementResponse> list(String seriesParam, Instant from, Instant to, String sort, Integer limit) {
        if (from != null && to != null && from.isAfter(to)) {
            throw ApiException.badRequest("from must be less than or equal to to");
        }
        boolean descending = "-timestamp".equals(sort);
        int pageSize = limit == null ? 1000 : limit;
        if (pageSize < 1 || pageSize > 10_000) {
            throw ApiException.badRequest("limit must be between 1 and 10000");
        }
        List<Long> seriesIds = parseSeriesIds(seriesParam);
        boolean useFilter = !seriesIds.isEmpty();
        Sort order = Sort.by(descending ? Sort.Direction.DESC : Sort.Direction.ASC, "timestamp");
        List<Long> idsForQuery = useFilter ? seriesIds : List.of(-1L);
        return measurementRepository.findFiltered(from, to, useFilter, idsForQuery, PageRequest.of(0, pageSize, order))
                .stream()
                .map(MeasurementResponse::from)
                .toList();
    }

    private List<Long> parseSeriesIds(String seriesParam) {
        if (seriesParam == null || seriesParam.isBlank()) {
            return List.of();
        }
        List<Long> ids = new ArrayList<>();
        for (String part : seriesParam.split(",")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                ids.add(Long.parseLong(trimmed));
            } catch (NumberFormatException ex) {
                throw ApiException.badRequest("Invalid series id: " + trimmed);
            }
        }
        return ids;
    }
}
