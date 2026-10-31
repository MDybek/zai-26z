package pl.zai.airquality.service;

import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.zai.airquality.domain.Measurement;
import pl.zai.airquality.domain.Sensor;
import pl.zai.airquality.domain.Series;
import pl.zai.airquality.repo.MeasurementRepository;
import pl.zai.airquality.repo.SensorRepository;
import pl.zai.airquality.security.Hashing;
import pl.zai.airquality.web.ApiException;
import pl.zai.airquality.web.dto.SensorCreatedResponse;
import pl.zai.airquality.web.dto.SensorInput;
import lombok.RequiredArgsConstructor;
import pl.zai.airquality.web.dto.SensorResponse;

@Service
@RequiredArgsConstructor
public class SensorService {

    private final SensorRepository sensorRepository;
    private final SeriesService seriesService;
    private final MeasurementRepository measurementRepository;

    @Transactional(readOnly = true)
    public List<SensorResponse> list() {
        return sensorRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public SensorResponse get(Long id) {
        return toResponse(require(id));
    }

    @Transactional
    public SensorCreatedResponse create(SensorInput input) {
        Series series;
        try {
            series = seriesService.require(input.seriesId());
        } catch (ApiException ex) {
            if (ex.getStatus().value() == 404) {
                throw ApiException.unprocessable("Series does not exist");
            }
            throw ex;
        }
        String apiKey = Hashing.randomToken(32);
        Sensor sensor = new Sensor();
        sensor.setName(input.name());
        sensor.setSeries(series);
        sensor.setApiKeyHash(Hashing.sha256(apiKey));
        sensor.setCreatedAt(Instant.now());
        Sensor saved = sensorRepository.save(sensor);
        return new SensorCreatedResponse(
                saved.getId(),
                saved.getName(),
                saved.getSeries().getId(),
                saved.getCreatedAt(),
                null,
                apiKey
        );
    }

    @Transactional
    public void delete(Long id) {
        Sensor sensor = require(id);
        measurementRepository.detachSensor(id);
        sensorRepository.delete(sensor);
    }

    public Sensor require(Long id) {
        return sensorRepository.findById(id).orElseThrow(() -> ApiException.notFound("Sensor not found"));
    }

    public Sensor requireByApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw ApiException.unauthorized("Missing sensor API key");
        }
        return sensorRepository.findByApiKeyHash(Hashing.sha256(apiKey))
                .orElseThrow(() -> ApiException.unauthorized("Invalid sensor API key"));
    }

    private SensorResponse toResponse(Sensor sensor) {
        Instant last = measurementRepository.findTopBySensorIdOrderByTimestampDesc(sensor.getId())
                .map(Measurement::getTimestamp)
                .orElse(null);
        return new SensorResponse(
                sensor.getId(),
                sensor.getName(),
                sensor.getSeries().getId(),
                sensor.getCreatedAt(),
                last
        );
    }
}
