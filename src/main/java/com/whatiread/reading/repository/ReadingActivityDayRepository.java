package com.whatiread.reading.repository;

import com.whatiread.reading.domain.ReadingActivityDay;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReadingActivityDayRepository extends JpaRepository<ReadingActivityDay, UUID> {

    boolean existsByUser_IdAndActivityDate(UUID userId, LocalDate activityDate);

    @Query("""
            SELECT d.activityDate FROM ReadingActivityDay d
            WHERE d.user.id = :userId
            ORDER BY d.activityDate DESC
            """)
    List<LocalDate> findActivityDatesByUserIdOrderByDateDesc(@Param("userId") UUID userId);
}
