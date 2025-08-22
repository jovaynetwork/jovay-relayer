package com.alipay.antchain.l2.relayer.commons.models;

import java.math.BigInteger;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.HexUtil;
import com.alipay.antchain.l2.relayer.commons.l2basic.L1MsgRawTransactionWrapper;
import com.alipay.antchain.l2.relayer.commons.l2basic.L1MsgTransaction;
import com.alipay.antchain.l2.relayer.commons.enums.InterBlockchainMessageStateEnum;
import com.alipay.antchain.l2.relayer.commons.enums.InterBlockchainMessageTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class InterBlockchainMessageDO {

    public static InterBlockchainMessageDO fromL1MsgTx(
            BigInteger sourceBlockHeight,
            String sourceTxHash,
            L1MsgTransaction l1MsgTransaction
    ) {
        L1MsgRawTransactionWrapper wrapper = new L1MsgRawTransactionWrapper(l1MsgTransaction);
        return new InterBlockchainMessageDO(
                InterBlockchainMessageTypeEnum.L1_MSG,
                wrapper.calcHash(),
                sourceBlockHeight,
                sourceTxHash,
                L1MsgTransaction.L1_MAILBOX_AS_SENDER.toString(),
                L1MsgTransaction.L2_MAILBOX_AS_RECEIVER.toString(),
                l1MsgTransaction.getNonce(),
                wrapper.encodeWithoutSig()
        );
    }

    public static InterBlockchainMessageDO fromL2MsgTx(BigInteger sourceBlockHeight, L2MsgDO l2MsgDO) {
        return new InterBlockchainMessageDO(
                InterBlockchainMessageTypeEnum.L2_MSG,
                l2MsgDO.getBatchIndex(),
                l2MsgDO.getMsgHash(),
                sourceBlockHeight,
                l2MsgDO.getSourceTxHash(),
                L1MsgTransaction.L2_MAILBOX_AS_RECEIVER.toString(),
                L1MsgTransaction.L1_MAILBOX_AS_SENDER.toString(),
                l2MsgDO.getMsgNonce()
        );
    }

    public InterBlockchainMessageDO(
            InterBlockchainMessageTypeEnum type,
            byte[] msgHash,
            BigInteger sourceBlockHeight,
            String sourceTxHash,
            String sender,
            String receiver,
            BigInteger nonce,
            byte[] rawMessage
    ) {
        this.type = type;
        this.msgHash = msgHash;
        this.sourceBlockHeight = sourceBlockHeight;
        this.sourceTxHash = sourceTxHash;
        this.sender = sender;
        this.receiver = receiver;
        this.nonce = nonce;
        this.rawMessage = rawMessage;
    }

    public InterBlockchainMessageDO(
            InterBlockchainMessageTypeEnum type,
            BigInteger l2BatchIndex,
            byte[] msgHash,
            BigInteger sourceBlockHeight,
            String sourceTxHash,
            String sender,
            String receiver,
            BigInteger nonce
    ) {
        this.type = type;
        this.batchIndex = l2BatchIndex;
        this.msgHash = msgHash;
        this.sourceBlockHeight = sourceBlockHeight;
        this.sourceTxHash = sourceTxHash;
        this.sender = sender;
        this.receiver = receiver;
        this.nonce = nonce;
        this.rawMessage = new byte[]{};
    }

    private InterBlockchainMessageTypeEnum type;

    private BigInteger batchIndex;

    private byte[] msgHash;

    private BigInteger sourceBlockHeight;

    private String sourceTxHash;

    private String sender;

    private String receiver;

    private BigInteger nonce;

    private byte[] rawMessage;

    private byte[] proof;

    private InterBlockchainMessageStateEnum state;

    public L1MsgTransaction toL1MsgTransaction() {
        Assert.equals(InterBlockchainMessageTypeEnum.L1_MSG, type);
        return L1MsgTransaction.decode(this.rawMessage);
    }

    public String getMsgHashHex() {
        return HexUtil.encodeHexStr(msgHash);
    }
}
