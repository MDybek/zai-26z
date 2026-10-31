package pl.zai.airquality.repo;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pl.zai.airquality.domain.AccessToken;

public interface AccessTokenRepository extends JpaRepository<AccessToken, Long> {
    @EntityGraph(attributePaths = "user")
    Optional<AccessToken> findByTokenHash(String tokenHash);

    void deleteByTokenHash(String tokenHash);
}
