package pl.zai.airquality.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordChange(
        @NotBlank String currentPassword,
        @NotBlank @Size(min = 8) String newPassword
) {
}
