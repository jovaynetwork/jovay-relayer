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
import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
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
public class IMailBoxBase extends Contract {
    public static final String BINARY = "Bin file was not provided";

    public static final String FUNC_PAUSE = "pause";

    public static final String FUNC_SENDMSG = "sendMsg";

    public static final String FUNC_UNPAUSE = "unpause";

    public static final Event APPENDMSG_EVENT = new Event("AppendMsg", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}, new TypeReference<Bytes32>() {}));
    ;

    public static final Event CLAIMMSG_EVENT = new Event("ClaimMsg", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>(true) {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event RELAYMSGFAILED_EVENT = new Event("RelayMsgFailed", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>(true) {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event RELAYMSGSUCCESS_EVENT = new Event("RelayMsgSuccess", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>(true) {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event RELAYEDMSG_EVENT = new Event("RelayedMsg", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>(true) {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event RELAYEDMSGFAILED_EVENT = new Event("RelayedMsgFailed", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>(true) {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event ROLLINGHASH_EVENT = new Event("RollingHash", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>(true) {}));
    ;

    public static final Event SENTMSG_EVENT = new Event("SentMsg", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<DynamicBytes>() {}, new TypeReference<Uint256>() {}, new TypeReference<Bytes32>() {}));
    ;

    @Deprecated
    protected IMailBoxBase(String contractAddress, Web3j web3j, Credentials credentials,
            BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected IMailBoxBase(String contractAddress, Web3j web3j, Credentials credentials,
            ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected IMailBoxBase(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected IMailBoxBase(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public RemoteFunctionCall<TransactionReceipt> pause() {
        final Function function = new Function(
                FUNC_PAUSE, 
                Arrays.<Type>asList(), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> sendMsg(String target_, BigInteger value_,
            byte[] msg_, BigInteger gasLimit_, String refundAddress_, BigInteger weiValue) {
        final Function function = new Function(
                FUNC_SENDMSG, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, target_), 
                new org.web3j.abi.datatypes.generated.Uint256(value_), 
                new org.web3j.abi.datatypes.DynamicBytes(msg_), 
                new org.web3j.abi.datatypes.generated.Uint256(gasLimit_), 
                new org.web3j.abi.datatypes.Address(160, refundAddress_)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function, weiValue);
    }

    public RemoteFunctionCall<TransactionReceipt> unpause() {
        final Function function = new Function(
                FUNC_UNPAUSE, 
                Arrays.<Type>asList(), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public static List<AppendMsgEventResponse> getAppendMsgEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(APPENDMSG_EVENT, transactionReceipt);
        ArrayList<AppendMsgEventResponse> responses = new ArrayList<AppendMsgEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            AppendMsgEventResponse typedResponse = new AppendMsgEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.index = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.messageHash = (byte[]) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static AppendMsgEventResponse getAppendMsgEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(APPENDMSG_EVENT, log);
        AppendMsgEventResponse typedResponse = new AppendMsgEventResponse();
        typedResponse.log = log;
        typedResponse.index = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        typedResponse.messageHash = (byte[]) eventValues.getNonIndexedValues().get(1).getValue();
        return typedResponse;
    }

    public Flowable<AppendMsgEventResponse> appendMsgEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getAppendMsgEventFromLog(log));
    }

    public Flowable<AppendMsgEventResponse> appendMsgEventFlowable(DefaultBlockParameter startBlock,
            DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(APPENDMSG_EVENT));
        return appendMsgEventFlowable(filter);
    }

    public static List<ClaimMsgEventResponse> getClaimMsgEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(CLAIMMSG_EVENT, transactionReceipt);
        ArrayList<ClaimMsgEventResponse> responses = new ArrayList<ClaimMsgEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            ClaimMsgEventResponse typedResponse = new ClaimMsgEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.hash = (byte[]) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.nonce = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static ClaimMsgEventResponse getClaimMsgEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(CLAIMMSG_EVENT, log);
        ClaimMsgEventResponse typedResponse = new ClaimMsgEventResponse();
        typedResponse.log = log;
        typedResponse.hash = (byte[]) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.nonce = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<ClaimMsgEventResponse> claimMsgEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getClaimMsgEventFromLog(log));
    }

    public Flowable<ClaimMsgEventResponse> claimMsgEventFlowable(DefaultBlockParameter startBlock,
            DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(CLAIMMSG_EVENT));
        return claimMsgEventFlowable(filter);
    }

    public static List<RelayMsgFailedEventResponse> getRelayMsgFailedEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(RELAYMSGFAILED_EVENT, transactionReceipt);
        ArrayList<RelayMsgFailedEventResponse> responses = new ArrayList<RelayMsgFailedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            RelayMsgFailedEventResponse typedResponse = new RelayMsgFailedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.hash = (byte[]) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.nonce = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static RelayMsgFailedEventResponse getRelayMsgFailedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(RELAYMSGFAILED_EVENT, log);
        RelayMsgFailedEventResponse typedResponse = new RelayMsgFailedEventResponse();
        typedResponse.log = log;
        typedResponse.hash = (byte[]) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.nonce = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<RelayMsgFailedEventResponse> relayMsgFailedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getRelayMsgFailedEventFromLog(log));
    }

    public Flowable<RelayMsgFailedEventResponse> relayMsgFailedEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(RELAYMSGFAILED_EVENT));
        return relayMsgFailedEventFlowable(filter);
    }

    public static List<RelayMsgSuccessEventResponse> getRelayMsgSuccessEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(RELAYMSGSUCCESS_EVENT, transactionReceipt);
        ArrayList<RelayMsgSuccessEventResponse> responses = new ArrayList<RelayMsgSuccessEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            RelayMsgSuccessEventResponse typedResponse = new RelayMsgSuccessEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.hash = (byte[]) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.nonce = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static RelayMsgSuccessEventResponse getRelayMsgSuccessEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(RELAYMSGSUCCESS_EVENT, log);
        RelayMsgSuccessEventResponse typedResponse = new RelayMsgSuccessEventResponse();
        typedResponse.log = log;
        typedResponse.hash = (byte[]) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.nonce = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<RelayMsgSuccessEventResponse> relayMsgSuccessEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getRelayMsgSuccessEventFromLog(log));
    }

    public Flowable<RelayMsgSuccessEventResponse> relayMsgSuccessEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(RELAYMSGSUCCESS_EVENT));
        return relayMsgSuccessEventFlowable(filter);
    }

    public static List<RelayedMsgEventResponse> getRelayedMsgEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(RELAYEDMSG_EVENT, transactionReceipt);
        ArrayList<RelayedMsgEventResponse> responses = new ArrayList<RelayedMsgEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            RelayedMsgEventResponse typedResponse = new RelayedMsgEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.hash = (byte[]) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.nonce = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static RelayedMsgEventResponse getRelayedMsgEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(RELAYEDMSG_EVENT, log);
        RelayedMsgEventResponse typedResponse = new RelayedMsgEventResponse();
        typedResponse.log = log;
        typedResponse.hash = (byte[]) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.nonce = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<RelayedMsgEventResponse> relayedMsgEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getRelayedMsgEventFromLog(log));
    }

    public Flowable<RelayedMsgEventResponse> relayedMsgEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(RELAYEDMSG_EVENT));
        return relayedMsgEventFlowable(filter);
    }

    public static List<RelayedMsgFailedEventResponse> getRelayedMsgFailedEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(RELAYEDMSGFAILED_EVENT, transactionReceipt);
        ArrayList<RelayedMsgFailedEventResponse> responses = new ArrayList<RelayedMsgFailedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            RelayedMsgFailedEventResponse typedResponse = new RelayedMsgFailedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.hash = (byte[]) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.nonce = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static RelayedMsgFailedEventResponse getRelayedMsgFailedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(RELAYEDMSGFAILED_EVENT, log);
        RelayedMsgFailedEventResponse typedResponse = new RelayedMsgFailedEventResponse();
        typedResponse.log = log;
        typedResponse.hash = (byte[]) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.nonce = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<RelayedMsgFailedEventResponse> relayedMsgFailedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getRelayedMsgFailedEventFromLog(log));
    }

    public Flowable<RelayedMsgFailedEventResponse> relayedMsgFailedEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(RELAYEDMSGFAILED_EVENT));
        return relayedMsgFailedEventFlowable(filter);
    }

    public static List<RollingHashEventResponse> getRollingHashEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(ROLLINGHASH_EVENT, transactionReceipt);
        ArrayList<RollingHashEventResponse> responses = new ArrayList<RollingHashEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            RollingHashEventResponse typedResponse = new RollingHashEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.hash = (byte[]) eventValues.getIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static RollingHashEventResponse getRollingHashEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(ROLLINGHASH_EVENT, log);
        RollingHashEventResponse typedResponse = new RollingHashEventResponse();
        typedResponse.log = log;
        typedResponse.hash = (byte[]) eventValues.getIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<RollingHashEventResponse> rollingHashEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getRollingHashEventFromLog(log));
    }

    public Flowable<RollingHashEventResponse> rollingHashEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(ROLLINGHASH_EVENT));
        return rollingHashEventFlowable(filter);
    }

    public static List<SentMsgEventResponse> getSentMsgEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(SENTMSG_EVENT, transactionReceipt);
        ArrayList<SentMsgEventResponse> responses = new ArrayList<SentMsgEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            SentMsgEventResponse typedResponse = new SentMsgEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.sender = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.target = (String) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.value = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.nonce = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            typedResponse.msg = (byte[]) eventValues.getNonIndexedValues().get(2).getValue();
            typedResponse.gasLimit = (BigInteger) eventValues.getNonIndexedValues().get(3).getValue();
            typedResponse.hash = (byte[]) eventValues.getNonIndexedValues().get(4).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static SentMsgEventResponse getSentMsgEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(SENTMSG_EVENT, log);
        SentMsgEventResponse typedResponse = new SentMsgEventResponse();
        typedResponse.log = log;
        typedResponse.sender = (String) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.target = (String) eventValues.getIndexedValues().get(1).getValue();
        typedResponse.value = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        typedResponse.nonce = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        typedResponse.msg = (byte[]) eventValues.getNonIndexedValues().get(2).getValue();
        typedResponse.gasLimit = (BigInteger) eventValues.getNonIndexedValues().get(3).getValue();
        typedResponse.hash = (byte[]) eventValues.getNonIndexedValues().get(4).getValue();
        return typedResponse;
    }

    public Flowable<SentMsgEventResponse> sentMsgEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getSentMsgEventFromLog(log));
    }

    public Flowable<SentMsgEventResponse> sentMsgEventFlowable(DefaultBlockParameter startBlock,
            DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(SENTMSG_EVENT));
        return sentMsgEventFlowable(filter);
    }

    @Deprecated
    public static IMailBoxBase load(String contractAddress, Web3j web3j, Credentials credentials,
            BigInteger gasPrice, BigInteger gasLimit) {
        return new IMailBoxBase(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static IMailBoxBase load(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new IMailBoxBase(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static IMailBoxBase load(String contractAddress, Web3j web3j, Credentials credentials,
            ContractGasProvider contractGasProvider) {
        return new IMailBoxBase(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static IMailBoxBase load(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new IMailBoxBase(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static class AppendMsgEventResponse extends BaseEventResponse {
        public BigInteger index;

        public byte[] messageHash;
    }

    public static class ClaimMsgEventResponse extends BaseEventResponse {
        public byte[] hash;

        public BigInteger nonce;
    }

    public static class RelayMsgFailedEventResponse extends BaseEventResponse {
        public byte[] hash;

        public BigInteger nonce;
    }

    public static class RelayMsgSuccessEventResponse extends BaseEventResponse {
        public byte[] hash;

        public BigInteger nonce;
    }

    public static class RelayedMsgEventResponse extends BaseEventResponse {
        public byte[] hash;

        public BigInteger nonce;
    }

    public static class RelayedMsgFailedEventResponse extends BaseEventResponse {
        public byte[] hash;

        public BigInteger nonce;
    }

    public static class RollingHashEventResponse extends BaseEventResponse {
        public byte[] hash;
    }

    public static class SentMsgEventResponse extends BaseEventResponse {
        public String sender;

        public String target;

        public BigInteger value;

        public BigInteger nonce;

        public byte[] msg;

        public BigInteger gasLimit;

        public byte[] hash;
    }
}
