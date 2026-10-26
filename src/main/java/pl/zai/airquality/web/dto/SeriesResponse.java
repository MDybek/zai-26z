package pl.zai.airquality.web.dto;

import pl.zai.airquality.domain.Series;

public record SeriesResponse(
        Long id,
        String name,
        double minValue,
        double maxValue,
        String color,
        String icon,
        String unit
) {
    public static SeriesResponse from(Series series) {
        return new SeriesResponse(
                series.getId(),
                series.getName(),
                series.getMinValue(),
                series.getMaxValue(),
                series.getColor(),
                series.getIcon(),
                series.getUnit()
        );
    }
}
