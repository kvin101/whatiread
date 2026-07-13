package com.whatiread.shared.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SlugUtilsTest {

    private static final String DEFAULT_SLUG = "item";

    @Test
    void slugifyNormalizesTitle() {
        assertEquals("the-left-hand-of-darkness", SlugUtils.slugify("The Left Hand of Darkness"));
    }

    @Test
    void slugifyStripsAccentsAndSpecialCharacters() {
        assertEquals("cafe-noir", SlugUtils.slugify("Café Noir!"));
    }

    @Test
    void slugifyReturnsDefaultForBlankInput() {
        assertEquals(DEFAULT_SLUG, SlugUtils.slugify(null));
        assertEquals(DEFAULT_SLUG, SlugUtils.slugify("   "));
        assertEquals(DEFAULT_SLUG, SlugUtils.slugify("!!!"));
    }
}
