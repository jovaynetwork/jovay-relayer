package com.alipay.antchain.l2.relayer.core.blockchain;

import java.util.ArrayList;

import com.alibaba.fastjson.JSON;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.EtherscanGasPriceProvider;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.OwlracleGasPriceProvider;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.etherscan.EtherscanGetGasOracleResult;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.etherscan.EtherscanResponse;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.gasprice.GasPriceProviderConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.owlracle.OwlracleGetGasPriceResult;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.owlracle.OwlracleResponse;
import lombok.SneakyThrows;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import okhttp3.HttpUrl;
import org.junit.Assert;
import org.junit.Test;
import org.web3j.protocol.Web3j;
import org.web3j.utils.Convert;

import static org.mockito.Mockito.mock;

public class GasProviderTest {

    @Test
    @SneakyThrows
    public void testOwlracle() {
        OwlracleResponse<OwlracleGetGasPriceResult> resp = new OwlracleResponse<>();
        resp.setSpeeds(new ArrayList<>());;
        resp.getSpeeds().add(null);
        resp.getSpeeds().add(null);
        resp.getSpeeds().add(null);
        resp.getSpeeds().add(OwlracleGetGasPriceResult.builder().maxFeePerGas("1234").build());

        MockWebServer server = new MockWebServer();
        // Schedule some responses.
        server.enqueue(new MockResponse.Builder().body(JSON.toJSONString(resp)).build());
        server.start();

        HttpUrl baseUrl = server.url("");

        GasPriceProviderConfig config = new GasPriceProviderConfig();
        config.setApiKey("test");
        config.setGasProviderUrl(baseUrl.toString());
        config.setGasUpdateInterval(1000);

        Web3j web3j = mock(Web3j.class);
        OwlracleGasPriceProvider provider = new OwlracleGasPriceProvider(web3j, config, null);

        Assert.assertEquals(Convert.toWei("1234", Convert.Unit.GWEI).toBigInteger(), provider.getGasPrice());
    }

    @Test
    @SneakyThrows
    public void testEtherscan() {
        EtherscanResponse<EtherscanGetGasOracleResult> resp = new EtherscanResponse<>();
        resp.setStatus("1");
        EtherscanGetGasOracleResult res = new EtherscanGetGasOracleResult();
        res.setProposeGasPrice("1234");
        resp.setResult(res);

        MockWebServer server = new MockWebServer();
        // Schedule some responses.
        server.enqueue(new MockResponse.Builder().body(JSON.toJSONString(resp)).build());
        server.start();

        HttpUrl baseUrl = server.url("");

        GasPriceProviderConfig config = new GasPriceProviderConfig();
        config.setApiKey("test");
        config.setGasProviderUrl(baseUrl.toString());
        config.setGasUpdateInterval(1000);

        Web3j web3j = mock(Web3j.class);
        EtherscanGasPriceProvider provider = new EtherscanGasPriceProvider(web3j, config, null);

        Assert.assertEquals(Convert.toWei("1234", Convert.Unit.GWEI).toBigInteger(), provider.getGasPrice());
    }
}
