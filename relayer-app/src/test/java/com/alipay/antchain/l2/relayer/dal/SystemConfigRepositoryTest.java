package com.alipay.antchain.l2.relayer.dal;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import cn.hutool.cache.Cache;
import com.alipay.antchain.l2.prover.controller.ProverControllerServerGrpc;
import com.alipay.antchain.l2.relayer.L2RelayerApplication;
import com.alipay.antchain.l2.relayer.TestBase;
import com.alipay.antchain.l2.relayer.commons.exceptions.L2RelayerException;
import com.alipay.antchain.l2.relayer.config.RollupConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.L1Client;
import com.alipay.antchain.l2.relayer.core.blockchain.L2Client;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.gasprice.L1GasPriceProviderConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.gasprice.L2GasPriceProviderConfig;
import com.alipay.antchain.l2.relayer.core.layer2.economic.RollupEconomicStrategyConfig;
import com.alipay.antchain.l2.relayer.dal.repository.IOracleRepository;
import com.alipay.antchain.l2.relayer.dal.repository.ISystemConfigRepository;
import com.alipay.antchain.l2.relayer.engine.core.Activator;
import com.alipay.antchain.l2.relayer.engine.core.Dispatcher;
import com.alipay.antchain.l2.tracer.TraceServiceGrpc;
import jakarta.annotation.Resource;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;
import org.web3j.protocol.Web3j;

import static org.junit.Assert.*;
import static org.mockito.Mockito.notNull;
import static org.mockito.Mockito.when;

@SpringBootTest(
        classes = L2RelayerApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {"spring.flyway.enabled=false", "l2-relayer.l1-client.eth-network-fork.unknown-network-config-file=bpo/unknown.json"}
)
@Sql(scripts = {"classpath:data/ddl.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:data/drop_all.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
public class SystemConfigRepositoryTest extends TestBase {

    @Resource
    private ISystemConfigRepository systemConfigRepository;

    @MockitoBean(name = "prover-client")
    private ProverControllerServerGrpc.ProverControllerServerBlockingStub proverStub;

    @MockitoBean(name = "tracer-client")
    private TraceServiceGrpc.TraceServiceBlockingStub tracerStub;

    @MockitoBean
    private L1Client l1Client;

    @MockitoBean
    private L2Client l2Client;

    @MockitoBean
    private RollupConfig rollupConfig;

    @MockitoBean
    private Activator activator;

    @MockitoBean
    private Dispatcher dispatcher;

    @MockitoBean
    private IOracleRepository oracleRepository;

    @MockitoBean
    private RollupEconomicStrategyConfig rollupEconomicStrategyConfig;

    @MockitoBean
    private L1GasPriceProviderConfig l1GasPriceProviderConfig;

    @MockitoBean
    private L2GasPriceProviderConfig l2GasPriceProviderConfig;

    @MockitoBean(name = "l1Web3j")
    private Web3j l1Web3j;

    @MockitoBean(name = "l1ChainId")
    private BigInteger l1ChainId;

    @MockitoSpyBean(name = "systemConfigCache")
    private Cache<String, String> systemConfigCache;

    @Test
    public void testAnchorBatchSet() {
        assertFalse(systemConfigRepository.isAnchorBatchSet());
        systemConfigRepository.markAnchorBatchHasBeenSet();
        assertTrue(systemConfigRepository.isAnchorBatchSet());
    }

    @Test
    public void testConfig() {
        assertFalse(systemConfigRepository.hasSystemConfig("test"));
        assertNull(systemConfigRepository.getSystemConfig("test"));
        systemConfigRepository.setSystemConfig("test", "test");
        assertEquals("test", systemConfigRepository.getSystemConfig("test"));
        systemConfigRepository.setSystemConfig("test", "test1");
        assertEquals("test1", systemConfigRepository.getSystemConfig("test"));

        Map<String, String> val = new HashMap<>();
        val.put("test2", "test3");
        systemConfigRepository.setSystemConfig(val);

        assertEquals("test3", systemConfigRepository.getSystemConfig("test2"));

        when(systemConfigCache.containsKey(notNull())).thenThrow(RuntimeException.class);
        assertThrows(L2RelayerException.class, () -> systemConfigRepository.getSystemConfig("wrong"));
    }
}
