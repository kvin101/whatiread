package com.whatiread.shelf.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.whatiread.library.api.UserBookDto;
import com.whatiread.library.domain.ReadingStatus;
import com.whatiread.library.service.LibraryService;
import com.whatiread.shelf.domain.Shelf;
import com.whatiread.shelf.domain.ShelfBook;
import com.whatiread.shelf.repository.ShelfBookCountView;
import com.whatiread.shelf.repository.ShelfBookRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShelfBookQueryServiceTest {

    @Mock
    private ShelfBookRepository shelfBookRepository;
    @Mock
    private LibraryService libraryService;

    @InjectMocks
    private ShelfBookQueryService shelfBookQueryService;

    @Test
    void loadBookCountsByIdsFillsMissingShelvesWithZero() {
        UUID shelfId = UUID.randomUUID();
        when(shelfBookRepository.countBooksByShelfIds(List.of(shelfId))).thenReturn(List.of());
        when(shelfBookRepository.countByShelf_Id(shelfId)).thenReturn(0L);

        Map<UUID, Integer> counts = shelfBookQueryService.loadBookCountsByIds(List.of(shelfId));

        assertThat(counts).containsEntry(shelfId, 0);
    }

    @Test
    void loadShelfUserBooksDelegatesToLibraryService() {
        UUID ownerId = UUID.randomUUID();
        UUID userBookId = UUID.randomUUID();
        ShelfBook entry = org.mockito.Mockito.mock(ShelfBook.class);
        var userBook = org.mockito.Mockito.mock(com.whatiread.library.domain.UserBook.class);
        when(entry.getUserBook()).thenReturn(userBook);
        when(userBook.getId()).thenReturn(userBookId);
        when(libraryService.listByIds(ownerId, List.of(userBookId)))
                .thenReturn(List.of(new UserBookDto(
                        userBookId,
                        null,
                        ReadingStatus.TO_READ,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        List.of(),
                        null,
                        null
                )));

        Map<UUID, UserBookDto> result = shelfBookQueryService.loadShelfUserBooks(ownerId, List.of(entry));

        assertThat(result).containsKey(userBookId);
    }

    @Test
    void loadBookCountsUsesBatchView() {
        UUID shelfId = UUID.randomUUID();
        Shelf shelf = org.mockito.Mockito.mock(Shelf.class);
        when(shelf.getId()).thenReturn(shelfId);
        when(shelfBookRepository.countBooksByShelfIds(List.of(shelfId)))
                .thenReturn(List.of(new ShelfBookCountView() {
                    @Override
                    public UUID getShelfId() {
                        return shelfId;
                    }

                    @Override
                    public long getBookCount() {
                        return 3L;
                    }
                }));

        assertThat(shelfBookQueryService.loadBookCounts(List.of(shelf))).containsEntry(shelfId, 3);
    }
}
