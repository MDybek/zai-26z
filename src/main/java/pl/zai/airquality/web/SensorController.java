package pl.zai.airquality.web;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.zai.airquality.service.SensorService;
import pl.zai.airquality.web.dto.SensorCreatedResponse;
import pl.zai.airquality.web.dto.SensorInput;
import pl.zai.airquality.web.dto.SensorResponse;

@RestController
@RequestMapping("/api/sensors")
public class SensorController {

    private final SensorService sensorService;

    public SensorController(SensorService sensorService) {
        this.sensorService = sensorService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<SensorResponse> list() {
        return sensorService.list();
    }

    @GetMapping(value = "/{sensorId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public SensorResponse get(@PathVariable Long sensorId) {
        return sensorService.get(sensorId);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SensorCreatedResponse> create(@Valid @RequestBody SensorInput input) {
        SensorCreatedResponse created = sensorService.create(input);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header("Location", "/api/sensors/" + created.id())
                .body(created);
    }

    @DeleteMapping("/{sensorId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long sensorId) {
        sensorService.delete(sensorId);
    }
}
