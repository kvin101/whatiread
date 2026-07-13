package com.whatiread.shelf.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ExploreShelfReadModelTest {


    private static final String READING = "Reading";
    private static final String READING_2 = "reading";
    private static final String TOKEN = "token";
    private static final String DESC = "desc";

    @Test
    void gettersAndSettersRoundTrip() {
        ExploreShelfReadModel model = new ExploreShelfReadModel();
        UUID shelfId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Instant updatedAt = Instant.parse("2024-06-01T00:00:00Z");

        model.setShelfId(shelfId);
        model.setOwnerId(ownerId);
        model.setName(READING);
        model.setSlug(READING_2);
        model.setVisibility(ShelfVisibility.PUBLIC);
        model.setDescription(DESC);
        model.setIcon("📚");
        model.setBookCount(5);
        model.setUpdatedAt(updatedAt);
        model.setCursorToken(TOKEN);

        assertThat(model.getShelfId()).isEqualTo(shelfId);
        assertThat(model.getOwnerId()).isEqualTo(ownerId);
        assertThat(model.getName()).isEqualTo(READING);
        assertThat(model.getSlug()).isEqualTo(READING_2);
        assertThat(model.getVisibility()).isEqualTo(ShelfVisibility.PUBLIC);
        assertThat(model.getDescription()).isEqualTo(DESC);
        assertThat(model.getIcon()).isEqualTo("📚");
        assertThat(model.getBookCount()).isEqualTo(5);
        assertThat(model.getUpdatedAt()).isEqualTo(updatedAt);
        assertThat(model.getCursorToken()).isEqualTo(TOKEN);
    }
}
