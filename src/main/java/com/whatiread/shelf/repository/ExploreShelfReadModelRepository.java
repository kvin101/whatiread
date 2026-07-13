package com.whatiread.shelf.repository;

import com.whatiread.shelf.domain.ExploreShelfReadModel;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExploreShelfReadModelRepository extends JpaRepository<ExploreShelfReadModel, UUID> {

    @Query("""
            SELECT m FROM ExploreShelfReadModel m
            WHERE (:cursor IS NULL OR m.cursorToken > :cursor)
            ORDER BY m.cursorToken ASC
            """)
    List<ExploreShelfReadModel> findPageAfterCursor(@Param("cursor") String cursor, org.springframework.data.domain.Pageable pageable);

    @Query("""
            SELECT m FROM ExploreShelfReadModel m
            WHERE m.updatedAt < :cursorTime
               OR (m.updatedAt = :cursorTime AND m.shelfId < :cursorId)
            ORDER BY m.updatedAt DESC, m.shelfId DESC
            """)
    List<ExploreShelfReadModel> findKeysetPage(
            @Param("cursorTime") Instant cursorTime,
            @Param("cursorId") UUID cursorId,
            org.springframework.data.domain.Pageable pageable
    );
}
