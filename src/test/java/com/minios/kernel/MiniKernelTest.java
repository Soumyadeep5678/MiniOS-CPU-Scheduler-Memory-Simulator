package com.minios.kernel;

import com.minios.memory.ContiguousAllocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MiniKernelTest {

    private ByteArrayOutputStream buffer;
    private MiniKernel kernel;

    @BeforeEach
    void setUp() {
        buffer = new ByteArrayOutputStream();
        kernel = new MiniKernel(100, ContiguousAllocator.Fit.FIRST, new PrintStream(buffer, true, StandardCharsets.UTF_8));
    }

    private String output() {
        String s = buffer.toString(StandardCharsets.UTF_8);
        buffer.reset();
        return s;
    }

    @Test
    void createKillAndRun() {
        kernel.execute("create editor 40 6");
        assertTrue(output().contains("Created PID 1 'editor': 40 KB at address 0"));
        kernel.execute("create shell 30 3 1");
        assertTrue(output().contains("PID 2"));
        kernel.execute("kill 1");
        assertTrue(output().contains("freed 40 KB"));
        kernel.execute("run rr 2");
        String out = output();
        assertTrue(out.contains("Round Robin (q=2)"));
        assertTrue(out.contains("memory reclaimed"));
        kernel.execute("ps");
        assertTrue(output().contains("No processes"));
    }

    @Test
    void reportsExternalFragmentation() {
        kernel.execute("create a 40 1");
        kernel.execute("create b 20 1");
        kernel.execute("create c 40 1");
        kernel.execute("kill 1");
        kernel.execute("kill 3");
        output();
        kernel.execute("create big 60 1"); // 80 KB free, but split into two 40 KB holes
        assertTrue(output().contains("external fragmentation"));
    }

    @Test
    void badInputDoesNotCrash() {
        kernel.execute("create x notanumber 5");
        assertTrue(output().contains("Expected a number"));
        kernel.execute("run nosuch");
        kernel.execute("create y 10 1");
        output();
        kernel.execute("run nosuch");
        assertTrue(output().contains("unknown scheduler"));
        kernel.execute("frobnicate");
        assertTrue(output().contains("Unknown command"));
        kernel.execute("exit");
        assertFalse(kernel.isRunning());
    }
}
