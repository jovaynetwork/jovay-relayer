package com.alipay.antchain.l2.relayer.dal.entities;


import java.math.BigInteger;

import com.alipay.antchain.l2.relayer.commons.enums.InterBlockchainMessageStateEnum;
import com.alipay.antchain.l2.relayer.commons.enums.InterBlockchainMessageTypeEnum;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("inter_bc_msg")
public class InterBlockchainMessageEntity extends BaseEntity {

    @TableField("type")
    private InterBlockchainMessageTypeEnum type;

    @TableField("batch_index")
    private BigInteger batchIndex;

    @TableField("msg_hash")
    private String msgHash;

    @TableField("source_block_height")
    private String sourceBlockHeight;

    @TableField("source_tx_hash")
    private String sourceTxHash;

    @TableField("sender")
    private String sender;

    @TableField("receiver")
    private String receiver;

    @TableField("nonce")
    private Long nonce;

    @TableField("raw_message")
    private byte[] rawMessage;

    @TableField("proof")
    private byte[] proof;

    @TableField("state")
    private InterBlockchainMessageStateEnum state;
}
