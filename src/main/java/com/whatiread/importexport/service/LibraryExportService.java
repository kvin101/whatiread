package com.whatiread.importexport.service;

import java.util.UUID;

public interface LibraryExportService {

    byte[] exportCsv(UUID userId);

    byte[] exportJson(UUID userId);
}
