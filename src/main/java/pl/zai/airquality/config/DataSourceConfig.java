package pl.zai.airquality.config;

import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSourceConfig {

    public DataSourceConfig(@Value("${spring.datasource.url}") String url) {
        if (!url.contains(":memory:") && !url.contains("mode=memory")) {
            String path = url.replace("jdbc:sqlite:", "");
            try {
                Path dbPath = Path.of(path);
                Path parent = dbPath.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
            } catch (Exception ignored) {
            }
        }
    }
}
