package pl.zai.airquality.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SensorInput(
        @NotBlank @Size(min = 1, max = 100) String name,
        @NotNull Long seriesId
) {
}
