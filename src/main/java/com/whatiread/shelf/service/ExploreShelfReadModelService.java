package com.whatiread.shelf.service;

import com.whatiread.shelf.domain.ExploreShelfReadModel;
import com.whatiread.shelf.domain.Shelf;
import com.whatiread.shelf.repository.ExploreShelfReadModelRepository;
import com.whatiread.shelf.repository.ShelfBookRepository;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExploreShelfReadModelService {

    private final ExploreShelfReadModelRepository repository;
    private final ShelfBookRepository shelfBookRepository;

    public ExploreShelfReadModelService(
            ExploreShelfReadModelRepository repository,
            ShelfBookRepository shelfBookRepository
    ) {
        this.repository = repository;
        this.shelfBookRepository = shelfBookRepository;
    }

    static String encodeCursor(Instant updatedAt, UUID shelfId) {
        String raw = updatedAt.toEpochMilli() + ":" + shelfId;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes());
    }

    static CursorParts decodeCursor(String cursor) {
        String decoded = new String(Base64.getUrlDecoder().decode(cursor));
        String[] parts = decoded.split(":", 2);
        return new CursorParts(Instant.ofEpochMilli(Long.parseLong(parts[0])), UUID.fromString(parts[1]));
    }

    @Transactional
    public void sync(Shelf shelf) {
        ExploreShelfReadModel model = repository.findById(shelf.getId()).orElseGet(ExploreShelfReadModel::new);
        model.setShelfId(shelf.getId());
        model.setOwnerId(shelf.getOwner().getId());
        model.setName(shelf.getName());
        model.setSlug(shelf.getSlug());
        model.setVisibility(shelf.getVisibility());
        model.setDescription(shelf.getDescription());
        model.setIcon(shelf.getIcon());
        model.setBookCount((int) shelfBookRepository.countByShelf_Id(shelf.getId()));
        Instant updatedAt = shelf.getUpdatedAt() != null ? shelf.getUpdatedAt() : Instant.now();
        model.setUpdatedAt(updatedAt);
        model.setCursorToken(encodeCursor(updatedAt, shelf.getId()));
        repository.save(model);
    }

    @Transactional
    public void remove(UUID shelfId) {
        repository.deleteById(shelfId);
    }

    record CursorParts(Instant updatedAt, UUID shelfId) {
    }
}
