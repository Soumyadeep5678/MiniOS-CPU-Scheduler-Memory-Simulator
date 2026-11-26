package com.minios.deadlock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** The worked example from Silberschatz, Operating System Concepts. */
class BankersAlgorithmTest {

    private BankersAlgorithm banker;

    @BeforeEach
    void setUp() {
        banker = new BankersAlgorithm(
                new int[] {3, 3, 2},
                new int[][] {{7, 5, 3}, {3, 2, 2}, {9, 0, 2}, {2, 2, 2}, {4, 3, 3}},
                new int[][] {{0, 1, 0}, {2, 0, 0}, {3, 0, 2}, {2, 1, 1}, {0, 0, 2}});
    }

    @Test
    void initialStateIsSafe() {
        assertEquals(List.of(1, 3, 4, 0, 2), banker.safeSequence().orElseThrow());
    }

    @Test
    void safeRequestIsGranted() {
        assertEquals(BankersAlgorithm.Outcome.GRANTED, banker.request(1, new int[] {1, 0, 2}));
        assertArrayEquals(new int[] {2, 3, 0}, banker.available());
    }

    @Test
    void requestLargerThanAvailableMustWait() {
        banker.request(1, new int[] {1, 0, 2});
        assertEquals(BankersAlgorithm.Outcome.MUST_WAIT, banker.request(4, new int[] {3, 3, 0}));
    }

    @Test
    void unsafeRequestIsDeniedAndRolledBack() {
        banker.request(1, new int[] {1, 0, 2});
        assertEquals(BankersAlgorithm.Outcome.DENIED_UNSAFE, banker.request(0, new int[] {0, 2, 0}));
        assertArrayEquals(new int[] {2, 3, 0}, banker.available());
    }

    @Test
    void requestBeyondClaimIsDenied() {
        assertEquals(BankersAlgorithm.Outcome.DENIED_EXCEEDS_CLAIM, banker.request(3, new int[] {1, 1, 1}));
    }
}
