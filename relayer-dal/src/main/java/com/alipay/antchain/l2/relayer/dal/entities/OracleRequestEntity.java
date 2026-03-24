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

import com.alipay.antchain.l2.relayer.commons.enums.OracleRequestTypeEnum;
import com.alipay.antchain.l2.relayer.commons.enums.OracleTransactionStateEnum;
import com.alipay.antchain.l2.relayer.commons.enums.OracleTypeEnum;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;

@Getter
@Setter
@TableName("oracle_request")
public class OracleRequestEntity extends BaseEntity {
    @TableField("oracle_type")
    private OracleTypeEnum oracleType;

    @TableField("oracle_task_type")
    private OracleRequestTypeEnum oracleTaskType;

    @TableField("request_index")
    private BigInteger requestIndex;

    @TableField("raw_data")
    private byte[] rawData;

    @TableField("tx_state")
    private OracleTransactionStateEnum txState;
}
