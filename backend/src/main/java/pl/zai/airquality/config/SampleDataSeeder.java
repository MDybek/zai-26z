package pl.zai.airquality.config;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import pl.zai.airquality.service.MeasurementService;
import pl.zai.airquality.service.SensorService;
import pl.zai.airquality.service.SeriesService;
import pl.zai.airquality.web.dto.MeasurementInput;
import pl.zai.airquality.web.dto.MeasurementResponse;
import pl.zai.airquality.web.dto.SensorCreatedResponse;
import pl.zai.airquality.web.dto.SensorInput;
import pl.zai.airquality.web.dto.SeriesInput;
import pl.zai.airquality.web.dto.SeriesResponse;

@Component
@Order(2)
@ConditionalOnProperty(name = "zai.seed", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class SampleDataSeeder implements ApplicationRunner {

    private final SeriesService seriesService;
    private final SensorService sensorService;
    private final MeasurementService measurementService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            List<SeriesResponse> seriesList = seriesService.list();
            int ready = 0;
            for (SeriesResponse item : seriesList) {
                List<MeasurementResponse> measurements = measurementService.list(
                        String.valueOf(item.id()), null, null, "timestamp", 10000
                );
                if (measurements.size() >= 15) {
                    ready++;
                }
            }
            if (ready >= 3) {
                log.info("Sample data already present ({} series with 15+ points)", ready);
                return;
            }

            seedSeries("PM2.5", 0.0, 150.0, "#1f77b4", "µg/m³", 50.0, 30.0);
            seedSeries("PM10", 0.0, 250.0, "#ff7f0e", "µg/m³", 80.0, 40.0);
            seedSeries("NO2", 0.0, 200.0, "#2ca02c", "µg/m³", 60.0, 30.0);
            log.info("Seeded sample air-quality series successfully");
        } catch (Exception ex) {
            log.warn("Sample data seeding encountered an issue: {}", ex.getMessage());
        }
    }

    private void seedSeries(
            String name,
            double min,
            double max,
            String color,
            String unit,
            double base,
            double amplitude
    ) {
        SeriesResponse series = seriesService.create(new SeriesInput(name, min, max, color, null, unit));
        SensorCreatedResponse sensor = sensorService.create(new SensorInput(name + " sensor", series.id()));
        Instant start = Instant.parse("2026-10-01T00:00:00Z");
        for (int i = 0; i < 16; i++) {
            double value = base + amplitude * Math.sin(i / 3.0);
            double rounded = Math.round(value * 10.0) / 10.0;
            Instant ts = start.plus(i, ChronoUnit.HOURS);
            measurementService.create(sensor.apiKey(), new MeasurementInput(rounded, ts));
        }
    }
}
