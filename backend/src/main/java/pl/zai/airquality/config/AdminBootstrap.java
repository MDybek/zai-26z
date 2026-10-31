package pl.zai.airquality.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import pl.zai.airquality.domain.AdminUser;
import pl.zai.airquality.repo.AdminUserRepository;

@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class AdminBootstrap implements ApplicationRunner {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${zai.admin.username}")
    private String username;

    @Value("${zai.admin.password}")
    private String password;

    @Override
    public void run(ApplicationArguments args) {
        if (adminUserRepository.findByUsername(username).isPresent()) {
            return;
        }
        AdminUser user = new AdminUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        adminUserRepository.save(user);
        log.info("Created administrator account '{}'", username);
    }
}
