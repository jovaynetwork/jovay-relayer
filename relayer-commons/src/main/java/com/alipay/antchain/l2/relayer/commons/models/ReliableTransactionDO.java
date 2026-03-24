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
