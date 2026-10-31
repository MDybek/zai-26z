package pl.zai.airquality.repo;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import pl.zai.airquality.domain.Measurement;

@Repository
@Primary
@RequiredArgsConstructor
public class MongoMeasurementRepository implements MeasurementRepository {

    private final MongoTemplate mongoTemplate;
    private final InMemoryMeasurementRepository fallback = new InMemoryMeasurementRepository();

    @Value("${zai.timeseries.storage:mongo}")
    private String storageMode;

    private volatile Boolean mongoAvailable = null;

    private boolean isMongoAvailable() {
        if ("memory".equalsIgnoreCase(storageMode)) {
            return false;
        }
        if (mongoAvailable != null) {
            return mongoAvailable;
        }
        try {
            mongoTemplate.getDb().runCommand(new Document("ping", 1));
            mongoAvailable = true;
        } catch (Exception ex) {
            mongoAvailable = false;
        }
        return mongoAvailable;
    }

    @Override
    public Measurement save(Measurement measurement) {
        if (!isMongoAvailable()) {
            return fallback.save(measurement);
        }
        return mongoTemplate.save(measurement);
    }

    @Override
    public Optional<Measurement> findById(String id) {
        if (!isMongoAvailable()) {
            return fallback.findById(id);
        }
        return Optional.ofNullable(mongoTemplate.findById(id, Measurement.class));
    }

    @Override
    public List<Measurement> findFiltered(
            Instant from,
            Instant to,
            boolean useSeriesFilter,
            Collection<Long> seriesIds,
            Pageable pageable
    ) {
        if (!isMongoAvailable()) {
            return fallback.findFiltered(from, to, useSeriesFilter, seriesIds, pageable);
        }

        List<Criteria> criteriaList = new ArrayList<>();
        if (from != null) {
            criteriaList.add(Criteria.where("timestamp").gte(from));
        }
        if (to != null) {
            criteriaList.add(Criteria.where("timestamp").lte(to));
        }
        if (useSeriesFilter && seriesIds != null && !seriesIds.isEmpty()) {
            criteriaList.add(Criteria.where("seriesId").in(seriesIds));
        }

        Query query = new Query();
        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }
        query.with(pageable);
        return mongoTemplate.find(query, Measurement.class);
    }

    @Override
    public boolean existsBySeriesIdAndValueLessThan(Long seriesId, double minValue) {
        if (!isMongoAvailable()) {
            return fallback.existsBySeriesIdAndValueLessThan(seriesId, minValue);
        }
        Query query = Query.query(Criteria.where("seriesId").is(seriesId).and("value").lt(minValue));
        return mongoTemplate.exists(query, Measurement.class);
    }

    @Override
    public boolean existsBySeriesIdAndValueGreaterThan(Long seriesId, double maxValue) {
        if (!isMongoAvailable()) {
            return fallback.existsBySeriesIdAndValueGreaterThan(seriesId, maxValue);
        }
        Query query = Query.query(Criteria.where("seriesId").is(seriesId).and("value").gt(maxValue));
        return mongoTemplate.exists(query, Measurement.class);
    }

    @Override
    public Optional<Measurement> findTopBySensorIdOrderByTimestampDesc(Long sensorId) {
        if (!isMongoAvailable()) {
            return fallback.findTopBySensorIdOrderByTimestampDesc(sensorId);
        }
        Query query = Query.query(Criteria.where("sensorId").is(sensorId))
                .with(Sort.by(Sort.Direction.DESC, "timestamp"))
                .limit(1);
        return Optional.ofNullable(mongoTemplate.findOne(query, Measurement.class));
    }

    @Override
    public void deleteBySeriesId(Long seriesId) {
        if (!isMongoAvailable()) {
            fallback.deleteBySeriesId(seriesId);
            return;
        }
        mongoTemplate.remove(Query.query(Criteria.where("seriesId").is(seriesId)), Measurement.class);
    }

    @Override
    public void detachSensor(Long sensorId) {
        if (!isMongoAvailable()) {
            fallback.detachSensor(sensorId);
            return;
        }
        mongoTemplate.updateMulti(
                Query.query(Criteria.where("sensorId").is(sensorId)),
                new Update().unset("sensorId"),
                Measurement.class
        );
    }

    @Override
    public void deleteAll() {
        if (!isMongoAvailable()) {
            fallback.deleteAll();
            return;
        }
        mongoTemplate.remove(new Query(), Measurement.class);
    }
}
