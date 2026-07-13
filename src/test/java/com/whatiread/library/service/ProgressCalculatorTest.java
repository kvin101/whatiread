package com.whatiread.library.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.whatiread.library.domain.ReadingStatus;
import org.junit.jupiter.api.Test;

class ProgressCalculatorTest {

    @Test
    void calculatesPagesAndPercent() {
        ProgressSnapshot snapshot = ProgressCalculator.calculate(
                ReadingStatus.READING,
                142,
                null,
                350
        );

        assertEquals(142, snapshot.progressPages());
        assertEquals(350, snapshot.pageCount());
        assertEquals((short) 41, snapshot.progressPercent());
        assertEquals("142 / 350 pages (41%)", snapshot.progressDisplay());
    }

    @Test
    void readStatusDefaultsToFullPercent() {
        ProgressSnapshot snapshot = ProgressCalculator.calculate(
                ReadingStatus.READ,
                null,
                null,
                350
        );

        assertEquals((short) 100, snapshot.progressPercent());
        assertEquals("100%", snapshot.progressDisplay());
    }

    @Test
    void percentOnlyWhenPageCountUnknown() {
        ProgressSnapshot snapshot = ProgressCalculator.calculate(
                ReadingStatus.READING,
                null,
                (short) 55,
                null
        );

        assertNull(snapshot.progressPages());
        assertNull(snapshot.pageCount());
        assertEquals((short) 55, snapshot.progressPercent());
        assertEquals("55%", snapshot.progressDisplay());
    }

    @Test
    void dnfStatusUsesStoredPercentWhenPagesUnknown() {
        ProgressSnapshot snapshot = ProgressCalculator.calculate(
                ReadingStatus.DNF,
                null,
                (short) 30,
                null
        );

        assertEquals((short) 30, snapshot.progressPercent());
        assertEquals("30%", snapshot.progressDisplay());
    }

    @Test
    void clampsComputedPercentToValidRange() {
        ProgressSnapshot over = ProgressCalculator.calculate(
                ReadingStatus.READING,
                400,
                null,
                350
        );
        assertEquals((short) 100, over.progressPercent());

        ProgressSnapshot atStart = ProgressCalculator.calculate(
                ReadingStatus.READING,
                0,
                null,
                350
        );
        assertEquals((short) 0, atStart.progressPercent());
    }
}
