package com.whatiread.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserTest {


    private static final String READER_EXAMPLE_COM = "reader@example.com";
    private static final String HASH = "hash";
    private static final String HTTPS_CDN_EXAMPLE_AVATAR_PNG = "https://cdn.example/avatar.png";
    private static final String SPRINGFIELD = "Springfield";
    private static final String V_1_MAIN_ST = "1 Main St";
    private static final String NEW_HASH = "new-hash";
    private static final String V_555_0100 = "555-0100";
    private static final String NOVELIST = "Novelist";
    private static final String READER = "Reader";
    private static final String JANET = "Janet";
    private static final String APT_2 = "Apt 2";
    private static final String JANE = "Jane";

    @Test
    void displayNameFallsBackToEmail() {
        User user = new User(READER_EXAMPLE_COM, "reader", HASH, null, null);

        assertThat(user.getDisplayName()).isEqualTo(READER_EXAMPLE_COM);
    }

    @Test
    void displayNameCombinesFirstAndLastName() {
        User user = new User(READER_EXAMPLE_COM, "reader", HASH, JANE, "Doe");

        assertThat(user.getDisplayName()).isEqualTo("Jane Doe");
    }

    @Test
    void gettersAndSettersRoundTripProfileFields() {
        User user = new User(READER_EXAMPLE_COM, "reader", HASH, JANE, "Doe");

        user.setPasswordHash(NEW_HASH);
        user.setFirstName(JANET);
        user.setLastName(READER);
        user.setPhoneNumber(V_555_0100);
        user.setAvatarUrl(HTTPS_CDN_EXAMPLE_AVATAR_PNG);
        user.setAddressLine1(V_1_MAIN_ST);
        user.setAddressLine2(APT_2);
        user.setCity(SPRINGFIELD);
        user.setState("IL");
        user.setPostalCode("62701");
        user.setCountry("US");
        user.setWriter(true);
        user.setWriterBio(NOVELIST);

        assertThat(user.getPasswordHash()).isEqualTo(NEW_HASH);
        assertThat(user.getFirstName()).isEqualTo(JANET);
        assertThat(user.getLastName()).isEqualTo(READER);
        assertThat(user.getPhoneNumber()).isEqualTo(V_555_0100);
        assertThat(user.getAvatarUrl()).isEqualTo(HTTPS_CDN_EXAMPLE_AVATAR_PNG);
        assertThat(user.getAddressLine1()).isEqualTo(V_1_MAIN_ST);
        assertThat(user.getAddressLine2()).isEqualTo(APT_2);
        assertThat(user.getCity()).isEqualTo(SPRINGFIELD);
        assertThat(user.getState()).isEqualTo("IL");
        assertThat(user.getPostalCode()).isEqualTo("62701");
        assertThat(user.getCountry()).isEqualTo("US");
        assertThat(user.isWriter()).isTrue();
        assertThat(user.getWriterBio()).isEqualTo(NOVELIST);
        assertThat(user.isEnabled()).isTrue();
    }

    @Test
    void acceptRecommendationsDefaultsTrue() {
        User user = new User(READER_EXAMPLE_COM, "reader", HASH, JANE, "Doe");

        assertThat(user.isAcceptRecommendations()).isTrue();
    }

    @Test
    void acceptRecommendationsCanBeToggled() {
        User user = new User(READER_EXAMPLE_COM, "reader", HASH, JANE, "Doe");

        user.setAcceptRecommendations(false);

        assertThat(user.isAcceptRecommendations()).isFalse();
    }
}
