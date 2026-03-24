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
import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Bytes32;
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
public class L2Mailbox extends Contract {
    public static final String BINARY = "Bin file was not provided";

    public static final String FUNC_ADDBRIDGE = "addBridge";

    public static final String FUNC_APPROVEMSG = "approveMsg";

    public static final String FUNC_BASEFEE = "baseFee";

    public static final String FUNC_BRANCHES = "branches";

    public static final String FUNC_CLAIMERC20 = "claimERC20";

    public static final String FUNC_CLAIMETH = "claimETH";

    public static final String FUNC_ESTIMATEMSGFEE = "estimateMsgFee";

    public static final String FUNC_FINALIZEL1MSGNONCE = "finalizeL1MsgNonce";

    public static final String FUNC_INITIALIZE = "initialize";

    public static final String FUNC_ISBRIDGE = "isBridge";

    public static final String FUNC_L1MAILBOX = "l1MailBox";

    public static final String FUNC_MSGORACLE = "msgOracle";

    public static final String FUNC_MSGROOT = "msgRoot";

    public static final String FUNC_OWNER = "owner";

    public static final String FUNC_PAUSE = "pause";

    public static final String FUNC_PAUSED = "paused";

    public static final String FUNC_PENDINGMSGMAP = "pendingMsgMap";

    public static final String FUNC_RECEIVEMSGMAP = "receiveMsgMap";

    public static final String FUNC_RECEIVEMSGSTATUS = "receiveMsgStatus";

    public static final String FUNC_RELAYMSG = "relayMsg";

    public static final String FUNC_REMOVEBRIDGE = "removeBridge";

    public static final String FUNC_RENOUNCEOWNERSHIP = "renounceOwnership";

    public static final String FUNC_ROLLINGHASH = "rollingHash";

    public static final String FUNC_SENDMSG = "sendMsg";

    public static final String FUNC_SENDMSGMAP = "sendMsgMap";

    public static final String FUNC_SETBASEFEE = "setBaseFee";

    public static final String FUNC_SETFINALIZEL1MSGNONCE = "setFinalizeL1MsgNonce";

    public static final String FUNC_SETMSGORACLE = "setMsgOracle";

    public static final String FUNC_TRANSFEROWNERSHIP = "transferOwnership";

    public static final String FUNC_UNPAUSE = "unpause";

    public static final String FUNC_VERSION = "version";

    public static final Event APPENDMSG_EVENT = new Event("AppendMsg", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}, new TypeReference<Bytes32>() {}));
    ;

    public static final Event BASEFEECHANGED_EVENT = new Event("BaseFeeChanged", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event BRIDGEADDED_EVENT = new Event("BridgeAdded", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}));
    ;

    public static final Event BRIDGEREMOVED_EVENT = new Event("BridgeRemoved", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}));
    ;

    public static final Event CLAIMMSG_EVENT = new Event("ClaimMsg", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>(true) {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event INITIALIZED1_EVENT = new Event("Initialized", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint8>() {}));
    ;

    public static final Event INITIALIZED_EVENT = new Event("Initialized", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event OWNERSHIPTRANSFERRED_EVENT = new Event("OwnershipTransferred", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}));
    ;

    public static final Event PAUSED_EVENT = new Event("Paused", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
    ;

    public static final Event PENDINGMSG_EVENT = new Event("PendingMsg", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<DynamicBytes>() {}));
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

    public static final Event SETFINALIZEL1MSGNONCE_EVENT = new Event("SetFinalizeL1MsgNonce", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
    ;

    public static final Event SETMSGORACLE_EVENT = new Event("SetMsgOracle", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}));
    ;

    public static final Event UNPAUSED_EVENT = new Event("Unpaused", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
    ;

    @Deprecated
    protected L2Mailbox(String contractAddress, Web3j web3j, Credentials credentials,
            BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected L2Mailbox(String contractAddress, Web3j web3j, Credentials credentials,
            ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected L2Mailbox(String contractAddress, Web3j web3j, TransactionManager transactionManager,
            BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected L2Mailbox(String contractAddress, Web3j web3j, TransactionManager transactionManager,
            ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public RemoteFunctionCall<TransactionReceipt> addBridge(String _bridge) {
        final Function function = new Function(
                FUNC_ADDBRIDGE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, _bridge)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> approveMsg(String sender_, String target_,
            BigInteger value_, BigInteger nonce_, byte[] msg_) {
        final Function function = new Function(
                FUNC_APPROVEMSG, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, sender_), 
                new org.web3j.abi.datatypes.Address(160, target_), 
                new org.web3j.abi.datatypes.generated.Uint256(value_), 
                new org.web3j.abi.datatypes.generated.Uint256(nonce_), 
                new org.web3j.abi.datatypes.DynamicBytes(msg_)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<BigInteger> baseFee() {
        final Function function = new Function(FUNC_BASEFEE, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<byte[]> branches(BigInteger param0) {
        final Function function = new Function(FUNC_BRANCHES, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(param0)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
        return executeRemoteCallSingleValueReturn(function, byte[].class);
    }

    public RemoteFunctionCall<TransactionReceipt> claimERC20(BigInteger nonce_, byte[] msgHash_) {
        final Function function = new Function(
                FUNC_CLAIMERC20, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(nonce_), 
                new org.web3j.abi.datatypes.generated.Bytes32(msgHash_)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> claimETH(String refundAddress_,
            BigInteger amount_, BigInteger nonce_, byte[] msgHash_) {
        final Function function = new Function(
                FUNC_CLAIMETH, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, refundAddress_), 
                new org.web3j.abi.datatypes.generated.Uint256(amount_), 
                new org.web3j.abi.datatypes.generated.Uint256(nonce_), 
                new org.web3j.abi.datatypes.generated.Bytes32(msgHash_)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<BigInteger> estimateMsgFee(BigInteger gasLimit_) {
        final Function function = new Function(FUNC_ESTIMATEMSGFEE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(gasLimit_)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<BigInteger> finalizeL1MsgNonce() {
        final Function function = new Function(FUNC_FINALIZEL1MSGNONCE, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<TransactionReceipt> initialize(String l1MailBox_, String owner_,
            BigInteger baseFee_) {
        final Function function = new Function(
                FUNC_INITIALIZE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, l1MailBox_), 
                new org.web3j.abi.datatypes.Address(160, owner_), 
                new org.web3j.abi.datatypes.generated.Uint256(baseFee_)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<Boolean> isBridge(String bridgeAddress) {
        final Function function = new Function(FUNC_ISBRIDGE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, bridgeAddress)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteFunctionCall<String> l1MailBox() {
        final Function function = new Function(FUNC_L1MAILBOX, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<String> msgOracle() {
        final Function function = new Function(FUNC_MSGORACLE, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<byte[]> msgRoot() {
        final Function function = new Function(FUNC_MSGROOT, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
        return executeRemoteCallSingleValueReturn(function, byte[].class);
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

    public RemoteFunctionCall<Boolean> pendingMsgMap(byte[] param0) {
        final Function function = new Function(FUNC_PENDINGMSGMAP, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(param0)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteFunctionCall<Boolean> receiveMsgMap(byte[] msgHash) {
        final Function function = new Function(FUNC_RECEIVEMSGMAP, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(msgHash)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteFunctionCall<Boolean> receiveMsgStatus(byte[] msgHash) {
        final Function function = new Function(FUNC_RECEIVEMSGSTATUS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(msgHash)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteFunctionCall<TransactionReceipt> relayMsg(String sender_, String target_,
            BigInteger value_, BigInteger nonce_, byte[] msg_) {
        final Function function = new Function(
                FUNC_RELAYMSG, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, sender_), 
                new org.web3j.abi.datatypes.Address(160, target_), 
                new org.web3j.abi.datatypes.generated.Uint256(value_), 
                new org.web3j.abi.datatypes.generated.Uint256(nonce_), 
                new org.web3j.abi.datatypes.DynamicBytes(msg_)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> removeBridge(String _bridge) {
        final Function function = new Function(
                FUNC_REMOVEBRIDGE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, _bridge)), 
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

    public RemoteFunctionCall<byte[]> rollingHash() {
        final Function function = new Function(FUNC_ROLLINGHASH, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
        return executeRemoteCallSingleValueReturn(function, byte[].class);
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

    public RemoteFunctionCall<Boolean> sendMsgMap(byte[] msgHash) {
        final Function function = new Function(FUNC_SENDMSGMAP, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(msgHash)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteFunctionCall<TransactionReceipt> setBaseFee(BigInteger _newBaseFee) {
        final Function function = new Function(
                FUNC_SETBASEFEE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(_newBaseFee)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> setFinalizeL1MsgNonce(
            BigInteger _finalizeL1MsgNonce) {
        final Function function = new Function(
                FUNC_SETFINALIZEL1MSGNONCE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(_finalizeL1MsgNonce)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> setMsgOracle(String msgOracle_) {
        final Function function = new Function(
                FUNC_SETMSGORACLE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, msgOracle_)), 
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

    public RemoteFunctionCall<BigInteger> version() {
        final Function function = new Function(FUNC_VERSION, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
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

    public static List<BaseFeeChangedEventResponse> getBaseFeeChangedEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(BASEFEECHANGED_EVENT, transactionReceipt);
        ArrayList<BaseFeeChangedEventResponse> responses = new ArrayList<BaseFeeChangedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            BaseFeeChangedEventResponse typedResponse = new BaseFeeChangedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.oldL2BaseFee = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.newL2BaseFee = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static BaseFeeChangedEventResponse getBaseFeeChangedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(BASEFEECHANGED_EVENT, log);
        BaseFeeChangedEventResponse typedResponse = new BaseFeeChangedEventResponse();
        typedResponse.log = log;
        typedResponse.oldL2BaseFee = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        typedResponse.newL2BaseFee = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        return typedResponse;
    }

    public Flowable<BaseFeeChangedEventResponse> baseFeeChangedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getBaseFeeChangedEventFromLog(log));
    }

    public Flowable<BaseFeeChangedEventResponse> baseFeeChangedEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(BASEFEECHANGED_EVENT));
        return baseFeeChangedEventFlowable(filter);
    }

    public static List<BridgeAddedEventResponse> getBridgeAddedEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(BRIDGEADDED_EVENT, transactionReceipt);
        ArrayList<BridgeAddedEventResponse> responses = new ArrayList<BridgeAddedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            BridgeAddedEventResponse typedResponse = new BridgeAddedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.bridge = (String) eventValues.getIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static BridgeAddedEventResponse getBridgeAddedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(BRIDGEADDED_EVENT, log);
        BridgeAddedEventResponse typedResponse = new BridgeAddedEventResponse();
        typedResponse.log = log;
        typedResponse.bridge = (String) eventValues.getIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<BridgeAddedEventResponse> bridgeAddedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getBridgeAddedEventFromLog(log));
    }

    public Flowable<BridgeAddedEventResponse> bridgeAddedEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(BRIDGEADDED_EVENT));
        return bridgeAddedEventFlowable(filter);
    }

    public static List<BridgeRemovedEventResponse> getBridgeRemovedEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(BRIDGEREMOVED_EVENT, transactionReceipt);
        ArrayList<BridgeRemovedEventResponse> responses = new ArrayList<BridgeRemovedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            BridgeRemovedEventResponse typedResponse = new BridgeRemovedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.bridge = (String) eventValues.getIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static BridgeRemovedEventResponse getBridgeRemovedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(BRIDGEREMOVED_EVENT, log);
        BridgeRemovedEventResponse typedResponse = new BridgeRemovedEventResponse();
        typedResponse.log = log;
        typedResponse.bridge = (String) eventValues.getIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<BridgeRemovedEventResponse> bridgeRemovedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getBridgeRemovedEventFromLog(log));
    }

    public Flowable<BridgeRemovedEventResponse> bridgeRemovedEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(BRIDGEREMOVED_EVENT));
        return bridgeRemovedEventFlowable(filter);
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
            typedResponse.l1MailBox = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.owner = (String) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.baseFee = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static InitializedEventResponse getInitializedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(INITIALIZED_EVENT, log);
        InitializedEventResponse typedResponse = new InitializedEventResponse();
        typedResponse.log = log;
        typedResponse.l1MailBox = (String) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.owner = (String) eventValues.getIndexedValues().get(1).getValue();
        typedResponse.baseFee = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
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

    public static List<PendingMsgEventResponse> getPendingMsgEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(PENDINGMSG_EVENT, transactionReceipt);
        ArrayList<PendingMsgEventResponse> responses = new ArrayList<PendingMsgEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            PendingMsgEventResponse typedResponse = new PendingMsgEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse._sender = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse._target = (String) eventValues.getIndexedValues().get(1).getValue();
            typedResponse._value = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse._nonce = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            typedResponse._msg = (byte[]) eventValues.getNonIndexedValues().get(2).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static PendingMsgEventResponse getPendingMsgEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(PENDINGMSG_EVENT, log);
        PendingMsgEventResponse typedResponse = new PendingMsgEventResponse();
        typedResponse.log = log;
        typedResponse._sender = (String) eventValues.getIndexedValues().get(0).getValue();
        typedResponse._target = (String) eventValues.getIndexedValues().get(1).getValue();
        typedResponse._value = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        typedResponse._nonce = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        typedResponse._msg = (byte[]) eventValues.getNonIndexedValues().get(2).getValue();
        return typedResponse;
    }

    public Flowable<PendingMsgEventResponse> pendingMsgEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getPendingMsgEventFromLog(log));
    }

    public Flowable<PendingMsgEventResponse> pendingMsgEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(PENDINGMSG_EVENT));
        return pendingMsgEventFlowable(filter);
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

    public static List<SetFinalizeL1MsgNonceEventResponse> getSetFinalizeL1MsgNonceEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(SETFINALIZEL1MSGNONCE_EVENT, transactionReceipt);
        ArrayList<SetFinalizeL1MsgNonceEventResponse> responses = new ArrayList<SetFinalizeL1MsgNonceEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            SetFinalizeL1MsgNonceEventResponse typedResponse = new SetFinalizeL1MsgNonceEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse._finalizeL1MsgNonce = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static SetFinalizeL1MsgNonceEventResponse getSetFinalizeL1MsgNonceEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(SETFINALIZEL1MSGNONCE_EVENT, log);
        SetFinalizeL1MsgNonceEventResponse typedResponse = new SetFinalizeL1MsgNonceEventResponse();
        typedResponse.log = log;
        typedResponse._finalizeL1MsgNonce = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<SetFinalizeL1MsgNonceEventResponse> setFinalizeL1MsgNonceEventFlowable(
            EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getSetFinalizeL1MsgNonceEventFromLog(log));
    }

    public Flowable<SetFinalizeL1MsgNonceEventResponse> setFinalizeL1MsgNonceEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(SETFINALIZEL1MSGNONCE_EVENT));
        return setFinalizeL1MsgNonceEventFlowable(filter);
    }

    public static List<SetMsgOracleEventResponse> getSetMsgOracleEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(SETMSGORACLE_EVENT, transactionReceipt);
        ArrayList<SetMsgOracleEventResponse> responses = new ArrayList<SetMsgOracleEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            SetMsgOracleEventResponse typedResponse = new SetMsgOracleEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse._msgOracle = (String) eventValues.getIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static SetMsgOracleEventResponse getSetMsgOracleEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(SETMSGORACLE_EVENT, log);
        SetMsgOracleEventResponse typedResponse = new SetMsgOracleEventResponse();
        typedResponse.log = log;
        typedResponse._msgOracle = (String) eventValues.getIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<SetMsgOracleEventResponse> setMsgOracleEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getSetMsgOracleEventFromLog(log));
    }

    public Flowable<SetMsgOracleEventResponse> setMsgOracleEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(SETMSGORACLE_EVENT));
        return setMsgOracleEventFlowable(filter);
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
    public static L2Mailbox load(String contractAddress, Web3j web3j, Credentials credentials,
            BigInteger gasPrice, BigInteger gasLimit) {
        return new L2Mailbox(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static L2Mailbox load(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new L2Mailbox(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static L2Mailbox load(String contractAddress, Web3j web3j, Credentials credentials,
            ContractGasProvider contractGasProvider) {
        return new L2Mailbox(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static L2Mailbox load(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new L2Mailbox(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static class AppendMsgEventResponse extends BaseEventResponse {
        public BigInteger index;

        public byte[] messageHash;
    }

    public static class BaseFeeChangedEventResponse extends BaseEventResponse {
        public BigInteger oldL2BaseFee;

        public BigInteger newL2BaseFee;
    }

    public static class BridgeAddedEventResponse extends BaseEventResponse {
        public String bridge;
    }

    public static class BridgeRemovedEventResponse extends BaseEventResponse {
        public String bridge;
    }

    public static class ClaimMsgEventResponse extends BaseEventResponse {
        public byte[] hash;

        public BigInteger nonce;
    }

    public static class Initialized1EventResponse extends BaseEventResponse {
        public BigInteger version;
    }

    public static class InitializedEventResponse extends BaseEventResponse {
        public String l1MailBox;

        public String owner;

        public BigInteger baseFee;
    }

    public static class OwnershipTransferredEventResponse extends BaseEventResponse {
        public String previousOwner;

        public String newOwner;
    }

    public static class PausedEventResponse extends BaseEventResponse {
        public String account;
    }

    public static class PendingMsgEventResponse extends BaseEventResponse {
        public String _sender;

        public String _target;

        public BigInteger _value;

        public BigInteger _nonce;

        public byte[] _msg;
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

    public static class SetFinalizeL1MsgNonceEventResponse extends BaseEventResponse {
        public BigInteger _finalizeL1MsgNonce;
    }

    public static class SetMsgOracleEventResponse extends BaseEventResponse {
        public String _msgOracle;
    }

    public static class UnpausedEventResponse extends BaseEventResponse {
        public String account;
    }
}
