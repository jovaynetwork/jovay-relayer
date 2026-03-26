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

package com.alipay.antchain.l2.relayer.commons.abi;

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
import org.web3j.abi.datatypes.Utf8String;
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

    public static final String FUNC_CONTRACT_VERSION = "CONTRACT_VERSION";

    public static final String FUNC_ADDRELAYER = "addRelayer";

    public static final String FUNC_COMMITBATCH = "commitBatch";

    public static final String FUNC_COMMITBATCHWITHDAPROOF = "commitBatchWithDaProof";

    public static final String FUNC_COMMITTEDBATCHES = "committedBatches";

    public static final String FUNC_DATYPE = "daType";

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

    public static final String FUNC_MODE = "mode";

    public static final String FUNC_OWNER = "owner";

    public static final String FUNC_PAUSED = "paused";

    public static final String FUNC_REMOVERELAYER = "removeRelayer";

    public static final String FUNC_RENOUNCEOWNERSHIP = "renounceOwnership";

    public static final String FUNC_REVERTBATCHES = "revertBatches";

    public static final String FUNC_ROLLUPTIMELIMIT = "rollupTimeLimit";

    public static final String FUNC_SETDATYPE = "setDaType";

    public static final String FUNC_SETL1BLOBNUMBERLIMIT = "setL1BlobNumberLimit";

    public static final String FUNC_SETMAXBLOCKINCHUNK = "setMaxBlockInChunk";

    public static final String FUNC_SETMAXCALLDATAINCHUNK = "setMaxCallDataInChunk";

    public static final String FUNC_SETMAXTXSINCHUNK = "setMaxTxsInChunk";

    public static final String FUNC_SETMODE = "setMode";

    public static final String FUNC_SETPAUSE = "setPause";

    public static final String FUNC_SETROLLUPTIMELIMIT = "setRollupTimeLimit";

    public static final String FUNC_SETTEEVERIFIERADDRESS = "setTeeVerifierAddress";

    public static final String FUNC_SETZKVERIFICATIONSTARTBATCH = "setZkVerificationStartBatch";

    public static final String FUNC_SETZKVERIFIERADDRESS = "setZkVerifierAddress";

    public static final String FUNC_TEE_VERIFIER = "tee_verifier";

    public static final String FUNC_TRANSFEROWNERSHIP = "transferOwnership";

    public static final String FUNC_VERIFYBATCH = "verifyBatch";

    public static final String FUNC_ZKVERIFICATIONSTARTBATCH = "zkVerificationStartBatch";

    public static final String FUNC_ZK_VERIFIER = "zk_verifier";

    public static final Event BATCHESREVERTED_EVENT = new Event("BatchesReverted", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
    ;

    public static final Event BLOBDATAHASH_EVENT = new Event("BlobDataHash", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>(true) {}));
    ;

    public static final Event COMMITBATCH_EVENT = new Event("CommitBatch", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}, new TypeReference<Bytes32>(true) {}));
    ;

    public static final Event DATYPEACCEPTEDCHANGED_EVENT = new Event("DaTypeAcceptedChanged", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint8>() {}, new TypeReference<Uint8>() {}));
    ;

    public static final Event INITIALIZED_EVENT = new Event("Initialized", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint8>() {}));
    ;

    public static final Event L1BLOBNUMBERLIMITCHANGED_EVENT = new Event("L1BlobNumberLimitChanged", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint32>() {}, new TypeReference<Uint32>() {}));
    ;

    public static final Event L2CHAINIDCHANGED_EVENT = new Event("L2ChainIdChanged", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint64>() {}, new TypeReference<Uint64>() {}));
    ;

    public static final Event MAXBLOCKINCHUNKCHANGED_EVENT = new Event("MaxBlockInChunkChanged", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint32>() {}, new TypeReference<Uint32>() {}));
    ;

    public static final Event MAXCALLDATAINCHUNKCHANGED_EVENT = new Event("MaxCallDataInChunkChanged", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint32>() {}, new TypeReference<Uint32>() {}));
    ;

    public static final Event MAXTXSINCHUNKCHANGED_EVENT = new Event("MaxTxsInChunkChanged", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint32>() {}, new TypeReference<Uint32>() {}));
    ;

    public static final Event MODECHANGED_EVENT = new Event("ModeChanged", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint32>() {}, new TypeReference<Uint32>() {}));
    ;

    public static final Event OWNERSHIPTRANSFERRED_EVENT = new Event("OwnershipTransferred", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}));
    ;

    public static final Event PAUSED_EVENT = new Event("Paused", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
    ;

    public static final Event RELAYERADDED_EVENT = new Event("RelayerAdded", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}));
    ;

    public static final Event RELAYERREMOVED_EVENT = new Event("RelayerRemoved", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}));
    ;

    public static final Event REVERTBATCH_EVENT = new Event("RevertBatch", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}));
    ;

    public static final Event ROLLUPINITIALIZED_EVENT = new Event("RollupInitialized", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint64>() {}, new TypeReference<Address>() {}, new TypeReference<Address>() {}, new TypeReference<Address>() {}, new TypeReference<Uint32>() {}, new TypeReference<Uint32>() {}, new TypeReference<Uint32>() {}, new TypeReference<Uint32>() {}, new TypeReference<Uint32>() {}, new TypeReference<Uint64>() {}));
    ;

    public static final Event ROLLUPTIMELIMITCHANGED_EVENT = new Event("RollupTimeLimitChanged", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint64>() {}, new TypeReference<Uint64>() {}));
    ;

    public static final Event TEEVERIFIERCHANGED_EVENT = new Event("TeeVerifierChanged", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}, new TypeReference<Address>() {}));
    ;

    public static final Event UNPAUSED_EVENT = new Event("Unpaused", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
    ;

    public static final Event VERIFYBATCH_EVENT = new Event("VerifyBatch", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint8>() {}, new TypeReference<Uint256>(true) {}, new TypeReference<Bytes32>(true) {}, new TypeReference<Bytes32>() {}, new TypeReference<Bytes32>() {}));
    ;

    public static final Event ZKVERIFICATIONSTARTBATCHANDZKLASTVERIFIEDBATCHCHANGED_EVENT = new Event("ZkVerificationStartBatchAndZkLastVerifiedBatchChanged", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event ZKVERIFIERCHANGED_EVENT = new Event("ZkVerifierChanged", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}, new TypeReference<Address>() {}));
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

    public RemoteFunctionCall<String> CONTRACT_VERSION() {
        final Function function = new Function(FUNC_CONTRACT_VERSION, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
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

    public RemoteFunctionCall<TransactionReceipt> commitBatchWithDaProof(byte[] _batchHeader,
            BigInteger _totalL1MessagePopped, byte[] _daProof) {
        final Function function = new Function(
                FUNC_COMMITBATCHWITHDAPROOF, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.DynamicBytes(_batchHeader), 
                new org.web3j.abi.datatypes.generated.Uint256(_totalL1MessagePopped), 
                new org.web3j.abi.datatypes.DynamicBytes(_daProof)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<byte[]> committedBatches(BigInteger batchIndex) {
        final Function function = new Function(FUNC_COMMITTEDBATCHES, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(batchIndex)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
        return executeRemoteCallSingleValueReturn(function, byte[].class);
    }

    public RemoteFunctionCall<BigInteger> daType() {
        final Function function = new Function(FUNC_DATYPE, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint8>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<byte[]> finalizedStateRoots(BigInteger batchIndex) {
        final Function function = new Function(FUNC_FINALIZEDSTATEROOTS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(batchIndex)), 
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
                new org.web3j.abi.datatypes.generated.Uint64(_rollupTimeLimit)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<Boolean> isRelayer(String relayerAddress) {
        final Function function = new Function(FUNC_ISRELAYER, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, relayerAddress)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteFunctionCall<BigInteger> l1BlobNumberLimit() {
        final Function function = new Function(FUNC_L1BLOBNUMBERLIMIT, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint32>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<BigInteger> l1MsgCount(BigInteger batchIndex) {
        final Function function = new Function(FUNC_L1MSGCOUNT, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(batchIndex)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<String> l1_mail_box() {
        final Function function = new Function(FUNC_L1_MAIL_BOX, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<byte[]> l2MsgRoots(BigInteger batchIndex) {
        final Function function = new Function(FUNC_L2MSGROOTS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(batchIndex)), 
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

    public RemoteFunctionCall<BigInteger> mode() {
        final Function function = new Function(FUNC_MODE, 
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

    public RemoteFunctionCall<TransactionReceipt> setDaType(BigInteger _type) {
        final Function function = new Function(
                FUNC_SETDATYPE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint8(_type)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> setL1BlobNumberLimit(
            BigInteger _l1BlobNumberLimit) {
        final Function function = new Function(
                FUNC_SETL1BLOBNUMBERLIMIT, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint32(_l1BlobNumberLimit)), 
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

    public RemoteFunctionCall<TransactionReceipt> setMode(BigInteger _mode) {
        final Function function = new Function(
                FUNC_SETMODE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint32(_mode)), 
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
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint64(_rollupTimeLimit)), 
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

    public RemoteFunctionCall<TransactionReceipt> setZkVerificationStartBatch(
            BigInteger _zkVerificationStartBatch) {
        final Function function = new Function(
                FUNC_SETZKVERIFICATIONSTARTBATCH, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(_zkVerificationStartBatch)), 
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

    public static List<BatchesRevertedEventResponse> getBatchesRevertedEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(BATCHESREVERTED_EVENT, transactionReceipt);
        ArrayList<BatchesRevertedEventResponse> responses = new ArrayList<BatchesRevertedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            BatchesRevertedEventResponse typedResponse = new BatchesRevertedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.newLastBatchIndex = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static BatchesRevertedEventResponse getBatchesRevertedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(BATCHESREVERTED_EVENT, log);
        BatchesRevertedEventResponse typedResponse = new BatchesRevertedEventResponse();
        typedResponse.log = log;
        typedResponse.newLastBatchIndex = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<BatchesRevertedEventResponse> batchesRevertedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getBatchesRevertedEventFromLog(log));
    }

    public Flowable<BatchesRevertedEventResponse> batchesRevertedEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(BATCHESREVERTED_EVENT));
        return batchesRevertedEventFlowable(filter);
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

    public static List<DaTypeAcceptedChangedEventResponse> getDaTypeAcceptedChangedEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(DATYPEACCEPTEDCHANGED_EVENT, transactionReceipt);
        ArrayList<DaTypeAcceptedChangedEventResponse> responses = new ArrayList<DaTypeAcceptedChangedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            DaTypeAcceptedChangedEventResponse typedResponse = new DaTypeAcceptedChangedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.oldValue = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.newValue = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static DaTypeAcceptedChangedEventResponse getDaTypeAcceptedChangedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(DATYPEACCEPTEDCHANGED_EVENT, log);
        DaTypeAcceptedChangedEventResponse typedResponse = new DaTypeAcceptedChangedEventResponse();
        typedResponse.log = log;
        typedResponse.oldValue = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        typedResponse.newValue = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        return typedResponse;
    }

    public Flowable<DaTypeAcceptedChangedEventResponse> daTypeAcceptedChangedEventFlowable(
            EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getDaTypeAcceptedChangedEventFromLog(log));
    }

    public Flowable<DaTypeAcceptedChangedEventResponse> daTypeAcceptedChangedEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(DATYPEACCEPTEDCHANGED_EVENT));
        return daTypeAcceptedChangedEventFlowable(filter);
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

    public static List<L1BlobNumberLimitChangedEventResponse> getL1BlobNumberLimitChangedEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(L1BLOBNUMBERLIMITCHANGED_EVENT, transactionReceipt);
        ArrayList<L1BlobNumberLimitChangedEventResponse> responses = new ArrayList<L1BlobNumberLimitChangedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            L1BlobNumberLimitChangedEventResponse typedResponse = new L1BlobNumberLimitChangedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.oldValue = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.newValue = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static L1BlobNumberLimitChangedEventResponse getL1BlobNumberLimitChangedEventFromLog(
            Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(L1BLOBNUMBERLIMITCHANGED_EVENT, log);
        L1BlobNumberLimitChangedEventResponse typedResponse = new L1BlobNumberLimitChangedEventResponse();
        typedResponse.log = log;
        typedResponse.oldValue = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        typedResponse.newValue = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        return typedResponse;
    }

    public Flowable<L1BlobNumberLimitChangedEventResponse> l1BlobNumberLimitChangedEventFlowable(
            EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getL1BlobNumberLimitChangedEventFromLog(log));
    }

    public Flowable<L1BlobNumberLimitChangedEventResponse> l1BlobNumberLimitChangedEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(L1BLOBNUMBERLIMITCHANGED_EVENT));
        return l1BlobNumberLimitChangedEventFlowable(filter);
    }

    public static List<L2ChainIdChangedEventResponse> getL2ChainIdChangedEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(L2CHAINIDCHANGED_EVENT, transactionReceipt);
        ArrayList<L2ChainIdChangedEventResponse> responses = new ArrayList<L2ChainIdChangedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            L2ChainIdChangedEventResponse typedResponse = new L2ChainIdChangedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.oldValue = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.newValue = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static L2ChainIdChangedEventResponse getL2ChainIdChangedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(L2CHAINIDCHANGED_EVENT, log);
        L2ChainIdChangedEventResponse typedResponse = new L2ChainIdChangedEventResponse();
        typedResponse.log = log;
        typedResponse.oldValue = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        typedResponse.newValue = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        return typedResponse;
    }

    public Flowable<L2ChainIdChangedEventResponse> l2ChainIdChangedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getL2ChainIdChangedEventFromLog(log));
    }

    public Flowable<L2ChainIdChangedEventResponse> l2ChainIdChangedEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(L2CHAINIDCHANGED_EVENT));
        return l2ChainIdChangedEventFlowable(filter);
    }

    public static List<MaxBlockInChunkChangedEventResponse> getMaxBlockInChunkChangedEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(MAXBLOCKINCHUNKCHANGED_EVENT, transactionReceipt);
        ArrayList<MaxBlockInChunkChangedEventResponse> responses = new ArrayList<MaxBlockInChunkChangedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            MaxBlockInChunkChangedEventResponse typedResponse = new MaxBlockInChunkChangedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.oldValue = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.newValue = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static MaxBlockInChunkChangedEventResponse getMaxBlockInChunkChangedEventFromLog(
            Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(MAXBLOCKINCHUNKCHANGED_EVENT, log);
        MaxBlockInChunkChangedEventResponse typedResponse = new MaxBlockInChunkChangedEventResponse();
        typedResponse.log = log;
        typedResponse.oldValue = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        typedResponse.newValue = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        return typedResponse;
    }

    public Flowable<MaxBlockInChunkChangedEventResponse> maxBlockInChunkChangedEventFlowable(
            EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getMaxBlockInChunkChangedEventFromLog(log));
    }

    public Flowable<MaxBlockInChunkChangedEventResponse> maxBlockInChunkChangedEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(MAXBLOCKINCHUNKCHANGED_EVENT));
        return maxBlockInChunkChangedEventFlowable(filter);
    }

    public static List<MaxCallDataInChunkChangedEventResponse> getMaxCallDataInChunkChangedEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(MAXCALLDATAINCHUNKCHANGED_EVENT, transactionReceipt);
        ArrayList<MaxCallDataInChunkChangedEventResponse> responses = new ArrayList<MaxCallDataInChunkChangedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            MaxCallDataInChunkChangedEventResponse typedResponse = new MaxCallDataInChunkChangedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.oldValue = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.newValue = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static MaxCallDataInChunkChangedEventResponse getMaxCallDataInChunkChangedEventFromLog(
            Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(MAXCALLDATAINCHUNKCHANGED_EVENT, log);
        MaxCallDataInChunkChangedEventResponse typedResponse = new MaxCallDataInChunkChangedEventResponse();
        typedResponse.log = log;
        typedResponse.oldValue = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        typedResponse.newValue = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        return typedResponse;
    }

    public Flowable<MaxCallDataInChunkChangedEventResponse> maxCallDataInChunkChangedEventFlowable(
            EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getMaxCallDataInChunkChangedEventFromLog(log));
    }

    public Flowable<MaxCallDataInChunkChangedEventResponse> maxCallDataInChunkChangedEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(MAXCALLDATAINCHUNKCHANGED_EVENT));
        return maxCallDataInChunkChangedEventFlowable(filter);
    }

    public static List<MaxTxsInChunkChangedEventResponse> getMaxTxsInChunkChangedEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(MAXTXSINCHUNKCHANGED_EVENT, transactionReceipt);
        ArrayList<MaxTxsInChunkChangedEventResponse> responses = new ArrayList<MaxTxsInChunkChangedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            MaxTxsInChunkChangedEventResponse typedResponse = new MaxTxsInChunkChangedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.oldValue = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.newValue = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static MaxTxsInChunkChangedEventResponse getMaxTxsInChunkChangedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(MAXTXSINCHUNKCHANGED_EVENT, log);
        MaxTxsInChunkChangedEventResponse typedResponse = new MaxTxsInChunkChangedEventResponse();
        typedResponse.log = log;
        typedResponse.oldValue = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        typedResponse.newValue = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        return typedResponse;
    }

    public Flowable<MaxTxsInChunkChangedEventResponse> maxTxsInChunkChangedEventFlowable(
            EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getMaxTxsInChunkChangedEventFromLog(log));
    }

    public Flowable<MaxTxsInChunkChangedEventResponse> maxTxsInChunkChangedEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(MAXTXSINCHUNKCHANGED_EVENT));
        return maxTxsInChunkChangedEventFlowable(filter);
    }

    public static List<ModeChangedEventResponse> getModeChangedEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(MODECHANGED_EVENT, transactionReceipt);
        ArrayList<ModeChangedEventResponse> responses = new ArrayList<ModeChangedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            ModeChangedEventResponse typedResponse = new ModeChangedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.oldValue = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.newValue = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static ModeChangedEventResponse getModeChangedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(MODECHANGED_EVENT, log);
        ModeChangedEventResponse typedResponse = new ModeChangedEventResponse();
        typedResponse.log = log;
        typedResponse.oldValue = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        typedResponse.newValue = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        return typedResponse;
    }

    public Flowable<ModeChangedEventResponse> modeChangedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getModeChangedEventFromLog(log));
    }

    public Flowable<ModeChangedEventResponse> modeChangedEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(MODECHANGED_EVENT));
        return modeChangedEventFlowable(filter);
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

    public static List<RelayerAddedEventResponse> getRelayerAddedEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(RELAYERADDED_EVENT, transactionReceipt);
        ArrayList<RelayerAddedEventResponse> responses = new ArrayList<RelayerAddedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            RelayerAddedEventResponse typedResponse = new RelayerAddedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.relayer = (String) eventValues.getIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static RelayerAddedEventResponse getRelayerAddedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(RELAYERADDED_EVENT, log);
        RelayerAddedEventResponse typedResponse = new RelayerAddedEventResponse();
        typedResponse.log = log;
        typedResponse.relayer = (String) eventValues.getIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<RelayerAddedEventResponse> relayerAddedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getRelayerAddedEventFromLog(log));
    }

    public Flowable<RelayerAddedEventResponse> relayerAddedEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(RELAYERADDED_EVENT));
        return relayerAddedEventFlowable(filter);
    }

    public static List<RelayerRemovedEventResponse> getRelayerRemovedEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(RELAYERREMOVED_EVENT, transactionReceipt);
        ArrayList<RelayerRemovedEventResponse> responses = new ArrayList<RelayerRemovedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            RelayerRemovedEventResponse typedResponse = new RelayerRemovedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.relayer = (String) eventValues.getIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static RelayerRemovedEventResponse getRelayerRemovedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(RELAYERREMOVED_EVENT, log);
        RelayerRemovedEventResponse typedResponse = new RelayerRemovedEventResponse();
        typedResponse.log = log;
        typedResponse.relayer = (String) eventValues.getIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<RelayerRemovedEventResponse> relayerRemovedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getRelayerRemovedEventFromLog(log));
    }

    public Flowable<RelayerRemovedEventResponse> relayerRemovedEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(RELAYERREMOVED_EVENT));
        return relayerRemovedEventFlowable(filter);
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

    public static List<RollupInitializedEventResponse> getRollupInitializedEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(ROLLUPINITIALIZED_EVENT, transactionReceipt);
        ArrayList<RollupInitializedEventResponse> responses = new ArrayList<RollupInitializedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            RollupInitializedEventResponse typedResponse = new RollupInitializedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.chainId = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.zkVerifier = (String) eventValues.getNonIndexedValues().get(1).getValue();
            typedResponse.teeVerifier = (String) eventValues.getNonIndexedValues().get(2).getValue();
            typedResponse.l1MailBox = (String) eventValues.getNonIndexedValues().get(3).getValue();
            typedResponse.maxTxsInChunk = (BigInteger) eventValues.getNonIndexedValues().get(4).getValue();
            typedResponse.maxBlockInChunk = (BigInteger) eventValues.getNonIndexedValues().get(5).getValue();
            typedResponse.maxCallDataInChunk = (BigInteger) eventValues.getNonIndexedValues().get(6).getValue();
            typedResponse.maxZkCircleInChunk = (BigInteger) eventValues.getNonIndexedValues().get(7).getValue();
            typedResponse.l1BlobNumberLimit = (BigInteger) eventValues.getNonIndexedValues().get(8).getValue();
            typedResponse.rollupTimeLimit = (BigInteger) eventValues.getNonIndexedValues().get(9).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static RollupInitializedEventResponse getRollupInitializedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(ROLLUPINITIALIZED_EVENT, log);
        RollupInitializedEventResponse typedResponse = new RollupInitializedEventResponse();
        typedResponse.log = log;
        typedResponse.chainId = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        typedResponse.zkVerifier = (String) eventValues.getNonIndexedValues().get(1).getValue();
        typedResponse.teeVerifier = (String) eventValues.getNonIndexedValues().get(2).getValue();
        typedResponse.l1MailBox = (String) eventValues.getNonIndexedValues().get(3).getValue();
        typedResponse.maxTxsInChunk = (BigInteger) eventValues.getNonIndexedValues().get(4).getValue();
        typedResponse.maxBlockInChunk = (BigInteger) eventValues.getNonIndexedValues().get(5).getValue();
        typedResponse.maxCallDataInChunk = (BigInteger) eventValues.getNonIndexedValues().get(6).getValue();
        typedResponse.maxZkCircleInChunk = (BigInteger) eventValues.getNonIndexedValues().get(7).getValue();
        typedResponse.l1BlobNumberLimit = (BigInteger) eventValues.getNonIndexedValues().get(8).getValue();
        typedResponse.rollupTimeLimit = (BigInteger) eventValues.getNonIndexedValues().get(9).getValue();
        return typedResponse;
    }

    public Flowable<RollupInitializedEventResponse> rollupInitializedEventFlowable(
            EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getRollupInitializedEventFromLog(log));
    }

    public Flowable<RollupInitializedEventResponse> rollupInitializedEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(ROLLUPINITIALIZED_EVENT));
        return rollupInitializedEventFlowable(filter);
    }

    public static List<RollupTimeLimitChangedEventResponse> getRollupTimeLimitChangedEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(ROLLUPTIMELIMITCHANGED_EVENT, transactionReceipt);
        ArrayList<RollupTimeLimitChangedEventResponse> responses = new ArrayList<RollupTimeLimitChangedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            RollupTimeLimitChangedEventResponse typedResponse = new RollupTimeLimitChangedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.oldValue = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.newValue = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static RollupTimeLimitChangedEventResponse getRollupTimeLimitChangedEventFromLog(
            Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(ROLLUPTIMELIMITCHANGED_EVENT, log);
        RollupTimeLimitChangedEventResponse typedResponse = new RollupTimeLimitChangedEventResponse();
        typedResponse.log = log;
        typedResponse.oldValue = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        typedResponse.newValue = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        return typedResponse;
    }

    public Flowable<RollupTimeLimitChangedEventResponse> rollupTimeLimitChangedEventFlowable(
            EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getRollupTimeLimitChangedEventFromLog(log));
    }

    public Flowable<RollupTimeLimitChangedEventResponse> rollupTimeLimitChangedEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(ROLLUPTIMELIMITCHANGED_EVENT));
        return rollupTimeLimitChangedEventFlowable(filter);
    }

    public static List<TeeVerifierChangedEventResponse> getTeeVerifierChangedEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(TEEVERIFIERCHANGED_EVENT, transactionReceipt);
        ArrayList<TeeVerifierChangedEventResponse> responses = new ArrayList<TeeVerifierChangedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            TeeVerifierChangedEventResponse typedResponse = new TeeVerifierChangedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.oldVerifier = (String) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.newVerifier = (String) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static TeeVerifierChangedEventResponse getTeeVerifierChangedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(TEEVERIFIERCHANGED_EVENT, log);
        TeeVerifierChangedEventResponse typedResponse = new TeeVerifierChangedEventResponse();
        typedResponse.log = log;
        typedResponse.oldVerifier = (String) eventValues.getNonIndexedValues().get(0).getValue();
        typedResponse.newVerifier = (String) eventValues.getNonIndexedValues().get(1).getValue();
        return typedResponse;
    }

    public Flowable<TeeVerifierChangedEventResponse> teeVerifierChangedEventFlowable(
            EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getTeeVerifierChangedEventFromLog(log));
    }

    public Flowable<TeeVerifierChangedEventResponse> teeVerifierChangedEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(TEEVERIFIERCHANGED_EVENT));
        return teeVerifierChangedEventFlowable(filter);
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

    public static List<ZkVerificationStartBatchAndZkLastVerifiedBatchChangedEventResponse> getZkVerificationStartBatchAndZkLastVerifiedBatchChangedEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(ZKVERIFICATIONSTARTBATCHANDZKLASTVERIFIEDBATCHCHANGED_EVENT, transactionReceipt);
        ArrayList<ZkVerificationStartBatchAndZkLastVerifiedBatchChangedEventResponse> responses = new ArrayList<ZkVerificationStartBatchAndZkLastVerifiedBatchChangedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            ZkVerificationStartBatchAndZkLastVerifiedBatchChangedEventResponse typedResponse = new ZkVerificationStartBatchAndZkLastVerifiedBatchChangedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.zkVsb = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.zkLastvb = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static ZkVerificationStartBatchAndZkLastVerifiedBatchChangedEventResponse getZkVerificationStartBatchAndZkLastVerifiedBatchChangedEventFromLog(
            Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(ZKVERIFICATIONSTARTBATCHANDZKLASTVERIFIEDBATCHCHANGED_EVENT, log);
        ZkVerificationStartBatchAndZkLastVerifiedBatchChangedEventResponse typedResponse = new ZkVerificationStartBatchAndZkLastVerifiedBatchChangedEventResponse();
        typedResponse.log = log;
        typedResponse.zkVsb = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        typedResponse.zkLastvb = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        return typedResponse;
    }

    public Flowable<ZkVerificationStartBatchAndZkLastVerifiedBatchChangedEventResponse> zkVerificationStartBatchAndZkLastVerifiedBatchChangedEventFlowable(
            EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getZkVerificationStartBatchAndZkLastVerifiedBatchChangedEventFromLog(log));
    }

    public Flowable<ZkVerificationStartBatchAndZkLastVerifiedBatchChangedEventResponse> zkVerificationStartBatchAndZkLastVerifiedBatchChangedEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(ZKVERIFICATIONSTARTBATCHANDZKLASTVERIFIEDBATCHCHANGED_EVENT));
        return zkVerificationStartBatchAndZkLastVerifiedBatchChangedEventFlowable(filter);
    }

    public static List<ZkVerifierChangedEventResponse> getZkVerifierChangedEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(ZKVERIFIERCHANGED_EVENT, transactionReceipt);
        ArrayList<ZkVerifierChangedEventResponse> responses = new ArrayList<ZkVerifierChangedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            ZkVerifierChangedEventResponse typedResponse = new ZkVerifierChangedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.oldVerifier = (String) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.newVerifier = (String) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static ZkVerifierChangedEventResponse getZkVerifierChangedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(ZKVERIFIERCHANGED_EVENT, log);
        ZkVerifierChangedEventResponse typedResponse = new ZkVerifierChangedEventResponse();
        typedResponse.log = log;
        typedResponse.oldVerifier = (String) eventValues.getNonIndexedValues().get(0).getValue();
        typedResponse.newVerifier = (String) eventValues.getNonIndexedValues().get(1).getValue();
        return typedResponse;
    }

    public Flowable<ZkVerifierChangedEventResponse> zkVerifierChangedEventFlowable(
            EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getZkVerifierChangedEventFromLog(log));
    }

    public Flowable<ZkVerifierChangedEventResponse> zkVerifierChangedEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(ZKVERIFIERCHANGED_EVENT));
        return zkVerifierChangedEventFlowable(filter);
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

    public static class BatchesRevertedEventResponse extends BaseEventResponse {
        public BigInteger newLastBatchIndex;
    }

    public static class BlobDataHashEventResponse extends BaseEventResponse {
        public byte[] batchDataHash;
    }

    public static class CommitBatchEventResponse extends BaseEventResponse {
        public BigInteger batchIndex;

        public byte[] batchHash;
    }

    public static class DaTypeAcceptedChangedEventResponse extends BaseEventResponse {
        public BigInteger oldValue;

        public BigInteger newValue;
    }

    public static class InitializedEventResponse extends BaseEventResponse {
        public BigInteger version;
    }

    public static class L1BlobNumberLimitChangedEventResponse extends BaseEventResponse {
        public BigInteger oldValue;

        public BigInteger newValue;
    }

    public static class L2ChainIdChangedEventResponse extends BaseEventResponse {
        public BigInteger oldValue;

        public BigInteger newValue;
    }

    public static class MaxBlockInChunkChangedEventResponse extends BaseEventResponse {
        public BigInteger oldValue;

        public BigInteger newValue;
    }

    public static class MaxCallDataInChunkChangedEventResponse extends BaseEventResponse {
        public BigInteger oldValue;

        public BigInteger newValue;
    }

    public static class MaxTxsInChunkChangedEventResponse extends BaseEventResponse {
        public BigInteger oldValue;

        public BigInteger newValue;
    }

    public static class ModeChangedEventResponse extends BaseEventResponse {
        public BigInteger oldValue;

        public BigInteger newValue;
    }

    public static class OwnershipTransferredEventResponse extends BaseEventResponse {
        public String previousOwner;

        public String newOwner;
    }

    public static class PausedEventResponse extends BaseEventResponse {
        public String account;
    }

    public static class RelayerAddedEventResponse extends BaseEventResponse {
        public String relayer;
    }

    public static class RelayerRemovedEventResponse extends BaseEventResponse {
        public String relayer;
    }

    public static class RevertBatchEventResponse extends BaseEventResponse {
        public BigInteger batchIndex;
    }

    public static class RollupInitializedEventResponse extends BaseEventResponse {
        public BigInteger chainId;

        public String zkVerifier;

        public String teeVerifier;

        public String l1MailBox;

        public BigInteger maxTxsInChunk;

        public BigInteger maxBlockInChunk;

        public BigInteger maxCallDataInChunk;

        public BigInteger maxZkCircleInChunk;

        public BigInteger l1BlobNumberLimit;

        public BigInteger rollupTimeLimit;
    }

    public static class RollupTimeLimitChangedEventResponse extends BaseEventResponse {
        public BigInteger oldValue;

        public BigInteger newValue;
    }

    public static class TeeVerifierChangedEventResponse extends BaseEventResponse {
        public String oldVerifier;

        public String newVerifier;
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

    public static class ZkVerificationStartBatchAndZkLastVerifiedBatchChangedEventResponse extends BaseEventResponse {
        public BigInteger zkVsb;

        public BigInteger zkLastvb;
    }

    public static class ZkVerifierChangedEventResponse extends BaseEventResponse {
        public String oldVerifier;

        public String newVerifier;
    }
}
