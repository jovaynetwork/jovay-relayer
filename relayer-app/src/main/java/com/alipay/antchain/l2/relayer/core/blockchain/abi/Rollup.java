package com.alipay.antchain.l2.relayer.core.blockchain.abi;

import io.reactivex.Flowable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint32;
import org.web3j.abi.datatypes.generated.Uint64;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.BaseEventResponse;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

/**
 * <p>Auto generated code.
 * <p><strong>Do not modify!</strong>
 * <p>Please use the <a href="https://docs.web3j.io/command_line.html">web3j command line tools</a>,
 * or the org.web3j.codegen.SolidityFunctionWrapperGenerator in the 
 * <a href="https://github.com/hyperledger-web3j/web3j/tree/main/codegen">codegen module</a> to update.
 *
 * <p>Generated with web3j version 4.13.0.
 */
@SuppressWarnings("rawtypes")
public class Rollup extends Contract {
    public static final String BINARY = "Bin file was not provided";

    public static final String FUNC_ADDRELAYER = "addRelayer";

    public static final String FUNC_COMMITBATCH = "commitBatch";

    public static final String FUNC_COMMITTEDBATCHES = "committedBatches";

    public static final String FUNC_FINALIZEDSTATEROOTS = "finalizedStateRoots";

    public static final String FUNC_GETL2MSGROOT = "getL2MsgRoot";

    public static final String FUNC_IMPORTGENESISBATCH = "importGenesisBatch";

    public static final String FUNC_INITIALIZE = "initialize";

    public static final String FUNC_ISRELAYER = "isRelayer";

    public static final String FUNC_L1BLOBNUMBERLIMIT = "l1BlobNumberLimit";

    public static final String FUNC_L1MSGCOUNT = "l1MsgCount";

    public static final String FUNC_L1_MAIL_BOX = "l1_mail_box";

    public static final String FUNC_L2MSGROOTS = "l2MsgRoots";

    public static final String FUNC_LASTCOMMITTEDBATCH = "lastCommittedBatch";

    public static final String FUNC_LASTTEEVERIFIEDBATCH = "lastTeeVerifiedBatch";

    public static final String FUNC_LASTZKVERIFIEDBATCH = "lastZkVerifiedBatch";

    public static final String FUNC_LAYER2CHAINID = "layer2ChainId";

    public static final String FUNC_MAXBLOCKINCHUNK = "maxBlockInChunk";

    public static final String FUNC_MAXCALLDATAINCHUNK = "maxCallDataInChunk";

    public static final String FUNC_MAXTXSINCHUNK = "maxTxsInChunk";

    public static final String FUNC_MAXZKCIRCLEINCHUNK = "maxZkCircleInChunk";

    public static final String FUNC_OWNER = "owner";

    public static final String FUNC_PAUSED = "paused";

    public static final String FUNC_REMOVERELAYER = "removeRelayer";

    public static final String FUNC_RENOUNCEOWNERSHIP = "renounceOwnership";

    public static final String FUNC_REVERTBATCHES = "revertBatches";

    public static final String FUNC_ROLLUPTIMELIMIT = "rollupTimeLimit";

    public static final String FUNC_SETL1BLOBNUMBERLIMIT = "setL1BlobNumberLimit";

    public static final String FUNC_SETL2CHAINID = "setL2ChainId";

    public static final String FUNC_SETMAXBLOCKINCHUNK = "setMaxBlockInChunk";

    public static final String FUNC_SETMAXCALLDATAINCHUNK = "setMaxCallDataInChunk";

    public static final String FUNC_SETMAXTXSINCHUNK = "setMaxTxsInChunk";

    public static final String FUNC_SETPAUSE = "setPause";

    public static final String FUNC_SETROLLUPTIMELIMIT = "setRollupTimeLimit";

    public static final String FUNC_SETTEEVERIFIERADDRESS = "setTeeVerifierAddress";

    public static final String FUNC_SETZKVERIFIERADDRESS = "setZkVerifierAddress";

    public static final String FUNC_TEE_VERIFIER = "tee_verifier";

    public static final String FUNC_TRANSFEROWNERSHIP = "transferOwnership";

    public static final String FUNC_VERIFYBATCH = "verifyBatch";

    public static final String FUNC_ZKVERIFICATIONSTARTBATCH = "zkVerificationStartBatch";

    public static final String FUNC_ZK_VERIFIER = "zk_verifier";

    public static final Event BLOBDATAHASH_EVENT = new Event("BlobDataHash", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>(true) {}));
    ;

    public static final Event COMMITBATCH_EVENT = new Event("CommitBatch", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}, new TypeReference<Bytes32>(true) {}));
    ;

    public static final Event INITIALIZED_EVENT = new Event("Initialized", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint8>() {}));
    ;

    public static final Event OWNERSHIPTRANSFERRED_EVENT = new Event("OwnershipTransferred", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}));
    ;

    public static final Event PAUSED_EVENT = new Event("Paused", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
    ;

    public static final Event REVERTBATCH_EVENT = new Event("RevertBatch", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}));
    ;

    public static final Event UNPAUSED_EVENT = new Event("Unpaused", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
    ;

    public static final Event VERIFYBATCH_EVENT = new Event("VerifyBatch", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint8>() {}, new TypeReference<Uint256>(true) {}, new TypeReference<Bytes32>(true) {}, new TypeReference<Bytes32>() {}, new TypeReference<Bytes32>() {}));
    ;

    @Deprecated
    protected Rollup(String contractAddress, Web3j web3j, Credentials credentials,
            BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected Rollup(String contractAddress, Web3j web3j, Credentials credentials,
            ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected Rollup(String contractAddress, Web3j web3j, TransactionManager transactionManager,
            BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected Rollup(String contractAddress, Web3j web3j, TransactionManager transactionManager,
            ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static List<BlobDataHashEventResponse> getBlobDataHashEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(BLOBDATAHASH_EVENT, transactionReceipt);
        ArrayList<BlobDataHashEventResponse> responses = new ArrayList<BlobDataHashEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            BlobDataHashEventResponse typedResponse = new BlobDataHashEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.batchDataHash = (byte[]) eventValues.getIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static BlobDataHashEventResponse getBlobDataHashEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(BLOBDATAHASH_EVENT, log);
        BlobDataHashEventResponse typedResponse = new BlobDataHashEventResponse();
        typedResponse.log = log;
        typedResponse.batchDataHash = (byte[]) eventValues.getIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<BlobDataHashEventResponse> blobDataHashEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getBlobDataHashEventFromLog(log));
    }

    public Flowable<BlobDataHashEventResponse> blobDataHashEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(BLOBDATAHASH_EVENT));
        return blobDataHashEventFlowable(filter);
    }

    public static List<CommitBatchEventResponse> getCommitBatchEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(COMMITBATCH_EVENT, transactionReceipt);
        ArrayList<CommitBatchEventResponse> responses = new ArrayList<CommitBatchEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            CommitBatchEventResponse typedResponse = new CommitBatchEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.batchIndex = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.batchHash = (byte[]) eventValues.getIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static CommitBatchEventResponse getCommitBatchEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(COMMITBATCH_EVENT, log);
        CommitBatchEventResponse typedResponse = new CommitBatchEventResponse();
        typedResponse.log = log;
        typedResponse.batchIndex = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.batchHash = (byte[]) eventValues.getIndexedValues().get(1).getValue();
        return typedResponse;
    }

    public Flowable<CommitBatchEventResponse> commitBatchEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getCommitBatchEventFromLog(log));
    }

    public Flowable<CommitBatchEventResponse> commitBatchEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(COMMITBATCH_EVENT));
        return commitBatchEventFlowable(filter);
    }

    public static List<InitializedEventResponse> getInitializedEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(INITIALIZED_EVENT, transactionReceipt);
        ArrayList<InitializedEventResponse> responses = new ArrayList<InitializedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            InitializedEventResponse typedResponse = new InitializedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.version = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static InitializedEventResponse getInitializedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(INITIALIZED_EVENT, log);
        InitializedEventResponse typedResponse = new InitializedEventResponse();
        typedResponse.log = log;
        typedResponse.version = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<InitializedEventResponse> initializedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getInitializedEventFromLog(log));
    }

    public Flowable<InitializedEventResponse> initializedEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(INITIALIZED_EVENT));
        return initializedEventFlowable(filter);
    }

    public static List<OwnershipTransferredEventResponse> getOwnershipTransferredEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(OWNERSHIPTRANSFERRED_EVENT, transactionReceipt);
        ArrayList<OwnershipTransferredEventResponse> responses = new ArrayList<OwnershipTransferredEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            OwnershipTransferredEventResponse typedResponse = new OwnershipTransferredEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.previousOwner = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.newOwner = (String) eventValues.getIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static OwnershipTransferredEventResponse getOwnershipTransferredEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(OWNERSHIPTRANSFERRED_EVENT, log);
        OwnershipTransferredEventResponse typedResponse = new OwnershipTransferredEventResponse();
        typedResponse.log = log;
        typedResponse.previousOwner = (String) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.newOwner = (String) eventValues.getIndexedValues().get(1).getValue();
        return typedResponse;
    }

    public Flowable<OwnershipTransferredEventResponse> ownershipTransferredEventFlowable(
            EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getOwnershipTransferredEventFromLog(log));
    }

    public Flowable<OwnershipTransferredEventResponse> ownershipTransferredEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(OWNERSHIPTRANSFERRED_EVENT));
        return ownershipTransferredEventFlowable(filter);
    }

    public static List<PausedEventResponse> getPausedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(PAUSED_EVENT, transactionReceipt);
        ArrayList<PausedEventResponse> responses = new ArrayList<PausedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            PausedEventResponse typedResponse = new PausedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.account = (String) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static PausedEventResponse getPausedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(PAUSED_EVENT, log);
        PausedEventResponse typedResponse = new PausedEventResponse();
        typedResponse.log = log;
        typedResponse.account = (String) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<PausedEventResponse> pausedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getPausedEventFromLog(log));
    }

    public Flowable<PausedEventResponse> pausedEventFlowable(DefaultBlockParameter startBlock,
            DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(PAUSED_EVENT));
        return pausedEventFlowable(filter);
    }

    public static List<RevertBatchEventResponse> getRevertBatchEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(REVERTBATCH_EVENT, transactionReceipt);
        ArrayList<RevertBatchEventResponse> responses = new ArrayList<RevertBatchEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            RevertBatchEventResponse typedResponse = new RevertBatchEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.batchIndex = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static RevertBatchEventResponse getRevertBatchEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(REVERTBATCH_EVENT, log);
        RevertBatchEventResponse typedResponse = new RevertBatchEventResponse();
        typedResponse.log = log;
        typedResponse.batchIndex = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<RevertBatchEventResponse> revertBatchEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getRevertBatchEventFromLog(log));
    }

    public Flowable<RevertBatchEventResponse> revertBatchEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(REVERTBATCH_EVENT));
        return revertBatchEventFlowable(filter);
    }

    public static List<UnpausedEventResponse> getUnpausedEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(UNPAUSED_EVENT, transactionReceipt);
        ArrayList<UnpausedEventResponse> responses = new ArrayList<UnpausedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            UnpausedEventResponse typedResponse = new UnpausedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.account = (String) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static UnpausedEventResponse getUnpausedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(UNPAUSED_EVENT, log);
        UnpausedEventResponse typedResponse = new UnpausedEventResponse();
        typedResponse.log = log;
        typedResponse.account = (String) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<UnpausedEventResponse> unpausedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getUnpausedEventFromLog(log));
    }

    public Flowable<UnpausedEventResponse> unpausedEventFlowable(DefaultBlockParameter startBlock,
            DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(UNPAUSED_EVENT));
        return unpausedEventFlowable(filter);
    }

    public static List<VerifyBatchEventResponse> getVerifyBatchEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(VERIFYBATCH_EVENT, transactionReceipt);
        ArrayList<VerifyBatchEventResponse> responses = new ArrayList<VerifyBatchEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            VerifyBatchEventResponse typedResponse = new VerifyBatchEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.batchIndex = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.batchHash = (byte[]) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.proveType = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.stateRoot = (byte[]) eventValues.getNonIndexedValues().get(1).getValue();
            typedResponse.l2MsgRoot = (byte[]) eventValues.getNonIndexedValues().get(2).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static VerifyBatchEventResponse getVerifyBatchEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(VERIFYBATCH_EVENT, log);
        VerifyBatchEventResponse typedResponse = new VerifyBatchEventResponse();
        typedResponse.log = log;
        typedResponse.batchIndex = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.batchHash = (byte[]) eventValues.getIndexedValues().get(1).getValue();
        typedResponse.proveType = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        typedResponse.stateRoot = (byte[]) eventValues.getNonIndexedValues().get(1).getValue();
        typedResponse.l2MsgRoot = (byte[]) eventValues.getNonIndexedValues().get(2).getValue();
        return typedResponse;
    }

    public Flowable<VerifyBatchEventResponse> verifyBatchEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getVerifyBatchEventFromLog(log));
    }

    public Flowable<VerifyBatchEventResponse> verifyBatchEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(VERIFYBATCH_EVENT));
        return verifyBatchEventFlowable(filter);
    }

    public RemoteFunctionCall<TransactionReceipt> addRelayer(String _account) {
        final Function function = new Function(
                FUNC_ADDRELAYER, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, _account)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> commitBatch(BigInteger _version,
            BigInteger _batchIndex, BigInteger _totalL1MessagePopped) {
        final Function function = new Function(
                FUNC_COMMITBATCH, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint8(_version), 
                new org.web3j.abi.datatypes.generated.Uint256(_batchIndex), 
                new org.web3j.abi.datatypes.generated.Uint256(_totalL1MessagePopped)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<byte[]> committedBatches(BigInteger param0) {
        final Function function = new Function(FUNC_COMMITTEDBATCHES, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(param0)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
        return executeRemoteCallSingleValueReturn(function, byte[].class);
    }

    public RemoteFunctionCall<byte[]> finalizedStateRoots(BigInteger param0) {
        final Function function = new Function(FUNC_FINALIZEDSTATEROOTS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(param0)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
        return executeRemoteCallSingleValueReturn(function, byte[].class);
    }

    public RemoteFunctionCall<byte[]> getL2MsgRoot(BigInteger batch_index) {
        final Function function = new Function(FUNC_GETL2MSGROOT, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(batch_index)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
        return executeRemoteCallSingleValueReturn(function, byte[].class);
    }

    public RemoteFunctionCall<TransactionReceipt> importGenesisBatch(byte[] _batchHeader,
            byte[] _stateRoot) {
        final Function function = new Function(
                FUNC_IMPORTGENESISBATCH, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.DynamicBytes(_batchHeader), 
                new org.web3j.abi.datatypes.generated.Bytes32(_stateRoot)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> initialize(BigInteger _chainId,
            String _zk_verifier, String _tee_verifier, String _l1_mail_box,
            BigInteger _maxTxsInChunk, BigInteger _maxBlockInChunk, BigInteger _maxCallDataInChunk,
            BigInteger _maxZkCircleInChunk, BigInteger _l1BlobNumberLimit,
            BigInteger _rollupTimeLimit) {
        final Function function = new Function(
                FUNC_INITIALIZE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint64(_chainId), 
                new org.web3j.abi.datatypes.Address(160, _zk_verifier), 
                new org.web3j.abi.datatypes.Address(160, _tee_verifier), 
                new org.web3j.abi.datatypes.Address(160, _l1_mail_box), 
                new org.web3j.abi.datatypes.generated.Uint32(_maxTxsInChunk), 
                new org.web3j.abi.datatypes.generated.Uint32(_maxBlockInChunk), 
                new org.web3j.abi.datatypes.generated.Uint32(_maxCallDataInChunk), 
                new org.web3j.abi.datatypes.generated.Uint32(_maxZkCircleInChunk), 
                new org.web3j.abi.datatypes.generated.Uint32(_l1BlobNumberLimit), 
                new org.web3j.abi.datatypes.generated.Uint32(_rollupTimeLimit)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<Boolean> isRelayer(String param0) {
        final Function function = new Function(FUNC_ISRELAYER, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, param0)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteFunctionCall<BigInteger> l1BlobNumberLimit() {
        final Function function = new Function(FUNC_L1BLOBNUMBERLIMIT, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint32>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<BigInteger> l1MsgCount(BigInteger param0) {
        final Function function = new Function(FUNC_L1MSGCOUNT, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(param0)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<String> l1_mail_box() {
        final Function function = new Function(FUNC_L1_MAIL_BOX, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<byte[]> l2MsgRoots(BigInteger param0) {
        final Function function = new Function(FUNC_L2MSGROOTS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(param0)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
        return executeRemoteCallSingleValueReturn(function, byte[].class);
    }

    public RemoteFunctionCall<BigInteger> lastCommittedBatch() {
        final Function function = new Function(FUNC_LASTCOMMITTEDBATCH, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<BigInteger> lastTeeVerifiedBatch() {
        final Function function = new Function(FUNC_LASTTEEVERIFIEDBATCH, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<BigInteger> lastZkVerifiedBatch() {
        final Function function = new Function(FUNC_LASTZKVERIFIEDBATCH, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<BigInteger> layer2ChainId() {
        final Function function = new Function(FUNC_LAYER2CHAINID, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint64>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<BigInteger> maxBlockInChunk() {
        final Function function = new Function(FUNC_MAXBLOCKINCHUNK, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint32>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<BigInteger> maxCallDataInChunk() {
        final Function function = new Function(FUNC_MAXCALLDATAINCHUNK, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint32>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<BigInteger> maxTxsInChunk() {
        final Function function = new Function(FUNC_MAXTXSINCHUNK, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint32>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<BigInteger> maxZkCircleInChunk() {
        final Function function = new Function(FUNC_MAXZKCIRCLEINCHUNK, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint32>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<String> owner() {
        final Function function = new Function(FUNC_OWNER, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<Boolean> paused() {
        final Function function = new Function(FUNC_PAUSED, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteFunctionCall<TransactionReceipt> removeRelayer(String _account) {
        final Function function = new Function(
                FUNC_REMOVERELAYER, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, _account)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> renounceOwnership() {
        final Function function = new Function(
                FUNC_RENOUNCEOWNERSHIP, 
                Arrays.<Type>asList(), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> revertBatches(BigInteger _newLastBatchIndex) {
        final Function function = new Function(
                FUNC_REVERTBATCHES, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(_newLastBatchIndex)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<BigInteger> rollupTimeLimit() {
        final Function function = new Function(FUNC_ROLLUPTIMELIMIT, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint64>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<TransactionReceipt> setL1BlobNumberLimit(
            BigInteger _l1BlobNumberLimit) {
        final Function function = new Function(
                FUNC_SETL1BLOBNUMBERLIMIT, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint32(_l1BlobNumberLimit)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> setL2ChainId(BigInteger _layer2ChainId) {
        final Function function = new Function(
                FUNC_SETL2CHAINID, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint64(_layer2ChainId)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> setMaxBlockInChunk(BigInteger _maxBlockInChunk) {
        final Function function = new Function(
                FUNC_SETMAXBLOCKINCHUNK, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint32(_maxBlockInChunk)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> setMaxCallDataInChunk(
            BigInteger _maxCallDataInChunk) {
        final Function function = new Function(
                FUNC_SETMAXCALLDATAINCHUNK, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint32(_maxCallDataInChunk)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> setMaxTxsInChunk(BigInteger _maxTxsInChunk) {
        final Function function = new Function(
                FUNC_SETMAXTXSINCHUNK, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint32(_maxTxsInChunk)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> setPause(Boolean _status) {
        final Function function = new Function(
                FUNC_SETPAUSE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Bool(_status)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> setRollupTimeLimit(BigInteger _rollupTimeLimit) {
        final Function function = new Function(
                FUNC_SETROLLUPTIMELIMIT, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint32(_rollupTimeLimit)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> setTeeVerifierAddress(
            String _teeVerifierAddress) {
        final Function function = new Function(
                FUNC_SETTEEVERIFIERADDRESS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, _teeVerifierAddress)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> setZkVerifierAddress(String _zkVerifierAddress) {
        final Function function = new Function(
                FUNC_SETZKVERIFIERADDRESS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, _zkVerifierAddress)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<String> tee_verifier() {
        final Function function = new Function(FUNC_TEE_VERIFIER, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<TransactionReceipt> transferOwnership(String newOwner) {
        final Function function = new Function(
                FUNC_TRANSFEROWNERSHIP, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, newOwner)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> verifyBatch(BigInteger _prove_type,
            byte[] _batchHeader, byte[] _postStateRoot, byte[] _l2MsgRoot, byte[] _proof) {
        final Function function = new Function(
                FUNC_VERIFYBATCH, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint8(_prove_type), 
                new org.web3j.abi.datatypes.DynamicBytes(_batchHeader), 
                new org.web3j.abi.datatypes.generated.Bytes32(_postStateRoot), 
                new org.web3j.abi.datatypes.generated.Bytes32(_l2MsgRoot), 
                new org.web3j.abi.datatypes.DynamicBytes(_proof)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<BigInteger> zkVerificationStartBatch() {
        final Function function = new Function(FUNC_ZKVERIFICATIONSTARTBATCH, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<String> zk_verifier() {
        final Function function = new Function(FUNC_ZK_VERIFIER, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    @Deprecated
    public static Rollup load(String contractAddress, Web3j web3j, Credentials credentials,
            BigInteger gasPrice, BigInteger gasLimit) {
        return new Rollup(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static Rollup load(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new Rollup(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static Rollup load(String contractAddress, Web3j web3j, Credentials credentials,
            ContractGasProvider contractGasProvider) {
        return new Rollup(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static Rollup load(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new Rollup(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static class BlobDataHashEventResponse extends BaseEventResponse {
        public byte[] batchDataHash;
    }

    public static class CommitBatchEventResponse extends BaseEventResponse {
        public BigInteger batchIndex;

        public byte[] batchHash;
    }

    public static class InitializedEventResponse extends BaseEventResponse {
        public BigInteger version;
    }

    public static class OwnershipTransferredEventResponse extends BaseEventResponse {
        public String previousOwner;

        public String newOwner;
    }

    public static class PausedEventResponse extends BaseEventResponse {
        public String account;
    }

    public static class RevertBatchEventResponse extends BaseEventResponse {
        public BigInteger batchIndex;
    }

    public static class UnpausedEventResponse extends BaseEventResponse {
        public String account;
    }

    public static class VerifyBatchEventResponse extends BaseEventResponse {
        public BigInteger batchIndex;

        public byte[] batchHash;

        public BigInteger proveType;

        public byte[] stateRoot;

        public byte[] l2MsgRoot;
    }
}
