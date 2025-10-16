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
import com.alipay.antchain.l2.relayer.commons.utils.Utils;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.GasPriceProviderConfig;
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

    private static final BigInteger MIN_BLOB_BASE_FEE = new BigInteger("1");
    private static final BigInteger BLOB_BASE_FEE_UPDATE_FRACTION = new BigInteger("5007716");

    public static ApiGasPriceProvider create(Web3j web3j, GasPriceProviderConfig config) {
        switch (config.getGasPriceProviderSupplier()) {
            case ETHERSCAN:
                return new EtherscanGasPriceProvider(web3j, config);
            case OWLRACLE:
                return new OwlracleGasPriceProvider(web3j, config);
            case ETHEREUM:
                return new EthereumGasPriceProvider(web3j, config);
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

    private GasPriceProviderConfig gasPriceProviderConfig;

    private BigInteger gasPrice;

    private BigInteger baseFee;

    private BigInteger maxFeePerGas;

    private BigInteger maxPriorityFeePerGas;

    @SneakyThrows
    public ApiGasPriceProvider(
            Web3j web3j,
            GasPriceProviderConfig config,
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
            return Utils.fakeExponential(ethBlock.getBlock().getExcessBlobGas());
        } catch (Exception e) {
            throw new RuntimeException("Failed to get baseFeePerBlobGas value: ", e);
        }
    }


}
