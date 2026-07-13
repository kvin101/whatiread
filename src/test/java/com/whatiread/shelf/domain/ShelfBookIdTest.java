package com.whatiread.shelf.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class ShelfBookIdTest {

    @Test
    void equalsAndHashCodeUseShelfAndUserBookIds() {
        UUID shelfId = UUID.randomUUID();
        UUID userBookId = UUID.randomUUID();
        ShelfBookId first = new ShelfBookId(shelfId, userBookId);
        ShelfBookId second = new ShelfBookId(shelfId, userBookId);
        ShelfBookId different = new ShelfBookId(shelfId, UUID.randomUUID());

        assertThat(first).isEqualTo(second);
        assertThat(first).hasSameHashCodeAs(second);
        assertThat(first).isNotEqualTo(different);
        assertThat(first.getShelfId()).isEqualTo(shelfId);
        assertThat(first.getUserBookId()).isEqualTo(userBookId);
    }
}
