package com.whatiread.catalog.author.repository;

import com.whatiread.catalog.author.domain.Author;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuthorRepository extends JpaRepository<Author, UUID> {

    Optional<Author> findBySlug(String slug);

    @Query("""
            SELECT a FROM Author a
            WHERE LOWER(TRIM(a.name)) = LOWER(TRIM(:name))
            """)
    Optional<Author> findByNormalizedName(@Param("name") String name);

    boolean existsBySlug(String slug);
}
