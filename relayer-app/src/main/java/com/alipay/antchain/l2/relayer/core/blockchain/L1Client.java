package com.alipay.antchain.l2.relayer.core.blockchain;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.l2.relayer.commons.enums.ChainTypeEnum;
import com.alipay.antchain.l2.relayer.commons.enums.TransactionTypeEnum;
import com.alipay.antchain.l2.relayer.commons.exceptions.*;
import com.alipay.antchain.l2.relayer.commons.l2basic.BatchHeader;
import com.alipay.antchain.l2.relayer.commons.l2basic.L1MsgTransaction;
import com.alipay.antchain.l2.relayer.commons.models.*;
import com.alipay.antchain.l2.relayer.commons.utils.EthTxDecoder;
import com.alipay.antchain.l2.relayer.core.blockchain.abi.IMailBoxBase;
import com.alipay.antchain.l2.relayer.core.blockchain.abi.Rollup;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.BaseRawTransactionManager;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.IGasPriceProvider;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.*;
import com.alipay.antchain.l2.relayer.core.layer2.economic.RollupEconomicStrategy;
import com.alipay.antchain.l2.relayer.dal.repository.IRollupRepository;
import com.alipay.antchain.l2.relayer.metrics.selfreport.ISelfReportMetric;
import com.alipay.antchain.l2.relayer.metrics.selfreport.RollupMetricRecord;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.reactivex.Flowable;
import jakarta.annotation.Resource;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.support.RetrySynchronizationManager;
import org.springframework.stereotype.Component;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.EventValues;
import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.transaction.type.Transaction1559;
import org.web3j.crypto.transaction.type.Transaction4844;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.DefaultBlockParameterNumber;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthGetCode;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.exceptions.ClientConnectionException;
import org.web3j.utils.Numeric;

import static com.alipay.antchain.l2.relayer.core.blockchain.abi.Rollup.FUNC_VERIFYBATCH;
import static org.web3j.tx.Contract.staticExtractEventParameters;

@Component("l1Client")
@Slf4j
@EnableRetry
public class L1Client extends AbstractWeb3jClient implements L1ClientInterface {

    private final String rollupContractAddress;

    private final String mailboxContractAddress;

    private final ExecutorService fetchingL1MsgThreadsPool;

    private final double txSpeedupPriceBump;

    private final double txSpeedUpBlobFeeBump;

    private final BigInteger txSpeedUpBlobFeeLimit;

    private final BigInteger txSpeedUpPriorityFeeLimit;

    private final long forceTxSpeedUpTimeLimit;

    @Resource
    private ISelfReportMetric selfReportMetric;

    @Resource
    private IRollupRepository rollupRepository;

    @Resource
    private RedissonClient redisson;

    @Resource
    private IContractErrorParser contractErrorParser;

    @Autowired
    public L1Client(
            @Qualifier("l1Web3j") Web3j l1Web3j,
            @Qualifier("l1BlobPoolTxTransactionManager") BaseRawTransactionManager l1BlobPoolTxTransactionManager,
            @Qualifier("l1LegacyPoolTxTransactionManager") BaseRawTransactionManager l1LegacyPoolTxTransactionManager,
            @Qualifier("l1GasPriceProvider") IGasPriceProvider l1GasPriceProvider,
            @Value("${l2-relayer.l1-client.gas-limit-policy:STATIC}") GasLimitPolicyEnum gasLimitPolicy,
            @Value("${l2-relayer.l1-client.extra-gas:0}") BigInteger extraGas,
            @Value("${l2-relayer.l1-client.static-gas-limit:7200000}") BigInteger staticGasLimit,
            @Value("${l2-relayer.l1-client.rollup-contract}") String rollupContractAddress,
            @Value("${l2-relayer.l1-client.mailbox-contract}") String mailboxContractAddress,
            @Value("${l2-relayer.l1-client.tx-speedup-price-bump:0.1}") double txSpeedupPriceBump,
            @Value("${l2-relayer.l1-client.tx-speedup-blob-price-bump:1}") double txSpeedUpBlobFeeBump,
            @Value("${l2-relayer.l1-client.force-tx-speedup-time-limit:900000}") long forceTxSpeedUpTimeLimit,
            @Value("${l2-relayer.l1-client.tx-speedup-priority-fee-limit:100000000000}") BigInteger txSpeedUpPriorityFeeLimit,
            @Value("${l2-relayer.l1-client.tx-speedup-blob-fee-limit:1000000000000}") BigInteger txSpeedUpBlobFeeLimit,
            @Qualifier("fetchingL1MsgThreadsPool") ExecutorService fetchingL1MsgThreadsPool,
            RollupEconomicStrategy rollupEconomicStrategy
    ) {
        super(
                l1Web3j,
                l1BlobPoolTxTransactionManager,
                l1LegacyPoolTxTransactionManager,
                l1GasPriceProvider,
                gasLimitPolicy,
                extraGas,
                staticGasLimit,
                rollupEconomicStrategy
        );
        if (StrUtil.equalsIgnoreCase(l1BlobPoolTxTransactionManager.getAddress(), l1LegacyPoolTxTransactionManager.getAddress())) {
            throw new RuntimeException("same accounts for blob and legacy pool is not allowed on L1");
        }
        this.rollupContractAddress = rollupContractAddress;
        this.mailboxContractAddress = mailboxContractAddress;
        this.fetchingL1MsgThreadsPool = fetchingL1MsgThreadsPool;
        this.txSpeedupPriceBump = txSpeedupPriceBump;
        this.txSpeedUpBlobFeeBump = txSpeedUpBlobFeeBump;
        this.txSpeedUpBlobFeeLimit = txSpeedUpBlobFeeLimit;
        this.forceTxSpeedUpTimeLimit = forceTxSpeedUpTimeLimit;
        this.txSpeedUpPriorityFeeLimit = txSpeedUpPriorityFeeLimit;
        checkIfRollupContractValid();
        checkIfMailboxContractValid();
    }

    @Override
    @WithSpan
    @Retryable(retryFor = {TxSimulateException.class, ClientConnectionException.class, SocketException.class, SocketTimeoutException.class}, backoff = @Backoff(delay = 300))
    public TransactionInfo commitBatch(BatchWrapper batchWrapper, BatchHeader parentBatchHeader) throws L2RelayerException {
        log.info("start sending tx to commit batch#{} with retry {}", batchWrapper.getBatchIndex(),
                ObjectUtil.isNull(RetrySynchronizationManager.getContext()) ? 0 : RetrySynchronizationManager.getContext().getRetryCount());
        selfReportMetric.recordStart(RollupMetricRecord.createCommitBatchRecord(batchWrapper.getBatch().getBatchIndex()));
        // 1. check
        // 2. create func
        var function = new Function(
                Rollup.FUNC_COMMITBATCH, // function name
                Arrays.asList(
                        new Uint8(batchWrapper.getBatch().getBatchHeader().getVersion().getValue()),
                        new Uint256(batchWrapper.getBatchIndex()),
                        new Uint256(batchWrapper.getTotalL1MessagePopped())
                ), // inputs
                Collections.emptyList()// outputs
        );

        return sendBlobTransaction(
                this.rollupContractAddress,
                function,
                batchWrapper.getBatch().getEthBlobs(),
                getRollupEconomicStrategy().createBatchCommitCostChecker(batchWrapper)
        );
    }

    @Override
    @WithSpan
    @Retryable(retryFor = {TxSimulateException.class, ClientConnectionException.class, SocketException.class, SocketTimeoutException.class}, backoff = @Backoff(delay = 300))
    public TransactionInfo verifyBatch(BatchWrapper batchWrapper, BatchProveRequestDO proveReq) throws L2RelayerException {
        log.info("start sending tx to verify batch#{} with retry {}", batchWrapper.getBatchIndex(),
                ObjectUtil.isNull(RetrySynchronizationManager.getContext()) ? 0 : RetrySynchronizationManager.getContext().getRetryCount());
        selfReportMetric.recordStart(RollupMetricRecord.createCommitProofRecord(proveReq.getProveType(), batchWrapper.getBatch().getBatchIndex()));
        // 1. check
        // 2. create func
        Function function = new Function(
                FUNC_VERIFYBATCH, // function name
                Arrays.asList(new Uint8(proveReq.getProveType().getRollupProofNum()),
                        new DynamicBytes(batchWrapper.getBatchHeader().serialize()),
                        new Bytes32(batchWrapper.getPostStateRoot()),
                        new Bytes32(batchWrapper.getL2MsgRoot()),
                        new DynamicBytes(proveReq.getProof())), // inputs
                Collections.emptyList()// outputs
        );

        return sendTransaction(this.rollupContractAddress, function, getRollupEconomicStrategy().createProofCommitCostChecker(proveReq));
    }

    @Override
    @Retryable(retryFor = {ClientConnectionException.class, SocketException.class, SocketTimeoutException.class}, backoff = @Backoff(delay = 100))
    public BigInteger lastTeeVerifiedBatch() {
        BigInteger lastTeeVerifiedBatchIndex;
        try {
            lastTeeVerifiedBatchIndex = this.getRollupForEthCall().lastTeeVerifiedBatch().send();
        } catch (Exception e) {
            throw new RuntimeException("failed to get last tee verified batch index", e);
        }
        log.debug("🔗 last tee verified batch index : {}", lastTeeVerifiedBatchIndex);
        return lastTeeVerifiedBatchIndex;
    }

    @Override
    @Retryable(retryFor = {ClientConnectionException.class, SocketException.class, SocketTimeoutException.class}, backoff = @Backoff(delay = 100))
    public BigInteger lastZkVerifiedBatch() {
        BigInteger lastZkVerifiedBatchIndex;
        try {
            lastZkVerifiedBatchIndex = this.getRollupForEthCall().lastZkVerifiedBatch().send();
        } catch (Exception e) {
            throw new RuntimeException("failed to get last zk verified batch index", e);
        }

        log.debug("🔗 last zk verified batch index: {}", lastZkVerifiedBatchIndex);
        return lastZkVerifiedBatchIndex;
    }

    @Override
    @Retryable(retryFor = {ClientConnectionException.class, SocketException.class, SocketTimeoutException.class}, backoff = @Backoff(delay = 100))
    public BigInteger lastCommittedBatch() {
        return lastCommittedBatch(DefaultBlockParameterName.LATEST);
    }

    @Override
    @Retryable(retryFor = {ClientConnectionException.class, SocketException.class, SocketTimeoutException.class}, backoff = @Backoff(delay = 100))
    public BigInteger lastCommittedBatch(DefaultBlockParameter blockParam) {
        try {
            var rollup = this.getRollupForEthCall();
            rollup.setDefaultBlockParameter(blockParam);
            var lastCommittedBatchIndex = rollup.lastCommittedBatch().send();
            log.debug("🔗 last commit batch index: {}", lastCommittedBatchIndex);
            return lastCommittedBatchIndex;
        } catch (Exception e) {
            throw new RuntimeException("failed to get last committed batch index", e);
        }
    }

    @Override
    @Retryable(retryFor = {ClientConnectionException.class, SocketException.class, SocketTimeoutException.class}, backoff = @Backoff(delay = 100))
    public BigInteger maxTxsInChunk() {
        BigInteger maxTxsInChunk;
        try {
            maxTxsInChunk = this.getRollupForEthCall().maxTxsInChunk().send();
        } catch (Exception e) {
            throw new RuntimeException("failed to get max tx size in chunk", e);
        }
        log.info("🔗 maxTxsInChunk {}", maxTxsInChunk);
        return maxTxsInChunk;
    }

    @Override
    @Retryable(retryFor = {ClientConnectionException.class, SocketException.class, SocketTimeoutException.class}, backoff = @Backoff(delay = 100))
    public BigInteger maxBlockInChunk() {
        BigInteger maxBlockInChunk;
        try {
            maxBlockInChunk = this.getRollupForEthCall().maxBlockInChunk().send();
        } catch (Exception e) {
            throw new RuntimeException("failed to get max block size in chunk", e);
        }
        log.info("🔗 maxBlockInChunk {}", maxBlockInChunk);
        return maxBlockInChunk;
    }

    @Override
    @Retryable(retryFor = {ClientConnectionException.class, SocketException.class, SocketTimeoutException.class}, backoff = @Backoff(delay = 100))
    public BigInteger maxCallDataInChunk() {
        BigInteger maxCallData;
        try {
            maxCallData = this.getRollupForEthCall().maxCallDataInChunk().send();
        } catch (Exception e) {
            throw new RuntimeException("failed to get max tx data size in chunk", e);
        }
        log.info("🔗 maxCallDataInChunk {}", maxCallData);
        return maxCallData;
    }

    @Override
    @Retryable(retryFor = {ClientConnectionException.class, SocketException.class, SocketTimeoutException.class}, backoff = @Backoff(delay = 100))
    public BigInteger maxZkCircleInChunk() {
        BigInteger maxZkCircleInChunk;
        try {
            maxZkCircleInChunk = this.getRollupForEthCall().maxZkCircleInChunk().send();
        } catch (Exception e) {
            throw new RuntimeException("failed to get max zk circle in chunk", e);
        }
        log.info("🔗 maxZkCircleInChunk {}", maxZkCircleInChunk);
        return maxZkCircleInChunk;
    }

    @Override
    @Retryable(retryFor = {ClientConnectionException.class, SocketException.class, SocketTimeoutException.class}, backoff = @Backoff(delay = 100))
    public long l1BlobNumLimit() {
        BigInteger l1BlobNumberLimit;
        try {
            l1BlobNumberLimit = this.getRollupForEthCall().l1BlobNumberLimit().send();
        } catch (Exception e) {
            throw new RuntimeException("failed to get l1BlobNumberLimit", e);
        }
        log.info("🔗 l1BlobNumLimit {}", l1BlobNumberLimit);
        return l1BlobNumberLimit.longValue();
    }

    @Override
    @Retryable(retryFor = {ClientConnectionException.class, SocketException.class, SocketTimeoutException.class}, backoff = @Backoff(delay = 100))
    public long maxTimeIntervalBetweenBatches() {
        BigInteger maxTimeIntervalBetweenBatches;
        try {
            maxTimeIntervalBetweenBatches = this.getRollupForEthCall().rollupTimeLimit().send();
        } catch (Exception e) {
            throw new RuntimeException("failed to get maxTimeIntervalBetweenBatches", e);
        }
        log.info("🔗 maxTimeIntervalBetweenBatches {}", maxTimeIntervalBetweenBatches);
        return maxTimeIntervalBetweenBatches.longValue();
    }

    @Override
    @SneakyThrows
    @Retryable(retryFor = {ClientConnectionException.class, SocketException.class, SocketTimeoutException.class}, backoff = @Backoff(delay = 100))
    public BigInteger zkVerificationStartBatch() {
        var start = this.getRollupForEthCall().zkVerificationStartBatch().send();
        log.info("🔗 zkVerificationStartBatch {}", start);
        return start;
    }

    @Override
    @Retryable(retryFor = {ClientConnectionException.class, SocketException.class, SocketTimeoutException.class}, backoff = @Backoff(delay = 100))
    public byte[] committedBatchHash(BigInteger batchIndex) {
        byte[] batchHash;
        try {
            batchHash = this.getRollupForEthCall().committedBatches(batchIndex).send();
            if (Arrays.equals(batchHash, Bytes32.DEFAULT.getValue())) {
                return null;
            }
        } catch (Exception e) {
            throw new RuntimeException("failed to get last committed batch hash", e);
        }
        log.info("🔗 commit batch {} hash: {}", batchIndex, HexUtil.encodeHexStr(batchHash));
        return batchHash;
    }

    @Override
    @WithSpan
    @Retryable(retryFor = {ClientConnectionException.class, SocketException.class, SocketTimeoutException.class, L1ContractInvalidParameterException.class}, backoff = @Backoff(delay = 100))
    public TransactionInfo resendRollupTx(ReliableTransactionDO reliableTx, String encodedFunc) {
        var call = ethCall(this.rollupContractAddress, encodedFunc);
        if (call.isReverted()) {
            processFailedEthCall(call, this.rollupContractAddress, StrUtil.sub(Numeric.cleanHexPrefix(encodedFunc), 0, 8));
            return null;
        }
        return sendTransaction(this.rollupContractAddress, encodedFunc, getRollupEconomicStrategy().createRetryTxCostChecker(reliableTx));
    }

    @Override
    @Retryable(retryFor = {ClientConnectionException.class, SocketException.class, SocketTimeoutException.class}, backoff = @Backoff(delay = 100))
    public TransactionInfo resendRollupTx(ReliableTransactionDO reliableTx, Transaction4844 transaction4844) {
        var call = ethCall(this.rollupContractAddress, transaction4844);
        if (call.isReverted()) {
            processFailedEthCall(call, this.rollupContractAddress, StrUtil.sub(Numeric.cleanHexPrefix(transaction4844.getData()), 0, 8));
            return null;
        }
        return sendBlobTransaction(
                this.rollupContractAddress,
                transaction4844.getData(),
                new EthBlobs(transaction4844.getBlobs().orElse(new ArrayList<>())),
                getRollupEconomicStrategy().createRetryTxCostChecker(reliableTx)
        );
    }

    @Override
    @SneakyThrows
    @WithSpan
    @Retryable(retryFor = {ClientConnectionException.class, SocketException.class, SocketTimeoutException.class}, backoff = @Backoff(delay = 100))
    public TransactionInfo speedUpRollupTx(@NonNull ReliableTransactionDO tx) {
        var lock = getSpeedupTxLock(tx);
        if (!lock.tryLock()) {
            throw new SpeedupAsyncNowException(tx);
        }
        try {
            var rawTransaction = EthTxDecoder.decode(Numeric.toHexString(tx.getRawTx()));
            var currentGasPrice = checkIfSpeedUpTx(tx.getTransactionType(), tx.getBatchIndex(), rawTransaction, tx.getLatestTxSendTime());
            return speedUpRollupTxLogic(tx, rawTransaction, currentGasPrice);
        } finally {
            lock.unlock();
        }
    }

    @Override
    @SneakyThrows
    public TransactionInfo speedUpRollupTx(ReliableTransactionDO tx, BigInteger maxFeePerGas, BigInteger maxPriorityFeePerGas, BigInteger maxFeePerBlobGas) {
        var lock = getSpeedupTxLock(tx);
        if (!lock.tryLock()) {
            throw new SpeedupAsyncNowException(tx);
        }
        log.info("speed up tx with specific gas price setting : {maxFeePerGas: {}, maxPriorityFeePerGas: {}, maxFeePerBlobGas: {}}",
                maxFeePerGas, maxPriorityFeePerGas, maxFeePerBlobGas);
        try {
            var netGasPrice = tx.getTransactionType() == TransactionTypeEnum.BATCH_COMMIT_TX ?
                    this.getGasPriceProvider().getEip4844GasPrice() :
                    this.getGasPriceProvider().getEip1559GasPrice();
            var currentGasPrice = netGasPrice instanceof Eip4844GasPrice ?
                    new Eip4844GasPrice(
                            maxFeePerGas.compareTo(netGasPrice.maxFeePerGas()) > 0 ? maxFeePerGas : netGasPrice.maxFeePerGas(),
                            maxPriorityFeePerGas.compareTo(netGasPrice.maxPriorityFeePerGas()) > 0 ? maxPriorityFeePerGas : netGasPrice.maxPriorityFeePerGas(),
                            maxFeePerBlobGas.compareTo(netGasPrice.maxFeePerBlobGas()) > 0 ? maxFeePerBlobGas : netGasPrice.maxFeePerBlobGas(),
                            netGasPrice.baseFee()
                    ) :
                    new Eip1559GasPrice(
                            maxFeePerGas.compareTo(netGasPrice.maxFeePerGas()) > 0 ? maxFeePerGas : netGasPrice.maxFeePerGas(),
                            maxPriorityFeePerGas.compareTo(netGasPrice.maxPriorityFeePerGas()) > 0 ? maxPriorityFeePerGas : netGasPrice.maxPriorityFeePerGas(),
                            netGasPrice.baseFee()
                    );
            return speedUpRollupTxLogic(
                    tx,
                    EthTxDecoder.decode(Numeric.toHexString(tx.getRawTx())),
                    currentGasPrice
            );
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void processFailedEthCall(EthCall call, String toAddress, String funcNameOrDigest) {
        if (call.getRevertReason().contains("WARNING")) {
            throw new L1ContractWarnException(L2RelayerErrorCodeEnum.CALL_WITH_WARNING,
                    String.format("failed to local call %s to %s error-msg %s", funcNameOrDigest, toAddress, call.getRevertReason())
            );
        } else if (call.getRevertReason().contains("INVALID_PERMISSION")) {
            throw new L1ContractInvalidPermissionException(
                    String.format("failed to local call %s to %s error-msg %s", funcNameOrDigest, toAddress, call.getRevertReason()),
                    call.getRevertReason()
            );
        } else if (call.getRevertReason().contains("INVALID_PARAMETER")) {
            throw new L1ContractInvalidParameterException(
                    String.format("failed to local call %s to %s error-msg %s", funcNameOrDigest, toAddress, call.getRevertReason()),
                    call.getRevertReason()
            );
        } else if (call.getRevertReason().contains("ERROR")) {
            throw new L1ContractSeriousException(
                    String.format("failed to local call %s to %s error-msg %s", funcNameOrDigest, toAddress, call.getRevertReason()),
                    call.getRevertReason()
            );
        } else {
            throw new L2RelayerException(L2RelayerErrorCodeEnum.ROLLUP_SEND_TX_ERROR,
                    String.format("failed to local call %s to %s error-msg %s", funcNameOrDigest, toAddress, getRpcError(call))
            );
        }
    }

    @Override
    public Flowable<L1MsgTransactionBatch> flowableL1MsgFromMailbox(BigInteger start, BigInteger end) {
        List<CompletableFuture<L1MsgTransactionBatch>> futures = new ArrayList<>();
        for (BigInteger h = start; h.compareTo(end) <= 0; h = h.add(BigInteger.ONE)) {
            BigInteger finalH = h;
            futures.add(CompletableFuture.supplyAsync(() -> getL1MsgsFromBlock(finalH), fetchingL1MsgThreadsPool));
        }
        return Flowable.merge(futures.stream().map(x -> Flowable.fromFuture(x, 30, TimeUnit.SECONDS)).collect(Collectors.toList()));
    }

    private TransactionInfo speedUpRollupTxLogic(ReliableTransactionDO reliableTxDO, RawTransaction rawTransaction, IGasPrice currentGasPrice) throws IOException {
        getRollupEconomicStrategy().createSpeedUpTxCostChecker(reliableTxDO).check(currentGasPrice);
        String fromAcc;
        SendTxResult result;
        var gasLimitProvider = createEthCallGasLimitProvider(this.rollupContractAddress, rawTransaction.getData());
        if (rawTransaction.getType().isEip4844()) {
            log.info("speed up eip 4844 tx with new gas price combination {}", currentGasPrice.toJson());
            result = this.getBlobPoolTxManager().sendTx(
                    ((Transaction4844) rawTransaction.getTransaction()).getBlobs().orElse(new ArrayList<>()),
                    currentGasPrice,
                    gasLimitProvider.getGasLimit(rawTransaction.getData()),
                    rawTransaction.getTo(),
                    rawTransaction.getNonce(),
                    rawTransaction.getValue(),
                    rawTransaction.getData()
            );
            fromAcc = this.getBlobPoolTxManager().getAddress();
        } else {
            log.info("speed up eip1559 tx with new gas price combination {}...", currentGasPrice.toJson());
            result = this.getLegacyPoolTxManager().sendTx(
                    currentGasPrice,
                    gasLimitProvider.getGasLimit(rawTransaction.getData()),
                    rawTransaction.getTo(),
                    rawTransaction.getData(),
                    rawTransaction.getNonce(),
                    rawTransaction.getValue(),
                    false
            );
            fromAcc = this.getLegacyPoolTxManager().getAddress();
        }
        dealWithTxResult(result);
        return TransactionInfo.builder()
                .rawTx(Numeric.hexStringToByteArray(result.getRawTxHex()))
                .txHash(result.getEthSendTransaction().getTransactionHash())
                .nonce(result.getNonce())
                .senderAccount(fromAcc)
                .sendTxTime(result.getTxSendTime())
                .build();
    }

    private IGasPrice checkIfSpeedUpTx(TransactionTypeEnum transactionType, BigInteger batchIndex, RawTransaction rawTransaction, Date lastSendTime) {
        checkTxIfNextNonce(rawTransaction);
        var lastTx = rollupRepository.getReliableTransaction(ChainTypeEnum.LAYER_ONE, batchIndex.subtract(BigInteger.ONE), transactionType);
        if (ObjectUtil.isNotNull(lastTx)) {
            log.info("latest tx hash of the last is {}", lastTx.getLatestTxHash());
            // if last tx not ready, we are not going to speed up this tx.
            if (!lastTx.getState().isExecuteAlrightAndOnchain()) {
                throw new PreviousTxNotReadyException(lastTx.getBatchIndex(), lastTx.getLatestTxHash(), lastTx.getState());
            }
            lastSendTime = lastSendTime.after(lastTx.getGmtModified()) ? lastSendTime : lastTx.getGmtModified();
            log.info("finally, last tx send time is {}", DateUtil.format(lastSendTime, DatePattern.NORM_DATETIME_MS_PATTERN));
        }
        return checkAndGetGasPrice(rawTransaction, lastSendTime);
    }

    private IGasPrice checkAndGetGasPrice(RawTransaction rawTransaction, Date lastSendTime) {
        var currentGasPrice = rawTransaction.getType().isEip4844() ? this.getGasPriceProvider().getEip4844GasPrice() : this.getGasPriceProvider().getEip1559GasPrice();
        if (rawTransaction.getType().isLegacy()) {
            if (!isOkToSpeedUpLegacyTx(currentGasPrice.maxFeePerGas(), rawTransaction.getGasPrice())) {
                throw new PreviousGasPriceJustFineException(rawTransaction.getGasPrice(), null, currentGasPrice.maxFeePerGas(), currentGasPrice.maxPriorityFeePerGas());
            }
        } else {
            var innerTx = (Transaction1559) rawTransaction.getTransaction();
            var speedupPrice = isOkToSpeedUpEip1559Tx(
                    currentGasPrice,
                    innerTx.getMaxFeePerGas(),
                    innerTx.getMaxPriorityFeePerGas(),
                    // unfortunately, blob pool wants over 100% price dump by default
                    innerTx.getType().isEip4844() ? txSpeedUpBlobFeeBump : txSpeedupPriceBump,
                    lastSendTime
            );
            if (ObjectUtil.isNull(speedupPrice)) {
                throw new PreviousGasPriceJustFineException(innerTx.getMaxFeePerGas(), innerTx.getMaxPriorityFeePerGas(), currentGasPrice.maxFeePerGas(), currentGasPrice.maxPriorityFeePerGas());
            }
            if (speedupPrice.maxPriorityFeePerGas().compareTo(txSpeedUpPriorityFeeLimit) > 0) {
                throw new SpeedUpOverLimitException("speed up priority fee over limit: curr is {}, prev is {} and limit is {}",
                        speedupPrice.maxPriorityFeePerGas(), innerTx.getMaxPriorityFeePerGas(), txSpeedUpPriorityFeeLimit.toString());
            }
            if (innerTx.getType().isEip4844()) {
                currentGasPrice = new Eip4844GasPrice(
                        speedupPrice.maxFeePerGas(),
                        speedupPrice.maxPriorityFeePerGas(),
                        calcSpeedUpMaxBlobFeePerGas(((Transaction4844) rawTransaction.getTransaction()).getMaxFeePerBlobGas(), currentGasPrice.maxFeePerBlobGas()),
                        speedupPrice.baseFee()
                );
            } else {
                currentGasPrice = speedupPrice;
            }
        }
        return currentGasPrice.validate();
    }

    private void checkTxIfNextNonce(RawTransaction rawTransaction) {
        // only speed up next nonce tx for my account
        var accAddress = rawTransaction.getTransaction().getType().isEip4844() ? this.getBlobPoolTxManager().getAddress() : this.getLegacyPoolTxManager().getAddress();
        var nextNonce = this.queryTxCount(accAddress, DefaultBlockParameterName.LATEST);
        log.info("next tx for {} is {}", accAddress, nextNonce);
        if (rawTransaction.getNonce().compareTo(nextNonce) > 0) {
            throw new CurrTxNotNextNonceException(rawTransaction.getNonce(), nextNonce, accAddress);
        }
    }

    private Rollup getRollupForEthCall() {
        return Rollup.load(
                this.rollupContractAddress,
                this.getWeb3j(),
                this.getLegacyPoolTxManager(),
                null
        );
    }

    @SneakyThrows
    private void checkIfRollupContractValid() {
        EthGetCode result = getWeb3j().ethGetCode(this.rollupContractAddress, DefaultBlockParameterName.LATEST).send();
        if (result.hasError()) {
            throw new RuntimeException("failed to get rollup contract code: " + result.getError().getMessage());
        }
        if (StrUtil.isEmpty(Numeric.cleanHexPrefix(result.getCode()))) {
            throw new RuntimeException("rollup contract not exist");
        }
    }

    @SneakyThrows
    private void checkIfMailboxContractValid() {
        EthGetCode result = getWeb3j().ethGetCode(this.mailboxContractAddress, DefaultBlockParameterName.LATEST).send();
        if (result.hasError()) {
            throw new RuntimeException("failed to get mailbox contract code: " + result.getError().getMessage());
        }
        if (StrUtil.isEmpty(Numeric.cleanHexPrefix(result.getCode()))) {
            throw new RuntimeException("mailbox contract not exist");
        }
    }

    private L1MsgTransactionBatch getL1MsgsFromBlock(BigInteger height) {
        log.debug("try to get l1Msg from height {}", height);
        try {
            List<EthLog.LogResult> logs = getWeb3j().ethGetLogs(
                    new EthFilter(
                            new DefaultBlockParameterNumber(height),
                            new DefaultBlockParameterNumber(height),
                            mailboxContractAddress
                    ).addSingleTopic(EventEncoder.encode(IMailBoxBase.SENTMSG_EVENT))
            ).send().getLogs();

            L1MsgTransactionBatch batch = new L1MsgTransactionBatch(new ArrayList<>(), height);
            for (EthLog.LogResult logResult : logs) {
                EthLog.LogObject logObject = (EthLog.LogObject) logResult.get();
                if (logObject.isRemoved()) {
                    continue;
                }
                EventValues eventValues = staticExtractEventParameters(IMailBoxBase.SENTMSG_EVENT, logObject);
                if (ObjectUtil.isNull(eventValues)) {
                    continue;
                }
                log.info("get l1Msg from log {} of tx {} in block {}", logObject.getLogIndex(), logObject.getTransactionHash(), logObject.getBlockNumber());
                batch.getL1MsgTransactionInfos().add(
                        new L1MsgTransactionInfo(
                                new L1MsgTransaction(
                                        (BigInteger) eventValues.getNonIndexedValues().get(1).getValue(),
                                        (BigInteger) eventValues.getNonIndexedValues().get(3).getValue(),
                                        Numeric.toHexString((byte[]) eventValues.getNonIndexedValues().get(2).getValue())
                                ),
                                logObject.getBlockNumber(),
                                logObject.getTransactionHash()
                        )
                );
            }

            log.debug("get {} l1Msg from block {}", batch.getL1MsgTransactionInfos().size(), height);
            return batch;
        } catch (IOException e) {
            throw new RuntimeException(StrUtil.format("failed to parse eth logs from height {}", height.toString()), e);
        }
    }

    /**
     * Only for legacy now.
     *
     * @param newPrice new price
     * @param oldPrice old price
     * @return boolean
     */
    private boolean isOkToSpeedUpLegacyTx(BigInteger newPrice, BigInteger oldPrice) {
        return newPrice.compareTo(new BigDecimal(oldPrice).multiply(BigDecimal.valueOf(1 + txSpeedupPriceBump)).toBigInteger()) >= 0;
    }

    /**
     * For txns over eip1559.
     *
     * @param newGasPrice
     * @param oldMaxFee
     * @param oldMaxPriorityFee
     * @return
     */
    private IGasPrice isOkToSpeedUpEip1559Tx(IGasPrice newGasPrice, BigInteger oldMaxFee, BigInteger oldMaxPriorityFee, double priceBump, Date lastSendTime) {
        var miniSpeedUpMaxFee = new BigDecimal(oldMaxFee).multiply(BigDecimal.valueOf(1 + priceBump)).toBigInteger();
        var miniSpeedUpMaxPriorityFee = new BigDecimal(oldMaxPriorityFee).multiply(BigDecimal.valueOf(1 + priceBump)).toBigInteger();
        var greaterMaxFee = newGasPrice.maxFeePerGas().compareTo(miniSpeedUpMaxFee) >= 0;
        var greaterPriorityFee = newGasPrice.maxPriorityFeePerGas().compareTo(miniSpeedUpMaxPriorityFee) >= 0;
        // if both are greater, speed it anyway
        if (greaterMaxFee && greaterPriorityFee) {
            return newGasPrice;
        }
        var result = new Eip1559GasPrice(
                greaterMaxFee ? newGasPrice.maxFeePerGas() : miniSpeedUpMaxFee,
                greaterPriorityFee ? newGasPrice.maxPriorityFeePerGas() : miniSpeedUpMaxPriorityFee,
                newGasPrice.baseFee()
        );
        if (lastSendTime.getTime() + forceTxSpeedUpTimeLimit < System.currentTimeMillis()) {
            // if pending time too long, force to speed it up
            return result;
        }

        // base fee
        var baseFee = this.queryLatestBaseFee();
        // if old one can't cover base fee, speed it !
        if (oldMaxFee.subtract(oldMaxPriorityFee).compareTo(baseFee) < 0) {
            // adjust the gas price to ensure they are grown over 10%
            return result;
        }

        return null;
    }

    private BigInteger calcSpeedUpMaxBlobFeePerGas(BigInteger previous, BigInteger maxBlobFeeFromNet) {
        var speedUpBlobFee = new BigDecimal(previous).multiply(BigDecimal.valueOf(1 + txSpeedUpBlobFeeBump)).toBigInteger();
        if (speedUpBlobFee.compareTo(txSpeedUpBlobFeeLimit) > 0) {
            throw new SpeedUpOverLimitException("speed up blob fee over limit: previous is {} and limit is {}", previous.toString(), txSpeedUpBlobFeeLimit.toString());
        }
        return speedUpBlobFee.compareTo(maxBlobFeeFromNet) > 0 ? speedUpBlobFee : maxBlobFeeFromNet;
    }

    @SneakyThrows
    private BigInteger queryLatestBaseFee() {
        var ethBlock = getWeb3j().ethGetBlockByNumber(DefaultBlockParameterName.LATEST, false).send();
        if (ObjectUtil.isNull(ethBlock) || ObjectUtil.isNull(ethBlock.getBlock())) {
            throw new RuntimeException("get null latest block from blockchain");
        }
        log.debug("get latest block base fee {}", ethBlock.getBlock().getBaseFeePerGas());
        return ethBlock.getBlock().getBaseFeePerGas();
    }

    private RLock getSpeedupTxLock(ReliableTransactionDO tx) {
        return redisson.getLock(getSpeedupTxLockName(tx));
    }

    private String getSpeedupTxLockName(ReliableTransactionDO tx) {
        return StrUtil.format("SPEEDUP_TX_{}-{}-{}", tx.getChainType().name(), tx.getTransactionType(), tx.getBatchIndex());
    }

    private String getRpcError(EthCall call) {
        if (!call.hasError()) {
            return "";
        }
        var revertReason = call.getRevertReason();
        if (StrUtil.length(revertReason) > 18) {
            // has info more tha 'execution reverted'
            return revertReason;
        }
        if (StrUtil.isNotEmpty(call.getError().getData())) {
            // add data for more detail
            var error = contractErrorParser.parse(call.getError().getData());
            revertReason = StrUtil.concat(true,
                    revertReason, ": ", ObjectUtil.isNull(error) ? call.getError().getData() : error.getReason());
        }
        return revertReason;
    }
}
