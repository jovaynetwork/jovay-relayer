package com.alipay.antchain.l2.relayer.core.blockchain.helper;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.Eip1559GasPrice;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.Eip4844GasPrice;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.GasPriceProviderConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.IGasPrice;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;

@Slf4j
public class EthereumGasPriceProvider extends ApiGasPriceProvider {

    public EthereumGasPriceProvider(
            Web3j web3j,
            GasPriceProviderConfig config
    ) {
        super(web3j, config, false);
    }

    @Override
    public void updateGasPrice() {}

    @Override
    @SneakyThrows
    public BigInteger getGasPrice(String contractFunc) {
        /* nasFee = baseFee + maxPriorityFee */
        var resp = getWeb3j().ethGasPrice().send();
        if (resp.hasError()) {
            throw new RuntimeException("get gas price failed: " + resp.getError().getMessage());
        }
        var baseFee = resp.getGasPrice();
        var maxPriorityFee = BigDecimal.valueOf(getGasPriceProviderConfig().getGasPriceIncreasedPercentage()).multiply(new BigDecimal(baseFee));
        maxPriorityFee = maxPriorityFee.compareTo(new BigDecimal(getGasPriceProviderConfig().getExtraGasPrice())) > 0 ?
                new BigDecimal(getGasPriceProviderConfig().getExtraGasPrice()) : maxPriorityFee;
        var maxFee = baseFee.add(maxPriorityFee.toBigInteger());
        log.debug("get gas price: {} wei", maxFee);
        return maxFee;
    }

    @Override
    @SneakyThrows
    public BigInteger getGasPrice() {
        return this.getGasPrice(null);
    }

    @Override
    @SneakyThrows
    public IGasPrice getEip1559GasPrice() {
        var tipRespFuture = getTipFeeAsync();
        var priorityRespFuture = getPriorityFeeAsync();

        var maxPriorityFee = new BigDecimal(priorityRespFuture.get(5, TimeUnit.SECONDS)).multiply(
                BigDecimal.valueOf(getGasPriceProviderConfig().getPriorityFeePerGasIncreasedPercentage() + 1)
        ).toBigInteger();
        maxPriorityFee = maxPriorityFee.compareTo(getGasPriceProviderConfig().getMinimumEip1559PriorityPrice()) > 0 ?
                maxPriorityFee :
                getGasPriceProviderConfig().getMinimumEip1559PriorityPrice();
        var baseFee = tipRespFuture.get(5, TimeUnit.SECONDS);
        getGasPriceProviderConfig().checkIfPriceOutOfLimit(baseFee, maxPriorityFee);
        return new Eip1559GasPrice(
                baseFee.multiply(BigInteger.valueOf(getGasPriceProviderConfig().getBaseFeeMultiplier())).add(maxPriorityFee),
                maxPriorityFee
        ).validate();
    }

    @Override
    @SneakyThrows
    public IGasPrice getEip4844GasPrice() {
        var tipRespFuture = getTipFeeAsync();
        var priorityRespFuture = getPriorityFeeAsync();

        var maxPriorityFee = new BigDecimal(priorityRespFuture.get(5, TimeUnit.SECONDS)).multiply(
                BigDecimal.valueOf(getGasPriceProviderConfig().getEip4844PriorityFeePerGasIncreasedPercentage() + 1)
        ).toBigInteger();
        maxPriorityFee = maxPriorityFee.compareTo(getGasPriceProviderConfig().getMinimumEip4844PriorityPrice()) > 0 ?
                maxPriorityFee :
                getGasPriceProviderConfig().getMinimumEip4844PriorityPrice();
        var baseFee = tipRespFuture.get(5, TimeUnit.SECONDS);
        getGasPriceProviderConfig().checkIfPriceOutOfLimit(baseFee, maxPriorityFee);
        return new Eip4844GasPrice(
                baseFee.multiply(BigInteger.valueOf(getGasPriceProviderConfig().getBaseFeeMultiplier())).add(maxPriorityFee),
                maxPriorityFee,
                getMaxFeePerBlobGas()
        ).validate();
    }

    private CompletableFuture<BigInteger> getPriorityFeeAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var priorityResp = getWeb3j().ethMaxPriorityFeePerGas().send();
                if (priorityResp.hasError()) {
                    throw new RuntimeException("get priority gas price failed: " + priorityResp.getError().getMessage());
                }
                return priorityResp.getMaxPriorityFeePerGas();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private CompletableFuture<BigInteger> getTipFeeAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var tipResp =
                        getWeb3j().ethGetBlockByNumber(DefaultBlockParameterName.LATEST, false)
                                .send();
                if (tipResp.hasError()) {
                    throw new RuntimeException("get the latest header failed: " + tipResp.getError().getMessage());
                }
                return tipResp.getBlock().getBaseFeePerGas();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
