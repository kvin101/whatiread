package com.whatiread.shelf.repository;

import java.util.UUID;

public interface ShelfBookCountView {

    UUID getShelfId();

    long getBookCount();
}
