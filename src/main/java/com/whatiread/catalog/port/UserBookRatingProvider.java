package com.whatiread.catalog.port;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface UserBookRatingProvider {

    List<BigDecimal> findRatingsForBook(UUID bookId);
}
