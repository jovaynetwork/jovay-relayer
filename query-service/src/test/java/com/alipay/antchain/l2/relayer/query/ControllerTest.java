package com.alipay.antchain.l2.relayer.query;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;

import cn.hutool.core.collection.ListUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.alipay.antchain.l2.relayer.commons.enums.InterBlockchainMessageTypeEnum;
import com.alipay.antchain.l2.relayer.commons.l2basic.L2MsgProofData;
import com.alipay.antchain.l2.relayer.dal.entities.BatchesEntity;
import com.alipay.antchain.l2.relayer.dal.entities.InterBlockchainMessageEntity;
import com.alipay.antchain.l2.relayer.dal.mapper.BatchesMapper;
import com.alipay.antchain.l2.relayer.dal.mapper.InterBlockchainMessageMapper;
import com.alipay.antchain.l2.relayer.query.commons.BatchMeta;
import com.alipay.antchain.l2.relayer.query.commons.ErrorCode;
import com.alipay.antchain.l2.relayer.query.controller.req.QueryBatchMetaRequest;
import com.alipay.antchain.l2.relayer.query.controller.req.QueryL2MsgProofRequest;
import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

public class ControllerTest extends TestBase {

    private static final QueryL2MsgProofRequest QUERY_L_2_MSG_PROOF_REQUEST = new QueryL2MsgProofRequest(1L);

    private static final QueryBatchMetaRequest QUERY_BATCH_META_RANGE_REQUEST = new QueryBatchMetaRequest(
            ListUtil.of(BigInteger.valueOf(1L), BigInteger.valueOf(3L))
    );

    @Resource
    private InterBlockchainMessageMapper interBlockchainMessageMapper;

    @Resource
    private BatchesMapper batchesMapper;

    @Test
    @SneakyThrows
    public void testQueryL2MsgProof() {
        // no verification code
        MvcResult result = mockMvc.perform(
                MockMvcRequestBuilders.post("/api/relayer/query/l2-msg-proof")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(QUERY_L_2_MSG_PROOF_REQUEST))
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

        Map<String, String> respMap =
                JSON.parseObject(result.getResponse().getContentAsString(), new TypeReference<Map<String, String>>() {
                });

        Assert.assertEquals(
                "false",
                respMap.get("success")
        );
        Assert.assertEquals(
                ErrorCode.L2_MSG_PROOF_NOT_FOUND.getCode(),
                respMap.get("errorCode")
        );

        InterBlockchainMessageEntity entity = new InterBlockchainMessageEntity();
        entity.setType(InterBlockchainMessageTypeEnum.L2_MSG);
        entity.setNonce(1L);
        entity.setProof(new byte[]{1, 2, 3});

        interBlockchainMessageMapper.insert(entity);

        result = mockMvc.perform(
                MockMvcRequestBuilders.post("/api/relayer/query/l2-msg-proof")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(QUERY_L_2_MSG_PROOF_REQUEST))
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

        respMap = JSON.parseObject(result.getResponse().getContentAsString(), new TypeReference<Map<String, String>>() {});
        Assert.assertEquals(
                "true",
                respMap.get("success")
        );
        L2MsgProofData proofData = JSON.parseObject(respMap.get("data"), L2MsgProofData.class);
        Assert.assertArrayEquals(
                new byte[]{1, 2, 3},
                proofData.getMerkleProof()
        );
    }

    @Test
    @SneakyThrows
    public void testQueryBatchMetaRange() {
        var result = mockMvc.perform(
                MockMvcRequestBuilders.post("/api/relayer/query/batchmetarange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(QUERY_BATCH_META_RANGE_REQUEST))
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

        var respMap =
                JSON.parseObject(result.getResponse().getContentAsString(), new TypeReference<Map<String, String>>() {
                });

        Assert.assertEquals(
                "false",
                respMap.get("success")
        );
        Assert.assertEquals(
                ErrorCode.BATCH_NOT_FOUND.getCode(),
                respMap.get("errorCode")
        );

        var entity = new BatchesEntity();
        entity.setBatchIndex("1");
        entity.setStartNumber("1");
        entity.setEndNumber("100");
        batchesMapper.insert(entity);

        entity = new BatchesEntity();
        entity.setBatchIndex("3");
        entity.setStartNumber("200");
        entity.setEndNumber("300");
        batchesMapper.insert(entity);

        result = mockMvc.perform(
                MockMvcRequestBuilders.post("/api/relayer/query/batchmetarange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(QUERY_BATCH_META_RANGE_REQUEST))
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

        respMap = JSON.parseObject(result.getResponse().getContentAsString(), new TypeReference<Map<String, String>>() {});
        Assert.assertEquals(
                "true",
                respMap.get("success")
        );
        var resData = JSON.parseObject(respMap.get("data"), new TypeReference<List<BatchMeta>>(){});
        Assert.assertEquals(2, resData.size());
        Assert.assertEquals(BigInteger.valueOf(1L), resData.get(0).getBatchIndex());
        Assert.assertEquals(BigInteger.valueOf(3L), resData.get(1).getBatchIndex());
        Assert.assertEquals(new BigInteger("1"), resData.get(0).getStartBlock());
        Assert.assertEquals(new BigInteger("100"), resData.get(0).getEndBlock());
        Assert.assertEquals(new BigInteger("200"), resData.get(1).getStartBlock());
        Assert.assertEquals(new BigInteger("300"), resData.get(1).getEndBlock());
    }
}
