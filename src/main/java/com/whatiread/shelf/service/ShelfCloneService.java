package com.whatiread.shelf.service;

import com.whatiread.library.port.UserBookPersistencePort;
import com.whatiread.library.service.LibraryService;
import com.whatiread.shelf.domain.Shelf;
import com.whatiread.shelf.domain.ShelfBook;
import com.whatiread.shelf.repository.ShelfBookRepository;
import java.util.UUID;
import java.util.function.Predicate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ShelfCloneService {

    private final ShelfBookRepository shelfBookRepository;
    private final UserBookPersistencePort userBookPersistencePort;
    private final LibraryService libraryService;

    public ShelfCloneService(
            ShelfBookRepository shelfBookRepository,
            UserBookPersistencePort userBookPersistencePort,
            LibraryService libraryService
    ) {
        this.shelfBookRepository = shelfBookRepository;
        this.userBookPersistencePort = userBookPersistencePort;
        this.libraryService = libraryService;
    }

    public void cloneWithBooks(
            Shelf source,
            Shelf clone,
            UUID recipientId,
            boolean includeBooks,
            Predicate<ShelfBook> canCloneBook
    ) {
        if (!includeBooks) {
            return;
        }
        int position = 0;
        for (ShelfBook sourceBook : shelfBookRepository.findByShelf_IdOrderByPositionAsc(source.getId())) {
            if (!canCloneBook.test(sourceBook)) {
                continue;
            }
            UUID bookId = sourceBook.getUserBook().getBook().getId();
            UUID userBookId = libraryService.ensureInLibrary(recipientId, bookId).id();
            ShelfBook cloneBook = new ShelfBook(
                    clone,
                    userBookPersistencePort.getOwnedReference(recipientId, userBookId),
                    position++,
                    recipientId
            );
            shelfBookRepository.save(cloneBook);
        }
    }
}
