package com.whatiread.goal.repository;

import com.whatiread.goal.domain.ReadingGoal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReadingGoalRepository extends JpaRepository<ReadingGoal, UUID> {

    Optional<ReadingGoal> findByUser_IdAndGoalYear(UUID userId, short goalYear);
}
