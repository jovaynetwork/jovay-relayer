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

package com.alipay.antchain.l2.relayer.core.blockchain.helper;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Timer;
import java.util.TimerTask;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.l2.relayer.core.blockchain.bpo.EthBlobForkConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.gasprice.IGasPriceProviderConfig;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;

@Setter(AccessLevel.PROTECTED)
@Getter(AccessLevel.PROTECTED)
@Slf4j
public abstract class ApiGasPriceProvider implements IGasPriceProvider {

    public static ApiGasPriceProvider create(Web3j web3j, IGasPriceProviderConfig config, EthBlobForkConfig ethBlobForkConfig) {
        switch (config.getGasPriceProviderSupplier()) {
            case ETHERSCAN:
                return new EtherscanGasPriceProvider(web3j, config, ethBlobForkConfig);
            case OWLRACLE:
                return new OwlracleGasPriceProvider(web3j, config, ethBlobForkConfig);
            case ETHEREUM:
                return new EthereumGasPriceProvider(web3j, config, ethBlobForkConfig);
            default:
                log.warn("use {} to get gas price not implemented yet", config.getGasPriceProviderSupplier());
                throw new RuntimeException(StrUtil.format("use {} to get gas price not implemented yet", config.getGasPriceProviderSupplier().toString()));
        }
    }

    private static TrustManager[] buildTrustManagers() {
        return new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new java.security.cert.X509Certificate[]{};
                    }
                }
        };
    }

    private Web3j web3j;

    private OkHttpClient client;

    private Timer timer;

    private IGasPriceProviderConfig gasPriceProviderConfig;

    private EthBlobForkConfig ethBlobForkConfig;

    private BigInteger gasPrice;

    private BigInteger baseFee;

    private BigInteger maxFeePerGas;

    private BigInteger maxPriorityFeePerGas;

    @SneakyThrows
    public ApiGasPriceProvider(
            Web3j web3j,
            IGasPriceProviderConfig config,
            EthBlobForkConfig ethBlobForkConfig,
            boolean ifStartTimer
    ) {
        TrustManager[] trustAllCerts = buildTrustManagers();
        SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

        client = new OkHttpClient.Builder()
                .sslSocketFactory(
                        sslContext.getSocketFactory(),
                        (X509TrustManager) trustAllCerts[0]
                ).hostnameVerifier((hostname, session) -> true)
                .build();
        this.web3j = web3j;
        this.gasPriceProviderConfig = config;
        this.ethBlobForkConfig = ethBlobForkConfig;

        updateGasPrice();
        if(ifStartTimer) {
            timer = new Timer();
            timer.scheduleAtFixedRate(
                    new TimerTask() {
                        @Override
                        public void run() {
                            updateGasPrice();
                        }
                    },
                    0, this.gasPriceProviderConfig.getGasUpdateInterval() // Owlracle can request our data up to 100 times per hour for free.
            );
        }
    }

    protected abstract void updateGasPrice();

    @Override
    public BigInteger getGasPrice(String contractFunc) {
        return gasPrice;
    }

    @Override
    public BigInteger getGasPrice() {
        return gasPrice;
    }

    protected BigInteger getMaxFeePerBlobGas() {
        var maxFee = ethGetBaseFeePerBlobGas();
        return new BigDecimal(maxFee)
                .multiply(BigDecimal.valueOf(getGasPriceProviderConfig().getBlobFeeMultiplier(maxFee)))
                .toBigInteger();
    }

    private BigInteger ethGetBaseFeePerBlobGas() {
        try {
            var ethBlock = getWeb3j().ethGetBlockByNumber(DefaultBlockParameterName.LATEST, false).send();
            if (ObjectUtil.isNull(ethBlock) || ObjectUtil.isNull(ethBlock.getBlock())) {
                throw new RuntimeException("get null latest block from blockchain");
            }
            if (ethBlock.hasError()) {
                throw new RuntimeException("failed to get block: " + ethBlock.getError().getMessage());
            }
            return this.ethBlobForkConfig.getCurrConfig().fakeExponential(ethBlock.getBlock().getExcessBlobGas());
        } catch (Exception e) {
            throw new RuntimeException("Failed to get baseFeePerBlobGas value: ", e);
        }
    }
}
