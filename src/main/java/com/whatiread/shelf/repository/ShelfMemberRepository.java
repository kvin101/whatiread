package com.whatiread.shelf.repository;

import com.whatiread.shelf.domain.ShelfMember;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShelfMemberRepository extends JpaRepository<ShelfMember, UUID> {

    List<ShelfMember> findByShelf_Id(UUID shelfId);

    List<ShelfMember> findByUser_Id(UUID userId);

    Optional<ShelfMember> findByShelf_IdAndUser_Id(UUID shelfId, UUID userId);

    @Query("""
            SELECT sm.shelf FROM ShelfMember sm
            WHERE sm.user.id = :userId
            ORDER BY sm.shelf.sortOrder ASC, sm.shelf.name ASC
            """)
    List<com.whatiread.shelf.domain.Shelf> findShelvesForUser(@Param("userId") UUID userId);
}
