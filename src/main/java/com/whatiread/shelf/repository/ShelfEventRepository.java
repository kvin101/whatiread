package com.whatiread.shelf.repository;

import com.whatiread.shelf.domain.ShelfEvent;
import com.whatiread.shelf.domain.ShelfEventType;
import java.util.Collection;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShelfEventRepository extends JpaRepository<ShelfEvent, UUID> {

    Page<ShelfEvent> findByShelf_IdOrderByCreatedAtDesc(UUID shelfId, Pageable pageable);

    Page<ShelfEvent> findByShelf_IdAndEventTypeInOrderByCreatedAtDesc(
            UUID shelfId, Collection<ShelfEventType> eventTypes, Pageable pageable);
}
