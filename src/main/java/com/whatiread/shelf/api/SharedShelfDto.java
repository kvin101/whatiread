package com.whatiread.shelf.api;

import java.util.List;

public record SharedShelfDto(
        ShelfDto shelf,
        List<ShelfBookDto> books
) {
}
