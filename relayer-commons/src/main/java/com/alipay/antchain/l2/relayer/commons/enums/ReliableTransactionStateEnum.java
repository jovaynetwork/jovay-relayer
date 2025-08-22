package com.alipay.antchain.l2.relayer.commons.enums;

public enum ReliableTransactionStateEnum {

    TX_PENDING,

    TX_PACKAGED,

    TX_SUCCESS,

    TX_FAILED,

    /**
     * Tx could be failed on chain, but from task corner, it's already made the target.
     */
    BIZ_SUCCESS;

    public static boolean considerAsSuccess(ReliableTransactionStateEnum state) {
        return state == TX_SUCCESS || state == BIZ_SUCCESS;
    }

    public static boolean considerAsFailed(ReliableTransactionStateEnum state) {
        return state == TX_FAILED;
    }

    public boolean isExecuteAlrightAndOnchain() {
        return this == TX_SUCCESS || this == TX_PACKAGED || this == BIZ_SUCCESS;
    }
}
