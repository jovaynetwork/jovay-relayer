package com.alipay.antchain.l2.relayer.core.blockchain;

import java.io.IOException;
import java.math.BigInteger;

import com.alipay.antchain.l2.relayer.commons.exceptions.L2RelayerErrorCodeEnum;
import com.alipay.antchain.l2.relayer.commons.exceptions.L2RelayerException;
import com.alipay.antchain.l2.relayer.commons.l2basic.L1MsgTransaction;
import com.alipay.antchain.l2.relayer.commons.models.TransactionInfo;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.BaseRawTransactionManager;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.GasLimitPolicyEnum;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.IGasPriceProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.utils.Numeric;

@Component("l2-client")
@Slf4j
public class L2Client extends AbstractWeb3jClient implements L2ClientInterface {

    @Autowired
    public L2Client(
            @Qualifier("l2Web3j") Web3j l2Web3j,
            @Qualifier("l2TransactionManager") BaseRawTransactionManager rawTransactionManager,
            @Qualifier("l2GasPriceProvider") IGasPriceProvider l2GasPriceProvider,
            @Value("${l2-relayer.l2-client.gas-limit-policy:ESTIMATE}") GasLimitPolicyEnum gasLimitPolicy,
            @Value("${l2-relayer.l2-client.extra-gas:0}") BigInteger extraGas,
            @Value("${l2-relayer.l2-client.static-gas-limit:9000000}") BigInteger staticGasLimit
    ) {
        super(l2Web3j, rawTransactionManager, rawTransactionManager, l2GasPriceProvider, gasLimitPolicy, extraGas, staticGasLimit);
    }

    @Override
    void processFailedEthCall(EthCall call, String toAddress, String funcNameOrDigest) throws L2RelayerException {
        throw new L2RelayerException(L2RelayerErrorCodeEnum.ROLLUP_SEND_TX_ERROR, "unexpected");
    }

    @Override
    public TransactionInfo sendL1MsgTx(L1MsgTransaction l1MsgTransaction) {
        try {
            var result = getLegacyPoolTxManager().sendL1MsgTx(l1MsgTransaction.getGasLimit(), l1MsgTransaction.getNonce(), l1MsgTransaction.getData());
            log.debug("sendL1MsgTx with tx: {}", result.getEthSendTransaction().getTransactionHash());
            dealWithTxResult(result);
            return TransactionInfo.builder()
                    .rawTx(Numeric.hexStringToByteArray(result.getRawTxHex()))
                    .txHash(result.getEthSendTransaction().getTransactionHash())
                    .nonce(result.getNonce())
                    .senderAccount(L1MsgTransaction.L1_MAILBOX_AS_SENDER.toString())
                    .sendTxTime(result.getTxSendTime())
                    .build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public BigInteger queryL2MailboxPendingNonce() {
        return queryTxCount(L1MsgTransaction.L1_MAILBOX_AS_SENDER.toString(), DefaultBlockParameterName.PENDING).subtract(BigInteger.ONE);
    }

    @Override
    public BigInteger queryL2MailboxLatestNonce() {
        return queryTxCount(L1MsgTransaction.L1_MAILBOX_AS_SENDER.toString(), DefaultBlockParameterName.LATEST).subtract(BigInteger.ONE);
    }
}
