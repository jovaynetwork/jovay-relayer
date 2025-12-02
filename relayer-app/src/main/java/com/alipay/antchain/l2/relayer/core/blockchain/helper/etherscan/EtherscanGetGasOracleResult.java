package com.alipay.antchain.l2.relayer.core.blockchain.helper.etherscan;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EtherscanGetGasOracleResult {

    private String lastBlock;

    private String safeGasPrice;

    private String proposeGasPrice;

    private String fastGasPrice;

    private String suggestBaseFee;

    private String gasUsedRatio;
}
