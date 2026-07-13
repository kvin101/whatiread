package com.whatiread.catalog.repository;

import com.whatiread.catalog.domain.Book;
import com.whatiread.catalog.domain.BookSource;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookRepository extends JpaRepository<Book, UUID> {

    Optional<Book> findByIsbn(String isbn);

    Optional<Book> findBySourceAndExternalId(BookSource source, String externalId);

    @Query("SELECT b FROM Book b WHERE LOWER(TRIM(b.title)) = LOWER(TRIM(:title))")
    List<Book> findByNormalizedTitle(@Param("title") String title);
}
