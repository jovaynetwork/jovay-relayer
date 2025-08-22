package com.alipay.antchain.l2.relayer.dal.entities;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("chunks")
public class ChunksEntity extends BaseEntity {

    @TableField("batch_index")
    private String batchIndex;

    @TableField("chunk_index")
    private Long chunkIndex;

    @TableField("chunk_hash")
    private String chunkHash;

    @TableField("num_blocks")
    private Integer numBlocks;

    @TableField("zk_cycle_sum")
    private Long zkCycleSum;

    // included
    @TableField("start_number")
    private String startNumber;

    // included
    @TableField("end_number")
    private String endNumber;

    @TableField("raw_chunk")
    private byte[] rawChunk;
}
