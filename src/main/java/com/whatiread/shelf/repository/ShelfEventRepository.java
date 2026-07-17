package com.whatiread.shelf.repository;

import com.whatiread.shelf.domain.ShelfEvent;
import com.whatiread.shelf.domain.ShelfEventType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShelfEventRepository extends JpaRepository<ShelfEvent, UUID> {

    Page<ShelfEvent> findByShelf_IdOrderByCreatedAtDesc(UUID shelfId, Pageable pageable);

    Page<ShelfEvent> findByShelf_IdAndEventTypeInOrderByCreatedAtDesc(
            UUID shelfId, Collection<ShelfEventType> eventTypes, Pageable pageable);

    @Query("""
            SELECT e FROM ShelfEvent e
            JOIN FETCH e.shelf s
            JOIN FETCH s.owner
            JOIN FETCH e.actor
            WHERE s.id IN :shelfIds
            ORDER BY e.createdAt DESC, e.id DESC
            """)
    List<ShelfEvent> findFirstKeysetPageByShelfIds(
            @Param("shelfIds") List<UUID> shelfIds,
            Pageable pageable
    );

    @Query("""
            SELECT e FROM ShelfEvent e
            JOIN FETCH e.shelf s
            JOIN FETCH s.owner
            JOIN FETCH e.actor
            WHERE s.id IN :shelfIds
            AND (
                e.createdAt < :cursorTime
                OR (e.createdAt = :cursorTime AND e.id < :cursorId)
            )
            ORDER BY e.createdAt DESC, e.id DESC
            """)
    List<ShelfEvent> findKeysetPageByShelfIds(
            @Param("shelfIds") List<UUID> shelfIds,
            @Param("cursorTime") Instant cursorTime,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );
}
