package com.alipay.antchain.l2.relayer.core.blockchain.helper.etherscan;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EtherscanResponse<T> {

    private String status;

    private String message;

    private T result;
}
