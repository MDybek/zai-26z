package pl.zai.airquality.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.zai.airquality.domain.Series;
import pl.zai.airquality.repo.MeasurementRepository;
import pl.zai.airquality.repo.SensorRepository;
import pl.zai.airquality.repo.SeriesRepository;
import pl.zai.airquality.web.ApiException;
import pl.zai.airquality.web.ProblemBody;
import lombok.RequiredArgsConstructor;
import pl.zai.airquality.web.dto.SeriesInput;
import pl.zai.airquality.web.dto.SeriesResponse;

@Service
@RequiredArgsConstructor
public class SeriesService {

    private final SeriesRepository seriesRepository;
    private final MeasurementRepository measurementRepository;
    private final SensorRepository sensorRepository;

    @Transactional(readOnly = true)
    public List<SeriesResponse> list() {
        return seriesRepository.findAll().stream().map(SeriesResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public SeriesResponse get(Long id) {
        return SeriesResponse.from(require(id));
    }

    @Transactional
    public SeriesResponse create(SeriesInput input) {
        validateRange(input);
        Series series = new Series();
        apply(series, input);
        return SeriesResponse.from(seriesRepository.save(series));
    }

    @Transactional
    public SeriesResponse update(Long id, SeriesInput input) {
        validateRange(input);
        Series series = require(id);
        boolean wouldExclude = measurementRepository.existsBySeriesIdAndValueLessThan(id, input.minValue())
                || measurementRepository.existsBySeriesIdAndValueGreaterThan(id, input.maxValue());
        if (wouldExclude) {
            throw ApiException.conflict("New value range would exclude existing measurements");
        }
        apply(series, input);
        return SeriesResponse.from(seriesRepository.save(series));
    }

    @Transactional
    public void delete(Long id) {
        Series series = require(id);
        measurementRepository.deleteBySeriesId(id);
        sensorRepository.deleteBySeries_Id(id);
        seriesRepository.delete(series);
    }

    public Series require(Long id) {
        return seriesRepository.findById(id).orElseThrow(() -> ApiException.notFound("Series not found"));
    }

    private void apply(Series series, SeriesInput input) {
        series.setName(input.name());
        series.setMinValue(input.minValue());
        series.setMaxValue(input.maxValue());
        series.setColor(input.color());
        series.setIcon(input.icon());
        series.setUnit(input.unit());
    }

    private void validateRange(SeriesInput input) {
        if (input.minValue() >= input.maxValue()) {
            throw ApiException.unprocessable(
                    "minValue must be less than maxValue",
                    List.of(new ProblemBody.FieldError("minValue", "minValue must be less than maxValue"))
            );
        }
    }
}
