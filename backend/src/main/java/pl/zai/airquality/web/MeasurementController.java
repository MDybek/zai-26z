package pl.zai.airquality.web;

import jakarta.validation.Valid;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.zai.airquality.service.MeasurementService;
import pl.zai.airquality.web.dto.MeasurementInput;
import lombok.RequiredArgsConstructor;
import pl.zai.airquality.web.dto.MeasurementResponse;

@RestController
@RequestMapping("/api/measurements")
@RequiredArgsConstructor
public class MeasurementController {

    private final MeasurementService measurementService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<MeasurementResponse> list(
            @RequestParam(required = false) String series,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false, defaultValue = "timestamp") String sort,
            @RequestParam(required = false) Integer limit
    ) {
        if (sort != null && !sort.equals("timestamp") && !sort.equals("-timestamp")) {
            throw ApiException.badRequest("sort must be timestamp or -timestamp");
        }
        return measurementService.list(series, parseInstant(from, "from"), parseInstant(to, "to"), sort, limit);
    }

    private Instant parseInstant(String value, String field) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ex) {
            throw ApiException.badRequest("Invalid " + field + " timestamp");
        }
    }

    @GetMapping(value = "/{measurementId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public MeasurementResponse get(@PathVariable Long measurementId) {
        return measurementService.get(measurementId);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MeasurementResponse> create(
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @Valid @RequestBody MeasurementInput input
    ) {
        MeasurementResponse created = measurementService.create(apiKey, input);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header("Location", "/api/measurements/" + created.id())
                .body(created);
    }
}
