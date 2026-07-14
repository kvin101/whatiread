package com.whatiread.identity.suggest;

import com.whatiread.identity.suggest.MeilisearchUserIndexClient.IndexedUserHit;
import com.whatiread.shared.suggest.SuggestCandidate;
import com.whatiread.shared.suggest.SuggestRanker;
import com.whatiread.social.domain.FriendRequestStatus;
import com.whatiread.social.repository.FriendRequestRepository;
import com.whatiread.social.repository.FriendshipRepository;
import com.whatiread.social.repository.UserBlockRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class UserSuggestService {

    private static final int DEFAULT_LIMIT = 8;
    private static final int MAX_LIMIT = 20;
    private static final int CANDIDATE_MULTIPLIER = 8;
    private static final int MAX_CANDIDATES = 150;

    private final MeilisearchUserIndexClient indexClient;
    private final FriendshipRepository friendshipRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final UserBlockRepository userBlockRepository;

    public UserSuggestService(
            MeilisearchUserIndexClient indexClient,
            FriendshipRepository friendshipRepository,
            FriendRequestRepository friendRequestRepository,
            UserBlockRepository userBlockRepository
    ) {
        this.indexClient = indexClient;
        this.friendshipRepository = friendshipRepository;
        this.friendRequestRepository = friendRequestRepository;
        this.userBlockRepository = userBlockRepository;
    }

    public List<UserSuggestDto> suggest(UUID viewerId, String query, UserSuggestScope scope, Integer limit) {
        int resolvedLimit = limit == null ? DEFAULT_LIMIT : Math.min(Math.max(limit, 1), MAX_LIMIT);
        FilteredCandidates filtered = filterCandidates(viewerId, query, scope, resolvedLimit);
        return SuggestRanker.rerank(query, filtered.candidates(), resolvedLimit).stream()
                .map(candidate -> toUserSuggestDto(filtered.hitsById().get(candidate.key())))
                .toList();
    }

    public List<AdminUserSuggestDto> suggestAdmin(String query, Integer limit) {
        int resolvedLimit = limit == null ? DEFAULT_LIMIT : Math.min(Math.max(limit, 1), MAX_LIMIT);
        int candidateLimit = candidatePoolSize(query, resolvedLimit);
        List<IndexedUserHit> hits = indexClient.search(query, candidateLimit);
        Map<String, IndexedUserHit> hitsById = new HashMap<>();
        List<SuggestCandidate> candidates = hits.stream()
                .peek(hit -> hitsById.put(hit.id(), hit))
                .map(this::toCandidate)
                .toList();
        return SuggestRanker.rerank(query, candidates, resolvedLimit).stream()
                .map(candidate -> toAdminUserSuggestDto(hitsById.get(candidate.key())))
                .toList();
    }

    private FilteredCandidates filterCandidates(
            UUID viewerId,
            String query,
            UserSuggestScope scope,
            int resolvedLimit
    ) {
        int candidateLimit = candidatePoolSize(query, resolvedLimit);
        List<IndexedUserHit> hits = indexClient.search(query, candidateLimit);
        Set<UUID> allowedIds = allowedUserIds(viewerId, scope);
        Set<UUID> excludedIds = excludedUserIds(viewerId, scope);

        Map<String, IndexedUserHit> hitsById = new HashMap<>();
        List<SuggestCandidate> candidates = new ArrayList<>();
        for (IndexedUserHit hit : hits) {
            UUID userId = UUID.fromString(hit.id());
            if (allowedIds != null && !allowedIds.contains(userId)) {
                continue;
            }
            if (excludedIds.contains(userId)) {
                continue;
            }
            hitsById.put(hit.id(), hit);
            candidates.add(toCandidate(hit));
        }
        return new FilteredCandidates(hitsById, candidates);
    }

    private Set<UUID> allowedUserIds(UUID viewerId, UserSuggestScope scope) {
        if (scope != UserSuggestScope.FRIENDS) {
            return null;
        }
        return new HashSet<>(friendshipRepository.findFriendIdsByUserId(viewerId));
    }

    private Set<UUID> excludedUserIds(UUID viewerId, UserSuggestScope scope) {
        if (scope == UserSuggestScope.ADMIN) {
            return Set.of();
        }
        Set<UUID> excluded = new HashSet<>();
        excluded.add(viewerId);
        if (scope == UserSuggestScope.INVITE) {
            excluded.addAll(friendshipRepository.findFriendIdsByUserId(viewerId));
            friendRequestRepository.findByAddressee_IdAndStatus(viewerId, FriendRequestStatus.PENDING).stream()
                    .map(request -> request.getRequester().getId())
                    .forEach(excluded::add);
            friendRequestRepository.findByRequester_IdAndStatus(viewerId, FriendRequestStatus.PENDING).stream()
                    .map(request -> request.getAddressee().getId())
                    .forEach(excluded::add);
        }
        userBlockRepository.findByBlockerIdOrderByCreatedAtDesc(viewerId).stream()
                .map(block -> block.getBlockedId())
                .forEach(excluded::add);
        userBlockRepository.findBlockerIdsByBlockedId(viewerId).forEach(excluded::add);
        return excluded;
    }

    private SuggestCandidate toCandidate(IndexedUserHit hit) {
        List<String> tokens = new ArrayList<>();
        tokens.addAll(SuggestRanker.tokenize(hit.username()));
        tokens.addAll(SuggestRanker.tokenize(hit.displayName()));
        if (!hit.email().isBlank()) {
            tokens.addAll(SuggestRanker.tokenize(hit.email()));
        }
        return new SuggestCandidate(hit.id(), hit.displayName(), tokens);
    }

    private UserSuggestDto toUserSuggestDto(IndexedUserHit hit) {
        return new UserSuggestDto(UUID.fromString(hit.id()), hit.username(), hit.displayName());
    }

    private AdminUserSuggestDto toAdminUserSuggestDto(IndexedUserHit hit) {
        return new AdminUserSuggestDto(
                UUID.fromString(hit.id()),
                hit.username(),
                hit.displayName(),
                hit.email()
        );
    }

    private static int candidatePoolSize(String query, int resolvedLimit) {
        int trimmedLength = query == null ? 0 : query.trim().length();
        int minimum = trimmedLength <= 5 ? 120 : 40;
        return Math.min(MAX_CANDIDATES, Math.max(resolvedLimit * CANDIDATE_MULTIPLIER, minimum));
    }

    private record FilteredCandidates(Map<String, IndexedUserHit> hitsById, List<SuggestCandidate> candidates) {
    }
}
