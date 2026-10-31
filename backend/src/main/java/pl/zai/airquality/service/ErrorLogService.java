package pl.zai.airquality.service;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pl.zai.airquality.domain.ErrorLogEntry;
import pl.zai.airquality.repo.ErrorLogRepository;

@Service
public class ErrorLogService {

    private static final Logger log = LoggerFactory.getLogger(ErrorLogService.class);

    private final ErrorLogRepository errorLogRepository;

    public ErrorLogService(ErrorLogRepository errorLogRepository) {
        this.errorLogRepository = errorLogRepository;
    }

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
