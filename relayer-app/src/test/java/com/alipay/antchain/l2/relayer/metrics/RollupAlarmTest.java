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

package com.alipay.antchain.l2.relayer.metrics;

import java.math.BigInteger;
import java.util.Date;

import cn.hutool.core.collection.ListUtil;
import com.alipay.antchain.l2.relayer.TestBase;
import com.alipay.antchain.l2.relayer.commons.enums.ChainTypeEnum;
import com.alipay.antchain.l2.relayer.commons.enums.ProveTypeEnum;
import com.alipay.antchain.l2.relayer.commons.enums.RollupNumberRecordTypeEnum;
import com.alipay.antchain.l2.relayer.commons.models.BatchProveRequestDO;
import com.alipay.antchain.l2.relayer.commons.models.ReliableTransactionDO;
import com.alipay.antchain.l2.relayer.commons.models.RollupNumberInfo;
import com.alipay.antchain.l2.relayer.config.RollupConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.L1Client;
import com.alipay.antchain.l2.relayer.core.blockchain.L2Client;
import com.alipay.antchain.l2.relayer.dal.repository.IOracleRepository;
import com.alipay.antchain.l2.relayer.dal.repository.IRollupRepository;
import com.alipay.antchain.l2.relayer.engine.DistributedTaskEngine;
import com.alipay.antchain.l2.relayer.metrics.alarm.RollupAlarm;
import jakarta.annotation.Resource;
import org.junit.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.web3j.protocol.Web3j;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

public class RollupAlarmTest extends TestBase {

    @MockitoBean
    private RollupConfig rollupConfig;

    @MockitoBean
    private L2Client l2Client;

    @MockitoBean
    private L1Client l1Client;

    @MockitoBean
    private DistributedTaskEngine distributedTaskEngine;

    @MockitoBean
    private IRollupRepository rollupRepository;

    @MockitoBean
    private IOracleRepository oracleRepository;

    @MockitoBean(name = "l1Web3j")
    private Web3j l1Web3j;

    @MockitoBean(name = "l1ChainId")
    private BigInteger l1ChainId;

    @Resource
    private RollupAlarm alarm;

    @Test
    public void testProcess() {
        BatchProveRequestDO requestDOTee = new BatchProveRequestDO();
        requestDOTee.setBatchIndex(BigInteger.ONE);
        requestDOTee.setGmtModified(new Date(System.currentTimeMillis() - 3600_000));
        requestDOTee.setProveType(ProveTypeEnum.TEE_PROOF);
        BatchProveRequestDO requestDOZk = new BatchProveRequestDO();
        requestDOZk.setBatchIndex(BigInteger.ONE);
        requestDOZk.setGmtModified(new Date(System.currentTimeMillis() - 3600_000));
        requestDOZk.setProveType(ProveTypeEnum.ZK_PROOF);

        when(rollupRepository.peekPendingBatchProveRequest(anyInt(), eq(ProveTypeEnum.TEE_PROOF)))
                .thenReturn(ListUtil.toList(requestDOTee));
        when(rollupRepository.peekPendingBatchProveRequest(anyInt(), eq(ProveTypeEnum.ZK_PROOF)))
                .thenReturn(ListUtil.toList(requestDOZk));

        when(rollupRepository.getRollupNumberInfo(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.BLOCK_PROCESSED)))
                .thenReturn(new RollupNumberInfo(BigInteger.ONE, new Date(System.currentTimeMillis() - 3600_000)));
        when(rollupRepository.getRollupNumberInfo(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK)))
                .thenReturn(new RollupNumberInfo(BigInteger.ONE, new Date(System.currentTimeMillis() - 3600_000)));
        when(rollupRepository.getRollupNumberInfo(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_BATCH)))
                .thenReturn(new RollupNumberInfo(BigInteger.ONE, new Date(System.currentTimeMillis() - 10800_000)));

        when(rollupRepository.getTxPendingReliableTransactions(anyInt()))
                .thenReturn(ListUtil.toList(
                        ReliableTransactionDO.builder()
                                .originalTxHash("0x123")
                                .gmtCreate(new Date(System.currentTimeMillis() - 3600_000))
                                .build()
                ));

        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.BATCH_COMMITTED)))
                .thenReturn(new BigInteger("21"));
        when(rollupRepository.getRollupNumberInfo(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.BATCH_COMMITTED)))
                .thenReturn(new RollupNumberInfo(new BigInteger("21"), new Date(System.currentTimeMillis() - 18010_000)));

        when(rollupRepository.calcWaitingBatchCountBeyondIndex(notNull())).thenReturn(100);

        alarm.process();
    }
}
