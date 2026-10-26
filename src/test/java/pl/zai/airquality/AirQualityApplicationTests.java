package pl.zai.airquality;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import pl.zai.airquality.repo.MeasurementRepository;
import pl.zai.airquality.repo.SensorRepository;
import pl.zai.airquality.repo.SeriesRepository;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AirQualityApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SeriesRepository seriesRepository;

    @Autowired
    private SensorRepository sensorRepository;

    @Autowired
    private MeasurementRepository measurementRepository;

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        // Authenticate as default admin
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", "admin", "password", "Admin123!"))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        this.adminToken = json.get("accessToken").asText();
    }

    @Test
    @DisplayName("GET /api/health returns 200 and {'status':'ok'}")
    void healthCheck() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    @DisplayName("GET / (strona glowna) returns 200 with Content-Security-Policy")
    void rootPage() throws Exception {
        mockMvc.perform(get("/").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(header().exists("Content-Security-Policy"));
    }

    @Test
    @DisplayName("API sends X-Content-Type-Options: nosniff")
    void noSniffHeader() throws Exception {
        mockMvc.perform(get("/api/series"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    @Test
    @DisplayName("Accept: application/xml returns 406 Not Acceptable")
    void notAcceptableHeader() throws Exception {
        mockMvc.perform(get("/api/series").accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotAcceptable());
    }

    @Test
    @DisplayName("Content-Type: text/plain returns 415 Unsupported Media Type")
    void unsupportedMediaType() throws Exception {
        mockMvc.perform(post("/api/measurements")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("value=123")
                        .header("X-API-Key", "dummy-key"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @DisplayName("Malformed JSON returns 400 Bad Request")
    void malformedJson() throws Exception {
        mockMvc.perform(post("/api/series")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").exists())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("Full series and sensor lifecycle with measurements")
    void seriesAndSensorLifecycle() throws Exception {
        // 1. Create series
        MvcResult seriesRes = mockMvc.perform(post("/api/series")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Test Temperature",
                                "minValue", -20.0,
                                "maxValue", 50.0,
                                "color", "#336699",
                                "unit", "°C"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn();

        JsonNode seriesJson = objectMapper.readTree(seriesRes.getResponse().getContentAsString());
        long seriesId = seriesJson.get("id").asLong();

        // 2. Register sensor
        MvcResult sensorRes = mockMvc.perform(post("/api/sensors")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Outdoor Temp Sensor",
                                "seriesId", seriesId
                        ))))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn();

        JsonNode sensorJson = objectMapper.readTree(sensorRes.getResponse().getContentAsString());
        long sensorId = sensorJson.get("id").asLong();
        String apiKey = sensorJson.get("apiKey").asText();
        assertThat(apiKey).hasSizeGreaterThanOrEqualTo(32);

        // 3. Sensor list does NOT expose apiKey
        MvcResult listSensors = mockMvc.perform(get("/api/sensors")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(listSensors.getResponse().getContentAsString()).doesNotContain(apiKey);

        // 4. Post measurement with sensor API key
        MvcResult measRes = mockMvc.perform(post("/api/measurements")
                        .header("X-API-Key", apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "value", 21.5,
                                "timestamp", "2026-01-10T12:00:00Z"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn();

        JsonNode measJson = objectMapper.readTree(measRes.getResponse().getContentAsString());
        long measId = measJson.get("id").asLong();
        assertThat(measJson.get("value").asDouble()).isEqualTo(21.5);
        assertThat(measJson.get("seriesId").asLong()).isEqualTo(seriesId);

        // 5. Boundary values min and max are accepted
        mockMvc.perform(post("/api/measurements")
                        .header("X-API-Key", apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "value", -20.0,
                                "timestamp", "2026-01-10T11:00:00Z"
                        ))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/measurements")
                        .header("X-API-Key", apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "value", 50.0,
                                "timestamp", "2026-01-10T13:00:00Z"
                        ))))
                .andExpect(status().isCreated());

        // 6. Out of range values are rejected with 422
        mockMvc.perform(post("/api/measurements")
                        .header("X-API-Key", apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "value", 50.1,
                                "timestamp", "2026-01-10T14:00:00Z"
                        ))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));

        mockMvc.perform(post("/api/measurements")
                        .header("X-API-Key", apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "value", -20.1,
                                "timestamp", "2026-01-10T14:00:00Z"
                        ))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));

        // 7. Future timestamp (> 5 min) is rejected with 422
        String futureTime = Instant.now().plusSeconds(3600).toString();
        mockMvc.perform(post("/api/measurements")
                        .header("X-API-Key", apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "value", 10.0,
                                "timestamp", futureTime
                        ))))
                .andExpect(status().isUnprocessableEntity());

        // 8. PUT series with conflicting range excluding existing measurements returns 409
        mockMvc.perform(put("/api/series/" + seriesId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Test Temperature Narrowed",
                                "minValue", 0.0,
                                "maxValue", 20.0,
                                "color", "#336699"
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        // 9. Measurements cannot be updated or deleted directly (405 Method Not Allowed)
        mockMvc.perform(put("/api/measurements/" + measId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\": 99.0}"))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(delete("/api/measurements/" + measId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isMethodNotAllowed());

        // 10. Deregister sensor (DELETE /api/sensors/{id}) -> 204
        mockMvc.perform(delete("/api/sensors/" + sensorId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // Key stops working immediately (401)
        mockMvc.perform(post("/api/measurements")
                        .header("X-API-Key", apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\": 10.0}"))
                .andExpect(status().isUnauthorized());

        // But previously collected measurement remains accessible (200)
        mockMvc.perform(get("/api/measurements/" + measId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(measId));

        // 11. DELETE series removes series and its measurements
        mockMvc.perform(delete("/api/series/" + seriesId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/series/" + seriesId))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/measurements/" + measId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Admin operations without token return 401 Unauthorized")
    void unauthorizedAdminOperations() throws Exception {
        mockMvc.perform(post("/api/series")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test\",\"minValue\":0,\"maxValue\":100,\"color\":\"#112233\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(get("/api/sensors"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Password change verification")
    void passwordChangeFlow() throws Exception {
        // Wrong current password returns 403 Forbidden
        mockMvc.perform(put("/api/auth/password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "currentPassword", "WrongPass!",
                                "newPassword", "NewValidPass123!"
                        ))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        // Correct password change returns 204
        mockMvc.perform(put("/api/auth/password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "currentPassword", "Admin123!",
                                "newPassword", "NewAdmin123!"
                        ))))
                .andExpect(status().isNoContent());

        // Verify login with new password
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", "admin", "password", "NewAdmin123!"))))
                .andExpect(status().isOk());

        // Reset password back for subsequent tests
        mockMvc.perform(put("/api/auth/password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "currentPassword", "NewAdmin123!",
                                "newPassword", "Admin123!"
                        ))))
                .andExpect(status().isNoContent());
    }
}
