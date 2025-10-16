package com.alipay.antchain.l2.relayer.config;

import java.math.BigInteger;
import java.nio.charset.Charset;
import jakarta.annotation.Resource;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjectUtil;
import com.alipay.antchain.l2.relayer.commons.specs.IRollupSpecs;
import com.alipay.antchain.l2.relayer.commons.specs.RollupSpecs;
import com.alipay.antchain.l2.relayer.commons.specs.RollupSpecsNetwork;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
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

    @Getter
    @Value("${l2-relayer.rollup.specs.network}")
    private RollupSpecsNetwork rollupSpecsNetwork;

    @Value("${l2-relayer.rollup.specs.private-net.specs-file:null}")
    private org.springframework.core.io.Resource privateNetFile;

    @Resource
    private BigInteger l2ChainId;

    @Resource
    private BigInteger l1ChainId;

    @Bean
    public IRollupSpecs rollupSpecs() {
        return switch (rollupSpecsNetwork) {
            case MAINNET -> readMainnetSpecs();
            case TESTNET -> readTestnetSpecs();
            case PRIVATE_NET -> readPrivateNetSpecs();
        };
    }

    private IRollupSpecs readMainnetSpecs() {
        log.info("🐤 reading Jovay mainnet rollup specs");
        var specs = RollupSpecs.fromJson(ResourceUtil.readStr("specs/mainnet.json", Charset.defaultCharset()));
        Assert.equals(
                specs.getLayer1ChainId(), l1ChainId,
                "l1ChainId {} from node not match with the one {} from specs",
                l1ChainId, specs.getLayer1ChainId()
        );
        Assert.equals(
                specs.getLayer2ChainId(), l2ChainId,
                "l2ChainId {} from node not match with the one {} from specs",
                l2ChainId, specs.getLayer2ChainId()
        );
        return specs;
    }

    private IRollupSpecs readTestnetSpecs() {
        log.info("🐥 reading Jovay testnet rollup specs");
        var specs = RollupSpecs.fromJson(ResourceUtil.readStr("specs/testnet.json", Charset.defaultCharset()));
        Assert.equals(
                specs.getLayer1ChainId(), l1ChainId,
                "l1ChainId {} from node not match with the one {} from specs",
                l1ChainId, specs.getLayer1ChainId()
        );
        Assert.equals(
                specs.getLayer2ChainId(), l2ChainId,
                "l2ChainId {} from node not match with the one {} from specs",
                l2ChainId, specs.getLayer2ChainId()
        );
        return specs;
    }

    @SneakyThrows
    private IRollupSpecs readPrivateNetSpecs() {
        log.info("🐣 read private net specs from {}", privateNetFile.getURI());
        if (ObjectUtil.isNull(privateNetFile)) {
            throw new RuntimeException("set l2-relayer.rollup.specs.private-net-file first");
        }
        var specs = RollupSpecs.fromJson(privateNetFile.getContentAsString(Charset.defaultCharset()));
        Assert.equals(
                specs.getLayer1ChainId(), l1ChainId,
                "l1ChainId {} from node not match with the one {} from specs",
                l1ChainId, specs.getLayer1ChainId()
        );
        Assert.equals(
                specs.getLayer2ChainId(), l2ChainId,
                "l2ChainId {} from node not match with the one {} from specs",
                l2ChainId, specs.getLayer2ChainId()
        );
        return specs;
    }
}
