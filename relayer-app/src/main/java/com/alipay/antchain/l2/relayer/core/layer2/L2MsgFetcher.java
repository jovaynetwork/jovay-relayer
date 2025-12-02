package com.alipay.antchain.l2.relayer.core.layer2;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import jakarta.annotation.Resource;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alipay.antchain.l2.relayer.commons.l2basic.L1MsgTransaction;
import com.alipay.antchain.l2.relayer.commons.enums.InterBlockchainMessageStateEnum;
import com.alipay.antchain.l2.relayer.commons.models.InterBlockchainMessageDO;
import com.alipay.antchain.l2.relayer.commons.models.L2MsgDO;
import com.alipay.antchain.l2.relayer.dal.repository.IMailboxRepository;
import com.alipay.antchain.l2.trace.BasicBlockTrace;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.utils.Numeric;

@Component
@Slf4j
public class L2MsgFetcher implements IL2MsgFetcher {

    private static final Event SEND_MSG_EVENT = new Event("SentMsg", Arrays.asList(
            new TypeReference<Address>(true) {},
            new TypeReference<Address>(true) {},
            new TypeReference<Uint256>() {},
            new TypeReference<Uint256>() {},
            new TypeReference<DynamicBytes>() {},
            new TypeReference<Uint256>() {},
            new TypeReference<Bytes32>() {}
    ));

    private static final byte[] SEND_MSG_TOPIC = Numeric.hexStringToByteArray(EventEncoder.encode(SEND_MSG_EVENT));

    @Resource
    private IMailboxRepository mailboxRepository;

    @Override
    public void process(@NonNull BasicBlockTrace blockTrace, BigInteger currBatchIndex) {
        log.info("process L2 block#{} for L2Msgs", blockTrace.getHeader().getNumber());

        List<L2MsgDO> l2Msgs = getL2MsgFromBlock(blockTrace, currBatchIndex);
        if (ObjectUtil.isEmpty(l2Msgs)) {
            log.debug("none l2 msgs on l2 block#{}", blockTrace.getHeader().getNumber());
            return;
        }

        mailboxRepository.saveMessages(
                l2Msgs.stream().sorted(Comparator.comparing(L2MsgDO::getMsgNonce)).map(x -> {
                    log.info("l2Msg {}-{}-{} found on height {} and save...", x.getBatchIndex(), x.getMsgNonce(), x.getMsgHashHex(), blockTrace.getHeader().getNumber());
                    InterBlockchainMessageDO messageDO = InterBlockchainMessageDO.fromL2MsgTx(BigInteger.valueOf(blockTrace.getHeader().getNumber()), x);
                    messageDO.setState(InterBlockchainMessageStateEnum.MSG_READY);
                    return messageDO;
                }).collect(Collectors.toList())
        );
    }

    private List<L2MsgDO> getL2MsgFromBlock(BasicBlockTrace blockTrace, BigInteger currBatchIndex) {
        if (blockTrace.getL2Msgs().getL2MsgsList().isEmpty()) {
            return ListUtil.empty();
        }
        return blockTrace.getL2Msgs().getL2MsgsList().stream()
                .filter(
                        l2Msg -> {
                            Address contract = new Address(Numeric.toHexString(l2Msg.getL2MsgLog().getAddress().toByteArray()));
                            log.debug("get l2Msg event topic {} from contract {} with data {}",
                                    Numeric.toHexString(l2Msg.getL2MsgLog().getTopics(0).getValue().toByteArray()),
                                    contract,
                                    HexUtil.encodeHexStr(l2Msg.getL2MsgLog().getData().toByteArray())
                            );
                            return contract.equals(L1MsgTransaction.L2_MAILBOX_AS_RECEIVER)
                                   && Arrays.equals(l2Msg.getL2MsgLog().getTopics(0).getValue().toByteArray(), SEND_MSG_TOPIC);
                        }
                ).map(
                        l2Msg -> {
                            List<Type> nonIndexedValues =
                                    FunctionReturnDecoder.decode(Numeric.toHexString(l2Msg.getL2MsgLog().getData().toByteArray()), SEND_MSG_EVENT.getNonIndexedParameters());
                            return new L2MsgDO(
                                    currBatchIndex,
                                    (BigInteger) nonIndexedValues.get(1).getValue(),
                                    (byte[]) nonIndexedValues.get(4).getValue(),
                                    HexUtil.encodeHexStr(l2Msg.getOriginTxHash().getValue().toByteArray())
                            );
                        }
                ).sorted(Comparator.comparing(L2MsgDO::getMsgNonce)).collect(Collectors.toList());
    }
}
