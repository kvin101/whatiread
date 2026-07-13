package com.whatiread.shelf.repository;

import com.whatiread.shelf.domain.Shelf;
import com.whatiread.shelf.domain.ShelfVisibility;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShelfRepository extends JpaRepository<Shelf, UUID> {

    List<Shelf> findByOwner_IdOrderBySortOrderAsc(UUID ownerId);

    List<Shelf> findByOwner_IdAndVisibilityOrderBySortOrderAsc(UUID ownerId, ShelfVisibility visibility);

    Optional<Shelf> findByOwner_IdAndSlug(UUID ownerId, String slug);

    Optional<Shelf> findByOwner_IdAndNameIgnoreCase(UUID ownerId, String name);

    boolean existsByOwner_IdAndSlug(UUID ownerId, String slug);

    Page<Shelf> findByVisibilityOrderByUpdatedAtDesc(ShelfVisibility visibility, Pageable pageable);

    /**
     * Shelves from other users visible to the viewer: public, friends-only (if friends), or shared via membership.
     */
    @Query(
            value = """
                    SELECT DISTINCT s FROM Shelf s
                    JOIN FETCH s.owner
                    LEFT JOIN ShelfMember sm ON sm.shelf = s AND sm.user.id = :viewerId
                    WHERE s.owner.id <> :viewerId
                    AND (
                        s.visibility = com.whatiread.shelf.domain.ShelfVisibility.PUBLIC
                        OR (
                            s.visibility = com.whatiread.shelf.domain.ShelfVisibility.FRIENDS
                            AND (:friendIdsSize > 0 AND s.owner.id IN :friendIds)
                        )
                        OR sm IS NOT NULL
                    )
                    ORDER BY s.updatedAt DESC
                    """,
            countQuery = """
                    SELECT COUNT(DISTINCT s.id) FROM Shelf s
                    LEFT JOIN ShelfMember sm ON sm.shelf = s AND sm.user.id = :viewerId
                    WHERE s.owner.id <> :viewerId
                    AND (
                        s.visibility = com.whatiread.shelf.domain.ShelfVisibility.PUBLIC
                        OR (
                            s.visibility = com.whatiread.shelf.domain.ShelfVisibility.FRIENDS
                            AND (:friendIdsSize > 0 AND s.owner.id IN :friendIds)
                        )
                        OR sm IS NOT NULL
                    )
                    """
    )
    Page<Shelf> findExploreFeed(
            @Param("viewerId") UUID viewerId,
            @Param("friendIdsSize") int friendIdsSize,
            @Param("friendIds") List<UUID> friendIds,
            Pageable pageable
    );
}
