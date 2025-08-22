package com.alipay.antchain.l2.relayer.dal.entities;

import java.math.BigInteger;
import java.util.Date;

import com.alipay.antchain.l2.relayer.commons.enums.ChainTypeEnum;
import com.alipay.antchain.l2.relayer.commons.enums.ReliableTransactionStateEnum;
import com.alipay.antchain.l2.relayer.commons.enums.TransactionTypeEnum;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("reliable_transaction")
public class ReliableTransactionEntity extends BaseEntity {

    @TableField("chain_type")
    private ChainTypeEnum chainType;

    @TableField("transaction_type")
    private TransactionTypeEnum transactionType;

    @TableField("batch_index")
    private BigInteger batchIndex;

    @TableField("sender_account")
    private String senderAccount;

    @TableField("nonce")
    private Long nonce;

    @TableField("original_tx_hash")
    private String originalTxHash;

    @TableField("latest_tx_hash")
    private String latestTxHash;

    @TableField("raw_tx")
    private byte[] rawTx;

    @TableField("latest_tx_send_time")
    private Date latestTxSendTime;

    @TableField("state")
    private ReliableTransactionStateEnum state;

    @TableField("retry_count")
    private Integer retryCount;

    @TableField("revert_reason")
    private String revertReason;
}
