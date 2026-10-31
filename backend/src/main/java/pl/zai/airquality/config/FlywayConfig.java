package pl.zai.airquality.config;

import java.sql.Connection;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayConfig {

    @Bean(initMethod = "migrate")
    public Flyway flyway(DataSource dataSource) {
        String location = "classpath:db/migration/postgres";
        try (Connection conn = dataSource.getConnection()) {
            String dbName = conn.getMetaData().getDatabaseProductName().toLowerCase();
            if (dbName.contains("sqlite")) {
                location = "classpath:db/migration/sqlite";
            }
        } catch (Exception ignored) {
        }

        return Flyway.configure()
                .dataSource(dataSource)
                .locations(location)
                .baselineOnMigrate(true)
                .load();
    }
}
