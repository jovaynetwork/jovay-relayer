package com.alipay.antchain.l2.relayer.config;

import java.math.BigInteger;

import com.alipay.antchain.l2.relayer.TestBase;
import com.alipay.antchain.l2.relayer.core.blockchain.L1Client;
import com.alipay.antchain.l2.relayer.core.blockchain.L2Client;
import com.alipay.antchain.l2.relayer.core.blockchain.bpo.EthBlobForkConfig;
import jakarta.annotation.Resource;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

public class EthForkConfigTest extends TestBase {

    @Resource
    private EthBlobForkConfig ethBlobForkConfig;

    @MockitoBean
    private BigInteger l1ChainId;

    @MockitoBean
    private L1Client l1Client;

    @MockitoBean
    private L2Client l2Client;

    @MockitoBean
    private RollupConfig rollupConfig;

    @Test
    public void testBean() {
        var curr = ethBlobForkConfig.getCurrConfig();
        Assert.assertEquals("Osaka", curr.getName());
        Assert.assertEquals(1, curr.getBlobSidecarVersion());
        Assert.assertEquals(BigInteger.valueOf(5007716), curr.getUpdateFraction());
    }
}
