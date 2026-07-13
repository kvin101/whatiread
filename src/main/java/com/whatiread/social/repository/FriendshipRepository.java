package com.whatiread.social.repository;

import com.whatiread.social.domain.Friendship;
import com.whatiread.social.domain.Friendship.FriendshipId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FriendshipRepository extends JpaRepository<Friendship, FriendshipId> {

    List<Friendship> findByUser_Id(UUID userId);

    boolean existsByUser_IdAndFriend_Id(UUID userId, UUID friendId);

    @Query("SELECT f.friend.id FROM Friendship f WHERE f.user.id = :userId")
    List<UUID> findFriendIdsByUserId(@Param("userId") UUID userId);
}
