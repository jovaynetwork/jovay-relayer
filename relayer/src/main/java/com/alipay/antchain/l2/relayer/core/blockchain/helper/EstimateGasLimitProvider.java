package com.alipay.antchain.l2.relayer.core.blockchain.helper;

import java.math.BigInteger;

import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.l2.relayer.commons.exceptions.TxSimulateException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.SneakyThrows;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthEstimateGas;

@AllArgsConstructor
@Builder
public class EstimateGasLimitProvider implements IGasLimitProvider {

    private Web3j web3j;

    private String fromAddress;

    private String toAddress;

    private String dataHex;

    private BigInteger extraGasLimit;

    @Override
    public BigInteger getGasLimit(String contractFunc) {
        return getGasLimitLogic(contractFunc);
    }

    @Override
    public BigInteger getGasLimit() {
        return getGasLimitLogic("");
    }

    @SneakyThrows
    private BigInteger getGasLimitLogic(String contractFunc) {
        EthEstimateGas ethEstimateGas;
        if (StrUtil.equals(contractFunc, "deploy")) {
            ethEstimateGas = web3j.ethEstimateGas(
                    Transaction.createEthCallTransaction(
                            fromAddress,
                            toAddress,
                            dataHex
                    )
            ).send();
        } else {
            ethEstimateGas = web3j.ethEstimateGas(
                    Transaction.createEthCallTransaction(
                            fromAddress,
                            toAddress,
                            dataHex
                    )
            ).send();
        }
        if (ethEstimateGas.hasError()) {
            throw new TxSimulateException(ethEstimateGas.getError());
        }

        return ethEstimateGas.getAmountUsed().add(extraGasLimit);
    }
}
