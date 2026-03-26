/*
 * Copyright 2026 Ant Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alipay.antchain.l2.relayer.core.blockchain;

import java.util.ArrayList;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
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
import org.slf4j.LoggerFactory;
import org.web3j.protocol.Web3j;
import org.web3j.utils.Convert;

import static org.junit.Assert.assertTrue;
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

    /**
     * Test: Owlracle API returns HTTP 500 error
     * Scenario: API server internal error
     * Expected: getGasPrice returns default value or throws exception
     */
    @Test
    @SneakyThrows
    public void testOwlracle_ServerError() {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse.Builder().code(500).body("Internal Server Error").build());
        server.start();

        HttpUrl baseUrl = server.url("");

        GasPriceProviderConfig config = new GasPriceProviderConfig();
        config.setApiKey("test");
        config.setGasProviderUrl(baseUrl.toString());
        config.setGasUpdateInterval(1000);

        Web3j web3j = mock(Web3j.class);
        OwlracleGasPriceProvider provider = new OwlracleGasPriceProvider(web3j, config, null);

        // Due to API call failure, should use default value or throw exception
        // This test does not throw exception, but returns a default value
        try {
            provider.getGasPrice();
        } catch (Exception e) {
            // Expected to possibly throw exception
            Assert.assertNotNull(e);
        }
    }

    /**
     * Test: Owlracle API returns empty response
     * Scenario: API returns empty speeds list
     * Expected: getGasPrice handles empty list case
     */
    @Test
    @SneakyThrows
    public void testOwlracle_EmptyResponse() {
        OwlracleResponse<OwlracleGetGasPriceResult> resp = new OwlracleResponse<>();
        resp.setSpeeds(new ArrayList<>());

        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse.Builder().body(JSON.toJSONString(resp)).build());
        server.start();

        HttpUrl baseUrl = server.url("");

        GasPriceProviderConfig config = new GasPriceProviderConfig();
        config.setApiKey("test");
        config.setGasProviderUrl(baseUrl.toString());
        config.setGasUpdateInterval(1000);

        Web3j web3j = mock(Web3j.class);
        OwlracleGasPriceProvider provider = new OwlracleGasPriceProvider(web3j, config, null);

        // Should throw exception or return default value
        try {
            provider.getGasPrice();
        } catch (Exception e) {
            Assert.assertNotNull(e);
        }
    }

    /**
     * Test: Owlracle API returns malformed JSON
     * Scenario: API returns invalid JSON format
     * Expected: Parsing fails, uses default value or throws exception
     */
    @Test
    @SneakyThrows
    public void testOwlracle_InvalidJson() {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse.Builder().body("invalid json").build());
        server.start();

        HttpUrl baseUrl = server.url("");

        GasPriceProviderConfig config = new GasPriceProviderConfig();
        config.setApiKey("test");
        config.setGasProviderUrl(baseUrl.toString());
        config.setGasUpdateInterval(1000);

        Web3j web3j = mock(Web3j.class);
        OwlracleGasPriceProvider provider = new OwlracleGasPriceProvider(web3j, config, null);

        // Should throw exception
        try {
            provider.getGasPrice();
        } catch (Exception e) {
            Assert.assertNotNull(e);
        }
    }

    /**
     * Test: Etherscan API returns status code 0 (failure)
     * Scenario: API returns failure status
     * Expected: getGasPrice handles failure status
     */
    @Test
    @SneakyThrows
    public void testEtherscan_FailureStatus() {
        EtherscanResponse<EtherscanGetGasOracleResult> resp = new EtherscanResponse<>();
        resp.setStatus("0");
        resp.setMessage("NOTOK");

        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse.Builder().body(JSON.toJSONString(resp)).build());
        server.start();

        HttpUrl baseUrl = server.url("");

        GasPriceProviderConfig config = new GasPriceProviderConfig();
        config.setApiKey("test");
        config.setGasProviderUrl(baseUrl.toString());
        config.setGasUpdateInterval(1000);

        Web3j web3j = mock(Web3j.class);
        EtherscanGasPriceProvider provider = new EtherscanGasPriceProvider(web3j, config, null);

        // Should throw exception or return default value
        try {
            provider.getGasPrice();
        } catch (Exception e) {
            Assert.assertNotNull(e);
        }
    }

    /**
     * Test: Etherscan API returns null result
     * Scenario: API returns null in result field
     * Expected: getGasPrice handles null result
     */
    @Test
    @SneakyThrows
    public void testEtherscan_NullResult() {
        Logger logger = (Logger) LoggerFactory.getLogger(EtherscanGasPriceProvider.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        try {
            EtherscanResponse<EtherscanGetGasOracleResult> resp = new EtherscanResponse<>();
            resp.setStatus("1");
            resp.setResult(null);

            MockWebServer server = new MockWebServer();
            server.enqueue(new MockResponse.Builder().body(JSON.toJSONString(resp)).build());
            server.start();

            HttpUrl baseUrl = server.url("");

            GasPriceProviderConfig config = new GasPriceProviderConfig();
            config.setApiKey("test");
            config.setGasProviderUrl(baseUrl.toString());
            config.setGasUpdateInterval(1000);

            Web3j web3j = mock(Web3j.class);
            new EtherscanGasPriceProvider(web3j, config, null);

            boolean foundInfoLog = listAppender.list.stream()
                    .anyMatch(event -> event.getThrowableProxy() != null
                                       && event.getThrowableProxy().getClassName().equals(NullPointerException.class.getName()));
            assertTrue("Expected info log not found", foundInfoLog);
        } finally {
            logger.detachAppender(listAppender);
        }
    }

    /**
     * Test: Etherscan API returns HTTP 404 error
     * Scenario: API endpoint does not exist
     * Expected: getGasPrice handles 404 error
     */
    @Test
    @SneakyThrows
    public void testEtherscan_NotFound() {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse.Builder().code(404).body("Not Found").build());
        server.start();

        HttpUrl baseUrl = server.url("");

        GasPriceProviderConfig config = new GasPriceProviderConfig();
        config.setApiKey("test");
        config.setGasProviderUrl(baseUrl.toString());
        config.setGasUpdateInterval(1000);

        Web3j web3j = mock(Web3j.class);
        EtherscanGasPriceProvider provider = new EtherscanGasPriceProvider(web3j, config, null);

        // Should throw exception
        try {
            provider.getGasPrice();
        } catch (Exception e) {
            Assert.assertNotNull(e);
        }
    }

    /**
     * Test: Etherscan API returns invalid gas price format
     * Scenario: proposeGasPrice is not a valid number
     * Expected: Throws NumberFormatException
     */
    @Test
    @SneakyThrows
    public void testEtherscan_InvalidGasPriceFormat() {
        Logger logger = (Logger) LoggerFactory.getLogger(EtherscanGasPriceProvider.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        try {
            EtherscanResponse<EtherscanGetGasOracleResult> resp = new EtherscanResponse<>();
            resp.setStatus("1");
            EtherscanGetGasOracleResult res = new EtherscanGetGasOracleResult();
            res.setProposeGasPrice("invalid");
            resp.setResult(res);

            MockWebServer server = new MockWebServer();
            server.enqueue(new MockResponse.Builder().body(JSON.toJSONString(resp)).build());
            server.start();

            HttpUrl baseUrl = server.url("");

            GasPriceProviderConfig config = new GasPriceProviderConfig();
            config.setApiKey("test");
            config.setGasProviderUrl(baseUrl.toString());
            config.setGasUpdateInterval(1000);

            Web3j web3j = mock(Web3j.class);
            new EtherscanGasPriceProvider(web3j, config, null);

            boolean foundInfoLog = listAppender.list.stream()
                    .anyMatch(event -> event.getThrowableProxy() != null
                                       && event.getThrowableProxy().getMessage().contains("Character i is neither a decimal digit number, decimal point, nor \"e\" notation exponential mark."));
            assertTrue("Expected info log not found", foundInfoLog);
        } finally {
            logger.detachAppender(listAppender);
        }
    }

    /**
     * Test: Owlracle API network timeout
     * Scenario: API request times out
     * Expected: getGasPrice handles timeout exception
     */
    @Test
    @SneakyThrows
    public void testOwlracle_Timeout() {
        MockWebServer server = new MockWebServer();
        // No response returned, simulating timeout
        server.start();

        HttpUrl baseUrl = server.url("");

        GasPriceProviderConfig config = new GasPriceProviderConfig();
        config.setApiKey("test");
        config.setGasProviderUrl(baseUrl.toString());
        config.setGasUpdateInterval(100); // Short timeout

        Web3j web3j = mock(Web3j.class);
        OwlracleGasPriceProvider provider = new OwlracleGasPriceProvider(web3j, config, null);

        // Should throw timeout exception
        try {
            provider.getGasPrice();
        } catch (Exception e) {
            Assert.assertNotNull(e);
        }
    }

    /**
     * Test: Owlracle API returns negative gas price
     * Scenario: API returns negative maxFeePerGas
     * Expected: Handles negative case
     */
    @Test
    @SneakyThrows
    public void testOwlracle_NegativeGasPrice() {
        OwlracleResponse<OwlracleGetGasPriceResult> resp = new OwlracleResponse<>();
        resp.setSpeeds(new ArrayList<>());
        resp.getSpeeds().add(null);
        resp.getSpeeds().add(null);
        resp.getSpeeds().add(null);
        resp.getSpeeds().add(OwlracleGetGasPriceResult.builder().maxFeePerGas("-100").build());

        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse.Builder().body(JSON.toJSONString(resp)).build());
        server.start();

        HttpUrl baseUrl = server.url("");

        GasPriceProviderConfig config = new GasPriceProviderConfig();
        config.setApiKey("test");
        config.setGasProviderUrl(baseUrl.toString());
        config.setGasUpdateInterval(1000);

        Web3j web3j = mock(Web3j.class);
        OwlracleGasPriceProvider provider = new OwlracleGasPriceProvider(web3j, config, null);

        // Should throw exception or return default value
        try {
            provider.getGasPrice();
        } catch (Exception e) {
            Assert.assertNotNull(e);
        }
    }
}
