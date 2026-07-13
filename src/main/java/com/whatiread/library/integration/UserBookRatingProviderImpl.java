package com.whatiread.library.integration;

import com.whatiread.catalog.port.UserBookRatingProvider;
import com.whatiread.library.repository.UserBookRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class UserBookRatingProviderImpl implements UserBookRatingProvider {

    private final UserBookRepository userBookRepository;

    public UserBookRatingProviderImpl(UserBookRepository userBookRepository) {
        this.userBookRepository = userBookRepository;
    }

    @Override
    public List<BigDecimal> findRatingsForBook(UUID bookId) {
        return userBookRepository.findRatingsByBookId(bookId);
    }
}
