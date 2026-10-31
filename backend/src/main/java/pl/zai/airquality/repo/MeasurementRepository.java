package pl.zai.airquality.repo;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import pl.zai.airquality.domain.Measurement;

public interface MeasurementRepository {

    Measurement save(Measurement measurement);

    Optional<Measurement> findById(String id);

    List<Measurement> findFiltered(
            Instant from,
            Instant to,
            boolean useSeriesFilter,
            Collection<Long> seriesIds,
            Pageable pageable
    );

    boolean existsBySeriesIdAndValueLessThan(Long seriesId, double minValue);

    boolean existsBySeriesIdAndValueGreaterThan(Long seriesId, double maxValue);

    Optional<Measurement> findTopBySensorIdOrderByTimestampDesc(Long sensorId);

    void deleteBySeriesId(Long seriesId);

    void detachSensor(Long sensorId);

    void deleteAll();
}
