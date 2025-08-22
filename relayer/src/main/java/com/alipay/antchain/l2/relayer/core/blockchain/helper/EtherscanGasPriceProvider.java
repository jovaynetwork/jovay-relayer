package com.alipay.antchain.l2.relayer.core.blockchain.helper;

import java.math.BigInteger;
import java.util.Objects;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.etherscan.EtherscanGetGasOracleResult;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.etherscan.EtherscanResponse;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.Eip1559GasPrice;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.GasPriceProviderConfig;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import okhttp3.Response;
import org.web3j.protocol.Web3j;
import org.web3j.utils.Convert;

@Slf4j
public class EtherscanGasPriceProvider extends ApiGasPriceProvider {

    public EtherscanGasPriceProvider(
            Web3j web3j,
            GasPriceProviderConfig config
    ) {
        super(web3j, config,true);
    }

    @Override
    protected void updateGasPrice() {
        try (
                Response response = getClient().newCall(
                        new Request.Builder()
                                .url(getGasPriceProviderConfig().getGasProviderUrl() + getGasPriceProviderConfig().getApiKey())
                                .build()
                ).execute()
        ) {
            if (!response.isSuccessful()) {
                throw new RuntimeException(
                        StrUtil.format(
                                "http request failed: {} - {}",
                                response.code(), response.message()
                        )
                );
            }
            EtherscanResponse<EtherscanGetGasOracleResult> resp = JSON.parseObject(
                    Objects.requireNonNull(response.body(), "empty resp body").string(),
                    new TypeReference<EtherscanResponse<EtherscanGetGasOracleResult>>() {
                    }
            );
            if (!StrUtil.equals(resp.getStatus(), "1")) {
                throw new RuntimeException(
                        StrUtil.format(
                                "etherscan api error: {} - {}",
                                resp.getStatus(), resp.getMessage()
                        )
                );
            }
            log.info("update gas price: {} gwei", resp.getResult().getProposeGasPrice());
            setGasPrice(Convert.toWei(resp.getResult().getProposeGasPrice(), Convert.Unit.GWEI).toBigInteger());
            setBaseFee(Convert.toWei(resp.getResult().getSuggestBaseFee(), Convert.Unit.GWEI).toBigInteger());
            setMaxPriorityFeePerGas(getGasPrice().subtract(getBaseFee()));
        } catch (Throwable t) {
            log.error("gas oracle from etherscan error", t);
        }
    }

    @Override
    public Eip1559GasPrice getEip1559GasPrice() {
        return new Eip1559GasPrice(getBaseFee().multiply(BigInteger.valueOf(2)), getMaxPriorityFeePerGas());
    }
}
