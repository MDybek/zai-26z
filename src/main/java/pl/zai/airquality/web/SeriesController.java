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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.zai.airquality.service.SeriesService;
import pl.zai.airquality.web.dto.SeriesInput;
import pl.zai.airquality.web.dto.SeriesResponse;

@RestController
@RequestMapping("/api/series")
public class SeriesController {

    private final SeriesService seriesService;

    public SeriesController(SeriesService seriesService) {
        this.seriesService = seriesService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<SeriesResponse> list() {
        return seriesService.list();
    }

    @GetMapping(value = "/{seriesId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public SeriesResponse get(@PathVariable Long seriesId) {
        return seriesService.get(seriesId);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SeriesResponse> create(@Valid @RequestBody SeriesInput input) {
        SeriesResponse created = seriesService.create(input);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header("Location", "/api/series/" + created.id())
                .body(created);
    }

    @PutMapping(value = "/{seriesId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public SeriesResponse update(@PathVariable Long seriesId, @Valid @RequestBody SeriesInput input) {
        return seriesService.update(seriesId, input);
    }

    @DeleteMapping("/{seriesId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long seriesId) {
        seriesService.delete(seriesId);
    }
}
