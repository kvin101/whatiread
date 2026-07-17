package com.whatiread.library.repository;

import com.whatiread.library.domain.ReadingStatus;
import com.whatiread.library.domain.UserBook;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserBookRepository extends JpaRepository<UserBook, UUID> {

    @EntityGraph(attributePaths = "book")
    Page<UserBook> findByUserId(UUID userId, Pageable pageable);

    @EntityGraph(attributePaths = "book")
    Page<UserBook> findByUserIdAndStatus(UUID userId, ReadingStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "book")
    Optional<UserBook> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByUserIdAndBookId(UUID userId, UUID bookId);

    @EntityGraph(attributePaths = "book")
    Optional<UserBook> findByUserIdAndBook_Id(UUID userId, UUID bookId);

    @Query("""
            SELECT ub.book.id FROM UserBook ub
            WHERE ub.user.id = :userId
            AND ub.book.id IN :bookIds
            """)
    Set<UUID> findOwnedBookIdsByUserIdAndBookIdIn(
            @Param("userId") UUID userId,
            @Param("bookIds") Collection<UUID> bookIds
    );

    @Query("""
            SELECT ub FROM UserBook ub JOIN FETCH ub.book b
            WHERE ub.user.id = :userId
            AND LOWER(TRIM(b.title)) = LOWER(TRIM(:title))
            """)
    List<UserBook> findByUserIdAndNormalizedBookTitle(
            @Param("userId") UUID userId,
            @Param("title") String title
    );

    @Query("""
            SELECT ub.rating FROM UserBook ub
            WHERE ub.book.id = :bookId AND ub.rating IS NOT NULL
            """)
    List<BigDecimal> findRatingsByBookId(@Param("bookId") UUID bookId);

    @EntityGraph(attributePaths = "book")
    @Query("SELECT ub FROM UserBook ub WHERE ub.user.id = :userId ORDER BY ub.updatedAt DESC")
    List<UserBook> findAllByUserIdOrderByUpdatedAtDesc(@Param("userId") UUID userId);

    @EntityGraph(attributePaths = "book")
    Page<UserBook> findByUserIdAndIdIn(UUID userId, List<UUID> ids, Pageable pageable);

    @EntityGraph(attributePaths = "book")
    Page<UserBook> findByUserIdAndStatusAndIdIn(UUID userId, ReadingStatus status, List<UUID> ids, Pageable pageable);

    @EntityGraph(attributePaths = "book")
    @Query("""
            SELECT ub FROM UserBook ub
            JOIN ub.book b
            WHERE ub.user.id = :userId
            AND ub.id IN :ids
            AND (
                LOWER(b.title) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(b.isbn, '')) LIKE LOWER(CONCAT('%', :q, '%'))
            )
            """)
    Page<UserBook> searchByUserIdAndIdIn(
            @Param("userId") UUID userId,
            @Param("ids") List<UUID> ids,
            @Param("q") String q,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "book")
    @Query("""
            SELECT ub FROM UserBook ub
            JOIN ub.book b
            WHERE ub.user.id = :userId
            AND ub.status = :status
            AND ub.id IN :ids
            AND (
                LOWER(b.title) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(b.isbn, '')) LIKE LOWER(CONCAT('%', :q, '%'))
            )
            """)
    Page<UserBook> searchByUserIdAndStatusAndIdIn(
            @Param("userId") UUID userId,
            @Param("status") ReadingStatus status,
            @Param("ids") List<UUID> ids,
            @Param("q") String q,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "book")
    @Query("""
            SELECT ub FROM UserBook ub
            JOIN ub.book b
            WHERE ub.user.id = :userId
            AND (
                LOWER(b.title) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(b.isbn, '')) LIKE LOWER(CONCAT('%', :q, '%'))
            )
            """)
    Page<UserBook> searchByUserId(@Param("userId") UUID userId, @Param("q") String q, Pageable pageable);

    @EntityGraph(attributePaths = "book")
    @Query("""
            SELECT ub FROM UserBook ub
            JOIN ub.book b
            WHERE ub.user.id = :userId
            AND ub.status = :status
            AND (
                LOWER(b.title) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(b.isbn, '')) LIKE LOWER(CONCAT('%', :q, '%'))
            )
            """)
    Page<UserBook> searchByUserIdAndStatus(
            @Param("userId") UUID userId,
            @Param("status") ReadingStatus status,
            @Param("q") String q,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "book")
    @Query("""
            SELECT ub FROM UserBook ub
            WHERE ub.user.id = :userId
            AND (:status IS NULL OR ub.status = :status)
            ORDER BY ub.updatedAt DESC, ub.id DESC
            """)
    List<UserBook> findFirstKeysetPageByUser(
            @Param("userId") UUID userId,
            @Param("status") ReadingStatus status,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "book")
    @Query("""
            SELECT ub FROM UserBook ub
            WHERE ub.user.id = :userId
            AND (:status IS NULL OR ub.status = :status)
            AND (
                ub.updatedAt < :cursorTime
                OR (ub.updatedAt = :cursorTime AND ub.id < :cursorId)
            )
            ORDER BY ub.updatedAt DESC, ub.id DESC
            """)
    List<UserBook> findKeysetByUser(
            @Param("userId") UUID userId,
            @Param("status") ReadingStatus status,
            @Param("cursorTime") Instant cursorTime,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );

    @Query(
            value = """
                    SELECT COUNT(*) FROM user_books ub
                    WHERE ub.user_id = :userId
                    AND ub.status = 'READ'
                    AND ub.finished_at IS NOT NULL
                    AND EXTRACT(YEAR FROM ub.finished_at) = :year
                    """,
            nativeQuery = true
    )
    int countBooksReadInYear(@Param("userId") UUID userId, @Param("year") short year);

    @Query(
            value = """
                    SELECT COALESCE(SUM(COALESCE(b.page_count, ub.progress_pages, 0)), 0)
                    FROM user_books ub
                    JOIN books b ON b.id = ub.book_id
                    WHERE ub.user_id = :userId
                    AND ub.status = 'READ'
                    AND ub.finished_at IS NOT NULL
                    AND EXTRACT(YEAR FROM ub.finished_at) = :year
                    """,
            nativeQuery = true
    )
    int sumPagesReadInYear(@Param("userId") UUID userId, @Param("year") short year);

    @EntityGraph(attributePaths = "book")
    Page<UserBook> findByUserIdAndBook_IdIn(UUID userId, Collection<UUID> bookIds, Pageable pageable);

    @EntityGraph(attributePaths = "book")
    @Query("""
            SELECT ub FROM UserBook ub
            JOIN ub.book b
            JOIN com.whatiread.catalog.author.domain.BookAuthor ba ON ba.book = b AND ba.author.id = :authorId
            WHERE ub.user.id = :userId
            """)
    Page<UserBook> findByUserIdAndAuthorId(
            @Param("userId") UUID userId,
            @Param("authorId") UUID authorId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "book")
    @Query("""
            SELECT ub FROM UserBook ub
            JOIN ub.book b
            JOIN com.whatiread.catalog.author.domain.BookAuthor ba ON ba.book = b AND ba.author.id = :authorId
            WHERE ub.user.id = :userId
            AND ub.status = :status
            """)
    Page<UserBook> findByUserIdAndStatusAndAuthorId(
            @Param("userId") UUID userId,
            @Param("status") ReadingStatus status,
            @Param("authorId") UUID authorId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "book")
    @Query("SELECT ub FROM UserBook ub WHERE ub.user.id = :userId AND ub.id IN :ids")
    List<UserBook> findOwnedByUserIdAndIdIn(@Param("userId") UUID userId, @Param("ids") Collection<UUID> ids);
    @Query("""
            SELECT ub FROM UserBook ub
            JOIN FETCH ub.user u
            JOIN FETCH ub.book b
            WHERE u.id IN :userIds
            AND b.id IN :bookIds
            AND ub.status = com.whatiread.library.domain.ReadingStatus.READING
            """)
    List<UserBook> findReadingByUsersAndBookIdsIn(
            @Param("userIds") Collection<UUID> userIds,
            @Param("bookIds") Collection<UUID> bookIds
    );

}
