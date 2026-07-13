package com.whatiread.social.repository;

import com.whatiread.social.domain.FriendRequest;
import com.whatiread.social.domain.FriendRequestStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FriendRequestRepository extends JpaRepository<FriendRequest, UUID> {

    Optional<FriendRequest> findByRequester_IdAndAddressee_Id(UUID requesterId, UUID addresseeId);

    List<FriendRequest> findByAddressee_IdAndStatus(UUID addresseeId, FriendRequestStatus status);

    List<FriendRequest> findByRequester_IdAndStatus(UUID requesterId, FriendRequestStatus status);

    Optional<FriendRequest> findByIdAndAddressee_Id(UUID id, UUID addresseeId);

    Optional<FriendRequest> findByIdAndRequester_Id(UUID id, UUID requesterId);
}
