package dk.ilios.spanner;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import dk.ilios.spanner.benchmarks.invalid.InvalidParameterBenchmark;
import dk.ilios.spanner.benchmarks.invalid.PrivateBenchmark;
import dk.ilios.spanner.benchmarks.invalid.StaticBenchmark;
import dk.ilios.spanner.benchmarks.valid.DefaultConfig;
import dk.ilios.spanner.benchmarks.valid.Empty;
import dk.ilios.spanner.benchmarks.valid.ValidBenchmarkMethods;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests how the Spanner JUnit runner reacts to various benchmark classes.
 */
public class RunnerTests {

    @Test(expected = IllegalArgumentException.class)
    public void testPrivateBenchmarkMethodsThrows() {
        Spanner.runAllBenchmarks(PrivateBenchmark.class, new SpannerCallbackAdapter());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testStaticBenchmarkMethodsThrows() {
        Spanner.runAllBenchmarks(StaticBenchmark.class, new SpannerCallbackAdapter());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidParameterBenchmarkMethodsThrows() {
        Spanner.runAllBenchmarks(InvalidParameterBenchmark.class, new SpannerCallbackAdapter());
    }

    @Test
    public void testEmptyBenchmarkClass() {
        final CountDownLatch benchmarkLatch = new CountDownLatch(1);
        final AtomicBoolean benchmarkDone = new AtomicBoolean(false);
        Spanner.runAllBenchmarks(Empty.class, new SpannerCallbackAdapter() {
            @Override
            public void onComplete() {
                benchmarkDone.set(true);
                benchmarkLatch.countDown();
            }
        });
        awaitOrFail(benchmarkLatch);
        assertTrue(benchmarkDone.get());
    }

    @Test
    public void testDefaultConfigBenchmarkClass() {
        final CountDownLatch benchmarkLatch = new CountDownLatch(1);
        final AtomicBoolean benchmarkDone = new AtomicBoolean(false);
        Spanner.runAllBenchmarks(DefaultConfig.class, new SpannerCallbackAdapter() {
            @Override
            public void onComplete() {
                benchmarkDone.set(true);
                benchmarkLatch.countDown();
            }
        });
        awaitOrFail(benchmarkLatch);
        assertTrue(benchmarkDone.get());
    }

    @Test
    public void testValidBenchmarkMethods() {
        final CountDownLatch benchmarkLatch = new CountDownLatch(1);
        final AtomicBoolean benchmarkDone = new AtomicBoolean(false);
        Spanner.runAllBenchmarks(ValidBenchmarkMethods.class, new SpannerCallbackAdapter() {
            @Override
            public void onComplete() {
                benchmarkDone.set(true);
                benchmarkLatch.countDown();
            }
        });
        awaitOrFail(benchmarkLatch);
        assertTrue(benchmarkDone.get());
    }

    private void awaitOrFail(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                fail("Unit test timed out.");
            }
        } catch (InterruptedException e) {
            fail("Unit test interrupted: " + e.toString());
        }
    }
}
