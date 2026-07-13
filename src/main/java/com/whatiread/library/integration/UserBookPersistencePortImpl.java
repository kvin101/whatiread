package com.whatiread.library.integration;

import com.whatiread.library.domain.UserBook;
import com.whatiread.library.port.UserBookPersistencePort;
import com.whatiread.library.repository.UserBookRepository;
import com.whatiread.shared.exception.ResourceNotFoundException;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class UserBookPersistencePortImpl implements UserBookPersistencePort {

    private final UserBookRepository userBookRepository;

    public UserBookPersistencePortImpl(UserBookRepository userBookRepository) {
        this.userBookRepository = userBookRepository;
    }

    @Override
    public UserBook getOwnedReference(UUID userId, UUID userBookId) {
        return userBookRepository.findByIdAndUserId(userBookId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Library entry not found"));
    }
}
