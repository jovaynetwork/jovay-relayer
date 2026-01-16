package com.alipay.antchain.l2.relayer.core.tracer;

import java.math.BigInteger;
import java.util.List;
import jakarta.annotation.Resource;

import cn.hutool.core.collection.ListUtil;
import com.alipay.antchain.l2.relayer.TestBase;
import com.alipay.antchain.l2.relayer.commons.exceptions.CallRemoteServiceFailedException;
import com.alipay.antchain.l2.relayer.commons.exceptions.RemoteServiceRetryException;
import com.alipay.antchain.l2.relayer.config.RollupConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.L1Client;
import com.alipay.antchain.l2.relayer.core.blockchain.L2Client;
import com.alipay.antchain.l2.relayer.dal.repository.IOracleRepository;
import com.alipay.antchain.l2.relayer.engine.DistributedTaskEngine;
import com.alipay.antchain.l2.status.L2ErrorCode;
import com.alipay.antchain.l2.status.L2Status;
import com.alipay.antchain.l2.trace.BasicBlockTrace;
import com.alipay.antchain.l2.tracer.GetBasicTraceResponse;
import com.alipay.antchain.l2.tracer.TraceServiceGrpc;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.web3j.protocol.Web3j;

import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.*;

public class TraceServiceClientTest extends TestBase {

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

    private TraceServiceGrpc.TraceServiceBlockingStub stub = mock(TraceServiceGrpc.TraceServiceBlockingStub.class);

    @Resource
    private TraceServiceClient traceServiceClient;

    @Before
    public void init() {
        traceServiceClient.setStub(stub);
    }

    @Test
    public void testFetchBasicTrace() {
        when(stub.fetchBasicTrace(notNull())).thenReturn(ListUtil.toList(BASIC_BLOCK_TRACE1, BASIC_BLOCK_TRACE2).stream().iterator());

        List<BasicBlockTrace> traces = traceServiceClient.fetchBasicTrace(BigInteger.ONE, BigInteger.valueOf(3));
        Assert.assertEquals(2, traces.size());
    }

    @Test
    public void testGetBasicTrace() {
        when(stub.getBasicTrace(notNull())).thenReturn(
                GetBasicTraceResponse.newBuilder().setBasicTrace(BASIC_BLOCK_TRACE1).setStatus(L2Status.newBuilder().setErrorCode(L2ErrorCode.L2_OK)).build(),
                GetBasicTraceResponse.newBuilder().setBasicTrace(BASIC_BLOCK_TRACE1).setStableBlockNumber(0).setStatus(L2Status.newBuilder().setErrorCode(L2ErrorCode.L2_TRACER_ERROR_INVALID_BLOCK_NUMBER)).build(),
                GetBasicTraceResponse.newBuilder().setBasicTrace(BASIC_BLOCK_TRACE1).setStatus(L2Status.newBuilder().setErrorCode(L2ErrorCode.L2_RESOURCE_EXHAUSTED)).build(),
                GetBasicTraceResponse.newBuilder().setBasicTrace(BASIC_BLOCK_TRACE1).setStatus(L2Status.newBuilder().setErrorCode(L2ErrorCode.L2_INTERNAL_ERROR)).build()
        );

        BasicBlockTrace res = traceServiceClient.getBasicTrace(BigInteger.ONE);
        Assert.assertEquals(1L, res.getHeader().getNumber());
        Assert.assertThrows(CallRemoteServiceFailedException.class, () -> traceServiceClient.getBasicTrace(BigInteger.ONE));
    }

    @Test
    public void testGetBasicTraceWithRetry() {
        when(stub.getBasicTrace(notNull())).thenThrow(
                RemoteServiceRetryException.class
        );

        Assert.assertThrows(RemoteServiceRetryException.class, () -> traceServiceClient.getBasicTrace(BigInteger.ONE));
        verify(stub, times(5)).getBasicTrace(notNull());
    }

    // ==================== Negative Case Tests ====================

    /**
     * Test fetch basic trace when tracer service is unavailable
     */
    @Test
    public void testFetchBasicTrace_ServiceUnavailable() {
        when(stub.fetchBasicTrace(notNull())).thenThrow(new RuntimeException("Tracer service unavailable"));

        Assert.assertThrows(RuntimeException.class,
            () -> traceServiceClient.fetchBasicTrace(BigInteger.ONE, BigInteger.valueOf(3)));
    }

    /**
     * Test fetch basic trace when stream returns null
     */
    @Test
    public void testFetchBasicTrace_NullInStream() {
        when(stub.fetchBasicTrace(notNull())).thenReturn(
            ListUtil.toList(BASIC_BLOCK_TRACE1, null, BASIC_BLOCK_TRACE2).stream().iterator()
        );

        Assert.assertThrows(RuntimeException.class,
            () -> traceServiceClient.fetchBasicTrace(BigInteger.ONE, BigInteger.valueOf(4)));
    }

    /**
     * Test get basic trace when block is not stable yet
     */
    @Test
    public void testGetBasicTrace_BlockNotStable() {
        when(stub.getBasicTrace(notNull())).thenReturn(
                GetBasicTraceResponse.newBuilder()
                    .setBasicTrace(BASIC_BLOCK_TRACE1)
                    .setStableBlockNumber(5)
                    .setStatus(L2Status.newBuilder()
                        .setErrorCode(L2ErrorCode.L2_TRACER_ERROR_INVALID_BLOCK_NUMBER)
                        .setErrorMessage("Block not stable yet"))
                    .build()
        );

        Assert.assertThrows(CallRemoteServiceFailedException.class,
            () -> traceServiceClient.getBasicTrace(BigInteger.valueOf(20)));
    }

    /**
     * Test get basic trace when tracer returns timeout error
     */
    @Test
    public void testGetBasicTrace_Timeout() {
        when(stub.getBasicTrace(notNull())).thenReturn(
                GetBasicTraceResponse.newBuilder()
                    .setBasicTrace(BASIC_BLOCK_TRACE1)
                    .setStatus(L2Status.newBuilder()
                        .setErrorCode(L2ErrorCode.L2_TIMEOUT)
                        .setErrorMessage("Trace generation timeout"))
                    .build()
        );

        Assert.assertThrows(RemoteServiceRetryException.class,
            () -> traceServiceClient.getBasicTrace(BigInteger.ONE));
    }

    /**
     * Test get basic trace when block number is invalid
     */
    @Test
    public void testGetBasicTrace_InvalidBlockNumber() {
        when(stub.getBasicTrace(notNull())).thenReturn(
                GetBasicTraceResponse.newBuilder()
                    .setBasicTrace(BASIC_BLOCK_TRACE1)
                    .setStableBlockNumber(10)
                    .setStatus(L2Status.newBuilder()
                        .setErrorCode(L2ErrorCode.L2_TRACER_ERROR_INVALID_BLOCK_NUMBER)
                        .setErrorMessage("Block number does not exist"))
                    .build()
        );

        Assert.assertThrows(CallRemoteServiceFailedException.class,
            () -> traceServiceClient.getBasicTrace(BigInteger.valueOf(100)));
    }

    /**
     * Test fetch basic trace when range is invalid
     */
    @Test
    public void testFetchBasicTrace_InvalidRange() {
        when(stub.fetchBasicTrace(notNull())).thenReturn(
            ListUtil.<BasicBlockTrace>empty().stream().iterator()
        );

        List<BasicBlockTrace> traces = traceServiceClient.fetchBasicTrace(BigInteger.valueOf(10), BigInteger.ONE);
        Assert.assertEquals(0, traces.size());
    }

    /**
     * Test get basic trace with multiple retries until success
     */
    @Test
    public void testGetBasicTrace_RetryUntilSuccess() {
        when(stub.getBasicTrace(notNull())).thenReturn(
                GetBasicTraceResponse.newBuilder()
                    .setBasicTrace(BASIC_BLOCK_TRACE1)
                    .setStatus(L2Status.newBuilder().setErrorCode(L2ErrorCode.L2_RESOURCE_EXHAUSTED))
                    .build(),
                GetBasicTraceResponse.newBuilder()
                    .setBasicTrace(BASIC_BLOCK_TRACE1)
                    .setStatus(L2Status.newBuilder().setErrorCode(L2ErrorCode.L2_RESOURCE_EXHAUSTED))
                    .build(),
                GetBasicTraceResponse.newBuilder()
                    .setBasicTrace(BASIC_BLOCK_TRACE1)
                    .setStatus(L2Status.newBuilder().setErrorCode(L2ErrorCode.L2_OK))
                    .build()
        );

        // Should succeed after retries
        BasicBlockTrace res = traceServiceClient.getBasicTrace(BigInteger.ONE);
        Assert.assertEquals(1L, res.getHeader().getNumber());
    }
}
