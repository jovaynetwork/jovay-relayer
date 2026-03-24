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
@TableName("batches")
public class BatchesEntity extends BaseEntity {

    @TableField("version")
    private Integer version;

    @TableField("batch_header_hash")
    private String batchHeaderHash;

    @TableField("batch_index")
    private String batchIndex;

    @TableField("l1_message_popped")
    private Long l1MessagePopped;

    @TableField("total_l1_message_popped")
    private Long totalL1MessagePopped;

    @TableField("l1msg_rolling_hash")
    private String l1MsgRollingHash;

    @TableField("data_hash")
    private String dataHash;

    @TableField("parent_batch_hash")
    private String parentBatchHash;

    @TableField("post_state_root")
    private String postStateRoot;

    @TableField("l2_msg_root")
    private String l2MsgRoot;

    // included
    @TableField("start_number")
    private String startNumber;

    // included
    @TableField("end_number")
    private String endNumber;

    @TableField("chunk_num")
    private int chunkNum;
}
