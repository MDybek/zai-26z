package pl.zai.airquality.web.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        int expiresIn
) {
}
