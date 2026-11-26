package com.minios.concurrency;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConcurrencyTest {

    @Test
    @Timeout(20)
    void producerConsumerLosesNothingAndNeverOverflows() throws InterruptedException {
        ProducerConsumer.Report r = ProducerConsumer.run(4, 3, 5_000, 4);
        assertEquals(20_000, r.consumed());
        assertEquals(r.producedSum(), r.consumedSum());
        assertTrue(r.maxOccupancy() <= 4);
        assertTrue(r.consistent());
    }

    @Test
    @Timeout(20)
    void naivePhilosophersDeadlock() throws InterruptedException {
        DiningPhilosophers.Report r = DiningPhilosophers.run(5, 100, DiningPhilosophers.Strategy.NAIVE, 10_000);
        assertTrue(r.deadlocked());
        assertEquals(0, r.totalMeals());
    }

    @ParameterizedTest
    @EnumSource(value = DiningPhilosophers.Strategy.class, names = {"RESOURCE_ORDERING", "WAITER"})
    @Timeout(30)
    void preventionStrategiesNeverDeadlock(DiningPhilosophers.Strategy strategy) throws InterruptedException {
        DiningPhilosophers.Report r = DiningPhilosophers.run(5, 2_000, strategy, 20_000);
        assertFalse(r.deadlocked());
        assertEquals(5 * 2_000, r.totalMeals());
    }
}
