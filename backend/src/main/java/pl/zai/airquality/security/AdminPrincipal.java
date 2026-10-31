package pl.zai.airquality.security;

public record AdminPrincipal(Long userId, String username, String tokenHash) {
}
