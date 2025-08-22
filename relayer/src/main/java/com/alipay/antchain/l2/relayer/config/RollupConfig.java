package com.alipay.antchain.l2.relayer.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class RollupConfig {

    @Value("#{l1Client.maxTxsInChunk().intValue()}")
    private int maxTxsInChunks;

    @Value("#{l1Client.maxBlockInChunk().longValue()}")
    private long oneChunkBlocksLimit;

    @Value("#{l1Client.maxCallDataInChunk().longValue()}")
    private long maxCallDataInChunk;

    @Value("#{l1Client.maxZkCircleInChunk().longValue()}")
    private long chunkZkCycleSumLimit;

    @Value("#{l1Client.l1BlobNumLimit().longValue()}")
    private int batchCommitBlobSizeLimit;

    @Value("#{l1Client.maxTimeIntervalBetweenBatches()}")
    private long maxTimeIntervalBetweenBatches;
}
