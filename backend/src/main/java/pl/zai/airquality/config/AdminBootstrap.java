package pl.zai.airquality.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String password;

    public AdminBootstrap(
            AdminUserRepository adminUserRepository,
            PasswordEncoder passwordEncoder,
            @Value("${zai.admin.username}") String username,
            @Value("${zai.admin.password}") String password
    ) {
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.password = password;
    }

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
