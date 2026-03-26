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

package com.alipay.antchain.l2.relayer.config;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.stream.Collectors;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.l2.relayer.commons.enums.DaType;
import com.alipay.antchain.l2.relayer.commons.enums.ParentChainType;
import com.alipay.antchain.l2.relayer.commons.specs.IRollupSpecs;
import com.alipay.antchain.l2.relayer.commons.specs.RollupSpecs;
import com.alipay.antchain.l2.relayer.commons.specs.RollupSpecsNetwork;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for Rollup parameters and specifications.
 * <p>
 * This class manages the core configuration settings for the L2 Relayer's rollup mechanism,
 * including batch and chunk parameters, blob size limits, and network-specific specifications.
 * It supports multiple network environments (mainnet, testnet, and private networks) and
 * validates chain IDs against the configured specifications.
 * </p>
 * <p>
 * Key configuration areas:
 * <ul>
 *   <li>Chunk and batch gas limits</li>
 *   <li>Blob size constraints for L1 commitment</li>
 *   <li>Time interval limits between batches</li>
 *   <li>Memory usage limits for chunk serialization</li>
 *   <li>Network-specific rollup specifications</li>
 * </ul>
 * </p>
 *
 * @author Aone Copilot
 * @since 1.0
 */
@Slf4j
@Configuration
@Getter
public class RollupConfig {

    /**
     * The type of parent chain (Layer 1) where rollup batches are committed.
     * <p>
     * Determines which blockchain network serves as the settlement layer for the L2 rollup.
     * Different parent chains may have different transaction formats, gas mechanisms,
     * and data availability solutions.
     * </p>
     * <p>
     * Supported values:
     * <ul>
     *   <li>ETHEREUM: Ethereum mainnet or compatible networks</li>
     *   <li>JOVAY: Jovay blockchain network</li>
     * </ul>
     * </p>
     * <p>
     * Default: ETHEREUM
     * </p>
     */
    @Value("${l2-relayer.rollup.config.parent-chain-type:ETHEREUM}")
    private ParentChainType parentChainType;

    /**
     * The data availability (DA) type used for storing rollup transaction data.
     * <p>
     * Specifies how and where the L2 transaction data is made available for verification.
     * The DA type must be compatible with the configured parent chain type.
     * </p>
     * <p>
     * Supported values:
     * <ul>
     *   <li>BLOBS: EIP-4844 blob transactions (for Ethereum)</li>
     *   <li>DAS: Data Availability Service (for Jovay and other chains)</li>
     * </ul>
     * </p>
     * <p>
     * Default: BLOBS
     * </p>
     * <p>
     * Note: The combination of parent chain type and DA type is validated during
     * initialization to ensure compatibility.
     * </p>
     */
    @Value("${l2-relayer.rollup.da-type:BLOBS}")
    private DaType daType;

    /**
     * Recommended gas limit per chunk.
     * <p>
     * This value determines the target gas consumption for each chunk in the rollup.
     * Chunks are sealed when their accumulated gas approaches this limit.
     * </p>
     * <p>
     * Default: 23,000,000 gas
     * </p>
     */
    @Value("${l2-relayer.rollup.config.gas-per-chunk-recommended:23000000}")
    private int gasPerChunk;

    /**
     * Maximum number of blobs allowed per batch commitment on L1.
     * <p>
     * This limit is retrieved from the L1 client and determines how many EIP-4844 blobs
     * can be included in a single batch commitment transaction.
     * </p>
     */
    @Value("#{l1Client.l1BlobNumLimit().longValue()}")
    private int batchCommitBlobSizeLimit;

    /**
     * Maximum time interval (in milliseconds) allowed between consecutive batches.
     * <p>
     * If this time limit is exceeded, a new batch will be sealed even if other
     * conditions (gas limit, blob size) are not met. This ensures timely batch
     * finalization and prevents indefinite delays.
     * </p>
     */
    @Value("#{l1Client.maxTimeIntervalBetweenBatches()}")
    private long maxTimeIntervalBetweenBatches;

    /**
     * The batch index from which ZK proof verification starts.
     * <p>
     * Batches with index greater than or equal to this value will require
     * ZK proof verification in addition to TEE proof.
     * </p>
     */
    @Value("#{l1Client.zkVerificationStartBatch()}")
    private BigInteger zkVerificationStartBatch;

    /**
     * Maximum memory size (in bytes) for serialized chunks in a growing batch.
     * <p>
     * This limit prevents excessive memory usage during batch construction.
     * If the serialized chunks exceed this limit, the batch will be sealed
     * regardless of other conditions.
     * </p>
     * <p>
     * Default: 1,073,741,824 bytes (1 GB)
     * </p>
     */
    @Value("${l2-relayer.rollup.config.max-chunks-memory-used:1073741824}")
    private int maxChunksMemoryUsed;

    /**
     * The network type for rollup specifications.
     * <p>
     * Determines which specification file to load:
     * <ul>
     *   <li>MAINNET: Production network specifications</li>
     *   <li>TESTNET: Test network specifications</li>
     *   <li>PRIVATE_NET: Custom private network specifications</li>
     * </ul>
     * </p>
     */
    @Value("${l2-relayer.rollup.specs.network}")
    private RollupSpecsNetwork rollupSpecsNetwork;

    /**
     * Path to the private network specifications file.
     * <p>
     * Only used when {@code rollupSpecsNetwork} is set to PRIVATE_NET.
     * Must be a valid resource path containing the network specifications in JSON format.
     * </p>
     */
    @Value("${l2-relayer.rollup.specs.private-net.specs-file:null}")
    private org.springframework.core.io.Resource privateNetFile;

    /**
     * The chain ID of the Layer 2 network.
     * <p>
     * Used to validate that the configured specifications match the actual L2 network.
     * </p>
     */
    @Resource
    private BigInteger l2ChainId;

    /**
     * The chain ID of the Layer 1 network.
     * <p>
     * Used to validate that the configured specifications match the actual L1 network
     * where batches are committed.
     * </p>
     */
    @Resource
    private BigInteger l1ChainId;

    /**
     * Validates the compatibility between parent chain type and data availability type.
     * <p>
     * This method is automatically invoked after the bean's properties have been set.
     * It ensures that the configured DA type is supported by the selected parent chain type.
     * </p>
     * <p>
     * For example:
     * <ul>
     *   <li>Ethereum parent chain supports BLOBS DA type</li>
     *   <li>Jovay parent chain supports DAS DA type</li>
     * </ul>
     * </p>
     *
     * @throws IllegalStateException if the DA type is not supported by the parent chain type
     */
    @PostConstruct
    public void validate() {
        if (!parentChainType.supportedDaTypes().contains(daType)) {
            throw new IllegalStateException(StrUtil.format(
                    "The DA type {} not supported for parent chain {}, and parent chain only supports {}",
                    daType, parentChainType, parentChainType.supportedDaTypes().stream().map(DaType::name).collect(Collectors.joining(", ", "[", "]"))
            ));
        }
    }

    /**
     * Creates and configures the rollup specifications bean based on the network type.
     * <p>
     * This method loads the appropriate specification file (mainnet, testnet, or private network)
     * and validates that the chain IDs in the specifications match the actual network chain IDs.
     * </p>
     *
     * @return the configured rollup specifications instance
     * @throws IllegalArgumentException if chain IDs don't match the specifications
     * @throws RuntimeException         if private network file is not configured when needed
     */
    @Bean
    public IRollupSpecs rollupSpecs() {
        return switch (rollupSpecsNetwork) {
            case MAINNET -> readMainnetSpecs();
            case TESTNET -> readTestnetSpecs();
            case PRIVATE_NET -> readPrivateNetSpecs();
        };
    }

    /**
     * Reads and validates mainnet rollup specifications.
     * <p>
     * Loads the mainnet specifications from the bundled resource file and validates
     * that both L1 and L2 chain IDs match the actual network configuration.
     * </p>
     *
     * @return the mainnet rollup specifications
     * @throws IllegalArgumentException if chain IDs don't match
     */
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

    /**
     * Reads and validates testnet rollup specifications.
     * <p>
     * Loads the testnet specifications from the bundled resource file and validates
     * that both L1 and L2 chain IDs match the actual network configuration.
     * </p>
     *
     * @return the testnet rollup specifications
     * @throws IllegalArgumentException if chain IDs don't match
     */
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

    /**
     * Reads and validates private network rollup specifications.
     * <p>
     * Loads the specifications from a custom file path specified in the configuration
     * and validates that both L1 and L2 chain IDs match the actual network configuration.
     * </p>
     * <p>
     * The private network file must be configured via the
     * {@code l2-relayer.rollup.specs.private-net-file} property.
     * </p>
     *
     * @return the private network rollup specifications
     * @throws RuntimeException         if the private network file is not configured
     * @throws IllegalArgumentException if chain IDs don't match
     */
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
