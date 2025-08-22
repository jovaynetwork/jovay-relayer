package com.alipay.antchain.l2.relayer.core.blockchain.helper.owlracle;

import java.util.List;

import lombok.*;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OwlracleResponse<T> {
    private String timestamp;

    private String lastBlock;

    private String avgTime;

    private String avgTx;

    private String avgGas;

    private List<T> speeds;
}