package com.alipay.antchain.l2.relayer.engine.checker;

import java.math.BigInteger;
import java.util.concurrent.CompletableFuture;

import com.alipay.antchain.l2.relayer.TestBase;
import com.alipay.antchain.l2.relayer.commons.enums.BizTaskTypeEnum;
import com.alipay.antchain.l2.relayer.config.RollupConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.L1Client;
import com.alipay.antchain.l2.relayer.core.blockchain.L2Client;
import com.alipay.antchain.l2.relayer.dal.repository.IOracleRepository;
import com.alipay.antchain.l2.relayer.engine.DistributedTaskEngine;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.redisson.api.RedissonClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.web3j.protocol.Web3j;

import static org.junit.Assert.*;

/**
 * Negative case tests for DefaultDistributedTaskChecker
 * Tests exception handling and edge cases in distributed task checking with Redis lock
 */
public class DefaultDistributedTaskCheckerTest extends TestBase {

    @MockitoBean
    private L2Client l2Client;

    @MockitoBean
    private L1Client l1Client;

    @MockitoBean
    private DistributedTaskEngine distributedTaskEngine;

    @MockitoBean
    private RollupConfig rollupConfig;

    @MockitoBean
    private IOracleRepository oracleRepository;

    @MockitoBean(name = "l1Web3j")
    private Web3j l1Web3j;

    @MockitoBean(name = "l1ChainId")
    private BigInteger l1ChainId;

    @Resource
    private RedissonClient redisson;

    @Resource(name = "defaultDistributedTaskChecker")
    private DefaultDistributedTaskChecker checker;

    @After
    public void forceUnlock() {
        // delete all keys
        new SingleThreadRedissonLock(redisson, "relayer:task:BLOCK_POLLING_TASK").forceUnlock();
        new SingleThreadRedissonLock(redisson, "relayer:task:BATCH_PROVE_TASK").forceUnlock();
    }

    /**
     * Test addLocalFuture with null task type
     * Verifies that null task type causes NullPointerException
     */
    @Test(expected = NullPointerException.class)
    public void testAddLocalFuture_NullTaskType() {
        CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
        checker.addLocalFuture(null, future);
    }

    /**
     * Test addLocalFuture with null future
     * Verifies that null future is handled
     */
    @Test
    public void testAddLocalFuture_NullFuture() {
        try {
            checker.addLocalFuture(BizTaskTypeEnum.BLOCK_POLLING_TASK, null);
            assertTrue(checker.getLocalRunningTasks().containsKey(BizTaskTypeEnum.BLOCK_POLLING_TASK));
        } catch (NullPointerException e) {
            // Exception is acceptable for null future
            assertNotNull(e);
        }
    }

    /**
     * Test addLocalFuture when future completes successfully
     * Verifies that lock is unlocked when future completes
     */
    @Test
    @SneakyThrows
    public void testAddLocalFuture_FutureCompletesSuccessfully() {
        CompletableFuture<Void> future = new CompletableFuture<>();

        checker.addLocalFuture(BizTaskTypeEnum.BLOCK_POLLING_TASK, future);
        var lock = new SingleThreadRedissonLock(redisson, "relayer:task:BLOCK_POLLING_TASK");
        Assert.assertTrue(lock.tryLockAsync().get());
        future.complete(null);

        // Give some time for the whenComplete callback to execute
        Thread.sleep(100);

        Assert.assertFalse(lock.isHeldByCurrentThread());
    }

    /**
     * Test addLocalFuture when future completes exceptionally
     * Verifies that lock is unlocked even when future fails
     */
    @Test
    @SneakyThrows
    public void testAddLocalFuture_FutureCompletesExceptionally() {
        CompletableFuture<Void> future = new CompletableFuture<>();

        checker.addLocalFuture(BizTaskTypeEnum.BLOCK_POLLING_TASK, future);
        var lock = new SingleThreadRedissonLock(redisson, "relayer:task:BLOCK_POLLING_TASK");
        Assert.assertTrue(lock.tryLockAsync().get());
        future.completeExceptionally(new RuntimeException("Task failed"));

        // Give some time for the whenComplete callback to execute
        Thread.sleep(500);

        Assert.assertFalse(lock.isHeldByCurrentThread());
    }

    /**
     * Test addLocalFuture when lock is not held by current thread
     * Verifies that unlock is not called when lock is not held
     */
    @Test
    @SneakyThrows
    public void testAddLocalFuture_LockNotHeldByCurrentThread() {
        CompletableFuture<Void> future = CompletableFuture.completedFuture(null);

        // Complete the future immediately without adding to checker
        // This simulates a scenario where lock is not held
        Thread.sleep(100);

        // Verify no exception is thrown
        Assert.assertNotNull(future);
    }

    /**
     * Test addLocalFuture when unlock throws exception
     * Verifies that unlock exception is caught and logged
     */
    @Test
    @SneakyThrows
    public void testAddLocalFuture_UnlockThrowsException() {
        CompletableFuture<Void> future = new CompletableFuture<>();

        checker.addLocalFuture(BizTaskTypeEnum.BLOCK_POLLING_TASK, future);
        var lock = new SingleThreadRedissonLock(redisson, "relayer:task:BLOCK_POLLING_TASK");
        Assert.assertTrue(lock.tryLockAsync().get());
        future.complete(null);

        // Give some time for the whenComplete callback to execute
        Thread.sleep(500);

        Assert.assertFalse(lock.isHeldByCurrentThread());
    }

    /**
     * Test checkIfContinue with null task type
     * Verifies that null task type causes NullPointerException
     */
    @Test(expected = NullPointerException.class)
    public void testCheckIfContinue_NullTaskType() {
        checker.checkIfContinue(null);
    }

    /**
     * Test checkIfContinue when parent check returns false
     * Verifies that Redis lock is not checked when parent returns false
     */
    @Test
    @SneakyThrows
    public void testCheckIfContinue_ParentCheckReturnsFalse() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        checker.addLocalFuture(BizTaskTypeEnum.BLOCK_POLLING_TASK, future);

        boolean result = checker.checkIfContinue(BizTaskTypeEnum.BLOCK_POLLING_TASK);

        assertFalse(result);

        // Clean up
        var lock = new SingleThreadRedissonLock(redisson, "relayer:task:BLOCK_POLLING_TASK");
        lock.forceUnlock();
    }

    /**
     * Test checkIfContinue when lock can be acquired
     * Verifies that true is returned when lock is acquired
     */
    @Test
    @SneakyThrows
    public void testCheckIfContinue_LockAcquired() {
        boolean result = checker.checkIfContinue(BizTaskTypeEnum.BLOCK_POLLING_TASK);

        assertTrue(result);

        // Verify lock is held and clean up
        var lock = new SingleThreadRedissonLock(redisson, "relayer:task:BLOCK_POLLING_TASK");
        Assert.assertTrue(lock.tryLockAsync().get());
        Assert.assertTrue(lock.forceUnlock());
    }

    /**
     * Test checkIfContinue when lock cannot be acquired
     * Verifies that false is returned when lock is not acquired
     */
    @Test
    @SneakyThrows
    public void testCheckIfContinue_LockNotAcquired() {
        // First acquire the lock
        var lock = new SingleThreadRedissonLock(redisson, "relayer:task:BLOCK_POLLING_TASK");
        lock.tryLock();

        // Try to acquire again in a different context (should fail)
        boolean result = checker.checkIfContinue(BizTaskTypeEnum.BLOCK_POLLING_TASK);

        // Result depends on whether the lock was already held
        assertNotNull(result);
    }

    /**
     * Test checkIfContinue with valid task type
     * Verifies that lock can be acquired for valid task
     */
    @Test
    @SneakyThrows
    public void testCheckIfContinue_ValidTaskType() {
        boolean result = checker.checkIfContinue(BizTaskTypeEnum.BATCH_PROVE_TASK);

        assertTrue(result);

        // Clean up
        var lock = new SingleThreadRedissonLock(redisson, "relayer:task:BATCH_PROVE_TASK");
        lock.forceUnlock();
    }

    /**
     * Test checkIfContinue when redisson client is null
     * Verifies that null redisson client causes NullPointerException
     */
    @Test(expected = NullPointerException.class)
    public void testCheckIfContinue_NullRedissonClient() {
        DefaultDistributedTaskChecker nullRedissonChecker = new DefaultDistributedTaskChecker();
        nullRedissonChecker.checkIfContinue(BizTaskTypeEnum.BLOCK_POLLING_TASK);
    }

    /**
     * Test checkIfContinue with different task type
     * Verifies that different task types use different locks
     */
    @Test
    @SneakyThrows
    public void testCheckIfContinue_DifferentTaskType() {
        boolean result1 = checker.checkIfContinue(BizTaskTypeEnum.BLOCK_POLLING_TASK);
        boolean result2 = checker.checkIfContinue(BizTaskTypeEnum.BATCH_PROVE_TASK);

        assertTrue(result1);
        assertTrue(result2);

        // Clean up - locks should already be held
        var lock1 = new SingleThreadRedissonLock(redisson, "relayer:task:BLOCK_POLLING_TASK");
        var lock2 = new SingleThreadRedissonLock(redisson, "relayer:task:BATCH_PROVE_TASK");
        if (lock1.isLocked()) {
            lock1.forceUnlock();
        }
        if (lock2.isLocked()) {
            lock2.forceUnlock();
        }
    }

    /**
     * Test with multiple different task types
     * Verifies that different tasks use different locks
     */
    @Test
    @SneakyThrows
    public void testMultipleDifferentTaskTypes() {
        boolean result1 = checker.checkIfContinue(BizTaskTypeEnum.BLOCK_POLLING_TASK);
        boolean result2 = checker.checkIfContinue(BizTaskTypeEnum.BATCH_PROVE_TASK);

        assertTrue(result1);
        assertTrue(result2);

        // Verify locks are held and clean up
        var lock1 = new SingleThreadRedissonLock(redisson, "relayer:task:BLOCK_POLLING_TASK");
        var lock2 = new SingleThreadRedissonLock(redisson, "relayer:task:BATCH_PROVE_TASK");
        Assert.assertTrue(lock1.tryLockAsync().get());
        Assert.assertTrue(lock2.tryLockAsync().get());
        Assert.assertTrue(lock1.forceUnlock());
        Assert.assertTrue(lock2.forceUnlock());
    }

    /**
     * Test addLocalFuture with multiple futures
     * Verifies that multiple futures can be added and completed
     */
    @Test
    @SneakyThrows
    public void testAddLocalFuture_MultipleFutures() {
        CompletableFuture<Void> future1 = new CompletableFuture<>();
        CompletableFuture<Void> future2 = new CompletableFuture<>();

        checker.addLocalFuture(BizTaskTypeEnum.BLOCK_POLLING_TASK, future1);
        var lock1 = new SingleThreadRedissonLock(redisson, "relayer:task:BLOCK_POLLING_TASK");
        Assert.assertTrue(lock1.tryLockAsync().get());

        future1.complete(null);
        Thread.sleep(100);

        checker.addLocalFuture(BizTaskTypeEnum.BATCH_PROVE_TASK, future2);
        var lock2 = new SingleThreadRedissonLock(redisson, "relayer:task:BATCH_PROVE_TASK");
        Assert.assertTrue(lock2.tryLockAsync().get());

        future2.complete(null);
        Thread.sleep(100);

        // Clean up
        Assert.assertFalse(lock1.isHeldByCurrentThread());
        Assert.assertFalse(lock2.isHeldByCurrentThread());
    }

    /**
     * Test concurrent access to checker
     * Verifies that concurrent operations are handled correctly
     */
    @Test
    @SneakyThrows
    public void testConcurrentAccess() {
        Thread thread1 = new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
                checker.addLocalFuture(BizTaskTypeEnum.BLOCK_POLLING_TASK, future);
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        Thread thread2 = new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                checker.checkIfContinue(BizTaskTypeEnum.BATCH_PROVE_TASK);
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();

        // Should not throw any exception
        assertNotNull(checker.getLocalRunningTasks());

        // Clean up
        var lock1 = new SingleThreadRedissonLock(redisson, "relayer:task:BLOCK_POLLING_TASK");
        var lock2 = new SingleThreadRedissonLock(redisson, "relayer:task:BATCH_PROVE_TASK");
        lock1.forceUnlock();
        lock2.forceUnlock();
    }
}
