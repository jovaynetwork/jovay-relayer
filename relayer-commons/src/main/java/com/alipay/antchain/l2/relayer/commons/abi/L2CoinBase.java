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
public class L2CoinBase extends Contract {
    public static final String BINARY = "Bin file was not provided";

    public static final String FUNC_ADDWHITEADDRESS = "addWhiteAddress";

    public static final String FUNC_ADDWITHDRAWER = "addWithdrawer";

    public static final String FUNC_INITIALIZE = "initialize";

    public static final String FUNC_INITIALIZEV2 = "initializeV2";

    public static final String FUNC_ISWITHDRAWER = "isWithdrawer";

    public static final String FUNC_L2ETHBRIDGE = "l2EthBridge";

    public static final String FUNC_OWNER = "owner";

    public static final String FUNC_PAUSE = "pause";

    public static final String FUNC_PAUSED = "paused";

    public static final String FUNC_REMOVEWHITEADDRESS = "removeWhiteAddress";

    public static final String FUNC_REMOVEWITHDRAWER = "removeWithdrawer";

    public static final String FUNC_RENOUNCEOWNERSHIP = "renounceOwnership";

    public static final String FUNC_SETL2ETHBRIDGE = "setL2EthBridge";

    public static final String FUNC_TRANSFEROWNERSHIP = "transferOwnership";

    public static final String FUNC_UNPAUSE = "unpause";

    public static final String FUNC_WHITELISTONL1 = "whiteListOnL1";

    public static final String FUNC_WITHDRAW = "withdraw";

    public static final String FUNC_WITHDRAWALL = "withdrawAll";

    public static final Event ADDWHITEADDRESS_EVENT = new Event("AddWhiteAddress", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}));
    ;

    public static final Event ADDWITHDRAWER_EVENT = new Event("AddWithdrawer", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}));
    ;

    public static final Event COINBASEWITHDRAW_EVENT = new Event("CoinBaseWithdraw", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Uint256>(true) {}));
    ;

    public static final Event INITIALIZED1_EVENT = new Event("Initialized", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint8>() {}));
    ;

    public static final Event INITIALIZED_EVENT = new Event("Initialized", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}));
    ;

    public static final Event INITIALIZEDV2_EVENT = new Event("InitializedV2", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint8>() {}));
    ;

    public static final Event OWNERSHIPTRANSFERRED_EVENT = new Event("OwnershipTransferred", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}));
    ;

    public static final Event PAUSED_EVENT = new Event("Paused", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
    ;

    public static final Event REMOVEWHITEADDRESS_EVENT = new Event("RemoveWhiteAddress", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}));
    ;

    public static final Event REMOVEWITHDRAWER_EVENT = new Event("RemoveWithdrawer", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}));
    ;

    public static final Event SETL2ETHBRIDGE_EVENT = new Event("SetL2EthBridge", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}));
    ;

    public static final Event UNPAUSED_EVENT = new Event("Unpaused", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
    ;

    @Deprecated
    protected L2CoinBase(String contractAddress, Web3j web3j, Credentials credentials,
            BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected L2CoinBase(String contractAddress, Web3j web3j, Credentials credentials,
            ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected L2CoinBase(String contractAddress, Web3j web3j, TransactionManager transactionManager,
            BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected L2CoinBase(String contractAddress, Web3j web3j, TransactionManager transactionManager,
            ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public RemoteFunctionCall<TransactionReceipt> addWhiteAddress(String _whiteAddress) {
        final Function function = new Function(
                FUNC_ADDWHITEADDRESS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, _whiteAddress)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> addWithdrawer(String _newWithdrawer) {
        final Function function = new Function(
                FUNC_ADDWITHDRAWER, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, _newWithdrawer)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> initialize(String _l2EthBridge) {
        final Function function = new Function(
                FUNC_INITIALIZE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, _l2EthBridge)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> initializeV2() {
        final Function function = new Function(
                FUNC_INITIALIZEV2, 
                Arrays.<Type>asList(), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<Boolean> isWithdrawer(String withdrawerAddress) {
        final Function function = new Function(FUNC_ISWITHDRAWER, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, withdrawerAddress)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteFunctionCall<String> l2EthBridge() {
        final Function function = new Function(FUNC_L2ETHBRIDGE, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<String> owner() {
        final Function function = new Function(FUNC_OWNER, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<TransactionReceipt> pause() {
        final Function function = new Function(
                FUNC_PAUSE, 
                Arrays.<Type>asList(), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<Boolean> paused() {
        final Function function = new Function(FUNC_PAUSED, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteFunctionCall<TransactionReceipt> removeWhiteAddress(String _whiteAddress) {
        final Function function = new Function(
                FUNC_REMOVEWHITEADDRESS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, _whiteAddress)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> removeWithdrawer(String _oldWithdrawer) {
        final Function function = new Function(
                FUNC_REMOVEWITHDRAWER, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, _oldWithdrawer)), 
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

    public RemoteFunctionCall<TransactionReceipt> setL2EthBridge(String _newL2EthBridge) {
        final Function function = new Function(
                FUNC_SETL2ETHBRIDGE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, _newL2EthBridge)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> transferOwnership(String newOwner) {
        final Function function = new Function(
                FUNC_TRANSFEROWNERSHIP, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, newOwner)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> unpause() {
        final Function function = new Function(
                FUNC_UNPAUSE, 
                Arrays.<Type>asList(), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<Boolean> whiteListOnL1(String whiteAddress) {
        final Function function = new Function(FUNC_WHITELISTONL1, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, whiteAddress)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteFunctionCall<TransactionReceipt> withdraw(String _target, BigInteger _amount) {
        final Function function = new Function(
                FUNC_WITHDRAW, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, _target), 
                new org.web3j.abi.datatypes.generated.Uint256(_amount)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> withdrawAll(String _target) {
        final Function function = new Function(
                FUNC_WITHDRAWALL, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, _target)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public static List<AddWhiteAddressEventResponse> getAddWhiteAddressEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(ADDWHITEADDRESS_EVENT, transactionReceipt);
        ArrayList<AddWhiteAddressEventResponse> responses = new ArrayList<AddWhiteAddressEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            AddWhiteAddressEventResponse typedResponse = new AddWhiteAddressEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.whiteAddress = (String) eventValues.getIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static AddWhiteAddressEventResponse getAddWhiteAddressEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(ADDWHITEADDRESS_EVENT, log);
        AddWhiteAddressEventResponse typedResponse = new AddWhiteAddressEventResponse();
        typedResponse.log = log;
        typedResponse.whiteAddress = (String) eventValues.getIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<AddWhiteAddressEventResponse> addWhiteAddressEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getAddWhiteAddressEventFromLog(log));
    }

    public Flowable<AddWhiteAddressEventResponse> addWhiteAddressEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(ADDWHITEADDRESS_EVENT));
        return addWhiteAddressEventFlowable(filter);
    }

    public static List<AddWithdrawerEventResponse> getAddWithdrawerEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(ADDWITHDRAWER_EVENT, transactionReceipt);
        ArrayList<AddWithdrawerEventResponse> responses = new ArrayList<AddWithdrawerEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            AddWithdrawerEventResponse typedResponse = new AddWithdrawerEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.newWithdrawer = (String) eventValues.getIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static AddWithdrawerEventResponse getAddWithdrawerEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(ADDWITHDRAWER_EVENT, log);
        AddWithdrawerEventResponse typedResponse = new AddWithdrawerEventResponse();
        typedResponse.log = log;
        typedResponse.newWithdrawer = (String) eventValues.getIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<AddWithdrawerEventResponse> addWithdrawerEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getAddWithdrawerEventFromLog(log));
    }

    public Flowable<AddWithdrawerEventResponse> addWithdrawerEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(ADDWITHDRAWER_EVENT));
        return addWithdrawerEventFlowable(filter);
    }

    public static List<CoinBaseWithdrawEventResponse> getCoinBaseWithdrawEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(COINBASEWITHDRAW_EVENT, transactionReceipt);
        ArrayList<CoinBaseWithdrawEventResponse> responses = new ArrayList<CoinBaseWithdrawEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            CoinBaseWithdrawEventResponse typedResponse = new CoinBaseWithdrawEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse._target = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.amount = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static CoinBaseWithdrawEventResponse getCoinBaseWithdrawEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(COINBASEWITHDRAW_EVENT, log);
        CoinBaseWithdrawEventResponse typedResponse = new CoinBaseWithdrawEventResponse();
        typedResponse.log = log;
        typedResponse._target = (String) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.amount = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
        return typedResponse;
    }

    public Flowable<CoinBaseWithdrawEventResponse> coinBaseWithdrawEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getCoinBaseWithdrawEventFromLog(log));
    }

    public Flowable<CoinBaseWithdrawEventResponse> coinBaseWithdrawEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(COINBASEWITHDRAW_EVENT));
        return coinBaseWithdrawEventFlowable(filter);
    }

    public static List<Initialized1EventResponse> getInitialized1Events(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(INITIALIZED1_EVENT, transactionReceipt);
        ArrayList<Initialized1EventResponse> responses = new ArrayList<Initialized1EventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            Initialized1EventResponse typedResponse = new Initialized1EventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.version = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static Initialized1EventResponse getInitialized1EventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(INITIALIZED1_EVENT, log);
        Initialized1EventResponse typedResponse = new Initialized1EventResponse();
        typedResponse.log = log;
        typedResponse.version = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<Initialized1EventResponse> initialized1EventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getInitialized1EventFromLog(log));
    }

    public Flowable<Initialized1EventResponse> initialized1EventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(INITIALIZED1_EVENT));
        return initialized1EventFlowable(filter);
    }

    public static List<InitializedEventResponse> getInitializedEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(INITIALIZED_EVENT, transactionReceipt);
        ArrayList<InitializedEventResponse> responses = new ArrayList<InitializedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            InitializedEventResponse typedResponse = new InitializedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.l2EthBridge = (String) eventValues.getIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static InitializedEventResponse getInitializedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(INITIALIZED_EVENT, log);
        InitializedEventResponse typedResponse = new InitializedEventResponse();
        typedResponse.log = log;
        typedResponse.l2EthBridge = (String) eventValues.getIndexedValues().get(0).getValue();
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

    public static List<InitializedV2EventResponse> getInitializedV2Events(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(INITIALIZEDV2_EVENT, transactionReceipt);
        ArrayList<InitializedV2EventResponse> responses = new ArrayList<InitializedV2EventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            InitializedV2EventResponse typedResponse = new InitializedV2EventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.version = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static InitializedV2EventResponse getInitializedV2EventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(INITIALIZEDV2_EVENT, log);
        InitializedV2EventResponse typedResponse = new InitializedV2EventResponse();
        typedResponse.log = log;
        typedResponse.version = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<InitializedV2EventResponse> initializedV2EventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getInitializedV2EventFromLog(log));
    }

    public Flowable<InitializedV2EventResponse> initializedV2EventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(INITIALIZEDV2_EVENT));
        return initializedV2EventFlowable(filter);
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

    public static List<RemoveWhiteAddressEventResponse> getRemoveWhiteAddressEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(REMOVEWHITEADDRESS_EVENT, transactionReceipt);
        ArrayList<RemoveWhiteAddressEventResponse> responses = new ArrayList<RemoveWhiteAddressEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            RemoveWhiteAddressEventResponse typedResponse = new RemoveWhiteAddressEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.whiteAddress = (String) eventValues.getIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static RemoveWhiteAddressEventResponse getRemoveWhiteAddressEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(REMOVEWHITEADDRESS_EVENT, log);
        RemoveWhiteAddressEventResponse typedResponse = new RemoveWhiteAddressEventResponse();
        typedResponse.log = log;
        typedResponse.whiteAddress = (String) eventValues.getIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<RemoveWhiteAddressEventResponse> removeWhiteAddressEventFlowable(
            EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getRemoveWhiteAddressEventFromLog(log));
    }

    public Flowable<RemoveWhiteAddressEventResponse> removeWhiteAddressEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(REMOVEWHITEADDRESS_EVENT));
        return removeWhiteAddressEventFlowable(filter);
    }

    public static List<RemoveWithdrawerEventResponse> getRemoveWithdrawerEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(REMOVEWITHDRAWER_EVENT, transactionReceipt);
        ArrayList<RemoveWithdrawerEventResponse> responses = new ArrayList<RemoveWithdrawerEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            RemoveWithdrawerEventResponse typedResponse = new RemoveWithdrawerEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.oldWithdrawer = (String) eventValues.getIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static RemoveWithdrawerEventResponse getRemoveWithdrawerEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(REMOVEWITHDRAWER_EVENT, log);
        RemoveWithdrawerEventResponse typedResponse = new RemoveWithdrawerEventResponse();
        typedResponse.log = log;
        typedResponse.oldWithdrawer = (String) eventValues.getIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<RemoveWithdrawerEventResponse> removeWithdrawerEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getRemoveWithdrawerEventFromLog(log));
    }

    public Flowable<RemoveWithdrawerEventResponse> removeWithdrawerEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(REMOVEWITHDRAWER_EVENT));
        return removeWithdrawerEventFlowable(filter);
    }

    public static List<SetL2EthBridgeEventResponse> getSetL2EthBridgeEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(SETL2ETHBRIDGE_EVENT, transactionReceipt);
        ArrayList<SetL2EthBridgeEventResponse> responses = new ArrayList<SetL2EthBridgeEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            SetL2EthBridgeEventResponse typedResponse = new SetL2EthBridgeEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.l2EthBridge = (String) eventValues.getIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static SetL2EthBridgeEventResponse getSetL2EthBridgeEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(SETL2ETHBRIDGE_EVENT, log);
        SetL2EthBridgeEventResponse typedResponse = new SetL2EthBridgeEventResponse();
        typedResponse.log = log;
        typedResponse.l2EthBridge = (String) eventValues.getIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<SetL2EthBridgeEventResponse> setL2EthBridgeEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getSetL2EthBridgeEventFromLog(log));
    }

    public Flowable<SetL2EthBridgeEventResponse> setL2EthBridgeEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(SETL2ETHBRIDGE_EVENT));
        return setL2EthBridgeEventFlowable(filter);
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

    @Deprecated
    public static L2CoinBase load(String contractAddress, Web3j web3j, Credentials credentials,
            BigInteger gasPrice, BigInteger gasLimit) {
        return new L2CoinBase(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static L2CoinBase load(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new L2CoinBase(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static L2CoinBase load(String contractAddress, Web3j web3j, Credentials credentials,
            ContractGasProvider contractGasProvider) {
        return new L2CoinBase(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static L2CoinBase load(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new L2CoinBase(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static class AddWhiteAddressEventResponse extends BaseEventResponse {
        public String whiteAddress;
    }

    public static class AddWithdrawerEventResponse extends BaseEventResponse {
        public String newWithdrawer;
    }

    public static class CoinBaseWithdrawEventResponse extends BaseEventResponse {
        public String _target;

        public BigInteger amount;
    }

    public static class Initialized1EventResponse extends BaseEventResponse {
        public BigInteger version;
    }

    public static class InitializedEventResponse extends BaseEventResponse {
        public String l2EthBridge;
    }

    public static class InitializedV2EventResponse extends BaseEventResponse {
        public BigInteger version;
    }

    public static class OwnershipTransferredEventResponse extends BaseEventResponse {
        public String previousOwner;

        public String newOwner;
    }

    public static class PausedEventResponse extends BaseEventResponse {
        public String account;
    }

    public static class RemoveWhiteAddressEventResponse extends BaseEventResponse {
        public String whiteAddress;
    }

    public static class RemoveWithdrawerEventResponse extends BaseEventResponse {
        public String oldWithdrawer;
    }

    public static class SetL2EthBridgeEventResponse extends BaseEventResponse {
        public String l2EthBridge;
    }

    public static class UnpausedEventResponse extends BaseEventResponse {
        public String account;
    }
}
