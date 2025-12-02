package com.alipay.antchain.l2.relayer.core.blockchain.helper;

import java.util.Objects;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.alipay.antchain.l2.relayer.core.blockchain.bpo.EthBlobForkConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.gasprice.IGasPriceProviderConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.*;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.owlracle.OwlracleGetGasPriceResult;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.owlracle.OwlracleResponse;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import okhttp3.Response;
import org.web3j.protocol.Web3j;
import org.web3j.utils.Convert;

@Slf4j
public class OwlracleGasPriceProvider extends ApiGasPriceProvider {

    public OwlracleGasPriceProvider(
            Web3j web3j,
            IGasPriceProviderConfig config,
            EthBlobForkConfig ethBlobForkConfig
    ) {
        super(web3j, config, ethBlobForkConfig, true);
    }

    @Override
    protected void updateGasPrice() {
        try (
                Response response = getClient().newCall(
                        new Request.Builder()
                                .url(StrUtil.format(
                                        getGasPriceProviderConfig().getGasProviderUrl() + getGasPriceProviderConfig().getApiKey())
                                ).get()
                                .build()
                ).execute()
        ) {
            if (!response.isSuccessful()) {
                throw new RuntimeException(
                        StrUtil.format("http request failed: {} - {}", response.code(), response.message())
                );
            }
            OwlracleResponse<OwlracleGetGasPriceResult> resp = JSON.parseObject(
                    Objects.requireNonNull(response.body(), "empty resp body").string(),
                    new TypeReference<OwlracleResponse<OwlracleGetGasPriceResult>>() {
                    }
            );
            log.info("update gas price: {} gwei", resp.getSpeeds().get(3).getMaxFeePerGas());
            setGasPrice(Convert.toWei(resp.getSpeeds().get(3).getMaxFeePerGas(), Convert.Unit.GWEI).toBigInteger());
            setMaxFeePerGas(Convert.toWei(resp.getSpeeds().get(3).getMaxFeePerGas(), Convert.Unit.GWEI).toBigInteger());
            setMaxPriorityFeePerGas(Convert.toWei(resp.getSpeeds().get(3).getMaxPriorityFeePerGas(), Convert.Unit.GWEI).toBigInteger());
        } catch (Throwable t) {
            log.error("gas oracle from Owlracle error", t);
        }
    }

    @Override
    public Eip1559GasPrice getEip1559GasPrice() {
        return new Eip1559GasPrice(getMaxFeePerGas(), getMaxPriorityFeePerGas(), getBaseFee());
    }

    @Override
    public IGasPrice getEip4844GasPrice() {
        return new Eip4844GasPrice(getMaxFeePerGas(), getMaxPriorityFeePerGas(), getMaxFeePerBlobGas(), getBaseFee());
    }
}
