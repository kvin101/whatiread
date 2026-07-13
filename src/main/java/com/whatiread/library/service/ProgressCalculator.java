package com.whatiread.library.service;

import com.whatiread.library.domain.ReadingStatus;

public final class ProgressCalculator {

    private ProgressCalculator() {
    }

    public static ProgressSnapshot calculate(
            ReadingStatus status,
            Integer progressPages,
            Short storedPercent,
            Integer pageCount
    ) {
        Short percent = resolvePercent(status, progressPages, storedPercent, pageCount);
        String display = buildDisplay(progressPages, pageCount, percent);
        return new ProgressSnapshot(progressPages, pageCount, percent, display);
    }

    private static Short resolvePercent(
            ReadingStatus status,
            Integer progressPages,
            Short storedPercent,
            Integer pageCount
    ) {
        if (status == ReadingStatus.READ) {
            return (short) 100;
        }
        if (pageCount != null && pageCount > 0 && progressPages != null && progressPages >= 0) {
            int computed = (int) Math.round(progressPages * 100.0 / pageCount);
            return (short) Math.clamp(computed, 0, 100);
        }
        if (storedPercent != null) {
            return (short) Math.clamp(storedPercent, 0, 100);
        }
        return null;
    }

    private static String buildDisplay(Integer progressPages, Integer pageCount, Short percent) {
        if (pageCount != null && pageCount > 0 && progressPages != null) {
            if (percent != null) {
                return progressPages + " / " + pageCount + " pages (" + percent + "%)";
            }
            return progressPages + " / " + pageCount + " pages";
        }
        if (percent != null) {
            return percent + "%";
        }
        return null;
    }
}
