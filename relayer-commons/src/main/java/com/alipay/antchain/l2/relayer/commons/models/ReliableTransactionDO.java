package com.alipay.antchain.l2.relayer.commons.models;

import java.math.BigInteger;
import java.util.Date;

import com.alipay.antchain.l2.relayer.commons.enums.ChainTypeEnum;
import com.alipay.antchain.l2.relayer.commons.enums.ReliableTransactionStateEnum;
import com.alipay.antchain.l2.relayer.commons.enums.TransactionTypeEnum;
import lombok.*;
import lombok.experimental.FieldNameConstants;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
public class ReliableTransactionDO {

    private ChainTypeEnum chainType;

    private TransactionTypeEnum transactionType;

    private BigInteger batchIndex;

    private String senderAccount;

    private Long nonce;

    private String originalTxHash;

    private String latestTxHash;

    private byte[] rawTx;

    private Date latestTxSendTime;

    private ReliableTransactionStateEnum state;

    private Integer retryCount;

    private String revertReason;

    private Date gmtCreate;

    private Date gmtModified;
}
