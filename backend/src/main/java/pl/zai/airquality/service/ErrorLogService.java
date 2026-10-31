package pl.zai.airquality.service;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.zai.airquality.domain.ErrorLogEntry;
import pl.zai.airquality.repo.ErrorLogRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class ErrorLogService {

    private final ErrorLogRepository errorLogRepository;

    @Transactional
    public void record(String message) {
        log.warn(message);
        try {
            ErrorLogEntry entry = new ErrorLogEntry();
            entry.setCreatedAt(Instant.now());
            entry.setMessage(message);
            errorLogRepository.save(entry);
        } catch (Exception ex) {
            log.error("Failed to persist error log entry: {}", ex.getMessage());
        }
    }
}
