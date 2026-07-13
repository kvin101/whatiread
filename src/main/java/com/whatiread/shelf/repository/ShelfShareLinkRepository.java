package com.whatiread.shelf.repository;

import com.whatiread.shelf.domain.ShelfShareLink;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShelfShareLinkRepository extends JpaRepository<ShelfShareLink, UUID> {

    Optional<ShelfShareLink> findByToken(UUID token);

    List<ShelfShareLink> findByShelf_IdOrderByCreatedAtDesc(UUID shelfId);
}
