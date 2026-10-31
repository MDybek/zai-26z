package pl.zai.airquality.repo;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pl.zai.airquality.domain.AdminUser;

public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {
    Optional<AdminUser> findByUsername(String username);
}
