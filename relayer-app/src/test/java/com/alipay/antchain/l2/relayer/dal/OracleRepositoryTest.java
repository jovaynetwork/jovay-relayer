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
import org.springframework.beans.factory.annotation.Autowired;
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
    @Autowired
    private OracleRepository oracleRepository;

    public abstract class BlockMixIn {
        @JsonIgnore
        private String totalDifficulty;
    }

    @Before
    public void initMock() {
        when(l1Client.l1BlobNumLimit()).thenReturn(4L);

        when(rollupConfig.getGasPerChunk()).thenReturn(3000_0000);
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

        Assert.assertNull(
                oracleRepository1.peekLatestRequestIndex(OracleTypeEnum.L2_GAS_ORACLE, OracleRequestTypeEnum.L1_BLOCK_UPDATE, OracleTransactionStateEnum.COMMITED)
        );
        l1BlockFeeInfo.setNumber("3");
        oracleRepository1.saveBlockFeeInfo(l1BlockFeeInfo);
        oracleRepository1.updateRequestState("3", OracleTypeEnum.L2_GAS_ORACLE, OracleRequestTypeEnum.L1_BLOCK_UPDATE, OracleTransactionStateEnum.COMMITED);

        Assert.assertEquals(
                BigInteger.valueOf(3),
                oracleRepository1.peekLatestRequestIndex(OracleTypeEnum.L2_GAS_ORACLE, OracleRequestTypeEnum.L1_BLOCK_UPDATE)
        );
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
}
