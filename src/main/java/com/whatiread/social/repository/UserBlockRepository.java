package com.whatiread.social.repository;

import com.whatiread.social.domain.UserBlock;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserBlockRepository extends JpaRepository<UserBlock, UserBlock.UserBlockId> {

    boolean existsByBlockerIdAndBlockedId(UUID blockerId, UUID blockedId);

    List<UserBlock> findByBlockerIdOrderByCreatedAtDesc(UUID blockerId);

    @Query("SELECT ub.blockerId FROM UserBlock ub WHERE ub.blockedId = :blockedId")
    List<UUID> findBlockerIdsByBlockedId(@Param("blockedId") UUID blockedId);
}
