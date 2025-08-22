package com.alipay.antchain.l2.relayer.query.controller.req;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QueryL2MsgProofRequest {
    private Long nonce;
}
