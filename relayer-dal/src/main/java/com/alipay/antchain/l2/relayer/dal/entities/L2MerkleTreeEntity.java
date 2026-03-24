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

import java.math.BigInteger;
import java.util.Arrays;

import com.alibaba.fastjson.annotation.JSONField;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.web3j.abi.datatypes.generated.Bytes32;

@Getter
@Setter
@TableName("l2_merkle_tree")
@NoArgsConstructor
@AllArgsConstructor
public class L2MerkleTreeEntity extends BaseEntity {

    @TableField("branch")
    private byte[] branches;

    @TableField("batch_index")
    private BigInteger batchIndex;

    @TableField("next_msg_nonce")
    private BigInteger nextMsgNonce;

    @JSONField(serialize = false, deserialize = false)
    public Bytes32[] toBranches() {
        Bytes32[] result = new Bytes32[branches.length / 32];
        for (int i = 0; i < branches.length / 32; i++) {
            result[i] = new Bytes32(Arrays.copyOfRange(branches, i * 32, (i + 1) * 32));
        }
        return result;
    }
}
