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

package com.alipay.antchain.l2.relayer.metrics.selfreport;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;

@Slf4j
public class SelfReportMetric implements ISelfReportMetric {

    @Setter
    private OkHttpClient httpClient;

    private final String serverUrl;

    private final int cacheTTL;

    private final RMapCache<String, RollupMetricRecord> startTimePoints;

    private final RMapCache<String, RollupMetricRecord> endTimePoints;

    public SelfReportMetric(String serverUrl, RedissonClient redisson, int cacheTTL) {
        this.httpClient = new OkHttpClient.Builder().build();
        this.serverUrl = serverUrl;
        this.startTimePoints = redisson.getMapCache("MetricStartTimePoints");
        this.endTimePoints = redisson.getMapCache("MetricEndTimePoints");
        this.cacheTTL = cacheTTL;
    }

    @Override
    public CompletableFuture<Void> reportAsync(String recordKey) {
        return CompletableFuture.runAsync(() -> {
            RollupMetricRecord endRecord = endTimePoints.get(recordKey);
            if (ObjectUtil.isNull(endRecord)) {
                log.debug("none end record for key {}", recordKey);
                return;
            }
            endTimePoints.removeAsync(recordKey);
            report(recordKey, endRecord);
        }).exceptionally(throwable -> {
            log.error("unexpected self report exception: ", throwable);
            return null;
        });
    }

    @Override
    public CompletableFuture<Void> recordEndAndReportAsync(String recordKey) {
        return CompletableFuture.runAsync(() -> report(recordKey, new RollupMetricRecord()))
                .exceptionally(throwable -> {
                    log.error("unexpected self report exception: ", throwable);
                    return null;
                });
    }

    @Override
    public void recordStart(RollupMetricRecord record) {
        try {
            startTimePoints.put(record.getKey(), record, cacheTTL, TimeUnit.SECONDS);
        } catch (Throwable t) {
            log.error("failed to record start metric: ", t);
        }
    }

    @Override
    public void recordEnd(RollupMetricRecord record) {
        try {
            endTimePoints.put(record.getKey(), record, cacheTTL, TimeUnit.SECONDS);
        } catch (Throwable t) {
            log.error("failed to record end metric: ", t);
        }
    }

    private void report(String recordKey, RollupMetricRecord endRecord) {
        RollupMetricRecord startRecord = startTimePoints.get(recordKey);
        if (ObjectUtil.isNull(startRecord)) {
            log.debug("none start record for key {}", recordKey);
            return;
        }
        startTimePoints.removeAsync(recordKey);

        try (
                Response response = httpClient.newCall(new Request.Builder()
                        .url(URLUtil.completeUrl(serverUrl, "/api/reporting/self"))
                        .post(RequestBody.create(
                                new SelfReportRequest(startRecord.getFrom(), startRecord.getTo(), endRecord.getTimePoint().getTime() - startRecord.getTimePoint().getTime()).toJson(),
                                MediaType.parse("application/json; charset=utf-8")
                        )).build()
                ).execute()
        ) {
            if (!response.isSuccessful()) {
                throw new RuntimeException(
                        StrUtil.format(
                                "metric http request failed: {} - {}",
                                response.code(), response.message()
                        )
                );
            }
            SelfReportResponse resp = SelfReportResponse.fromJson(Objects.requireNonNull(response.body(), "empty resp body").string());
            if (!resp.isSuccess()) {
                throw new RuntimeException(StrUtil.format("self reporting req error: {}", resp.getErrorMessage()));
            }
            log.debug("self report: {}-{}-{}", startRecord.getFrom(), startRecord.getTo(), endRecord.getTimePoint().getTime() - startRecord.getTimePoint().getTime());
        } catch (Throwable t) {
            log.error("failed to self report metric: ", t);
        }
    }
}
