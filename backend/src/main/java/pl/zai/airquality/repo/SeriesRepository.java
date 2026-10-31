package pl.zai.airquality.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.zai.airquality.domain.Series;

public interface SeriesRepository extends JpaRepository<Series, Long> {
}
