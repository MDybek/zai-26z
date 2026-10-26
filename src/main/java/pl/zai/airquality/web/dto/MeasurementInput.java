package pl.zai.airquality.web.dto;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record MeasurementInput(
        @NotNull Double value,
        Instant timestamp
) {
}
