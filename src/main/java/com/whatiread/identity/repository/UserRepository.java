package com.whatiread.identity.repository;

import com.whatiread.identity.domain.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    Optional<User> findFirstByOrderByCreatedAtAsc();

    Page<User> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("""
            SELECT u FROM User u
            WHERE LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(u.firstName, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(u.lastName, '')) LIKE LOWER(CONCAT('%', :q, '%'))
            ORDER BY u.createdAt DESC
            """)
    Page<User> searchByEmailOrName(@Param("q") String query, Pageable pageable);
}
