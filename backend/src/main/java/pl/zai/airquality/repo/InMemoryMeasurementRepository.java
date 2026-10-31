package pl.zai.airquality.repo;

import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Pageable;
import pl.zai.airquality.domain.Measurement;

public class InMemoryMeasurementRepository implements MeasurementRepository {

    private final Map<String, Measurement> storage = new ConcurrentHashMap<>();

    @Override
    public Measurement save(Measurement measurement) {
        if (measurement.getId() == null || measurement.getId().isBlank()) {
            measurement.setId(new ObjectId().toHexString());
        }
        storage.put(measurement.getId(), measurement);
        return measurement;
    }

    @Override
    public Optional<Measurement> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<Measurement> findFiltered(
            Instant from,
            Instant to,
            boolean useSeriesFilter,
            Collection<Long> seriesIds,
            Pageable pageable
    ) {
        Stream<Measurement> stream = storage.values().stream();
        if (from != null) {
            stream = stream.filter(m -> !m.getTimestamp().isBefore(from));
        }
        if (to != null) {
            stream = stream.filter(m -> !m.getTimestamp().isAfter(to));
        }
        if (useSeriesFilter && seriesIds != null && !seriesIds.isEmpty()) {
            stream = stream.filter(m -> seriesIds.contains(m.getSeriesId()));
        }

        boolean descending = pageable.getSort().getOrderFor("timestamp") != null
                && pageable.getSort().getOrderFor("timestamp").isDescending();
        Comparator<Measurement> comp = Comparator.comparing(Measurement::getTimestamp);
        if (descending) {
            comp = comp.reversed();
        }

        return stream.sorted(comp).limit(pageable.getPageSize()).toList();
    }

    @Override
    public boolean existsBySeriesIdAndValueLessThan(Long seriesId, double minValue) {
        return storage.values().stream()
                .anyMatch(m -> seriesId.equals(m.getSeriesId()) && m.getValue() < minValue);
    }

    @Override
    public boolean existsBySeriesIdAndValueGreaterThan(Long seriesId, double maxValue) {
        return storage.values().stream()
                .anyMatch(m -> seriesId.equals(m.getSeriesId()) && m.getValue() > maxValue);
    }

    @Override
    public Optional<Measurement> findTopBySensorIdOrderByTimestampDesc(Long sensorId) {
        return storage.values().stream()
                .filter(m -> sensorId.equals(m.getSensorId()))
                .max(Comparator.comparing(Measurement::getTimestamp));
    }

    @Override
    public void deleteBySeriesId(Long seriesId) {
        storage.entrySet().removeIf(entry -> seriesId.equals(entry.getValue().getSeriesId()));
    }

    @Override
    public void detachSensor(Long sensorId) {
        storage.values().stream()
                .filter(m -> sensorId.equals(m.getSensorId()))
                .forEach(m -> m.setSensorId(null));
    }

    @Override
    public void deleteAll() {
        storage.clear();
    }
}
