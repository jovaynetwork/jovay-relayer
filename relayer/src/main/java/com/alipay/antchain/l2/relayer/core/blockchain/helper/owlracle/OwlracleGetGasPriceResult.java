package com.alipay.antchain.l2.relayer.core.blockchain.helper.owlracle;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OwlracleGetGasPriceResult {

    private String acceptance;

    private String maxFeePerGas;

    private String maxPriorityFeePerGas;

    private String baseFee;

    private String estimatedFee;
}
