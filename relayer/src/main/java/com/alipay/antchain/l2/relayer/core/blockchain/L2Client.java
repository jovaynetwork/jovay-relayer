package com.alipay.antchain.l2.relayer.core.blockchain;

import java.io.IOException;
import java.math.BigInteger;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.l2.relayer.commons.exceptions.L2RelayerErrorCodeEnum;
import com.alipay.antchain.l2.relayer.commons.exceptions.L2RelayerException;
import com.alipay.antchain.l2.relayer.commons.exceptions.TxNotFoundButRetryException;
import com.alipay.antchain.l2.relayer.commons.exceptions.TxSimulateException;
import com.alipay.antchain.l2.relayer.commons.l2basic.L1MsgTransaction;
import com.alipay.antchain.l2.relayer.commons.models.TransactionInfo;
import com.alipay.antchain.l2.relayer.core.blockchain.abi.L1GasOracle;
import com.alipay.antchain.l2.relayer.core.blockchain.abi.L2CoinBase;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.BaseRawTransactionManager;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.GasLimitPolicyEnum;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.IGasPriceProvider;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.support.RetrySynchronizationManager;
import org.springframework.stereotype.Component;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthGetCode;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.exceptions.ClientConnectionException;
import org.web3j.utils.Numeric;

@Component("l2-client")
@Slf4j
public class L2Client extends AbstractWeb3jClient implements L2ClientInterface {

    private final String gasOracleContractAddress;

    private final String coinbaseContractAddress;

    @Autowired
    public L2Client(
            @Qualifier("l2Web3j") Web3j l2Web3j,
            @Qualifier("l2TransactionManager") BaseRawTransactionManager rawTransactionManager,
            @Qualifier("l2GasPriceProvider") IGasPriceProvider l2GasPriceProvider,
            @Value("${l2-relayer.l2-client.gas-oracle-contract}") String gasOracleContractAddress,
            @Value("${l2-relayer.l2-client.coinbase-contract}") String coinbaseContractAddress,
            @Value("${l2-relayer.l2-client.gas-limit-policy:ESTIMATE}") GasLimitPolicyEnum gasLimitPolicy,
            @Value("${l2-relayer.l2-client.extra-gas:0}") BigInteger extraGas,
            @Value("${l2-relayer.l2-client.static-gas-limit:9000000}") BigInteger staticGasLimit
    ) {
        super(
                l2Web3j,
                rawTransactionManager,
                rawTransactionManager,
                l2GasPriceProvider,
                gasLimitPolicy,
                extraGas,
                staticGasLimit
        );
        this.gasOracleContractAddress = gasOracleContractAddress;
        this.coinbaseContractAddress = coinbaseContractAddress;
        checkIfL2GasOracleContractValid();
        checkIfL2FeeVaultContractValid();
    }

    @Override
    void processFailedEthCall(EthCall call, String toAddress, String funcNameOrDigest) throws L2RelayerException {
        throw new L2RelayerException(L2RelayerErrorCodeEnum.ORACLE_SEND_TX_ERROR, "unexpected");
    }

    @Override
    @SneakyThrows
    public TransactionInfo sendL1MsgTx(L1MsgTransaction l1MsgTransaction) {
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
    }

    @Override
    @WithSpan
    @Retryable(retryFor = {ClientConnectionException.class, SocketException.class, SocketTimeoutException.class}, backoff = @Backoff(delay = 100))
    public TransactionInfo resendGasFeedTx(String encodedFunc) {
        var call = ethCall(gasOracleContractAddress, encodedFunc);
        if (call.isReverted()) {
            processFailedEthCall(call, gasOracleContractAddress, StrUtil.sub(Numeric.cleanHexPrefix(encodedFunc), 0, 8));
            return null;
        }
        return sendTransaction(gasOracleContractAddress, encodedFunc);
    }

    @Override
    public BigInteger queryL2MailboxPendingNonce() {
        return queryTxCount(L1MsgTransaction.L1_MAILBOX_AS_SENDER.toString(), DefaultBlockParameterName.PENDING).subtract(BigInteger.ONE);
    }

    @Override
    public BigInteger queryL2MailboxLatestNonce() {
        return queryTxCount(L1MsgTransaction.L1_MAILBOX_AS_SENDER.toString(), DefaultBlockParameterName.LATEST).subtract(BigInteger.ONE);
    }

    @Override
    @Retryable(retryFor = {ClientConnectionException.class, SocketException.class, SocketTimeoutException.class}, backoff = @Backoff(delay = 100))
    public BigInteger queryL2GasOracleLastBatchDaFee() {
        BigInteger lastBatchDaFee;
        try {
            lastBatchDaFee = this.getL1GasOracleForEthCall().lastBatchDaFee().send();
        } catch (Exception e) {
            throw new RuntimeException("failed to get last batch da fee", e);
        }
        log.debug("🔗 last batch da fee : {}", lastBatchDaFee);
        return lastBatchDaFee;
    }

    @Override
    public BigInteger queryL2GasOracleLastBatchExecFee() {
        BigInteger lastBatchExecFee;
        try {
            lastBatchExecFee = this.getL1GasOracleForEthCall().lastBatchExecFee().send();
        } catch (Exception e) {
            throw new RuntimeException("failed to get last batch exec fee", e);
        }
        log.debug("🔗 last batch exec fee : {}", lastBatchExecFee);
        return lastBatchExecFee;
    }

    @Override
    public BigInteger queryL2GasOracleLastBatchByteLength() {
        BigInteger lastBatchByteLength;
        try {
            lastBatchByteLength = this.getL1GasOracleForEthCall().lastBatchByteLength().send();
        } catch (Exception e) {
            throw new RuntimeException("failed to get last batch byte length", e);
        }
        log.debug("🔗 last batch byte length : {}", lastBatchByteLength);
        return lastBatchByteLength;
    }

    @Override
    @SneakyThrows
    @WithSpan
    @Retryable(retryFor = {TxSimulateException.class, ClientConnectionException.class, SocketException.class, SocketTimeoutException.class}, backoff = @Backoff(delay = 300))
    public TransactionInfo updateBatchRollupFee(BigInteger lastBatchDaFee, BigInteger lastBatchExecFee, BigInteger lastBatchByteLength) {
        log.info("start sending tx to update rollup fee#(lastBatchDaFee: {}, lastBatchExecFee: {}, lastBatchByteLength: {}) with retry {}", lastBatchDaFee, lastBatchExecFee, lastBatchByteLength,
                ObjectUtil.isNull(RetrySynchronizationManager.getContext()) ? 0 : RetrySynchronizationManager.getContext().getRetryCount());
        // 1. check
        // 2. create func
        var function = new Function(
                L1GasOracle.FUNC_SETNEWBATCHBLOBFEEANDTXFEE,
                Arrays.asList(
                        new Uint256(lastBatchDaFee),
                        new Uint256(lastBatchExecFee),
                        new Uint256(lastBatchByteLength)
                ),
                Collections.emptyList()
        );
        var result = sendTx(gasOracleContractAddress, FunctionEncoder.encode(function), createEthCallGasLimitProvider(gasOracleContractAddress, function));
        log.info("setNewBatchBlobFeeAndTxFee with tx: {}", result.getEthSendTransaction().getTransactionHash());
        dealWithTxResult(result);
        return TransactionInfo.builder()
                .rawTx(Numeric.hexStringToByteArray(result.getRawTxHex()))
                .txHash(result.getEthSendTransaction().getTransactionHash())
                .nonce(result.getNonce())
                .senderAccount(this.getLegacyPoolTxManager().getAddress())
                .sendTxTime(result.getTxSendTime())
                .build();
    }

    @Override
    @WithSpan
    @Retryable(retryFor = {TxSimulateException.class, ClientConnectionException.class, SocketException.class, SocketTimeoutException.class}, backoff = @Backoff(delay = 300))
    public TransactionInfo updateBaseFeeScala(BigInteger baseFeeScala, BigInteger blobBaseFeeScala) throws IOException { // WARN: scalar*100避免合约上decimal丢失
        log.info("start sending tx to update base fee scala#(baseFeeScala: {}, blobBaseFeeScala: {}) with retry {}", baseFeeScala, blobBaseFeeScala,
                ObjectUtil.isNull(RetrySynchronizationManager.getContext()) ? 0 : RetrySynchronizationManager.getContext().getRetryCount());
        var function = new Function(
                L1GasOracle.FUNC_SETBLOBBASEFEESCALAANDTXFEESCALA,
                Arrays.asList(
                        new Uint256(baseFeeScala),
                        new Uint256(blobBaseFeeScala)
                ),
                Collections.emptyList()
        );
        try {
            var result = sendTx(gasOracleContractAddress, FunctionEncoder.encode(function), createEthCallGasLimitProvider(gasOracleContractAddress, function));
            log.debug("setBlobBaseFeeScalaAndTxFeeScala with tx: {}", result.getEthSendTransaction().getTransactionHash());
            dealWithTxResult(result);
            return TransactionInfo.builder()
                    .rawTx(Numeric.hexStringToByteArray(result.getRawTxHex()))
                    .txHash(result.getEthSendTransaction().getTransactionHash())
                    .nonce(result.getNonce())
                    .senderAccount(this.getLegacyPoolTxManager().getAddress())
                    .sendTxTime(result.getTxSendTime())
                    .build();
        } catch (IOException e) {
            throw new RuntimeException(StrUtil.format("send tx to oracle contract: {}, update baseFee scala failed.", gasOracleContractAddress), e);
        }
    }

    @Override
    @SneakyThrows
    @WithSpan
    @Retryable(retryFor = {TxSimulateException.class, ClientConnectionException.class, SocketException.class, SocketTimeoutException.class}, backoff = @Backoff(delay = 300))
    public TransactionInfo updateFixedProfit(BigInteger fixedProfit) {
        log.info("start sending tx to update fixed profit#(fixedProfit: {}) with retry {}", fixedProfit,
                ObjectUtil.isNull(RetrySynchronizationManager.getContext()) ? 0 : RetrySynchronizationManager.getContext().getRetryCount());
        var function = new Function(
                L1GasOracle.FUNC_SETL1PROFIT,
                List.of(
                        new Uint256(fixedProfit)
                ),
                Collections.emptyList()
        );
        var result = sendTx(gasOracleContractAddress, FunctionEncoder.encode(function), createEthCallGasLimitProvider(gasOracleContractAddress, function));
        log.debug("setL1Profit with tx: {}", result.getEthSendTransaction().getTransactionHash());
        dealWithTxResult(result);
        return TransactionInfo.builder()
                .rawTx(Numeric.hexStringToByteArray(result.getRawTxHex()))
                .txHash(result.getEthSendTransaction().getTransactionHash())
                .nonce(result.getNonce())
                .senderAccount(this.getLegacyPoolTxManager().getAddress())
                .sendTxTime(result.getTxSendTime())
                .build();
    }

    @Override
    @SneakyThrows
    @WithSpan
    @Retryable(retryFor = {TxSimulateException.class, ClientConnectionException.class, SocketException.class, SocketTimeoutException.class}, backoff = @Backoff(delay = 300))
    public TransactionInfo updateTotalScala(BigInteger totalScala) {
        log.info("start sending tx to update total scala#(totalScala: {}) with retry {}", totalScala,
                ObjectUtil.isNull(RetrySynchronizationManager.getContext()) ? 0 : RetrySynchronizationManager.getContext().getRetryCount());
        var function = new Function(
                L1GasOracle.FUNC_SETTOTALSCALA,
                List.of(
                        new Uint256(totalScala)
                ),
                Collections.emptyList()
        );
        var result = sendTx(gasOracleContractAddress, FunctionEncoder.encode(function), createEthCallGasLimitProvider(gasOracleContractAddress, function));
        log.debug("setTotalScala with tx: {}", result.getEthSendTransaction().getTransactionHash());
        dealWithTxResult(result);
        return TransactionInfo.builder()
                .rawTx(Numeric.hexStringToByteArray(result.getRawTxHex()))
                .txHash(result.getEthSendTransaction().getTransactionHash())
                .nonce(result.getNonce())
                .senderAccount(this.getLegacyPoolTxManager().getAddress())
                .sendTxTime(result.getTxSendTime())
                .build();
    }

    @Override
    @SneakyThrows
    @WithSpan
    @Retryable(retryFor = {TxSimulateException.class, ClientConnectionException.class, SocketException.class, SocketTimeoutException.class}, backoff = @Backoff(delay = 300))
    public TransactionInfo withdrawVault(String account, BigInteger amount) {
        log.info("start sending tx to withdraw specific ETH: {} from vault contract to account: {} with retry {}", amount, account,
                ObjectUtil.isNull(RetrySynchronizationManager.getContext()) ? 0 : RetrySynchronizationManager.getContext().getRetryCount());
        var function = new Function(
                L2CoinBase.FUNC_WITHDRAW,
                ListUtil.toList(
                        new Utf8String(account),
                        new Uint256(amount)
                ),
                Collections.emptyList()
        );
        var result = sendTx(coinbaseContractAddress, FunctionEncoder.encode(function), createEthCallGasLimitProvider(coinbaseContractAddress, function));
        log.debug("send withdraw vault with tx: {}", result.getEthSendTransaction().getTransactionHash());
        dealWithTxResult(result);
        return TransactionInfo.builder()
                .rawTx(Numeric.hexStringToByteArray(result.getRawTxHex()))
                .txHash(result.getEthSendTransaction().getTransactionHash())
                .nonce(result.getNonce())
                .senderAccount(this.getLegacyPoolTxManager().getAddress())
                .sendTxTime(result.getTxSendTime())
                .build();
    }

    @Override
    @Retryable(
            retryFor = {TxNotFoundButRetryException.class},
            maxAttempts = 5,
            recover = "recoverForTxNotFound",
            backoff = @Backoff(delay = 300)
    )
    public Transaction queryTxWithRetry(String from, String txHash, BigInteger nonce) {
        // after Jovay rpc eth_getTransactionByHash can search the tx from pool,
        // gonna to delete this func ✊
        log.debug("query L2 tx with retry, from: {}, txHash: {}, nonce: {}", from, txHash, nonce);
        var nextPackagedNonce = queryTxCount(from, DefaultBlockParameterName.LATEST);
        var tx = queryTx(txHash);
        if (ObjectUtil.isNull(tx)) {
            if (nextPackagedNonce.compareTo(nonce) > 0) {
                // bigger nonce on chain means that this null tx is lost
                return null;
            }
            throw new TxNotFoundButRetryException();
        }
        return tx;
    }

    @SneakyThrows
    private void checkIfL2GasOracleContractValid() {
        EthGetCode result = getWeb3j().ethGetCode(gasOracleContractAddress, DefaultBlockParameterName.FINALIZED).send();
        if (result.hasError()) {
            throw new RuntimeException("failed to get l2-gas-oracle contract code: " + result.getError().getMessage());
        }
        if (StrUtil.isEmpty(Numeric.cleanHexPrefix(result.getCode()))) {
            throw new RuntimeException("l2-gas-oracle contract not exist");
        }
    }

    @SneakyThrows
    private void checkIfL2FeeVaultContractValid() {
        EthGetCode result = getWeb3j().ethGetCode(coinbaseContractAddress, DefaultBlockParameterName.FINALIZED).send();
        if (result.hasError()) {
            throw new RuntimeException("failed to get l2-gas-fee-vault contract code: " + result.getError().getMessage());
        }
        if (StrUtil.isEmpty(Numeric.cleanHexPrefix(result.getCode()))) {
            throw new RuntimeException("l2-gas-fee-vault contract not exist");
        }
    }

    @Recover
    private Transaction recoverForTxNotFound() {
        log.warn("L2 tx still not found after retry multi times");
        return null;
    }

    private L1GasOracle getL1GasOracleForEthCall() {
        return L1GasOracle.load(
                gasOracleContractAddress,
                this.getWeb3j(),
                this.getLegacyPoolTxManager(),
                null
        );
    }
}
