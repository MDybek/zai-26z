package pl.zai.airquality.domain;

import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.TimeSeries;

@Document(collection = "measurements")
@TimeSeries(timeField = "timestamp", metaField = "seriesId")
@CompoundIndex(name = "series_time_idx", def = "{'seriesId': 1, 'timestamp': -1}")
@Getter
@Setter
public class Measurement {

    @Id
    private String id;

    @Indexed
    private Long seriesId;

    @Indexed
    private Long sensorId;

    private double value;

    @Indexed
    private Instant timestamp;
}
