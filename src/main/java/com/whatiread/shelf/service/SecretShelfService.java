package com.whatiread.shelf.service;

import com.whatiread.shared.exception.ConflictException;
import com.whatiread.shared.exception.ForbiddenException;
import com.whatiread.shelf.domain.Shelf;
import com.whatiread.shelf.domain.ShelfVisibility;
import com.whatiread.shelf.repository.ShelfMemberRepository;
import com.whatiread.shelf.repository.ShelfRepository;
import com.whatiread.shelf.repository.ShelfShareLinkRepository;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SecretShelfService {

    private static final Pattern PIN_PATTERN = Pattern.compile("^\\d{4}$");

    private final PasswordEncoder passwordEncoder;
    private final ShelfRepository shelfRepository;
    private final ShelfMemberRepository shelfMemberRepository;
    private final ShelfShareLinkRepository shelfShareLinkRepository;

    public SecretShelfService(
            PasswordEncoder passwordEncoder,
            ShelfRepository shelfRepository,
            ShelfMemberRepository shelfMemberRepository,
            ShelfShareLinkRepository shelfShareLinkRepository
    ) {
        this.passwordEncoder = passwordEncoder;
        this.shelfRepository = shelfRepository;
        this.shelfMemberRepository = shelfMemberRepository;
        this.shelfShareLinkRepository = shelfShareLinkRepository;
    }

    public void validatePinFormat(String pin) {
        if (!StringUtils.hasText(pin) || !PIN_PATTERN.matcher(pin).matches()) {
            throw new IllegalArgumentException("PIN must be exactly 4 digits");
        }
    }

    public String hashPin(String pin) {
        validatePinFormat(pin);
        return passwordEncoder.encode(pin);
    }

    public boolean verifyPin(Shelf shelf, String pin) {
        if (!StringUtils.hasText(shelf.getPinHash())) {
            return false;
        }
        validatePinFormat(pin);
        return passwordEncoder.matches(pin, shelf.getPinHash());
    }

    public void requirePinWhenSecret(ShelfVisibility visibility, String pin) {
        if (visibility == ShelfVisibility.SECRET && !StringUtils.hasText(pin)) {
            throw new IllegalArgumentException("A 4-digit PIN is required for secret shelves");
        }
    }

    public void ensureSingleSecretShelf(UUID ownerId, UUID excludeShelfId) {
        boolean exists = excludeShelfId == null
                ? shelfRepository.existsByOwner_IdAndVisibility(ownerId, ShelfVisibility.SECRET)
                : shelfRepository.existsByOwner_IdAndVisibilityAndIdNot(ownerId, ShelfVisibility.SECRET, excludeShelfId);
        if (exists) {
            throw new ConflictException("You can only have one secret shelf");
        }
    }

    public void enforceOwnerOnly(Shelf shelf, UUID userId) {
        if (shelf.getVisibility() == ShelfVisibility.SECRET && !shelf.getOwner().getId().equals(userId)) {
            throw new ForbiddenException("Secret shelves are only visible to their owner");
        }
    }

    public void enforceNotShareable(Shelf shelf) {
        if (shelf.getVisibility() == ShelfVisibility.SECRET) {
            throw new ForbiddenException("Secret shelves cannot be shared");
        }
    }

    public void stripSharing(Shelf shelf) {
        UUID ownerId = shelf.getOwner().getId();
        shelfMemberRepository.findByShelf_Id(shelf.getId()).stream()
                .filter(member -> !member.getUser().getId().equals(ownerId))
                .forEach(shelfMemberRepository::delete);
        shelfShareLinkRepository.findByShelf_IdOrderByCreatedAtDesc(shelf.getId()).forEach(link -> {
            if (link.getRevokedAt() == null) {
                link.revoke();
                shelfShareLinkRepository.save(link);
            }
        });
    }

    public boolean isSecret(Shelf shelf) {
        return shelf.getVisibility() == ShelfVisibility.SECRET;
    }
}
