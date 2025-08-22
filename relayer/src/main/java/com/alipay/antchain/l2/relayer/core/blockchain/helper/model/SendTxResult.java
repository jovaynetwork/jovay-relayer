package com.alipay.antchain.l2.relayer.core.blockchain.helper.model;

import java.math.BigInteger;
import java.util.Date;

import lombok.Builder;
import lombok.Getter;
import org.web3j.protocol.core.methods.response.EthSendTransaction;

@Getter
@Builder
public class SendTxResult {

    private EthSendTransaction ethSendTransaction;

    private String rawTxHex;

    private BigInteger nonce;

    private Date txSendTime;
}
