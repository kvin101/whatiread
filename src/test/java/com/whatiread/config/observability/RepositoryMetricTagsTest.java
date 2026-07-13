package com.whatiread.config.observability;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RepositoryMetricTagsTest {

    private static final String UNKNOWN_TABLE = "unknown";

    @ParameterizedTest
    @CsvSource({
            "findByUserId, select",
            "getReference, select",
            "readAll, select",
            "countUnread, select",
            "existsById, select",
            "listActive, select",
            "searchByTitle, select",
            "queryCustom, select",
            "deleteById, delete",
            "removeAll, delete",
            "save, write",
            "insertRow, write",
            "persistEntity, write",
            "updateStatus, update",
            "customQueryAll, other"
    })
    void infersOperationFromMethodPrefix(String methodName, String expected) {
        assertThat(RepositoryMetricTags.inferOperation(methodName)).isEqualTo(expected);
    }

    @Test
    void infersSnakeCaseTableFromRepositoryName() {
        assertThat(RepositoryMetricTags.inferTable("UserBookRepository")).isEqualTo("user_book");
        assertThat(RepositoryMetricTags.inferTable("MessageRepository")).isEqualTo("message");
        assertThat(RepositoryMetricTags.inferTable("FriendRequest")).isEqualTo("friend_request");
    }

    @Test
    void fallsBackToUnknownTableForBlankName() {
        assertThat(RepositoryMetricTags.inferTable("")).isEqualTo(UNKNOWN_TABLE);
        assertThat(RepositoryMetricTags.inferTable("   ")).isEqualTo(UNKNOWN_TABLE);
    }
}
