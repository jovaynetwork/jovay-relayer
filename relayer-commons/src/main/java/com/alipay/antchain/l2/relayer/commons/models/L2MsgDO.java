package com.alipay.antchain.l2.relayer.commons.models;

import java.math.BigInteger;

import cn.hutool.core.util.HexUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class L2MsgDO {

    private BigInteger batchIndex;

    private BigInteger msgNonce;

    private byte[] msgHash;

    private String sourceTxHash;

    public String getMsgHashHex() {
        return HexUtil.encodeHexStr(msgHash);
    }
}
