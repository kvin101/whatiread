package com.whatiread.catalog.author.repository;

import com.whatiread.catalog.author.domain.BookAuthor;
import com.whatiread.catalog.author.domain.BookAuthor.BookAuthorId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookAuthorRepository extends JpaRepository<BookAuthor, BookAuthorId> {

    @Query("""
            SELECT ba.book FROM BookAuthor ba
            WHERE ba.author.id = :authorId
            ORDER BY ba.book.title ASC
            """)
    Page<com.whatiread.catalog.domain.Book> findBooksByAuthorId(@Param("authorId") UUID authorId, Pageable pageable);

    @Query("""
            SELECT DISTINCT ba.book.id FROM BookAuthor ba
            WHERE ba.author.id = :authorId
            """)
    List<UUID> findBookIdsByAuthorId(@Param("authorId") UUID authorId);
}
