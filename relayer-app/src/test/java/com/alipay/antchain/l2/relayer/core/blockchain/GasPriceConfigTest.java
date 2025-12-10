package com.alipay.antchain.l2.relayer.core.blockchain;

import java.math.BigInteger;
import java.util.concurrent.TimeUnit;

import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alipay.antchain.l2.relayer.TestBase;
import com.alipay.antchain.l2.relayer.config.BlockchainConfig;
import com.alipay.antchain.l2.relayer.config.RollupConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.gasprice.DynamicGasPriceProviderConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.gasprice.GasPriceProviderConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.GasPriceProviderSupplierEnum;
import com.alipay.antchain.l2.relayer.dal.repository.IOracleRepository;
import com.alipay.antchain.l2.relayer.dal.repository.ISystemConfigRepository;
import com.alipay.antchain.l2.relayer.engine.DistributedTaskEngine;
import com.alipay.antchain.l2.relayer.engine.dynamicconf.PrefixedDynamicConfig;
import com.alipay.antchain.l2.relayer.engine.dynamicconf.ValueDesensitizeFilter;
import com.alipay.antchain.l2.relayer.metrics.alarm.RollupAlarm;
import com.alipay.antchain.l2.relayer.metrics.monitor.AccountBalanceMonitor;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.Test;
import org.redisson.api.RedissonClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

public class GasPriceConfigTest extends TestBase {

    private static final String L1_GAS_PRICE_CONF_MAP_KEY = "relayer-dynamic-config@l1-gasprice-provider-conf";
    public static final String L1_GASPRICE_PROVIDER_CONF_PREFIX = "l1-gasprice-provider-conf@";

    @Resource
    private ISystemConfigRepository systemConfigRepository;

    @Resource
    private RedissonClient redisson;

    @MockitoBean
    private BlockchainConfig blockchainConfig;

    @MockitoBean
    private L1Client l1Client;

    @MockitoBean
    private L2Client l2Client;

    @MockitoBean
    private RollupConfig rollupConfig;

    @MockitoBean
    private IOracleRepository oracleRepository;

    @MockitoBean
    private DistributedTaskEngine distributedTaskEngine;

    @MockitoBean
    private RollupAlarm rollupAlarm;

    @MockitoBean
    private AccountBalanceMonitor accountBalanceMonitor;

    @MockitoBean(name = "l1ChainId")
    private BigInteger l1ChainId;

    @Resource(name = "l1-gasprice-provider-default-conf")
    private GasPriceProviderConfig l1GasPriceProviderDefaultConfig;

    @Resource(name = "l1-gasprice-provider-conf")
    private DynamicGasPriceProviderConfig l1GasPriceProviderConfig;

    @Test
    public void testDefaultConfig() {
        Assert.assertEquals(1000000000L, l1GasPriceProviderDefaultConfig.getMinimumEip4844PriorityPrice().longValue());
    }

    @Test
    public void testDynamicConfigFromDefault() {
        // clean the cache
        redisson.getMap(L1_GAS_PRICE_CONF_MAP_KEY).clear();

        Assert.assertEquals(1000000000L, l1GasPriceProviderConfig.getMinimumEip4844PriorityPrice().longValue());
        Assert.assertEquals(l1GasPriceProviderDefaultConfig.getMinimumEip1559PriorityPrice(), l1GasPriceProviderConfig.getMinimumEip1559PriorityPrice());
        Assert.assertEquals(l1GasPriceProviderDefaultConfig.getFeePerBlobGasDividingVal(), l1GasPriceProviderConfig.getFeePerBlobGasDividingVal());
        Assert.assertEquals(l1GasPriceProviderDefaultConfig.getMaxPriceLimit(), l1GasPriceProviderConfig.getMaxPriceLimit());
        Assert.assertEquals(l1GasPriceProviderDefaultConfig.getExtraGasPrice(), l1GasPriceProviderConfig.getExtraGasPrice());

        Assert.assertEquals(GasPriceProviderSupplierEnum.ETHEREUM, l1GasPriceProviderConfig.getGasPriceProviderSupplier());

        Assert.assertEquals(l1GasPriceProviderDefaultConfig.getGasProviderUrl(), l1GasPriceProviderConfig.getGasProviderUrl());
        Assert.assertEquals(l1GasPriceProviderDefaultConfig.getApiKey(), l1GasPriceProviderConfig.getApiKey());

        Assert.assertEquals(l1GasPriceProviderDefaultConfig.getGasUpdateInterval(), l1GasPriceProviderConfig.getGasUpdateInterval());

        Assert.assertEquals(l1GasPriceProviderDefaultConfig.getGasPriceIncreasedPercentage(), l1GasPriceProviderConfig.getGasPriceIncreasedPercentage(), 0.001);
        Assert.assertEquals(l1GasPriceProviderDefaultConfig.getLargerFeePerBlobGasMultiplier(), l1GasPriceProviderConfig.getLargerFeePerBlobGasMultiplier(), 0.001);
        Assert.assertEquals(l1GasPriceProviderDefaultConfig.getSmallerFeePerBlobGasMultiplier(), l1GasPriceProviderConfig.getSmallerFeePerBlobGasMultiplier(), 0.001);
        Assert.assertEquals(l1GasPriceProviderDefaultConfig.getPriorityFeePerGasIncreasedPercentage(), l1GasPriceProviderConfig.getPriorityFeePerGasIncreasedPercentage(), 0.001);
        Assert.assertEquals(l1GasPriceProviderDefaultConfig.getEip4844PriorityFeePerGasIncreasedPercentage(), l1GasPriceProviderConfig.getEip4844PriorityFeePerGasIncreasedPercentage(), 0.001);

        Assert.assertEquals(l1GasPriceProviderDefaultConfig.getBaseFeeMultiplier(), l1GasPriceProviderConfig.getBaseFeeMultiplier());
    }

    @Test
    public void testWriteAndReadDynamicConfig() {
        // Test BigInteger configurations using var and modern syntax
        var testEip4844Price = BigInteger.ONE;
        l1GasPriceProviderConfig.setMinimumEip4844PriorityPrice(testEip4844Price);
        Assert.assertEquals(testEip4844Price, l1GasPriceProviderConfig.getMinimumEip4844PriorityPrice());

        var testEip1559Price = BigInteger.valueOf(2_000_000_000L);
        l1GasPriceProviderConfig.setMinimumEip1559PriorityPrice(testEip1559Price);
        Assert.assertEquals(testEip1559Price, l1GasPriceProviderConfig.getMinimumEip1559PriorityPrice());

        var testDividingVal = BigInteger.valueOf(5);
        l1GasPriceProviderConfig.setFeePerBlobGasDividingVal(testDividingVal);
        Assert.assertEquals(testDividingVal, l1GasPriceProviderConfig.getFeePerBlobGasDividingVal());

        var testMaxPrice = BigInteger.valueOf(100_000_000_000L);
        l1GasPriceProviderConfig.setMaxPriceLimit(testMaxPrice);
        Assert.assertEquals(testMaxPrice, l1GasPriceProviderConfig.getMaxPriceLimit());

        var testExtraGas = BigInteger.valueOf(500_000_000L);
        l1GasPriceProviderConfig.setExtraGasPrice(testExtraGas);
        Assert.assertEquals(testExtraGas, l1GasPriceProviderConfig.getExtraGasPrice());

        var testBaseFeeMultiplier = 3;
        l1GasPriceProviderConfig.setBaseFeeMultiplier(3);
        Assert.assertEquals(testBaseFeeMultiplier, l1GasPriceProviderConfig.getBaseFeeMultiplier());

        // Test enum configuration
        var testSupplier = GasPriceProviderSupplierEnum.ETHEREUM;
        l1GasPriceProviderConfig.setGasPriceProviderSupplier(testSupplier);
        Assert.assertEquals(testSupplier, l1GasPriceProviderConfig.getGasPriceProviderSupplier());

        // Test String configurations using text blocks for better readability
        var testUrl = """
                https://test-gas-provider.com/api/v1
                """.trim();
        l1GasPriceProviderConfig.setGasProviderUrl(testUrl);
        Assert.assertEquals(testUrl, l1GasPriceProviderConfig.getGasProviderUrl());

        var testApiKey = "test-api-key-123";
        l1GasPriceProviderConfig.setApiKey(testApiKey);
        Assert.assertEquals(testApiKey, l1GasPriceProviderConfig.getApiKey());

        // Test Long configuration
        var testInterval = 30_000L;
        l1GasPriceProviderConfig.setGasUpdateInterval(testInterval);
        Assert.assertEquals(testInterval, l1GasPriceProviderConfig.getGasUpdateInterval());

        // Test double configurations with modern syntax
        var testGasPricePercentage = 1.5d;
        l1GasPriceProviderConfig.setGasPriceIncreasedPercentage(testGasPricePercentage);
        Assert.assertEquals(testGasPricePercentage, l1GasPriceProviderConfig.getGasPriceIncreasedPercentage(), 0.001);

        var testLargerMultiplier = 2.0d;
        l1GasPriceProviderConfig.setLargerFeePerBlobGasMultiplier(testLargerMultiplier);
        Assert.assertEquals(testLargerMultiplier, l1GasPriceProviderConfig.getLargerFeePerBlobGasMultiplier(), 0.001);

        var testSmallerMultiplier = 0.8d;
        l1GasPriceProviderConfig.setSmallerFeePerBlobGasMultiplier(testSmallerMultiplier);
        Assert.assertEquals(testSmallerMultiplier, l1GasPriceProviderConfig.getSmallerFeePerBlobGasMultiplier(), 0.001);

        var testPriorityPercentage = 1.2d;
        l1GasPriceProviderConfig.setPriorityFeePerGasIncreasedPercentage(testPriorityPercentage);
        Assert.assertEquals(testPriorityPercentage, l1GasPriceProviderConfig.getPriorityFeePerGasIncreasedPercentage(), 0.001);

        var testEip4844Percentage = 1.3d;
        l1GasPriceProviderConfig.setEip4844PriorityFeePerGasIncreasedPercentage(testEip4844Percentage);
        Assert.assertEquals(testEip4844Percentage, l1GasPriceProviderConfig.getEip4844PriorityFeePerGasIncreasedPercentage(), 0.001);
    }

    @Test
    @SneakyThrows
    public void testDynamicConfigFromStorage() {
        var lock = redisson.getLock("persist_config@l1-gasprice-provider-conf");
        Assert.assertTrue(lock.tryLock(30, 30, TimeUnit.SECONDS));
        // clean the cache
        redisson.getMap(L1_GAS_PRICE_CONF_MAP_KEY).clear();

        l1GasPriceProviderConfig.setGasProviderUrl("https://test-gas-provider.com/api/v1");
        systemConfigRepository.setSystemConfig(L1_GASPRICE_PROVIDER_CONF_PREFIX + GasPriceProviderConfig.Fields.minimumEip4844PriorityPrice, BigInteger.valueOf(100).toString());
        systemConfigRepository.setSystemConfig(L1_GASPRICE_PROVIDER_CONF_PREFIX + GasPriceProviderConfig.Fields.apiKey, "123");

        var initCacheFromStorageM = ReflectUtil.getMethod(DynamicGasPriceProviderConfig.class, "initCacheFromStorage");
        initCacheFromStorageM.setAccessible(true);
        initCacheFromStorageM.invoke(l1GasPriceProviderConfig);

        Assert.assertEquals("https://test-gas-provider.com/api/v1", l1GasPriceProviderConfig.getGasProviderUrl());
        Assert.assertEquals(BigInteger.valueOf(100), l1GasPriceProviderConfig.getMinimumEip4844PriorityPrice());

        // test the value desensitize filter
        var rawJsonObj = JSON.parseObject("""
                {"l1-gasprice-provider-conf@apiKey":"abc"}""");
        var valueDesensitizeFilterF = ReflectUtil.getField(PrefixedDynamicConfig.class, "valueDesensitizeFilter");
        valueDesensitizeFilterF.setAccessible(true);
        Assert.assertEquals(
                "******",
                ((ValueDesensitizeFilter) valueDesensitizeFilterF.get(l1GasPriceProviderConfig)).process(rawJsonObj, rawJsonObj.keySet().iterator().next(), rawJsonObj.values().iterator().next())
        );

        lock.unlock();
    }

    @Test
    @SneakyThrows
    public void testDynamicConfigPersist() {
        // clean the cache
        redisson.getMap(L1_GAS_PRICE_CONF_MAP_KEY).clear();

        l1GasPriceProviderConfig.setMinimumEip4844PriorityPrice(BigInteger.valueOf(123));
        int maxTries = 10;
        while (!StrUtil.equals(systemConfigRepository.getSystemConfig(L1_GASPRICE_PROVIDER_CONF_PREFIX + GasPriceProviderConfig.Fields.minimumEip4844PriorityPrice), "123") && maxTries-- > 0) {
            Thread.sleep(1000);
        }
        Assert.assertTrue(maxTries > 0);
        Assert.assertEquals("123", systemConfigRepository.getSystemConfig(L1_GASPRICE_PROVIDER_CONF_PREFIX + GasPriceProviderConfig.Fields.minimumEip4844PriorityPrice));
    }
}