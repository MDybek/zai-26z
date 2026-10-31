package pl.zai.airquality.repo;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.zai.airquality.domain.Measurement;

public interface MeasurementRepository extends JpaRepository<Measurement, Long> {

    @Query("""
            select m from Measurement m
            where (:from is null or m.timestamp >= :from)
              and (:to is null or m.timestamp <= :to)
              and (:useSeriesFilter = false or m.series.id in :seriesIds)
            """)
    List<Measurement> findFiltered(
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("useSeriesFilter") boolean useSeriesFilter,
            @Param("seriesIds") Collection<Long> seriesIds,
            Pageable pageable);

    @Query("select case when count(m) > 0 then true else false end from Measurement m where m.series.id = :seriesId and m.value < :minValue")
    boolean existsBySeriesIdAndValueLessThan(@Param("seriesId") Long seriesId, @Param("minValue") double minValue);

    @Query("select case when count(m) > 0 then true else false end from Measurement m where m.series.id = :seriesId and m.value > :maxValue")
    boolean existsBySeriesIdAndValueGreaterThan(@Param("seriesId") Long seriesId, @Param("maxValue") double maxValue);

    Optional<Measurement> findTopBySensor_IdOrderByTimestampDesc(Long sensorId);

    void deleteBySeries_Id(Long seriesId);

    @Modifying
    @Query("update Measurement m set m.sensor = null where m.sensor.id = :sensorId")
    void detachSensor(@Param("sensorId") Long sensorId);
}
