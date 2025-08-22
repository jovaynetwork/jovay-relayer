package com.alipay.antchain.l2.relayer.query.controller.req;

import java.math.BigInteger;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QueryBatchMetaRequest {

    private List<BigInteger> batchIndexList;
}
