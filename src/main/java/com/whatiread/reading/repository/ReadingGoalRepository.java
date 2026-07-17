package com.whatiread.reading.repository;

import com.whatiread.reading.domain.ReadingGoal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReadingGoalRepository extends JpaRepository<ReadingGoal, UUID> {

    Optional<ReadingGoal> findByUser_IdAndYear(UUID userId, short year);
}
