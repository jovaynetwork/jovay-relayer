package com.alipay.antchain.l2.relayer.dal.repository;

import java.math.BigInteger;
import java.util.List;

import com.alipay.antchain.l2.relayer.commons.enums.OracleRequestTypeEnum;
import com.alipay.antchain.l2.relayer.commons.enums.OracleTransactionStateEnum;
import com.alipay.antchain.l2.relayer.commons.enums.OracleTypeEnum;
import com.alipay.antchain.l2.relayer.commons.models.L1BlockFeeInfo;
import com.alipay.antchain.l2.relayer.commons.models.OracleRequestDO;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

public interface IOracleRepository {
    List<OracleRequestDO> peekRequests(OracleTypeEnum oracleType, OracleRequestTypeEnum requestType, OracleTransactionStateEnum state, int size);

    OracleRequestDO peekLatestRequest(OracleTypeEnum oracleType, OracleRequestTypeEnum requestType, OracleTransactionStateEnum state);

    BigInteger peekLatestRequestIndex(OracleTypeEnum oracleType, OracleRequestTypeEnum requestType, OracleTransactionStateEnum state);

    OracleRequestDO peekRequestByTypeAndIndex(OracleTypeEnum oracleType, OracleRequestTypeEnum requestTypeEnum, String index);

    void saveBlockFeeInfo(L1BlockFeeInfo blockFeeInfo);

    void saveRollupTxReceipt(BigInteger batchIndex, OracleTypeEnum oracleType, OracleRequestTypeEnum requestTypeEnum, TransactionReceipt txReceipt);

    void updateRequestState(String requestIndex, OracleTypeEnum oracleType, OracleRequestTypeEnum requestType, OracleTransactionStateEnum state);
}
