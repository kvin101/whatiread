package com.whatiread.shelf.repository;

import com.whatiread.shelf.domain.ShelfBook;
import com.whatiread.shelf.domain.ShelfBookId;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShelfBookRepository extends JpaRepository<ShelfBook, ShelfBookId> {

    List<ShelfBook> findByShelf_IdOrderByPositionAsc(UUID shelfId);

    @EntityGraph(attributePaths = "shelf")
    List<ShelfBook> findByUserBook_Id(UUID userBookId);

    @EntityGraph(attributePaths = "shelf")
    @Query("""
            SELECT sb FROM ShelfBook sb
            WHERE sb.userBook.id IN :userBookIds
              AND sb.shelf.owner.id = :userId
            """)
    List<ShelfBook> findByUserBookIdsAndOwner(
            @Param("userId") UUID userId,
            @Param("userBookIds") Collection<UUID> userBookIds
    );

    Optional<ShelfBook> findByShelf_IdAndUserBook_Id(UUID shelfId, UUID userBookId);

    boolean existsByShelf_IdAndUserBook_Id(UUID shelfId, UUID userBookId);

    @Query("SELECT COALESCE(MAX(sb.position), -1) FROM ShelfBook sb WHERE sb.shelf.id = :shelfId")
    int maxPosition(@Param("shelfId") UUID shelfId);

    long countByShelf_Id(UUID shelfId);

    @Query("""
            SELECT sb.shelf.id AS shelfId, COUNT(sb) AS bookCount
            FROM ShelfBook sb
            WHERE sb.shelf.id IN :shelfIds
            GROUP BY sb.shelf.id
            """)
    List<ShelfBookCountView> countBooksByShelfIds(@Param("shelfIds") Collection<UUID> shelfIds);

    @Query("""
            SELECT sb FROM ShelfBook sb
            JOIN FETCH sb.userBook ub
            JOIN FETCH ub.book b
            WHERE sb.shelf.id = :shelfId
            AND LOWER(TRIM(b.title)) = LOWER(TRIM(:title))
            """)
    List<ShelfBook> findByShelfIdAndBookTitle(@Param("shelfId") UUID shelfId, @Param("title") String title);

    @EntityGraph(attributePaths = {"shelf", "userBook"})
    @Query("SELECT sb FROM ShelfBook sb JOIN sb.userBook ub WHERE ub.book.id = :bookId")
    List<ShelfBook> findByBookId(@Param("bookId") UUID bookId);

    @Query("""
            SELECT sb.userBook.id FROM ShelfBook sb
            WHERE sb.shelf.id = :shelfId AND sb.userBook.user.id = :userId
            ORDER BY sb.position ASC
            """)
    List<UUID> findUserBookIdsByShelfAndOwner(@Param("userId") UUID userId, @Param("shelfId") UUID shelfId);
}
