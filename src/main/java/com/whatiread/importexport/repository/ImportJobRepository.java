package com.whatiread.importexport.repository;

import com.whatiread.importexport.domain.ImportJob;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportJobRepository extends JpaRepository<ImportJob, UUID> {

    Optional<ImportJob> findByIdAndUserId(UUID id, UUID userId);
}
