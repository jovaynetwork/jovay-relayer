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

import com.alipay.antchain.l2.relayer.commons.enums.BatchProveRequestStateEnum;
import com.alipay.antchain.l2.relayer.commons.enums.ProveTypeEnum;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("batch_prove_request")
public class BatchProveRequestEntity extends BaseEntity {

    @TableField("batch_index")
    private BigInteger batchIndex;

    @TableField("prove_type")
    private ProveTypeEnum proveType;

    @TableField("proof")
    private byte[] proof;

    @TableField("state")
    private BatchProveRequestStateEnum state;
}
