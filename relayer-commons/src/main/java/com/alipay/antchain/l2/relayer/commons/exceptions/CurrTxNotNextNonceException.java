package com.alipay.antchain.l2.relayer.commons.exceptions;

import java.math.BigInteger;

public class CurrTxNotNextNonceException extends NoNeedToSpeedUpException {

    public CurrTxNotNextNonceException(BigInteger txNonce, BigInteger nextNonce, String accAddress) {
        super(L2RelayerErrorCodeEnum.CURR_TX_NOT_NEXT_NONCE, "{}'s next nonce is {} not {}", accAddress, nextNonce, txNonce);
    }
}
