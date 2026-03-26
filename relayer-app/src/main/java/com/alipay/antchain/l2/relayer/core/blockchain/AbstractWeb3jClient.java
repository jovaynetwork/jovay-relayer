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

import java.io.IOException;
import java.math.BigInteger;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.l2.relayer.commons.exceptions.L2RelayerException;
import com.alipay.antchain.l2.relayer.commons.l2basic.L1MsgTransaction;
import com.alipay.antchain.l2.relayer.commons.models.EthBlobs;
import com.alipay.antchain.l2.relayer.commons.models.TransactionInfo;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.*;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.EthCallTransaction;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.GasLimitPolicyEnum;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.SendTxResult;
import com.alipay.antchain.l2.relayer.core.layer2.economic.IRollupCostChecker;
import com.alipay.antchain.l2.relayer.core.layer2.economic.RollupEconomicStrategy;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.crypto.transaction.type.Transaction4844;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.DefaultBlockParameterNumber;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.protocol.exceptions.ClientConnectionException;
import org.web3j.utils.Numeric;

import static com.alipay.antchain.l2.relayer.commons.exceptions.L2RelayerErrorCodeEnum.ROLLUP_SEND_TX_ERROR;

@Slf4j
@Getter(AccessLevel.PACKAGE)
public abstract class AbstractWeb3jClient implements BasicBlockchainClient {

    private final Web3j web3j;

    @Getter(AccessLevel.PUBLIC)
    private final BaseRawTransactionManager blobPoolTxManager;

    @Getter(AccessLevel.PUBLIC)
    private final BaseRawTransactionManager legacyPoolTxManager;

    @Getter(AccessLevel.PUBLIC)
    private final IGasPriceProvider gasPriceProvider;

    private final GasLimitPolicyEnum gasLimitPolicy;

    private final BigInteger staticGasLimit;

    private final BigInteger extraGas;

    private final RollupEconomicStrategy rollupEconomicStrategy;

    @SneakyThrows
    public AbstractWeb3jClient(
            Web3j web3j,
            BaseRawTransactionManager l1BlobPoolTxTransactionManager,
            BaseRawTransactionManager l1LegacyPoolTxTransactionManager,
            IGasPriceProvider gasPriceProvider,
            GasLimitPolicyEnum gasLimitPolicy,
            BigInteger extraGas,
            BigInteger staticGasLimit,
            RollupEconomicStrategy rollupEconomicStrategy
    ) {
        // 2. Connect to the Ethereum network
        log.info("🔗 connecting blockchain node...");

        this.web3j = web3j;
        this.gasPriceProvider = gasPriceProvider;
        this.blobPoolTxManager = l1BlobPoolTxTransactionManager;
        this.legacyPoolTxManager = l1LegacyPoolTxTransactionManager;
        this.gasLimitPolicy = gasLimitPolicy;
        this.staticGasLimit = staticGasLimit;
        this.extraGas = extraGas;
        this.rollupEconomicStrategy = rollupEconomicStrategy;
    }

    @SneakyThrows
    @Retryable(retryFor = {ClientConnectionException.class, SocketException.class, SocketTimeoutException.class}, backoff = @Backoff(delay = 100), notRecoverable = Exception.class)
    @Override
    public EthBlock queryLatestBlockHeader(DefaultBlockParameterName blockParameterName) {
        EthBlock ethBlock = getWeb3j().ethGetBlockByNumber(blockParameterName, false).send();
        if (ObjectUtil.isNull(ethBlock) || ObjectUtil.isNull(ethBlock.getBlock())) {
            throw new RuntimeException("get null latest block from blockchain");
        }
        log.debug("get latest block, block height {}", ethBlock.getBlock().getNumber());
        return ethBlock;
    }

    @Override
    @SneakyThrows
    @Retryable(retryFor = {ClientConnectionException.class, SocketException.class, SocketTimeoutException.class}, backoff = @Backoff(delay = 100), notRecoverable = Exception.class)
    public BigInteger queryLatestBlockNumber(DefaultBlockParameterName blockParameterName) {
        EthBlock ethBlock = getWeb3j().ethGetBlockByNumber(blockParameterName, false).send();
        if (ObjectUtil.isNull(ethBlock) || ObjectUtil.isNull(ethBlock.getBlock())) {
            throw new RuntimeException("get null latest block from blockchain");
        }
        log.debug("get latest block number {}", ethBlock.getBlock().getNumber());
        return ethBlock.getBlock().getNumber();
    }

    @Override
    @SneakyThrows
    @Retryable(retryFor = {ClientConnectionException.class, SocketException.class, SocketTimeoutException.class}, backoff = @Backoff(delay = 100), notRecoverable = Exception.class)
    public EthBlock queryBlockByNumber(BigInteger height) {
        EthBlock ethBlock = getWeb3j().ethGetBlockByNumber(new DefaultBlockParameterNumber(height), false).send();
        if (ObjectUtil.isNull(ethBlock) || ObjectUtil.isNull(ethBlock.getBlock())) {
            throw new RuntimeException("get null block from blockchain by height: " + height);
        }
        log.debug("get block number {}", height);
        return ethBlock;
    }

    @SneakyThrows
    @Override
    @Retryable(retryFor = {ClientConnectionException.class, SocketException.class, SocketTimeoutException.class}, backoff = @Backoff(delay = 100), notRecoverable = Exception.class)
    public TransactionReceipt queryTxReceipt(String txhash) {
        EthGetTransactionReceipt result = getWeb3j().ethGetTransactionReceipt(txhash).send();
        if (result.hasError()) {
            throw new RuntimeException("failed to get tx receipt: " + result.getError().getMessage());
        }
        return result.getTransactionReceipt().orElse(null);
    }

    @Override
    @SneakyThrows
    @Retryable(retryFor = {ClientConnectionException.class, SocketException.class, SocketTimeoutException.class}, backoff = @Backoff(delay = 100), notRecoverable = Exception.class)
    public org.web3j.protocol.core.methods.response.Transaction queryTx(String txhash) {
        EthTransaction result = getWeb3j().ethGetTransactionByHash(txhash).send();
        if (result.hasError()) {
            throw new RuntimeException("failed to get tx: " + result.getError().getMessage());
        }
        return result.getTransaction().orElse(null);
    }

    @Override
    @SneakyThrows
    @Retryable(retryFor = {ClientConnectionException.class, SocketException.class, SocketTimeoutException.class}, backoff = @Backoff(delay = 100), notRecoverable = Exception.class)
    public BigInteger queryTxCount(String address, DefaultBlockParameterName name) {
        EthGetTransactionCount result = getWeb3j().ethGetTransactionCount(address, name).send();
        if (result.hasError()) {
            throw new RuntimeException("failed to get tx count: " + result.getError().getMessage());
        }
        return result.getTransactionCount();
    }

    @Override
    @Retryable(retryFor = {ClientConnectionException.class, SocketException.class, SocketTimeoutException.class}, backoff = @Backoff(delay = 100), notRecoverable = Exception.class)
    public EthSendTransaction sendRawTx(byte[] rawSignedTx) {
        try {
            return web3j.ethSendRawTransaction(Numeric.toHexString(rawSignedTx)).send();
        } catch (IOException e) {
            throw new RuntimeException("failed to send raw tx: ", e);
        }
    }

    @Override
    @Retryable(retryFor = {ClientConnectionException.class, SocketException.class, SocketTimeoutException.class}, backoff = @Backoff(delay = 100), notRecoverable = Exception.class)
    public BigInteger queryAccountBalance(String address, DefaultBlockParameter blockParameter) {
        try {
            return getWeb3j().ethGetBalance(address, blockParameter).send().getBalance();
        } catch (Exception e) {
            throw new RuntimeException(StrUtil.format("failed to get account balance for {}: ", address), e);
        }
    }

    @SneakyThrows
    public EthSendTransaction sendTransferValueTx(String from, String to, BigInteger nonce, BigInteger value) {
        BaseRawTransactionManager txManager;
        if (ObjectUtil.isNotNull(blobPoolTxManager) && StrUtil.equalsIgnoreCase(from, blobPoolTxManager.getAddress())) {
            log.info("going to send transfer value tx from blob pool tx manager");
            txManager = blobPoolTxManager;
        } else if (StrUtil.equalsIgnoreCase(from, legacyPoolTxManager.getAddress())) {
            log.info("going to send transfer value tx from legacy pool tx manager");
            txManager = legacyPoolTxManager;
        } else {
            throw new RuntimeException("unknown from address: " + from);
        }
        var result = txManager.sendTx(
                gasPriceProvider.getEip1559GasPrice(),
                new EstimateGasLimitProvider(web3j, txManager.getAddress(), to, "", extraGas).getGasLimit(),
                to,
                "",
                nonce,
                value,
                false
        );
        log.info("send transfer value tx success with txhash {} : (from: {}, to: {}, value: {}, nonce: {})",
                result.getEthSendTransaction().getTransactionHash(), from, to, value, nonce);
        return result.getEthSendTransaction();
    }

    protected IGasLimitProvider createEthCallGasLimitProvider(String toAddr, Function function) {
        return switch (gasLimitPolicy) {
            case ESTIMATE ->
                    new EstimateGasLimitProvider(web3j, legacyPoolTxManager.getAddress(), toAddr, FunctionEncoder.encode(function), extraGas);
            default -> new StaticGasLimitProvider(staticGasLimit);
        };
    }

    protected IGasLimitProvider createEthCallGasLimitProvider(String toAddr, String encodedFunc) {
        return switch (gasLimitPolicy) {
            case ESTIMATE ->
                    new EstimateGasLimitProvider(web3j, legacyPoolTxManager.getAddress(), toAddr, encodedFunc, extraGas);
            default -> new StaticGasLimitProvider(staticGasLimit);
        };
    }

    abstract void processFailedEthCall(EthCall call, String toAddress, String funcNameOrDigest) throws L2RelayerException;

    @WithSpan
    protected TransactionInfo sendTransaction(String to, Function function, IRollupCostChecker costChecker) throws L2RelayerException {
        var encodedFunc = FunctionEncoder.encode(function);
        try {
            var result = sendTx(to, encodedFunc, createEthCallGasLimitProvider(to, function), costChecker);
            log.info("🔗 send tx without eth-call to call {} success with txhash: {}", function.getName(), result.getEthSendTransaction().getTransactionHash());

            return TransactionInfo.builder()
                    .rawTx(Numeric.hexStringToByteArray(result.getRawTxHex()))
                    .txHash(result.getEthSendTransaction().getTransactionHash())
                    .nonce(result.getNonce())
                    .senderAccount(legacyPoolTxManager.getAddress())
                    .sendTxTime(result.getTxSendTime())
                    .build();
        } catch (IOException e) {
            throw new L2RelayerException(ROLLUP_SEND_TX_ERROR,
                    String.format("failed to call to %s", to), e
            );
        }
    }

    @WithSpan
    protected TransactionInfo sendTransaction(String to, String encodedFunc, IRollupCostChecker costChecker) throws L2RelayerException {
        try {
            var result = sendTx(to, encodedFunc, createEthCallGasLimitProvider(to, encodedFunc), costChecker);
            log.info("🔗 call raw func {} success hash: {}", StrUtil.sub(encodedFunc, 0, 10), result.getEthSendTransaction().getTransactionHash());

            return TransactionInfo.builder()
                    .rawTx(Numeric.hexStringToByteArray(result.getRawTxHex()))
                    .txHash(result.getEthSendTransaction().getTransactionHash())
                    .nonce(result.getNonce())
                    .senderAccount(legacyPoolTxManager.getAddress())
                    .sendTxTime(result.getTxSendTime())
                    .build();
        } catch (IOException e) {
            throw new L2RelayerException(ROLLUP_SEND_TX_ERROR,
                    String.format("failed to call to %s", to), e
            );
        }
    }

    @WithSpan
    protected TransactionInfo sendBlobTransaction(String to, String encodedFunc, EthBlobs blobs, IRollupCostChecker costChecker) throws L2RelayerException {
        try {
            var gasPrice = this.gasPriceProvider.getEip4844GasPrice();
            costChecker.check(gasPrice);
            var result = this.getBlobPoolTxManager().sendTx(
                    blobs.blobs(),
                    gasPrice,
                    createEthCallGasLimitProvider(to, encodedFunc).getGasLimit(encodedFunc),
                    to,
                    BigInteger.ZERO,
                    encodedFunc
            );
            dealWithTxResult(result);
            log.info("🔗 send eip4844 tx call {} success with txhash: {}", StrUtil.sub(encodedFunc, 0, 10), result.getEthSendTransaction().getTransactionHash());

            return TransactionInfo.builder()
                    .rawTx(Numeric.hexStringToByteArray(result.getRawTxHex()))
                    .txHash(result.getEthSendTransaction().getTransactionHash())
                    .nonce(result.getNonce())
                    .senderAccount(blobPoolTxManager.getAddress())
                    .sendTxTime(result.getTxSendTime())
                    .build();
        } catch (IOException e) {
            throw new L2RelayerException(ROLLUP_SEND_TX_ERROR,
                    String.format("failed to call to %s", to), e
            );
        }
    }

    @WithSpan
    protected TransactionInfo sendBlobTransaction(String to, Function function, EthBlobs blobs, IRollupCostChecker costChecker) throws L2RelayerException {
        return sendBlobTransaction(to, FunctionEncoder.encode(function), blobs, costChecker);
    }

    @SneakyThrows
    protected EthCall ethCall(String to, String encodedFunc) {
        return this.getWeb3j().ethCall(
                EthCallTransaction.createEthCallTransaction(
                        this.legacyPoolTxManager.getAddress(),
                        to,
                        encodedFunc
                ),
                DefaultBlockParameterName.LATEST
        ).send();
    }

    @SneakyThrows
    protected EthCall ethCall(Transaction4844 transaction4844) {
        return this.getWeb3j().ethCall(
                EthCallTransaction.createEthCallTransaction(this.blobPoolTxManager.getAddress(), transaction4844),
                DefaultBlockParameterName.LATEST
        ).send();
    }

    protected SendTxResult sendTx(String to, String encodedFunc, IGasLimitProvider ethCallGasLimitProvider, IRollupCostChecker costChecker) throws IOException {
        var gasPrice = this.getGasPriceProvider().getEip1559GasPrice();
        costChecker.check(gasPrice);
        var result = this.getLegacyPoolTxManager().sendTx(
                gasPrice,
                ethCallGasLimitProvider.getGasLimit(encodedFunc),
                to,
                encodedFunc,
                BigInteger.ZERO,
                false
        );
        dealWithTxResult(result);
        return result;
    }

    protected SendTxResult sendL1MsgTx(BigInteger gasLimit, String data) throws IOException {
        var result = this.getLegacyPoolTxManager().sendTx(
                null,
                gasLimit,
                L1MsgTransaction.L2_MAILBOX_AS_RECEIVER.toString(),
                data,
                BigInteger.ZERO,
                false
        );
        dealWithTxResult(result);
        return result;
    }

    protected SendTxResult resendL1MsgTx(BigInteger gasLimit, BigInteger nonce, String data) throws IOException {
        var result = this.getLegacyPoolTxManager().sendTx(
                null,
                gasLimit,
                L1MsgTransaction.L2_MAILBOX_AS_RECEIVER.toString(),
                data,
                nonce,
                BigInteger.ZERO,
                false
        );
        dealWithTxResult(result);
        return result;
    }

    void dealWithTxResult(SendTxResult result) {
        if (ObjectUtil.isNull(result.getEthSendTransaction())) {
            throw new RuntimeException("send tx with null result");
        }
        if (result.getEthSendTransaction().hasError()) {
            throw new RuntimeException(StrUtil.format("tx error: {} - {}",
                    result.getEthSendTransaction().getError().getCode(), result.getEthSendTransaction().getError().getMessage()));
        }
        if (StrUtil.isEmpty(result.getEthSendTransaction().getTransactionHash())) {
            throw new RuntimeException("tx hash is empty");
        }
    }
}
