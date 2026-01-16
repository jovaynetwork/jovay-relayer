package com.alipay.antchain.l2.relayer.dal;

import java.math.BigInteger;
import java.util.List;

import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSON;
import com.alipay.antchain.l2.prover.controller.ProverControllerServerGrpc;
import com.alipay.antchain.l2.relayer.L2RelayerApplication;
import com.alipay.antchain.l2.relayer.TestBase;
import com.alipay.antchain.l2.relayer.commons.enums.OracleRequestTypeEnum;
import com.alipay.antchain.l2.relayer.commons.enums.OracleTransactionStateEnum;
import com.alipay.antchain.l2.relayer.commons.enums.OracleTypeEnum;
import com.alipay.antchain.l2.relayer.commons.models.L1BlockFeeInfo;
import com.alipay.antchain.l2.relayer.commons.models.OracleRequestDO;
import com.alipay.antchain.l2.relayer.config.RollupConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.L1Client;
import com.alipay.antchain.l2.relayer.core.blockchain.L2Client;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.gasprice.L1GasPriceProviderConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.gasprice.L2GasPriceProviderConfig;
import com.alipay.antchain.l2.relayer.core.layer2.economic.RollupEconomicStrategyConfig;
import com.alipay.antchain.l2.relayer.dal.repository.OracleRepository;
import com.alipay.antchain.l2.relayer.service.IOracleService;
import com.alipay.antchain.l2.tracer.TraceServiceGrpc;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.web3j.protocol.ObjectMapperFactory;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import static org.mockito.Mockito.when;

@SpringBootTest(
        classes = L2RelayerApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {"spring.flyway.enabled=false"}
)
@Sql(scripts = {"classpath:data/ddl.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:data/drop_all.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
public class OracleRepositoryTest extends TestBase {
    @MockitoBean(name = "prover-client")
    private ProverControllerServerGrpc.ProverControllerServerBlockingStub proverStub;

    @MockitoBean(name = "tracer-client")
    private TraceServiceGrpc.TraceServiceBlockingStub tracerStub;

    @MockitoBean
    private L1Client l1Client;

    @MockitoBean
    private L2Client l2Client;

    @MockitoBean
    private RollupConfig rollupConfig;

    @MockitoBean
    private IOracleService oracleService;

    @MockitoBean
    private RollupEconomicStrategyConfig rollupEconomicStrategyConfig;

    @MockitoBean
    private L1GasPriceProviderConfig l1GasPriceProviderConfig;

    @MockitoBean
    private L2GasPriceProviderConfig l2GasPriceProviderConfig;

    @Resource
    private OracleRepository oracleRepository1;

    private static final int perRoundLimit = 10;
    private static final String mockTxHash = "0x05f71e1b2cb4f03e547739db15d080fd30c989eda04d37ce6264c5686e0722c9";

    public abstract class BlockMixIn {
        @JsonIgnore
        private String totalDifficulty;
    }

    @Before
    public void initMock() {
        when(l1Client.maxCallDataInChunk()).thenReturn(BigInteger.valueOf(1000_000));
        when(l1Client.maxBlockInChunk()).thenReturn(BigInteger.valueOf(32));
        when(l1Client.maxTxsInChunk()).thenReturn(BigInteger.valueOf(1000));
        when(l1Client.maxZkCircleInChunk()).thenReturn(BigInteger.valueOf(940000));
        when(l1Client.l1BlobNumLimit()).thenReturn(4L);

        when(rollupConfig.getMaxCallDataInChunk()).thenReturn(1000_000L);
        when(rollupConfig.getOneChunkBlocksLimit()).thenReturn(32L);
        when(rollupConfig.getMaxTxsInChunks()).thenReturn(1000);
    }

    @Test
    @SneakyThrows
    public void testPeekRequest() {
        int number = RandomUtil.randomInt();
        EthBlock ethBlock = mockBlockHeader(number, RandomUtil.randomLong(), RandomUtil.randomLong(), RandomUtil.randomLong(), RandomUtil.randomLong(), RandomUtil.randomLong());
        L1BlockFeeInfo l1BlockFeeInfo = L1BlockFeeInfo.builder()
                .number(String.valueOf(ethBlock.getBlock().getNumber()))
                .baseFeePerGas(String.valueOf(ethBlock.getBlock().getBaseFeePerGas()))
                .gasUsed(String.valueOf(ethBlock.getBlock().getGasUsed()))
                .gasLimit(String.valueOf(ethBlock.getBlock().getGasLimit()))
                .blobGasUsed(String.valueOf(ethBlock.getBlock().getBlobGasUsed()))
                .excessBlobGas(String.valueOf(ethBlock.getBlock().getExcessBlobGas()))
                .build();
        oracleRepository1.saveBlockFeeInfo(l1BlockFeeInfo);

        List<OracleRequestDO> oracleRequestDOS = oracleRepository1.peekRequests(OracleTypeEnum.L2_GAS_ORACLE, OracleRequestTypeEnum.L1_BLOCK_UPDATE, OracleTransactionStateEnum.INIT, perRoundLimit);
        Assert.assertNotNull(oracleRequestDOS);
        Assert.assertEquals(1, oracleRequestDOS.size());

        BigInteger index = oracleRequestDOS.get(0).getRequestIndex();
        Assert.assertNotNull(index);
        Assert.assertEquals(index, BigInteger.valueOf(number));
        Assert.assertNotNull(oracleRequestDOS.get(0).getRawData());

        // select empty
        List<OracleRequestDO> emptyOracleRequestDOS = oracleRepository1.peekRequests(OracleTypeEnum.L1_GAS_ORACLE, OracleRequestTypeEnum.L2_VAULT_WITHDRAW, OracleTransactionStateEnum.COMMITED, 1);
        Assert.assertEquals(0, emptyOracleRequestDOS.size());
    }

    @Test
    @SneakyThrows
    public void testPeekLatestRequest() {
        EthBlock ethBlock = mockBlockHeader(1, RandomUtil.randomLong(), RandomUtil.randomLong(), RandomUtil.randomLong(), RandomUtil.randomLong(), RandomUtil.randomLong());
        L1BlockFeeInfo l1BlockFeeInfo = L1BlockFeeInfo.builder()
                .number(String.valueOf(ethBlock.getBlock().getNumber()))
                .baseFeePerGas(String.valueOf(ethBlock.getBlock().getBaseFeePerGas()))
                .gasUsed(String.valueOf(ethBlock.getBlock().getGasUsed()))
                .gasLimit(String.valueOf(ethBlock.getBlock().getGasLimit()))
                .blobGasUsed(String.valueOf(ethBlock.getBlock().getBlobGasUsed()))
                .excessBlobGas(String.valueOf(ethBlock.getBlock().getExcessBlobGas()))
                .build();
        oracleRepository1.saveBlockFeeInfo(l1BlockFeeInfo);


        l1BlockFeeInfo.setNumber(String.valueOf(2));
        oracleRepository1.saveBlockFeeInfo(l1BlockFeeInfo);

        // select empty
        OracleRequestDO oracleRequestDO1 = oracleRepository1.peekLatestRequest(OracleTypeEnum.L1_GAS_ORACLE, OracleRequestTypeEnum.L2_VAULT_WITHDRAW, OracleTransactionStateEnum.COMMITED);
        Assert.assertNull(oracleRequestDO1);

        // test peek latest
        OracleRequestDO oracleRequestDO2 = oracleRepository1.peekLatestRequest(OracleTypeEnum.L2_GAS_ORACLE, OracleRequestTypeEnum.L1_BLOCK_UPDATE, OracleTransactionStateEnum.INIT);
        Assert.assertNotNull(oracleRequestDO2);
        Assert.assertEquals(BigInteger.valueOf(2), oracleRequestDO2.getRequestIndex());
        Assert.assertEquals(OracleTypeEnum.L2_GAS_ORACLE, oracleRequestDO2.getOracleType());

        var index = oracleRepository1.peekLatestRequestIndex(OracleTypeEnum.L2_GAS_ORACLE, OracleRequestTypeEnum.L1_BLOCK_UPDATE, OracleTransactionStateEnum.INIT);
        Assert.assertEquals(BigInteger.valueOf(2), index);
    }

    @Test
    @SneakyThrows
    public void testPeekRequestByTypeAndIndex() {
        // save and peek block
        int number = RandomUtil.randomInt();
        EthBlock ethBlock = mockBlockHeader(number, RandomUtil.randomLong(), RandomUtil.randomLong(), RandomUtil.randomLong(), RandomUtil.randomLong(), RandomUtil.randomLong());
        L1BlockFeeInfo l1BlockFeeInfo = L1BlockFeeInfo.builder()
                .number(String.valueOf(ethBlock.getBlock().getNumber()))
                .baseFeePerGas(String.valueOf(ethBlock.getBlock().getBaseFeePerGas()))
                .gasUsed(String.valueOf(ethBlock.getBlock().getGasUsed()))
                .gasLimit(String.valueOf(ethBlock.getBlock().getGasLimit()))
                .blobGasUsed(String.valueOf(ethBlock.getBlock().getBlobGasUsed()))
                .excessBlobGas(String.valueOf(ethBlock.getBlock().getExcessBlobGas()))
                .build();
        oracleRepository1.saveBlockFeeInfo(l1BlockFeeInfo);

        OracleRequestDO oracleRequestDO1 = oracleRepository1.peekRequestByTypeAndIndex(OracleTypeEnum.L2_GAS_ORACLE, OracleRequestTypeEnum.L1_BLOCK_UPDATE, String.valueOf(ethBlock.getBlock().getNumber()));
        Assert.assertNotNull(oracleRequestDO1);
        Assert.assertNotNull(oracleRequestDO1.getRequestIndex());
        Assert.assertEquals(ethBlock.getBlock().getNumber(), oracleRequestDO1.getRequestIndex());

        EthBlock.Block block = ObjectMapperFactory.getObjectMapper().readValue(oracleRequestDO1.getRawData(), EthBlock.Block.class);
        Assert.assertNotNull(block);
        Assert.assertNotNull(block.getNumber());
        Assert.assertEquals(ethBlock.getBlock().getNumber(), block.getNumber());

        // save and peek transaction receipt
        long txIndex = RandomUtil.randomLong();
        TransactionReceipt txReceipt = mockTransactionReceipt(txIndex);
        int batchIndex = RandomUtil.randomInt();
        oracleRepository1.saveRollupTxReceipt(BigInteger.valueOf(batchIndex), OracleTypeEnum.L2_GAS_ORACLE, OracleRequestTypeEnum.L2_BATCH_COMMIT, txReceipt);

        OracleRequestDO oracleRequestDO = oracleRepository1.peekRequestByTypeAndIndex(OracleTypeEnum.L2_GAS_ORACLE, OracleRequestTypeEnum.L2_BATCH_COMMIT, String.valueOf(batchIndex));
        Assert.assertNotNull(oracleRequestDO);
        Assert.assertEquals(oracleRequestDO.getRequestIndex(), BigInteger.valueOf(batchIndex));
        Assert.assertNotNull(oracleRequestDO.getRawData());

        TransactionReceipt getTxReceipt = JSON.parseObject(oracleRequestDO.getRawData(), TransactionReceipt.class);
        Assert.assertNotNull(getTxReceipt);
        Assert.assertNotNull(getTxReceipt.getTransactionIndex());
        Assert.assertEquals(getTxReceipt.getTransactionIndex(), BigInteger.valueOf(txIndex));

        // select empty
        List<OracleRequestDO> emptyOracleRequestDOS = oracleRepository1.peekRequests(OracleTypeEnum.L1_GAS_ORACLE, OracleRequestTypeEnum.L2_VAULT_WITHDRAW, OracleTransactionStateEnum.COMMITED, 1);
        Assert.assertEquals(0, emptyOracleRequestDOS.size());
    }

    @Test
    @SneakyThrows
    public void testSaveBlockFeeInfo() {
        int mockNumber = RandomUtil.randomInt();
        L1BlockFeeInfo mockL1BlockFee = L1BlockFeeInfo.builder()
                .number(String.valueOf(mockNumber))
                .baseFeePerGas(String.valueOf(RandomUtil.randomLong()))
                .gasUsed(String.valueOf(RandomUtil.randomLong()))
                .gasLimit(String.valueOf(RandomUtil.randomLong()))
                .blobGasUsed(String.valueOf(RandomUtil.randomLong()))
                .excessBlobGas(String.valueOf(RandomUtil.randomLong()))
                .build();
        oracleRepository1.saveBlockFeeInfo(mockL1BlockFee);

        List<OracleRequestDO> oracleRequestDOS = oracleRepository1.peekRequests(OracleTypeEnum.L2_GAS_ORACLE, OracleRequestTypeEnum.L1_BLOCK_UPDATE, OracleTransactionStateEnum.INIT, 1);
        Assert.assertNotNull(oracleRequestDOS);
        Assert.assertEquals(1, oracleRequestDOS.size());
        Assert.assertNotNull(oracleRequestDOS.get(0).getRequestIndex());
        Assert.assertEquals(BigInteger.valueOf(mockNumber), oracleRequestDOS.get(0).getRequestIndex());
    }

    @Test
    @SneakyThrows
    public void testSaveRollupTxReceipt() {
        long txIndex = RandomUtil.randomLong();
        TransactionReceipt txReceipt = mockTransactionReceipt(txIndex);
        int batchIndex = RandomUtil.randomInt();
        oracleRepository1.saveRollupTxReceipt(BigInteger.valueOf(batchIndex), OracleTypeEnum.L2_GAS_ORACLE, OracleRequestTypeEnum.L2_BATCH_COMMIT, txReceipt);

        List<OracleRequestDO> oracleRequestDOS = oracleRepository1.peekRequests(OracleTypeEnum.L2_GAS_ORACLE, OracleRequestTypeEnum.L2_BATCH_COMMIT, OracleTransactionStateEnum.INIT, perRoundLimit);
        Assert.assertNotNull(oracleRequestDOS);
        Assert.assertEquals(1, oracleRequestDOS.size());
        Assert.assertNotNull(oracleRequestDOS.get(0).getRequestIndex());
        Assert.assertEquals(batchIndex, oracleRequestDOS.get(0).getRequestIndex().intValue());

        List<OracleRequestDO> oracleRequestDOS1 = oracleRepository1.peekRequests(OracleTypeEnum.L1_GAS_ORACLE, OracleRequestTypeEnum.L2_VAULT_WITHDRAW, OracleTransactionStateEnum.COMMITED, 1);
        Assert.assertEquals(0, oracleRequestDOS1.size());
    }

    @Test
    @SneakyThrows
    public void testUpdateRequestState() {
        // test COMMITED
        int mockNumber = RandomUtil.randomInt();
        EthBlock ethBlock = mockBlockHeader(mockNumber, RandomUtil.randomLong(), RandomUtil.randomLong(), RandomUtil.randomLong(), RandomUtil.randomLong(), RandomUtil.randomLong());
        L1BlockFeeInfo l1BlockFeeInfo = L1BlockFeeInfo.builder()
                .number(String.valueOf(ethBlock.getBlock().getNumber()))
                .baseFeePerGas(String.valueOf(ethBlock.getBlock().getBaseFeePerGas()))
                .gasUsed(String.valueOf(ethBlock.getBlock().getGasUsed()))
                .gasLimit(String.valueOf(ethBlock.getBlock().getGasLimit()))
                .blobGasUsed(String.valueOf(ethBlock.getBlock().getBlobGasUsed()))
                .excessBlobGas(String.valueOf(ethBlock.getBlock().getExcessBlobGas()))
                .build();
        oracleRepository1.saveBlockFeeInfo(l1BlockFeeInfo);

        oracleRepository1.updateRequestState(String.valueOf(ethBlock.getBlock().getNumber()), OracleTypeEnum.L2_GAS_ORACLE, OracleRequestTypeEnum.L1_BLOCK_UPDATE, OracleTransactionStateEnum.COMMITED);

        OracleRequestDO oracleRequestDO = oracleRepository1.peekRequestByTypeAndIndex(OracleTypeEnum.L2_GAS_ORACLE, OracleRequestTypeEnum.L1_BLOCK_UPDATE, String.valueOf(ethBlock.getBlock().getNumber()));
        Assert.assertNotNull(oracleRequestDO);
        Assert.assertEquals(oracleRequestDO.getTxState(), OracleTransactionStateEnum.COMMITED);

        // test SKIP
        long txIndex = RandomUtil.randomLong();
        TransactionReceipt txReceipt = mockTransactionReceipt(txIndex);
        int batchIndex = RandomUtil.randomInt();
        oracleRepository1.saveRollupTxReceipt(BigInteger.valueOf(batchIndex), OracleTypeEnum.L2_GAS_ORACLE, OracleRequestTypeEnum.L2_BATCH_COMMIT, txReceipt);

        oracleRepository1.updateRequestState(String.valueOf(batchIndex), OracleTypeEnum.L2_GAS_ORACLE, OracleRequestTypeEnum.L2_BATCH_COMMIT, OracleTransactionStateEnum.SKIP);

        OracleRequestDO oracleRequestDO1 = oracleRepository1.peekRequestByTypeAndIndex(OracleTypeEnum.L2_GAS_ORACLE, OracleRequestTypeEnum.L2_BATCH_COMMIT, String.valueOf(batchIndex));
        Assert.assertNotNull(oracleRequestDO1);
        Assert.assertEquals(oracleRequestDO1.getTxState(), OracleTransactionStateEnum.SKIP);
    }

    @Test
    @SneakyThrows
    public void testUpdateOracleRequestState() {
        // test COMMITED
        int mockNumber = RandomUtil.randomInt();
        EthBlock ethBlock = mockBlockHeader(mockNumber, RandomUtil.randomLong(), RandomUtil.randomLong(), RandomUtil.randomLong(), RandomUtil.randomLong(), RandomUtil.randomLong());
        L1BlockFeeInfo l1BlockFeeInfo = L1BlockFeeInfo.builder()
                .number(String.valueOf(ethBlock.getBlock().getNumber()))
                .baseFeePerGas(String.valueOf(ethBlock.getBlock().getBaseFeePerGas()))
                .gasUsed(String.valueOf(ethBlock.getBlock().getGasUsed()))
                .gasLimit(String.valueOf(ethBlock.getBlock().getGasLimit()))
                .blobGasUsed(String.valueOf(ethBlock.getBlock().getBlobGasUsed()))
                .excessBlobGas(String.valueOf(ethBlock.getBlock().getExcessBlobGas()))
                .build();
        oracleRepository1.saveBlockFeeInfo(l1BlockFeeInfo);

        oracleRepository1.updateRequestState(String.valueOf(ethBlock.getBlock().getNumber()), OracleTypeEnum.L2_GAS_ORACLE, OracleRequestTypeEnum.L1_BLOCK_UPDATE, OracleTransactionStateEnum.COMMITED);

        OracleRequestDO oracleRequestDO = oracleRepository1.peekRequestByTypeAndIndex(OracleTypeEnum.L2_GAS_ORACLE, OracleRequestTypeEnum.L1_BLOCK_UPDATE, String.valueOf(ethBlock.getBlock().getNumber()));
        Assert.assertNotNull(oracleRequestDO);
        Assert.assertEquals(oracleRequestDO.getTxState(), OracleTransactionStateEnum.COMMITED);

        // test SKIP
        long txIndex = RandomUtil.randomLong();
        TransactionReceipt txReceipt = mockTransactionReceipt(txIndex);
        int batchIndex = RandomUtil.randomInt();
        oracleRepository1.saveRollupTxReceipt(BigInteger.valueOf(batchIndex), OracleTypeEnum.L2_GAS_ORACLE, OracleRequestTypeEnum.L2_BATCH_COMMIT, txReceipt);

        oracleRepository1.updateRequestState(String.valueOf(batchIndex), OracleTypeEnum.L2_GAS_ORACLE, OracleRequestTypeEnum.L2_BATCH_COMMIT, OracleTransactionStateEnum.SKIP);

        OracleRequestDO oracleRequestDO1 = oracleRepository1.peekRequestByTypeAndIndex(OracleTypeEnum.L2_GAS_ORACLE, OracleRequestTypeEnum.L2_BATCH_COMMIT, String.valueOf(batchIndex));
        Assert.assertNotNull(oracleRequestDO1);
        Assert.assertEquals(oracleRequestDO1.getTxState(), OracleTransactionStateEnum.SKIP);
    }

    @Test
    @SneakyThrows
    public void testSerializeAndDeserializeBlock() {
        int number = RandomUtil.randomInt();
        EthBlock ethBlock = mockBlockHeader(number, RandomUtil.randomLong(), RandomUtil.randomLong(), RandomUtil.randomLong(), RandomUtil.randomLong(), RandomUtil.randomLong());
        byte[] rawBytes = ObjectMapperFactory.getObjectMapper().addMixIn(EthBlock.Block.class, BlockMixIn.class).writeValueAsBytes(ethBlock.getBlock());

        // deserialize
        EthBlock.Block block = ObjectMapperFactory.getObjectMapper().readValue(rawBytes, EthBlock.Block.class);
        Assert.assertNotNull(block);
        Assert.assertNotNull(block.getBaseFeePerGas());
        Assert.assertNotNull(block.getGasUsed());
        Assert.assertNotNull(block.getGasLimit());
        Assert.assertNotNull(block.getBlobGasUsed());
        Assert.assertNotNull(block.getExcessBlobGas());
    }

    @Test
    @SneakyThrows
    public void testSerializeAndDeserializeRollupTxReceipt() {
        // serialize
        long txIndex = RandomUtil.randomLong();
        TransactionReceipt txReceipt = mockTransactionReceipt(txIndex);
        byte[] txReceiptBytes = JSON.toJSONBytes(txReceipt);

        // deserialize
        TransactionReceipt getTxReceipt = JSON.parseObject(txReceiptBytes, TransactionReceipt.class);
        Assert.assertNotNull(getTxReceipt);
        Assert.assertNotNull(getTxReceipt.getTransactionIndex());
        Assert.assertEquals(getTxReceipt.getTransactionIndex(), BigInteger.valueOf(txIndex));
    }

    private EthBlock mockBlockHeader(int number, long baseFeePerGas, long gasUsed, long gasLimit, long blobGasUsed, long excessBlobGas) {
        EthBlock mockEthBlock = new EthBlock();
        EthBlock.Block mockBlock = new EthBlock.Block();
        mockBlock.setNumber(String.valueOf(number));
        mockBlock.setDifficulty("0x1");
        mockBlock.setNonce("0x1");
        mockBlock.setTimestamp("0x1");
        // mockBlock.setTotalDifficulty("0x1");

        mockBlock.setBaseFeePerGas(String.valueOf(baseFeePerGas));
        mockBlock.setGasUsed(String.valueOf(gasUsed));
        mockBlock.setGasLimit(String.valueOf(gasLimit));
        mockBlock.setBlobGasUsed(String.valueOf(blobGasUsed));
        mockBlock.setExcessBlobGas(String.valueOf(excessBlobGas));

        mockEthBlock.setResult(mockBlock);
        return mockEthBlock;
    }

    private TransactionReceipt mockTransactionReceipt(long txIndex) {
        TransactionReceipt transactionReceipt = new TransactionReceipt();
        transactionReceipt.setBlobGasPrice(String.valueOf(RandomUtil.randomLong()));
        transactionReceipt.setGasUsed(String.valueOf(RandomUtil.randomLong()));
        transactionReceipt.setBlockNumber(String.valueOf(RandomUtil.randomLong()));
        transactionReceipt.setCumulativeGasUsed(String.valueOf(RandomUtil.randomLong()));
        transactionReceipt.setTransactionIndex(String.valueOf(txIndex));

        transactionReceipt.setEffectiveGasPrice(String.valueOf(RandomUtil.randomLong()));
        transactionReceipt.setGasUsed(String.valueOf(RandomUtil.randomLong()));
        transactionReceipt.setBlobGasPrice(String.valueOf(RandomUtil.randomLong()));
        transactionReceipt.setBlobGasUsed(String.valueOf(RandomUtil.randomLong()));

        return transactionReceipt;
    }

    // ==================== Negative Case Tests ====================

    /**
     * Test duplicate block fee info insertion
     * Verifies that duplicate block fee info throws RuntimeException
     */
    @Test
    @SneakyThrows
    public void testSaveBlockFeeInfo_DuplicateInsertion() {
        int mockNumber = RandomUtil.randomInt();
        L1BlockFeeInfo mockL1BlockFee = L1BlockFeeInfo.builder()
                .number(String.valueOf(mockNumber))
                .baseFeePerGas(String.valueOf(RandomUtil.randomLong()))
                .gasUsed(String.valueOf(RandomUtil.randomLong()))
                .gasLimit(String.valueOf(RandomUtil.randomLong()))
                .blobGasUsed(String.valueOf(RandomUtil.randomLong()))
                .excessBlobGas(String.valueOf(RandomUtil.randomLong()))
                .build();

        // Save first time
        oracleRepository1.saveBlockFeeInfo(mockL1BlockFee);

        // Save second time - should throw RuntimeException
        try {
            oracleRepository1.saveBlockFeeInfo(mockL1BlockFee);
            Assert.fail("Expected RuntimeException for duplicate insertion");
        } catch (RuntimeException e) {
            // Expected exception
            Assert.assertTrue(e.getMessage().contains("save block header to DB failed"));
        }
    }

    /**
     * Test saving rollup transaction receipt with duplicate batch index
     * Verifies that duplicate transaction receipts throw RuntimeException
     */
    @Test
    @SneakyThrows
    public void testSaveRollupTxReceipt_DuplicateBatchIndex() {
        long txIndex = RandomUtil.randomLong();
        TransactionReceipt txReceipt = mockTransactionReceipt(txIndex);
        int batchIndex = RandomUtil.randomInt();

        // Save first time
        oracleRepository1.saveRollupTxReceipt(
                BigInteger.valueOf(batchIndex),
                OracleTypeEnum.L2_GAS_ORACLE,
                OracleRequestTypeEnum.L2_BATCH_COMMIT,
                txReceipt
        );

        // Save second time with same batch index - should throw RuntimeException
        try {
            oracleRepository1.saveRollupTxReceipt(
                    BigInteger.valueOf(batchIndex),
                    OracleTypeEnum.L2_GAS_ORACLE,
                    OracleRequestTypeEnum.L2_BATCH_COMMIT,
                    txReceipt
            );
            Assert.fail("Expected RuntimeException for duplicate batch index");
        } catch (RuntimeException e) {
            // Expected exception
            Assert.assertTrue(e.getMessage().contains("insert rollup gas oracle request record to DB failed"));
        }
    }

    /**
     * Test peek request with invalid parameters
     * Verifies that querying with non-existent type/state returns empty list
     */
    @Test
    @SneakyThrows
    public void testPeekRequest_InvalidParameters() {
        // Save some data first
        int mockNumber = RandomUtil.randomInt();
        L1BlockFeeInfo mockL1BlockFee = L1BlockFeeInfo.builder()
                .number(String.valueOf(mockNumber))
                .baseFeePerGas(String.valueOf(RandomUtil.randomLong()))
                .gasUsed(String.valueOf(RandomUtil.randomLong()))
                .gasLimit(String.valueOf(RandomUtil.randomLong()))
                .blobGasUsed(String.valueOf(RandomUtil.randomLong()))
                .excessBlobGas(String.valueOf(RandomUtil.randomLong()))
                .build();
        oracleRepository1.saveBlockFeeInfo(mockL1BlockFee);

        // Query with wrong oracle type
        List<OracleRequestDO> result1 = oracleRepository1.peekRequests(
                OracleTypeEnum.L1_GAS_ORACLE,
                OracleRequestTypeEnum.L1_BLOCK_UPDATE,
                OracleTransactionStateEnum.INIT,
                10
        );
        Assert.assertEquals(0, result1.size());

        // Query with wrong request type
        List<OracleRequestDO> result2 = oracleRepository1.peekRequests(
                OracleTypeEnum.L2_GAS_ORACLE,
                OracleRequestTypeEnum.L2_VAULT_WITHDRAW,
                OracleTransactionStateEnum.INIT,
                10
        );
        Assert.assertEquals(0, result2.size());

        // Query with wrong state
        List<OracleRequestDO> result3 = oracleRepository1.peekRequests(
                OracleTypeEnum.L2_GAS_ORACLE,
                OracleRequestTypeEnum.L1_BLOCK_UPDATE,
                OracleTransactionStateEnum.COMMITED,
                10
        );
        Assert.assertEquals(0, result3.size());
    }

    /**
     * Test peek request by type and index with non-existent index
     * Verifies that querying non-existent data returns null
     */
    @Test
    @SneakyThrows
    public void testPeekRequestByTypeAndIndex_NonExistentIndex() {
        OracleRequestDO result = oracleRepository1.peekRequestByTypeAndIndex(
                OracleTypeEnum.L2_GAS_ORACLE,
                OracleRequestTypeEnum.L1_BLOCK_UPDATE,
                String.valueOf(999999)
        );
        Assert.assertNull(result);
    }

    /**
     * Test update request state for non-existent request
     * Verifies that updating non-existent request throws IllegalArgumentException
     */
    @Test
    @SneakyThrows
    public void testUpdateRequestState_NonExistentRequest() {
        // Should throw IllegalArgumentException when updating non-existent request
        try {
            oracleRepository1.updateRequestState(
                    String.valueOf(999999),
                    OracleTypeEnum.L2_GAS_ORACLE,
                    OracleRequestTypeEnum.L1_BLOCK_UPDATE,
                    OracleTransactionStateEnum.COMMITED
            );
            Assert.fail("Expected IllegalArgumentException for non-existent request");
        } catch (IllegalArgumentException e) {
            // Expected exception - update count must be 1
            Assert.assertTrue(e.getMessage().contains("must be equals"));
        }
    }

    /**
     * Test peek latest request with empty database
     * Verifies that querying empty database returns null
     */
    @Test
    @SneakyThrows
    public void testPeekLatestRequest_EmptyDatabase() {
        OracleRequestDO result = oracleRepository1.peekLatestRequest(
                OracleTypeEnum.L2_GAS_ORACLE,
                OracleRequestTypeEnum.L1_BLOCK_UPDATE,
                OracleTransactionStateEnum.INIT
        );
        Assert.assertNull(result);
    }

    /**
     * Test peek latest request index with empty database
     * Verifies that querying empty database returns null
     */
    @Test
    @SneakyThrows
    public void testPeekLatestRequestIndex_EmptyDatabase() {
        BigInteger result = oracleRepository1.peekLatestRequestIndex(
                OracleTypeEnum.L2_GAS_ORACLE,
                OracleRequestTypeEnum.L1_BLOCK_UPDATE,
                OracleTransactionStateEnum.INIT
        );
        Assert.assertNull(result);
    }

    /**
     * Test save block fee info with null values
     * Verifies that saving block with null fields is handled correctly
     */
    @Test
    @SneakyThrows
    public void testSaveBlockFeeInfo_WithNullFields() {
        int mockNumber = RandomUtil.randomInt();
        L1BlockFeeInfo mockL1BlockFee = L1BlockFeeInfo.builder()
                .number(String.valueOf(mockNumber))
                .baseFeePerGas(null)  // null field
                .gasUsed(String.valueOf(RandomUtil.randomLong()))
                .gasLimit(String.valueOf(RandomUtil.randomLong()))
                .blobGasUsed(null)  // null field
                .excessBlobGas(null)  // null field
                .build();

        // Should handle null fields gracefully
        oracleRepository1.saveBlockFeeInfo(mockL1BlockFee);

        OracleRequestDO result = oracleRepository1.peekRequestByTypeAndIndex(
                OracleTypeEnum.L2_GAS_ORACLE,
                OracleRequestTypeEnum.L1_BLOCK_UPDATE,
                String.valueOf(mockNumber)
        );
        Assert.assertNotNull(result);
    }

    /**
     * Test peek requests with zero limit
     * Verifies that zero limit returns empty list
     */
    @Test
    @SneakyThrows
    public void testPeekRequests_ZeroLimit() {
        // Save some data first
        int mockNumber = RandomUtil.randomInt();
        L1BlockFeeInfo mockL1BlockFee = L1BlockFeeInfo.builder()
                .number(String.valueOf(mockNumber))
                .baseFeePerGas(String.valueOf(RandomUtil.randomLong()))
                .gasUsed(String.valueOf(RandomUtil.randomLong()))
                .gasLimit(String.valueOf(RandomUtil.randomLong()))
                .blobGasUsed(String.valueOf(RandomUtil.randomLong()))
                .excessBlobGas(String.valueOf(RandomUtil.randomLong()))
                .build();
        oracleRepository1.saveBlockFeeInfo(mockL1BlockFee);

        // Query with zero limit
        List<OracleRequestDO> result = oracleRepository1.peekRequests(
                OracleTypeEnum.L2_GAS_ORACLE,
                OracleRequestTypeEnum.L1_BLOCK_UPDATE,
                OracleTransactionStateEnum.INIT,
                0
        );
        Assert.assertEquals(0, result.size());
    }

    /**
     * Test multiple state transitions
     * Verifies that request state can be updated multiple times
     */
    @Test
    @SneakyThrows
    public void testUpdateRequestState_MultipleTransitions() {
        int mockNumber = RandomUtil.randomInt();
        L1BlockFeeInfo mockL1BlockFee = L1BlockFeeInfo.builder()
                .number(String.valueOf(mockNumber))
                .baseFeePerGas(String.valueOf(RandomUtil.randomLong()))
                .gasUsed(String.valueOf(RandomUtil.randomLong()))
                .gasLimit(String.valueOf(RandomUtil.randomLong()))
                .blobGasUsed(String.valueOf(RandomUtil.randomLong()))
                .excessBlobGas(String.valueOf(RandomUtil.randomLong()))
                .build();
        oracleRepository1.saveBlockFeeInfo(mockL1BlockFee);

        String index = String.valueOf(mockNumber);

        // Transition: INIT -> COMMITED
        oracleRepository1.updateRequestState(
                index,
                OracleTypeEnum.L2_GAS_ORACLE,
                OracleRequestTypeEnum.L1_BLOCK_UPDATE,
                OracleTransactionStateEnum.COMMITED
        );
        OracleRequestDO result1 = oracleRepository1.peekRequestByTypeAndIndex(
                OracleTypeEnum.L2_GAS_ORACLE,
                OracleRequestTypeEnum.L1_BLOCK_UPDATE,
                index
        );
        Assert.assertEquals(OracleTransactionStateEnum.COMMITED, result1.getTxState());

        // Transition: COMMITED -> SKIP
        oracleRepository1.updateRequestState(
                index,
                OracleTypeEnum.L2_GAS_ORACLE,
                OracleRequestTypeEnum.L1_BLOCK_UPDATE,
                OracleTransactionStateEnum.SKIP
        );
        OracleRequestDO result2 = oracleRepository1.peekRequestByTypeAndIndex(
                OracleTypeEnum.L2_GAS_ORACLE,
                OracleRequestTypeEnum.L1_BLOCK_UPDATE,
                index
        );
        Assert.assertEquals(OracleTransactionStateEnum.SKIP, result2.getTxState());
    }
}
