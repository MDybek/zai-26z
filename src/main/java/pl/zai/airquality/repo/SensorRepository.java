package pl.zai.airquality.repo;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pl.zai.airquality.domain.Sensor;

public interface SensorRepository extends JpaRepository<Sensor, Long> {
    Optional<Sensor> findByApiKeyHash(String apiKeyHash);

    void deleteBySeries_Id(Long seriesId);
}
