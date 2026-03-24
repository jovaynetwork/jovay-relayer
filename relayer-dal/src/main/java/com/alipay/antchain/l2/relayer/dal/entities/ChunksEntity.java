/*
 * Copyright 2026 Ant Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alipay.antchain.l2.relayer.dal.entities;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("chunks")
public class ChunksEntity extends BaseEntity {

    @TableField("batch_version")
    private Integer batchVersion;

    @TableField("batch_index")
    private String batchIndex;

    @TableField("chunk_index")
    private Long chunkIndex;

    @TableField("chunk_hash")
    private String chunkHash;

    @TableField("num_blocks")
    private long numBlocks;

    @TableField("zk_cycle_sum")
    private Long zkCycleSum;

    @TableField("gas_sum")
    private Long gasSum;

    // included
    @TableField("start_number")
    private String startNumber;

    // included
    @TableField("end_number")
    private String endNumber;

    @TableField("raw_chunk")
    private byte[] rawChunk;
}
