package com.alipay.antchain.l2.relayer.service;

import com.alipay.antchain.l2.relayer.commons.models.OracleRequestDO;
import com.alipay.antchain.l2.relayer.commons.models.TransactionInfo;

import java.math.BigInteger;

public interface IOracleService {
    void processBlockOracle();

    void processBatchOracle();

    void updateBatchBlobFeeAndTxFee(OracleRequestDO oracleRequestDO) throws Exception;

    void updateBlobBaseFeeScalaAndTxFeeScala(OracleRequestDO oracleRequestDO);

    TransactionInfo updateFixedProfit(BigInteger profit);

    TransactionInfo updateTotalScala(BigInteger totalScala);

    TransactionInfo withdrawVault(String address, BigInteger amount);
}
