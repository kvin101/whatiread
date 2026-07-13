package com.whatiread.shelf.integration;

import com.whatiread.library.port.ShelfBookQueryPort;
import com.whatiread.shelf.repository.ShelfBookRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ShelfBookQueryPortImpl implements ShelfBookQueryPort {

    private final ShelfBookRepository shelfBookRepository;

    public ShelfBookQueryPortImpl(ShelfBookRepository shelfBookRepository) {
        this.shelfBookRepository = shelfBookRepository;
    }

    @Override
    public List<UUID> findUserBookIdsOnShelf(UUID userId, UUID shelfId) {
        return shelfBookRepository.findUserBookIdsByShelfAndOwner(userId, shelfId);
    }

    @Override
    public Map<UUID, List<String>> getShelfNamesByUserBookIds(UUID userId, List<UUID> userBookIds) {
        Map<UUID, List<String>> result = new HashMap<>();
        for (UUID userBookId : userBookIds) {
            List<String> names = shelfBookRepository.findByUserBook_Id(userBookId).stream()
                    .filter(sb -> sb.getShelf().getOwner().getId().equals(userId))
                    .map(sb -> sb.getShelf().getName())
                    .toList();
            result.put(userBookId, names);
        }
        return result;
    }

    @Override
    public boolean existsOnShelf(UUID shelfId, UUID userBookId) {
        return shelfBookRepository.existsByShelf_IdAndUserBook_Id(shelfId, userBookId);
    }
}
