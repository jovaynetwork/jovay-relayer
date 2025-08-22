package com.alipay.antchain.l2.relayer.core.blockchain.helper;

public enum EthNoncePolicyEnum {

    FAST,

    /**
     * <p>
     *     Get nonce dynamically from ethereum node synchronously.
     * </p>
     * <p>
     *     Please check {@link AcbRawTransactionManager#getNonce() here}.
     * </p>
     */
    NORMAL
}
