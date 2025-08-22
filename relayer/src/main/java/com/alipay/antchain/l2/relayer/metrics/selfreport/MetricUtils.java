package com.alipay.antchain.l2.relayer.metrics.selfreport;

import java.math.BigInteger;

import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.l2.relayer.commons.enums.ProveTypeEnum;

public class MetricUtils {

    public static String getBatchCommitMetricKey(BigInteger batchIndex) {
        return StrUtil.format("CMT_B-{}", batchIndex.toString());
    }

    public static String getProofCommitMetricKey(ProveTypeEnum type, BigInteger batchIndex) {
        return StrUtil.format("CMT_P-{}@{}", type.getProverType(), batchIndex.toString());
    }
}
