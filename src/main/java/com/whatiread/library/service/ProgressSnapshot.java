package com.whatiread.library.service;

public record ProgressSnapshot(
        Integer progressPages,
        Integer pageCount,
        Short progressPercent,
        String progressDisplay
) {
}
