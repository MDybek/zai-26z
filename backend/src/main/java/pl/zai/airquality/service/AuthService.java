package pl.zai.airquality.service;

import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.zai.airquality.domain.AccessToken;
import pl.zai.airquality.domain.AdminUser;
import pl.zai.airquality.repo.AccessTokenRepository;
import pl.zai.airquality.repo.AdminUserRepository;
import pl.zai.airquality.security.AdminPrincipal;
import pl.zai.airquality.security.Hashing;
import pl.zai.airquality.web.ApiException;
import pl.zai.airquality.web.dto.LoginRequest;
import pl.zai.airquality.web.dto.LoginResponse;
import pl.zai.airquality.web.dto.PasswordChange;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AdminUserRepository adminUserRepository;
    private final AccessTokenRepository accessTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${zai.token-ttl-seconds:3600}")
    private int tokenTtlSeconds;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        AdminUser user = adminUserRepository.findByUsername(request.username())
                .orElseThrow(() -> ApiException.unauthorized("Invalid username or password"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw ApiException.unauthorized("Invalid username or password");
        }
        String rawToken = Hashing.randomToken(32);
        AccessToken token = new AccessToken();
        token.setUser(user);
        token.setTokenHash(Hashing.sha256(rawToken));
        Instant now = Instant.now();
        token.setCreatedAt(now);
        token.setExpiresAt(now.plusSeconds(tokenTtlSeconds));
        accessTokenRepository.save(token);
        return new LoginResponse(rawToken, "Bearer", tokenTtlSeconds);
    }

    @Transactional
    public void logout(AdminPrincipal principal) {
        accessTokenRepository.deleteByTokenHash(principal.tokenHash());
    }

    @Transactional
    public void changePassword(AdminPrincipal principal, PasswordChange request) {
        AdminUser user = adminUserRepository.findById(principal.userId())
                .orElseThrow(() -> ApiException.unauthorized("Invalid token"));
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw ApiException.forbidden("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        adminUserRepository.save(user);
    }

    @Transactional(readOnly = true)
    public Optional<AdminPrincipal> authenticate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }
        return accessTokenRepository.findByTokenHash(Hashing.sha256(rawToken))
                .filter(token -> token.getExpiresAt().isAfter(Instant.now()))
                .map(token -> new AdminPrincipal(
                        token.getUser().getId(),
                        token.getUser().getUsername(),
                        token.getTokenHash()
                ));
    }
}
