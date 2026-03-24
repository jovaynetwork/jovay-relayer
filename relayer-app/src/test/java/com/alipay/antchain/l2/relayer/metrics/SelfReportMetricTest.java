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
import java.util.concurrent.CompletableFuture;

import com.alibaba.fastjson.JSON;
import com.alipay.antchain.l2.relayer.commons.enums.ProveTypeEnum;
import com.alipay.antchain.l2.relayer.metrics.selfreport.MetricUtils;
import com.alipay.antchain.l2.relayer.metrics.selfreport.RollupMetricRecord;
import com.alipay.antchain.l2.relayer.metrics.selfreport.SelfReportMetric;
import com.alipay.antchain.l2.relayer.metrics.selfreport.SelfReportResponse;
import lombok.SneakyThrows;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.Test;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class SelfReportMetricTest {

    @Test
    @SneakyThrows
    public void testReport() {
        RedissonClient redisson = mock(RedissonClient.class);
        RMapCache startMap = mock(RMapCache.class);
        RMapCache endMap = mock(RMapCache.class);
        when(redisson.getMapCache(anyString())).thenReturn(startMap, endMap);

        RollupMetricRecord batchCommitRecord = RollupMetricRecord.createCommitBatchRecord(BigInteger.ONE);
        RollupMetricRecord proofCommitRecord = RollupMetricRecord.createCommitProofRecord(ProveTypeEnum.TEE_PROOF, BigInteger.ONE);

        when(startMap.get(eq(batchCommitRecord.getKey()))).thenReturn(batchCommitRecord);
        when(startMap.get(eq(proofCommitRecord.getKey()))).thenReturn(proofCommitRecord);
        when(endMap.get(eq(batchCommitRecord.getKey()))).thenReturn(batchCommitRecord);
        when(endMap.get(eq(proofCommitRecord.getKey()))).thenReturn(proofCommitRecord);

        SelfReportMetric selfReportMetric = new SelfReportMetric("http://localhost:8080", redisson, 100);
        OkHttpClient httpClient = mock(OkHttpClient.class);
        selfReportMetric.setHttpClient(httpClient);
        Call call = mock(Call.class);
        when(httpClient.newCall(notNull())).thenReturn(call);
        Response response = mock(Response.class);
        when(call.execute()).thenReturn(response);
        when(response.isSuccessful()).thenReturn(true);
        ResponseBody responseBody = mock(ResponseBody.class);
        when(response.body()).thenReturn(responseBody);
        when(responseBody.string()).thenReturn(JSON.toJSONString(new SelfReportResponse(true, "")));

        selfReportMetric.recordStart(batchCommitRecord);
        verify(startMap, times(1)).put(eq(batchCommitRecord.getKey()), notNull(), anyLong(), any());
        RollupMetricRecord end = RollupMetricRecord.createCommitBatchRecord(BigInteger.valueOf(2));
        selfReportMetric.recordEnd(end);
        verify(endMap, times(1)).put(eq(end.getKey()), notNull(), anyLong(), any());
        CompletableFuture<Void> future = selfReportMetric.recordEndAndReportAsync(MetricUtils.getBatchCommitMetricKey(BigInteger.ONE));
        future.join();
        verify(startMap, times(1)).removeAsync(anyString());

        clearInvocations(startMap);
        future = selfReportMetric.reportAsync(MetricUtils.getBatchCommitMetricKey(BigInteger.ONE));
        future.join();
        verify(startMap, times(1)).removeAsync(anyString());
        verify(endMap, times(1)).removeAsync(anyString());

        clearInvocations(startMap);
        selfReportMetric.recordStart(proofCommitRecord);
        verify(startMap, times(1)).put(eq(proofCommitRecord.getKey()), notNull(), anyLong(), any());
        end = RollupMetricRecord.createCommitProofRecord(ProveTypeEnum.TEE_PROOF, BigInteger.valueOf(2));
        selfReportMetric.recordEnd(end);
        verify(endMap, times(1)).put(eq(end.getKey()), notNull(), anyLong(), any());
        future = selfReportMetric.recordEndAndReportAsync(MetricUtils.getProofCommitMetricKey(ProveTypeEnum.TEE_PROOF, BigInteger.ONE));
        future.join();
        verify(startMap, times(1)).removeAsync(anyString());

        clearInvocations(startMap);
        clearInvocations(endMap);
        future = selfReportMetric.reportAsync(MetricUtils.getProofCommitMetricKey(ProveTypeEnum.TEE_PROOF, BigInteger.ONE));
        future.join();
        verify(startMap, times(1)).removeAsync(anyString());
        verify(endMap, times(1)).removeAsync(anyString());
    }
}
