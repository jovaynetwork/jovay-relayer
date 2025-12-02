package com.alipay.antchain.l2.relayer.core.layer2.economic;

import java.math.BigInteger;
import java.util.concurrent.ScheduledExecutorService;

import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.l2.relayer.dal.repository.ISystemConfigRepository;
import com.alipay.antchain.l2.relayer.engine.dynamicconf.PrefixedDynamicConfig;
import lombok.experimental.FieldNameConstants;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@FieldNameConstants
public class RollupEconomicStrategyConfig extends PrefixedDynamicConfig {

    @Value("${l2-relayer.rollup.economic-strategy-conf.default-mid-eip1559-price-limit:3000000000}")
    private BigInteger midEip1559PriceLimit;

    @Value("${l2-relayer.rollup.economic-strategy-conf.default-high-eip1559-price-limit:8000000000}")
    private BigInteger highEip1559PriceLimit;

    @Value("${l2-relayer.rollup.economic-strategy-conf.default-max-pending-batch-count:12}")
    private int maxPendingBatchCount;

    @Value("${l2-relayer.rollup.economic-strategy-conf.default-max-pending-proof-count:12}")
    private int maxPendingProofCount;

    @Value("${l2-relayer.rollup.economic-strategy-conf.default-max-batch-waiting-time:43200}")
    private long maxBatchWaitingTime;

    @Value("${l2-relayer.rollup.economic-strategy-conf.default-max-proof-waiting-time:43200}")
    private long maxProofWaitingTime;

    public RollupEconomicStrategyConfig(
            RedissonClient redisson,
            ISystemConfigRepository systemConfigRepository,
            ScheduledExecutorService dynamicPersisterScheduledExecutors,
            @Value("${l2-relayer.rollup.economic-strategy-conf.persist-interval:60}") int persistInterval
    ) {
        super(
                "rollup-economic-strategy-conf",
                persistInterval,
                redisson,
                systemConfigRepository,
                dynamicPersisterScheduledExecutors
        );
    }

    public BigInteger getMidEip1559PriceLimit() {
        var val = super.get(Fields.midEip1559PriceLimit);
        if (StrUtil.isEmpty(val)) {
            return midEip1559PriceLimit;
        }
        return new BigInteger(val);
    }

    public BigInteger getHighEip1559PriceLimit() {
        var val = super.get(Fields.highEip1559PriceLimit);
        if (StrUtil.isEmpty(val)) {
            return highEip1559PriceLimit;
        }
        return new BigInteger(val);
    }

    public long getMaxBatchWaitingTime() {
        var val = super.get(Fields.maxBatchWaitingTime);
        if (StrUtil.isEmpty(val)) {
            return maxBatchWaitingTime;
        }
        return Long.parseLong(val);
    }

    public long getMaxProofWaitingTime() {
        var val = super.get(Fields.maxProofWaitingTime);
        if (StrUtil.isEmpty(val)) {
            return maxProofWaitingTime;
        }
        return Long.parseLong(val);
    }

    public int getMaxPendingBatchCount() {
        var val = super.get(Fields.maxPendingBatchCount);
        if (StrUtil.isEmpty(val)) {
            return maxPendingBatchCount;
        }
        return Integer.parseInt(val);
    }

    public int getMaxPendingProofCount() {
        var val = super.get(Fields.maxPendingProofCount);
        if (StrUtil.isEmpty(val)) {
            return maxPendingProofCount;
        }
        return Integer.parseInt(val);
    }

    public void setMidEip1559PriceLimit(BigInteger midEip1559PriceLimit) {
        super.set(Fields.midEip1559PriceLimit, midEip1559PriceLimit.toString());
    }

    public void setHighEip1559PriceLimit(BigInteger highEip1559PriceLimit) {
        super.set(Fields.highEip1559PriceLimit, highEip1559PriceLimit.toString());
    }

    public void setMaxBatchWaitingTime(long maxBatchWaitingTime) {
        super.set(Fields.maxBatchWaitingTime, String.valueOf(maxBatchWaitingTime));
    }

    public void setMaxProofWaitingTime(long maxProofWaitingTime) {
        super.set(Fields.maxProofWaitingTime, String.valueOf(maxProofWaitingTime));
    }

    public void setMaxPendingBatchCount(int maxPendingBatchCount) {
        super.set(Fields.maxPendingBatchCount, String.valueOf(maxPendingBatchCount));
    }

    public void setMaxPendingProofCount(int maxPendingProofCount) {
        super.set(Fields.maxPendingProofCount, String.valueOf(maxPendingProofCount));
    }
}
