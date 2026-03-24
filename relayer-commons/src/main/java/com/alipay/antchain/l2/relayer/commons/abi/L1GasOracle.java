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
import org.web3j.abi.datatypes.generated.Uint256;
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
public class L1GasOracle extends Contract {
    public static final String BINARY = "Bin file was not provided";

    public static final String FUNC_ADDRELAYER = "addRelayer";

    public static final String FUNC_BASEFEESCALA = "baseFeeScala";

    public static final String FUNC_BLOBBASEFEESCALA = "blobBaseFeeScala";

    public static final String FUNC_GETTXL1FEE = "getTxL1Fee";

    public static final String FUNC_INITIALIZE = "initialize";

    public static final String FUNC_ISRELAYER = "isRelayer";

    public static final String FUNC_L1FEEPERBYTE = "l1FeePerByte";

    public static final String FUNC_L1PROFIT = "l1Profit";

    public static final String FUNC_LASTBATCHBYTELENGTH = "lastBatchByteLength";

    public static final String FUNC_LASTBATCHDAFEE = "lastBatchDaFee";

    public static final String FUNC_LASTBATCHEXECFEE = "lastBatchExecFee";

    public static final String FUNC_MAXL1BLOBGASUSEDLIMIT = "maxL1BlobGasUsedLimit";

    public static final String FUNC_MAXL1EXECGASUSEDLIMIT = "maxL1ExecGasUsedLimit";

    public static final String FUNC_OWNER = "owner";

    public static final String FUNC_REMOVERELAYER = "removeRelayer";

    public static final String FUNC_RENOUNCEOWNERSHIP = "renounceOwnership";

    public static final String FUNC_SETBLOBBASEFEESCALAANDTXFEESCALA = "setBlobBaseFeeScalaAndTxFeeScala";

    public static final String FUNC_SETL1PROFIT = "setL1Profit";

    public static final String FUNC_SETMAXL1BLOBGASUSEDLIMIT = "setMaxL1BlobGasUsedLimit";

    public static final String FUNC_SETMAXL1EXECGASUSEDLIMIT = "setMaxL1ExecGasUsedLimit";

    public static final String FUNC_SETNEWBATCHBLOBFEEANDTXFEE = "setNewBatchBlobFeeAndTxFee";

    public static final String FUNC_SETTOTALSCALA = "setTotalScala";

    public static final String FUNC_TOTALSCALA = "totalScala";

    public static final String FUNC_TRANSFEROWNERSHIP = "transferOwnership";

    public static final Event ADDRELAYER_EVENT = new Event("AddRelayer", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
    ;

    public static final Event INITIALIZED_EVENT = new Event("Initialized", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint8>() {}));
    ;

    public static final Event OWNERSHIPTRANSFERRED_EVENT = new Event("OwnershipTransferred", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}));
    ;

    public static final Event REMOVERELAYER_EVENT = new Event("RemoveRelayer", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
    ;

    public static final Event SETBLOBBASEFEESCALAANDTXFEESCALA_EVENT = new Event("SetBlobBaseFeeScalaAndTxFeeScala", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event SETL1PROFIT_EVENT = new Event("SetL1Profit", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
    ;

    public static final Event SETMAXL1BLOBGASUSEDLIMIT_EVENT = new Event("SetMaxL1BlobGasUsedLimit", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
    ;

    public static final Event SETMAXL1EXECGASUSEDLIMIT_EVENT = new Event("SetMaxL1ExecGasUsedLimit", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
    ;

    public static final Event SETNEWBATCHBLOBFEEANDTXFEE_EVENT = new Event("SetNewBatchBlobFeeAndTxFee", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event SETTOTALSCALA_EVENT = new Event("SetTotalScala", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
    ;

    @Deprecated
    protected L1GasOracle(String contractAddress, Web3j web3j, Credentials credentials,
            BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected L1GasOracle(String contractAddress, Web3j web3j, Credentials credentials,
            ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected L1GasOracle(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected L1GasOracle(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public RemoteFunctionCall<TransactionReceipt> addRelayer(String _newRelayer) {
        final Function function = new Function(
                FUNC_ADDRELAYER, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, _newRelayer)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<BigInteger> baseFeeScala() {
        final Function function = new Function(FUNC_BASEFEESCALA, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<BigInteger> blobBaseFeeScala() {
        final Function function = new Function(FUNC_BLOBBASEFEESCALA, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<BigInteger> getTxL1Fee(BigInteger txLength) {
        final Function function = new Function(FUNC_GETTXL1FEE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(txLength)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<TransactionReceipt> initialize(BigInteger _lastBatchDaFee,
            BigInteger _lastBatchExecFee, BigInteger _lastBatchByteLength) {
        final Function function = new Function(
                FUNC_INITIALIZE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(_lastBatchDaFee), 
                new org.web3j.abi.datatypes.generated.Uint256(_lastBatchExecFee), 
                new org.web3j.abi.datatypes.generated.Uint256(_lastBatchByteLength)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<Boolean> isRelayer(String param0) {
        final Function function = new Function(FUNC_ISRELAYER, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, param0)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteFunctionCall<BigInteger> l1FeePerByte() {
        final Function function = new Function(FUNC_L1FEEPERBYTE, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<BigInteger> l1Profit() {
        final Function function = new Function(FUNC_L1PROFIT, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<BigInteger> lastBatchByteLength() {
        final Function function = new Function(FUNC_LASTBATCHBYTELENGTH, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<BigInteger> lastBatchDaFee() {
        final Function function = new Function(FUNC_LASTBATCHDAFEE, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<BigInteger> lastBatchExecFee() {
        final Function function = new Function(FUNC_LASTBATCHEXECFEE, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<BigInteger> maxL1BlobGasUsedLimit() {
        final Function function = new Function(FUNC_MAXL1BLOBGASUSEDLIMIT, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<BigInteger> maxL1ExecGasUsedLimit() {
        final Function function = new Function(FUNC_MAXL1EXECGASUSEDLIMIT, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<String> owner() {
        final Function function = new Function(FUNC_OWNER, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<TransactionReceipt> removeRelayer(String _oldRelayer) {
        final Function function = new Function(
                FUNC_REMOVERELAYER, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, _oldRelayer)), 
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

    public RemoteFunctionCall<TransactionReceipt> setBlobBaseFeeScalaAndTxFeeScala(
            BigInteger _baseFeeScala, BigInteger _blobBaseFeeScala) {
        final Function function = new Function(
                FUNC_SETBLOBBASEFEESCALAANDTXFEESCALA, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(_baseFeeScala), 
                new org.web3j.abi.datatypes.generated.Uint256(_blobBaseFeeScala)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> setL1Profit(BigInteger _l1Profit) {
        final Function function = new Function(
                FUNC_SETL1PROFIT, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(_l1Profit)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> setMaxL1BlobGasUsedLimit(
            BigInteger _maxL1BlobGasUsedLimit) {
        final Function function = new Function(
                FUNC_SETMAXL1BLOBGASUSEDLIMIT, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(_maxL1BlobGasUsedLimit)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> setMaxL1ExecGasUsedLimit(
            BigInteger _maxL1ExecGasUsedLimit) {
        final Function function = new Function(
                FUNC_SETMAXL1EXECGASUSEDLIMIT, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(_maxL1ExecGasUsedLimit)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> setNewBatchBlobFeeAndTxFee(
            BigInteger _lastBatchDaFee, BigInteger _lastBatchExecFee,
            BigInteger _lastBatchByteLength) {
        final Function function = new Function(
                FUNC_SETNEWBATCHBLOBFEEANDTXFEE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(_lastBatchDaFee), 
                new org.web3j.abi.datatypes.generated.Uint256(_lastBatchExecFee), 
                new org.web3j.abi.datatypes.generated.Uint256(_lastBatchByteLength)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> setTotalScala(BigInteger _totalScala) {
        final Function function = new Function(
                FUNC_SETTOTALSCALA, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(_totalScala)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<BigInteger> totalScala() {
        final Function function = new Function(FUNC_TOTALSCALA, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<TransactionReceipt> transferOwnership(String newOwner) {
        final Function function = new Function(
                FUNC_TRANSFEROWNERSHIP, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, newOwner)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public static List<AddRelayerEventResponse> getAddRelayerEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(ADDRELAYER_EVENT, transactionReceipt);
        ArrayList<AddRelayerEventResponse> responses = new ArrayList<AddRelayerEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            AddRelayerEventResponse typedResponse = new AddRelayerEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.relayer = (String) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static AddRelayerEventResponse getAddRelayerEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(ADDRELAYER_EVENT, log);
        AddRelayerEventResponse typedResponse = new AddRelayerEventResponse();
        typedResponse.log = log;
        typedResponse.relayer = (String) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<AddRelayerEventResponse> addRelayerEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getAddRelayerEventFromLog(log));
    }

    public Flowable<AddRelayerEventResponse> addRelayerEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(ADDRELAYER_EVENT));
        return addRelayerEventFlowable(filter);
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

    public static List<RemoveRelayerEventResponse> getRemoveRelayerEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(REMOVERELAYER_EVENT, transactionReceipt);
        ArrayList<RemoveRelayerEventResponse> responses = new ArrayList<RemoveRelayerEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            RemoveRelayerEventResponse typedResponse = new RemoveRelayerEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.oldRelayer = (String) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static RemoveRelayerEventResponse getRemoveRelayerEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(REMOVERELAYER_EVENT, log);
        RemoveRelayerEventResponse typedResponse = new RemoveRelayerEventResponse();
        typedResponse.log = log;
        typedResponse.oldRelayer = (String) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<RemoveRelayerEventResponse> removeRelayerEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getRemoveRelayerEventFromLog(log));
    }

    public Flowable<RemoveRelayerEventResponse> removeRelayerEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(REMOVERELAYER_EVENT));
        return removeRelayerEventFlowable(filter);
    }

    public static List<SetBlobBaseFeeScalaAndTxFeeScalaEventResponse> getSetBlobBaseFeeScalaAndTxFeeScalaEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(SETBLOBBASEFEESCALAANDTXFEESCALA_EVENT, transactionReceipt);
        ArrayList<SetBlobBaseFeeScalaAndTxFeeScalaEventResponse> responses = new ArrayList<SetBlobBaseFeeScalaAndTxFeeScalaEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            SetBlobBaseFeeScalaAndTxFeeScalaEventResponse typedResponse = new SetBlobBaseFeeScalaAndTxFeeScalaEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse._baseFeeScala = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse._blobBaseFeeScala = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static SetBlobBaseFeeScalaAndTxFeeScalaEventResponse getSetBlobBaseFeeScalaAndTxFeeScalaEventFromLog(
            Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(SETBLOBBASEFEESCALAANDTXFEESCALA_EVENT, log);
        SetBlobBaseFeeScalaAndTxFeeScalaEventResponse typedResponse = new SetBlobBaseFeeScalaAndTxFeeScalaEventResponse();
        typedResponse.log = log;
        typedResponse._baseFeeScala = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        typedResponse._blobBaseFeeScala = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        return typedResponse;
    }

    public Flowable<SetBlobBaseFeeScalaAndTxFeeScalaEventResponse> setBlobBaseFeeScalaAndTxFeeScalaEventFlowable(
            EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getSetBlobBaseFeeScalaAndTxFeeScalaEventFromLog(log));
    }

    public Flowable<SetBlobBaseFeeScalaAndTxFeeScalaEventResponse> setBlobBaseFeeScalaAndTxFeeScalaEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(SETBLOBBASEFEESCALAANDTXFEESCALA_EVENT));
        return setBlobBaseFeeScalaAndTxFeeScalaEventFlowable(filter);
    }

    public static List<SetL1ProfitEventResponse> getSetL1ProfitEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(SETL1PROFIT_EVENT, transactionReceipt);
        ArrayList<SetL1ProfitEventResponse> responses = new ArrayList<SetL1ProfitEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            SetL1ProfitEventResponse typedResponse = new SetL1ProfitEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse._l1Profit = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static SetL1ProfitEventResponse getSetL1ProfitEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(SETL1PROFIT_EVENT, log);
        SetL1ProfitEventResponse typedResponse = new SetL1ProfitEventResponse();
        typedResponse.log = log;
        typedResponse._l1Profit = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<SetL1ProfitEventResponse> setL1ProfitEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getSetL1ProfitEventFromLog(log));
    }

    public Flowable<SetL1ProfitEventResponse> setL1ProfitEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(SETL1PROFIT_EVENT));
        return setL1ProfitEventFlowable(filter);
    }

    public static List<SetMaxL1BlobGasUsedLimitEventResponse> getSetMaxL1BlobGasUsedLimitEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(SETMAXL1BLOBGASUSEDLIMIT_EVENT, transactionReceipt);
        ArrayList<SetMaxL1BlobGasUsedLimitEventResponse> responses = new ArrayList<SetMaxL1BlobGasUsedLimitEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            SetMaxL1BlobGasUsedLimitEventResponse typedResponse = new SetMaxL1BlobGasUsedLimitEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse._maxL1BlobGasUsedLimit = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static SetMaxL1BlobGasUsedLimitEventResponse getSetMaxL1BlobGasUsedLimitEventFromLog(
            Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(SETMAXL1BLOBGASUSEDLIMIT_EVENT, log);
        SetMaxL1BlobGasUsedLimitEventResponse typedResponse = new SetMaxL1BlobGasUsedLimitEventResponse();
        typedResponse.log = log;
        typedResponse._maxL1BlobGasUsedLimit = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<SetMaxL1BlobGasUsedLimitEventResponse> setMaxL1BlobGasUsedLimitEventFlowable(
            EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getSetMaxL1BlobGasUsedLimitEventFromLog(log));
    }

    public Flowable<SetMaxL1BlobGasUsedLimitEventResponse> setMaxL1BlobGasUsedLimitEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(SETMAXL1BLOBGASUSEDLIMIT_EVENT));
        return setMaxL1BlobGasUsedLimitEventFlowable(filter);
    }

    public static List<SetMaxL1ExecGasUsedLimitEventResponse> getSetMaxL1ExecGasUsedLimitEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(SETMAXL1EXECGASUSEDLIMIT_EVENT, transactionReceipt);
        ArrayList<SetMaxL1ExecGasUsedLimitEventResponse> responses = new ArrayList<SetMaxL1ExecGasUsedLimitEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            SetMaxL1ExecGasUsedLimitEventResponse typedResponse = new SetMaxL1ExecGasUsedLimitEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse._maxL1ExecGasUsedLimit = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static SetMaxL1ExecGasUsedLimitEventResponse getSetMaxL1ExecGasUsedLimitEventFromLog(
            Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(SETMAXL1EXECGASUSEDLIMIT_EVENT, log);
        SetMaxL1ExecGasUsedLimitEventResponse typedResponse = new SetMaxL1ExecGasUsedLimitEventResponse();
        typedResponse.log = log;
        typedResponse._maxL1ExecGasUsedLimit = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<SetMaxL1ExecGasUsedLimitEventResponse> setMaxL1ExecGasUsedLimitEventFlowable(
            EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getSetMaxL1ExecGasUsedLimitEventFromLog(log));
    }

    public Flowable<SetMaxL1ExecGasUsedLimitEventResponse> setMaxL1ExecGasUsedLimitEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(SETMAXL1EXECGASUSEDLIMIT_EVENT));
        return setMaxL1ExecGasUsedLimitEventFlowable(filter);
    }

    public static List<SetNewBatchBlobFeeAndTxFeeEventResponse> getSetNewBatchBlobFeeAndTxFeeEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(SETNEWBATCHBLOBFEEANDTXFEE_EVENT, transactionReceipt);
        ArrayList<SetNewBatchBlobFeeAndTxFeeEventResponse> responses = new ArrayList<SetNewBatchBlobFeeAndTxFeeEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            SetNewBatchBlobFeeAndTxFeeEventResponse typedResponse = new SetNewBatchBlobFeeAndTxFeeEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse._lastBatchDaFee = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse._lastBatchExecFee = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            typedResponse._lastBatchByteLength = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static SetNewBatchBlobFeeAndTxFeeEventResponse getSetNewBatchBlobFeeAndTxFeeEventFromLog(
            Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(SETNEWBATCHBLOBFEEANDTXFEE_EVENT, log);
        SetNewBatchBlobFeeAndTxFeeEventResponse typedResponse = new SetNewBatchBlobFeeAndTxFeeEventResponse();
        typedResponse.log = log;
        typedResponse._lastBatchDaFee = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        typedResponse._lastBatchExecFee = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        typedResponse._lastBatchByteLength = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
        return typedResponse;
    }

    public Flowable<SetNewBatchBlobFeeAndTxFeeEventResponse> setNewBatchBlobFeeAndTxFeeEventFlowable(
            EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getSetNewBatchBlobFeeAndTxFeeEventFromLog(log));
    }

    public Flowable<SetNewBatchBlobFeeAndTxFeeEventResponse> setNewBatchBlobFeeAndTxFeeEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(SETNEWBATCHBLOBFEEANDTXFEE_EVENT));
        return setNewBatchBlobFeeAndTxFeeEventFlowable(filter);
    }

    public static List<SetTotalScalaEventResponse> getSetTotalScalaEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(SETTOTALSCALA_EVENT, transactionReceipt);
        ArrayList<SetTotalScalaEventResponse> responses = new ArrayList<SetTotalScalaEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            SetTotalScalaEventResponse typedResponse = new SetTotalScalaEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse._totalScala = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static SetTotalScalaEventResponse getSetTotalScalaEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(SETTOTALSCALA_EVENT, log);
        SetTotalScalaEventResponse typedResponse = new SetTotalScalaEventResponse();
        typedResponse.log = log;
        typedResponse._totalScala = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<SetTotalScalaEventResponse> setTotalScalaEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getSetTotalScalaEventFromLog(log));
    }

    public Flowable<SetTotalScalaEventResponse> setTotalScalaEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(SETTOTALSCALA_EVENT));
        return setTotalScalaEventFlowable(filter);
    }

    @Deprecated
    public static L1GasOracle load(String contractAddress, Web3j web3j, Credentials credentials,
            BigInteger gasPrice, BigInteger gasLimit) {
        return new L1GasOracle(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static L1GasOracle load(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new L1GasOracle(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static L1GasOracle load(String contractAddress, Web3j web3j, Credentials credentials,
            ContractGasProvider contractGasProvider) {
        return new L1GasOracle(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static L1GasOracle load(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new L1GasOracle(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static class AddRelayerEventResponse extends BaseEventResponse {
        public String relayer;
    }

    public static class InitializedEventResponse extends BaseEventResponse {
        public BigInteger version;
    }

    public static class OwnershipTransferredEventResponse extends BaseEventResponse {
        public String previousOwner;

        public String newOwner;
    }

    public static class RemoveRelayerEventResponse extends BaseEventResponse {
        public String oldRelayer;
    }

    public static class SetBlobBaseFeeScalaAndTxFeeScalaEventResponse extends BaseEventResponse {
        public BigInteger _baseFeeScala;

        public BigInteger _blobBaseFeeScala;
    }

    public static class SetL1ProfitEventResponse extends BaseEventResponse {
        public BigInteger _l1Profit;
    }

    public static class SetMaxL1BlobGasUsedLimitEventResponse extends BaseEventResponse {
        public BigInteger _maxL1BlobGasUsedLimit;
    }

    public static class SetMaxL1ExecGasUsedLimitEventResponse extends BaseEventResponse {
        public BigInteger _maxL1ExecGasUsedLimit;
    }

    public static class SetNewBatchBlobFeeAndTxFeeEventResponse extends BaseEventResponse {
        public BigInteger _lastBatchDaFee;

        public BigInteger _lastBatchExecFee;

        public BigInteger _lastBatchByteLength;
    }

    public static class SetTotalScalaEventResponse extends BaseEventResponse {
        public BigInteger _totalScala;
    }
}
