package com.alipay.antchain.l2.relayer.core.blockchain;

import java.io.IOException;
import java.math.BigInteger;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.l2.relayer.commons.exceptions.L2RelayerException;
import com.alipay.antchain.l2.relayer.commons.models.EthBlobs;
import com.alipay.antchain.l2.relayer.commons.models.TransactionInfo;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.*;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.EthCallTransaction;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.SendTxResult;
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

    @SneakyThrows
    public AbstractWeb3jClient(
            Web3j web3j,
            BaseRawTransactionManager l1BlobPoolTxTransactionManager,
            BaseRawTransactionManager l1LegacyPoolTxTransactionManager,
            IGasPriceProvider gasPriceProvider,
            GasLimitPolicyEnum gasLimitPolicy,
            BigInteger extraGas,
            BigInteger staticGasLimit
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
    }

    @SneakyThrows
    @Retryable(retryFor = {ClientConnectionException.class, SocketException.class, SocketTimeoutException.class}, backoff = @Backoff(delay = 100))
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
    @Retryable(retryFor = {ClientConnectionException.class, SocketException.class, SocketTimeoutException.class}, backoff = @Backoff(delay = 100))
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
    @Retryable(retryFor = {ClientConnectionException.class, SocketException.class, SocketTimeoutException.class}, backoff = @Backoff(delay = 100))
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
    @Retryable(retryFor = {ClientConnectionException.class, SocketException.class, SocketTimeoutException.class}, backoff = @Backoff(delay = 100))
    public TransactionReceipt queryTxReceipt(String txhash) {
        EthGetTransactionReceipt result = getWeb3j().ethGetTransactionReceipt(txhash).send();
        if (result.hasError()) {
            throw new RuntimeException("failed to get tx receipt: " + result.getError().getMessage());
        }
        return result.getTransactionReceipt().orElse(null);
    }

    @Override
    @SneakyThrows
    @Retryable(retryFor = {ClientConnectionException.class, SocketException.class, SocketTimeoutException.class}, backoff = @Backoff(delay = 100))
    public org.web3j.protocol.core.methods.response.Transaction queryTx(String txhash) {
        EthTransaction result = getWeb3j().ethGetTransactionByHash(txhash).send();
        if (result.hasError()) {
            throw new RuntimeException("failed to get tx: " + result.getError().getMessage());
        }
        return result.getTransaction().orElse(null);
    }

    @Override
    @SneakyThrows
    @Retryable(retryFor = {ClientConnectionException.class, SocketException.class, SocketTimeoutException.class}, backoff = @Backoff(delay = 100))
    public BigInteger queryTxCount(String address, DefaultBlockParameterName name) {
        EthGetTransactionCount result = getWeb3j().ethGetTransactionCount(address, name).send();
        if (result.hasError()) {
            throw new RuntimeException("failed to get tx count: " + result.getError().getMessage());
        }
        return result.getTransactionCount();
    }

    @Override
    @Retryable(retryFor = {ClientConnectionException.class, SocketException.class, SocketTimeoutException.class}, backoff = @Backoff(delay = 100))
    public EthSendTransaction sendRawTx(byte[] rawSignedTx) {
        try {
            return web3j.ethSendRawTransaction(Numeric.toHexString(rawSignedTx)).send();
        } catch (IOException e) {
            throw new RuntimeException("failed to send raw tx: ", e);
        }
    }

    @Override
    @Retryable(retryFor = {ClientConnectionException.class, SocketException.class, SocketTimeoutException.class}, backoff = @Backoff(delay = 100))
    public BigInteger queryAccountBalance(String address, DefaultBlockParameter blockParameter) {
        try {
            return getWeb3j().ethGetBalance(address, blockParameter).send().getBalance();
        } catch (Exception e) {
            throw new RuntimeException(StrUtil.format("failed to get account balance for {}: ", address), e);
        }
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
    protected TransactionInfo sendTransaction(String to, Function function) throws L2RelayerException {
        var encodedFunc = FunctionEncoder.encode(function);
        try {
            var result = sendTx(to, encodedFunc, createEthCallGasLimitProvider(to, function));
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
    protected TransactionInfo sendTransaction(String to, String encodedFunc) throws L2RelayerException {
        try {
            var result = sendTx(to, encodedFunc, createEthCallGasLimitProvider(to, encodedFunc));
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
    protected TransactionInfo sendBlobTransaction(String to, String encodedFunc, EthBlobs blobs) throws L2RelayerException {
        try {
            var result = this.getBlobPoolTxManager().sendTx(
                    blobs.blobs(),
                    this.gasPriceProvider.getEip4844GasPrice(),
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
    protected TransactionInfo sendBlobTransaction(String to, Function function, EthBlobs blobs) throws L2RelayerException {
        return sendBlobTransaction(to, FunctionEncoder.encode(function), blobs);
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
    protected EthCall ethCall(String to, Transaction4844 transaction4844) {
        return this.getWeb3j().ethCall(
                EthCallTransaction.createEthCallTransaction(this.blobPoolTxManager.getAddress(), transaction4844),
                DefaultBlockParameterName.LATEST
        ).send();
    }

    protected SendTxResult sendTx(String to, String encodedFunc, IGasLimitProvider ethCallGasLimitProvider) throws IOException {
        var result = this.getLegacyPoolTxManager().sendTx(
                this.getGasPriceProvider().getEip1559GasPrice(),
                ethCallGasLimitProvider.getGasLimit(encodedFunc),
                to,
                encodedFunc,
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
