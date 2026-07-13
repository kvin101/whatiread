package com.whatiread.shared.idempotency;

import com.whatiread.identity.security.SecurityUtils;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdempotencyService {

    private final IdempotencyRecordRepository repository;

    public IdempotencyService(IdempotencyRecordRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Optional<IdempotencyRecord> findValid(String key) {
        return repository.findByIdempotencyKey(key)
                .filter(record -> record.getExpiresAt().isAfter(Instant.now()));
    }

    @Transactional
    public void store(String key, String method, String path, int status, String body) {
        IdempotencyRecord record = new IdempotencyRecord();
        record.setIdempotencyKey(key);
        record.setHttpMethod(method);
        record.setRequestPath(path);
        record.setResponseStatus(status);
        record.setResponseBody(body);
        record.setExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
        SecurityUtils.currentUserIdOptional().ifPresent(record::setUserId);
        repository.save(record);
    }
}
