package pl.zai.airquality.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.zai.airquality.domain.ErrorLogEntry;

public interface ErrorLogRepository extends JpaRepository<ErrorLogEntry, Long> {
}
