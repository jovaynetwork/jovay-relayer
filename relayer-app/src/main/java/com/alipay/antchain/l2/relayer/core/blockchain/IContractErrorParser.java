package com.alipay.antchain.l2.relayer.core.blockchain;

import com.alipay.antchain.l2.relayer.commons.abi.AbiCustomerError;

public interface IContractErrorParser {

    /**
     * Parse the raw hex error data to {@link AbiCustomerError } from eth call result.
     *
     * @param hexError the raw hex error data
     * @return {@link AbiCustomerError }
     */
    AbiCustomerError parse(String hexError);
}
