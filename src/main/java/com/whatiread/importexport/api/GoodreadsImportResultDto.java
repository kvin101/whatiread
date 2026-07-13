package com.whatiread.importexport.api;

public record GoodreadsImportResultDto(
        int rowsProcessed,
        int booksImported,
        int shelvesCreated,
        int skipped,
        int duplicatesSkipped,
        int errors
) {
}
