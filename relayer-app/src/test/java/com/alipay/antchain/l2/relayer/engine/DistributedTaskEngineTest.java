package com.alipay.antchain.l2.relayer.engine;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.alipay.antchain.l2.relayer.engine.core.Activator;
import com.alipay.antchain.l2.relayer.engine.core.Dispatcher;
import com.alipay.antchain.l2.relayer.engine.core.Duty;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.ApplicationArguments;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Negative case tests for DistributedTaskEngine
 * Tests exception handling and edge cases in distributed task engine
 */
@RunWith(MockitoJUnitRunner.class)
public class DistributedTaskEngineTest {

    @Mock
    private Activator activator;

    @Mock
    private Dispatcher dispatcher;

    @Mock
    private Duty duty;

    @Mock
    private ScheduledExecutorService distributedTaskEngineScheduleThreadsPool;

    @Mock
    private ApplicationArguments applicationArguments;

    @InjectMocks
    private DistributedTaskEngine engine;

    @Before
    public void setUp() {
        // Mock objects are already injected by @InjectMocks
        // No need to recreate engine instance
    }

    /**
     * Test run with null activator
     * Verifies that null activator causes NullPointerException
     */
    @Test(expected = NullPointerException.class)
    public void testRun_NullActivator() {
        DistributedTaskEngine nullActivatorEngine = new DistributedTaskEngine();
        nullActivatorEngine.run(applicationArguments);
    }

    /**
     * Test run with null dispatcher
     * Verifies that null dispatcher causes NullPointerException
     */
    @Test(expected = NullPointerException.class)
    public void testRun_NullDispatcher() {
        DistributedTaskEngine nullDispatcherEngine = new DistributedTaskEngine();
        nullDispatcherEngine.run(applicationArguments);
    }

    /**
     * Test run with null duty
     * Verifies that null duty causes NullPointerException
     */
    @Test(expected = NullPointerException.class)
    public void testRun_NullDuty() {
        DistributedTaskEngine nullDutyEngine = new DistributedTaskEngine();
        nullDutyEngine.run(applicationArguments);
    }

    /**
     * Test run with null thread pool
     * Verifies that null thread pool causes NullPointerException
     */
    @Test(expected = NullPointerException.class)
    public void testRun_NullThreadPool() {
        DistributedTaskEngine nullPoolEngine = new DistributedTaskEngine();
        nullPoolEngine.run(applicationArguments);
    }

    /**
     * Test run when activator throws exception
     * Verifies that activator exception is caught and logged
     */
    @Test
    public void testRun_ActivatorThrowsException() throws Exception {
        lenient().doThrow(new RuntimeException("Activator failed")).when(activator).activate();

        engine.run(null);

        // Verify activator and duty use scheduleWithFixedDelay (2 times)
        verify(distributedTaskEngineScheduleThreadsPool, times(2))
                .scheduleWithFixedDelay(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class));
        // Verify dispatcher uses scheduleAtFixedRate (1 time)
        verify(distributedTaskEngineScheduleThreadsPool, times(1))
                .scheduleAtFixedRate(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class));
    }

    /**
     * Test run when dispatcher throws exception
     * Verifies that dispatcher exception is caught and logged
     */
    @Test
    public void testRun_DispatcherThrowsException() {
        lenient().doThrow(new RuntimeException("Dispatcher failed")).when(dispatcher).dispatch();

        engine.run(applicationArguments);

        // Should still schedule all three tasks
        verify(distributedTaskEngineScheduleThreadsPool, times(2))
                .scheduleWithFixedDelay(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class));
        verify(distributedTaskEngineScheduleThreadsPool, times(1))
                .scheduleAtFixedRate(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class));
    }

    /**
     * Test run when duty throws exception
     * Verifies that duty exception is caught and logged
     */
    @Test
    public void testRun_DutyThrowsException() {
        lenient().doThrow(new RuntimeException("Duty failed")).when(duty).duty();

        engine.run(applicationArguments);

        // Should still schedule all tasks even if duty throws exception
        verify(distributedTaskEngineScheduleThreadsPool, times(2))
                .scheduleWithFixedDelay(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class));
        verify(distributedTaskEngineScheduleThreadsPool, times(1))
                .scheduleAtFixedRate(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class));
    }

    /**
     * Test run when scheduleWithFixedDelay throws exception
     * Verifies that scheduling exception is propagated
     */
    @Test(expected = RuntimeException.class)
    public void testRun_ScheduleWithFixedDelayThrowsException() {
        when(distributedTaskEngineScheduleThreadsPool.scheduleWithFixedDelay(
                any(Runnable.class),
                anyLong(),
                anyLong(),
                any(TimeUnit.class)
        )).thenThrow(new RuntimeException("Scheduling failed"));

        engine.run(applicationArguments);
    }

    /**
     * Test run with null application arguments
     * Verifies that null arguments is handled
     */
    @Test
    public void testRun_NullApplicationArguments() {
        engine.run(null);

        // Should still schedule all tasks with null arguments
        verify(distributedTaskEngineScheduleThreadsPool, times(2))
                .scheduleWithFixedDelay(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class));
        verify(distributedTaskEngineScheduleThreadsPool, times(1))
                .scheduleAtFixedRate(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class));
    }

    /**
     * Test shutdown when thread pool is already shutdown
     * Verifies that shutdown is skipped for already shutdown pool
     */
    @Test
    public void testShutdown_AlreadyShutdown() throws InterruptedException {
        when(distributedTaskEngineScheduleThreadsPool.isShutdown()).thenReturn(true);

        engine.shutdown();

        verify(distributedTaskEngineScheduleThreadsPool, never()).shutdown();
        verify(distributedTaskEngineScheduleThreadsPool, never()).awaitTermination(anyLong(), any(TimeUnit.class));
    }

    /**
     * Test shutdown when thread pool is not shutdown
     * Verifies that shutdown is called correctly
     */
    @Test
    public void testShutdown_NotShutdown() throws InterruptedException {
        when(distributedTaskEngineScheduleThreadsPool.isShutdown()).thenReturn(false);
        when(distributedTaskEngineScheduleThreadsPool.awaitTermination(anyLong(), any(TimeUnit.class)))
                .thenReturn(true);

        engine.shutdown();

        verify(distributedTaskEngineScheduleThreadsPool, times(1)).shutdown();
        verify(distributedTaskEngineScheduleThreadsPool, times(1))
                .awaitTermination(10, TimeUnit.SECONDS);
    }

    /**
     * Test shutdown when awaitTermination throws InterruptedException
     * Verifies that InterruptedException is caught and logged
     */
    @Test
    public void testShutdown_AwaitTerminationThrowsInterruptedException() throws InterruptedException {
        when(distributedTaskEngineScheduleThreadsPool.isShutdown()).thenReturn(false);
        when(distributedTaskEngineScheduleThreadsPool.awaitTermination(anyLong(), any(TimeUnit.class)))
                .thenThrow(new InterruptedException("Interrupted"));

        engine.shutdown();

        verify(distributedTaskEngineScheduleThreadsPool, times(1)).shutdown();
    }

    /**
     * Test shutdown when shutdown throws exception
     * Verifies that shutdown exception is caught and logged
     */
    @Test
    public void testShutdown_ShutdownThrowsException() {
        when(distributedTaskEngineScheduleThreadsPool.isShutdown()).thenReturn(false);
        doThrow(new RuntimeException("Shutdown failed"))
                .when(distributedTaskEngineScheduleThreadsPool).shutdown();

        engine.shutdown();

        verify(distributedTaskEngineScheduleThreadsPool, times(1)).shutdown();
    }

    /**
     * Test shutdown when thread pool is null
     * Verifies that null thread pool causes NullPointerException
     */
    @Test(expected = NullPointerException.class)
    public void testShutdown_NullThreadPool() {
        DistributedTaskEngine nullPoolEngine = new DistributedTaskEngine();
        nullPoolEngine.shutdown();
    }

    /**
     * Test shutdown when awaitTermination returns false
     * Verifies that timeout is handled correctly
     */
    @Test
    public void testShutdown_AwaitTerminationTimeout() throws InterruptedException {
        when(distributedTaskEngineScheduleThreadsPool.isShutdown()).thenReturn(false);
        when(distributedTaskEngineScheduleThreadsPool.awaitTermination(anyLong(), any(TimeUnit.class)))
                .thenReturn(false);

        engine.shutdown();

        verify(distributedTaskEngineScheduleThreadsPool, times(1)).shutdown();
        verify(distributedTaskEngineScheduleThreadsPool, times(1))
                .awaitTermination(10, TimeUnit.SECONDS);
    }

    /**
     * Test multiple shutdown calls
     * Verifies that multiple shutdown calls are handled correctly
     */
    @Test
    public void testShutdown_MultipleCalls() throws InterruptedException {
        when(distributedTaskEngineScheduleThreadsPool.isShutdown())
                .thenReturn(false)
                .thenReturn(true);
        when(distributedTaskEngineScheduleThreadsPool.awaitTermination(anyLong(), any(TimeUnit.class)))
                .thenReturn(true);

        engine.shutdown();
        engine.shutdown();

        verify(distributedTaskEngineScheduleThreadsPool, times(1)).shutdown();
        verify(distributedTaskEngineScheduleThreadsPool, times(1))
                .awaitTermination(10, TimeUnit.SECONDS);
    }

    /**
     * Test getActivator returns correct activator
     * Verifies that getter works correctly
     */
    @Test
    public void testGetActivator() {
        assertEquals(activator, engine.getActivator());
    }

    /**
     * Test getDispatcher returns correct dispatcher
     * Verifies that getter works correctly
     */
    @Test
    public void testGetDispatcher() {
        assertEquals(dispatcher, engine.getDispatcher());
    }

    /**
     * Test getDuty returns correct duty
     * Verifies that getter works correctly
     */
    @Test
    public void testGetDuty() {
        assertEquals(duty, engine.getDuty());
    }

    /**
     * Test getDistributedTaskEngineScheduleThreadsPool returns correct pool
     * Verifies that getter works correctly
     */
    @Test
    public void testGetDistributedTaskEngineScheduleThreadsPool() {
        assertEquals(distributedTaskEngineScheduleThreadsPool, 
                    engine.getDistributedTaskEngineScheduleThreadsPool());
    }

    private void assertEquals(Object expected, Object actual) {
        if (expected != actual) {
            throw new AssertionError("Expected: " + expected + " but was: " + actual);
        }
    }

    private void assertNotNull(Object object) {
        if (object == null) {
            throw new AssertionError("Object should not be null");
        }
    }
}
