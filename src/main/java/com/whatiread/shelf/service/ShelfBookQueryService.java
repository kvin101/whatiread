package com.whatiread.shelf.service;

import com.whatiread.library.api.UserBookDto;
import com.whatiread.library.service.LibraryService;
import com.whatiread.shelf.domain.Shelf;
import com.whatiread.shelf.domain.ShelfBook;
import com.whatiread.shelf.repository.ShelfBookRepository;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ShelfBookQueryService {

    private final ShelfBookRepository shelfBookRepository;
    private final LibraryService libraryService;

    public ShelfBookQueryService(ShelfBookRepository shelfBookRepository, LibraryService libraryService) {
        this.shelfBookRepository = shelfBookRepository;
        this.libraryService = libraryService;
    }

    public Map<UUID, UserBookDto> loadShelfUserBooks(UUID ownerId, List<ShelfBook> entries) {
        List<UUID> userBookIds = entries.stream()
                .map(sb -> sb.getUserBook().getId())
                .toList();
        return libraryService.listByIds(ownerId, userBookIds).stream()
                .collect(java.util.stream.Collectors.toMap(UserBookDto::id, dto -> dto));
    }

    public Map<UUID, Integer> loadBookCounts(Collection<Shelf> shelves) {
        return loadBookCountsByIds(shelves.stream().map(Shelf::getId).toList());
    }

    public Map<UUID, Integer> loadBookCountsByIds(Collection<UUID> shelfIds) {
        if (shelfIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Integer> counts = new HashMap<>();
        shelfBookRepository.countBooksByShelfIds(shelfIds).forEach(view ->
                counts.put(view.getShelfId(), Math.toIntExact(view.getBookCount()))
        );
        for (UUID shelfId : shelfIds) {
            if (!counts.containsKey(shelfId)) {
                counts.put(shelfId, Math.toIntExact(shelfBookRepository.countByShelf_Id(shelfId)));
            }
        }
        return counts;
    }
}
