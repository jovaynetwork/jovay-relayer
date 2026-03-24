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

package com.alipay.antchain.l2.relayer.query.controller;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import jakarta.annotation.Resource;

import cn.hutool.core.util.ObjectUtil;
import com.alipay.antchain.l2.relayer.commons.l2basic.L2MsgProofData;
import com.alipay.antchain.l2.relayer.query.commons.BatchMeta;
import com.alipay.antchain.l2.relayer.query.commons.ErrorCode;
import com.alipay.antchain.l2.relayer.query.controller.req.QueryBatchMetaRequest;
import com.alipay.antchain.l2.relayer.query.controller.req.QueryL2MsgProofRequest;
import com.alipay.antchain.l2.relayer.query.controller.resp.QueryResponse;
import com.alipay.antchain.l2.relayer.query.dal.repo.IBatchDataRepository;
import com.alipay.antchain.l2.relayer.query.dal.repo.IMsgRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequestMapping("/api/relayer")
@RestController
public class RelayerDataController {

    @Resource
    private IMsgRepository msgRepository;

    @Resource
    private IBatchDataRepository batchDataRepository;

    @RequestMapping(value = "/query/l2-msg-proof", method = RequestMethod.POST)
    @ResponseBody
    public QueryResponse<L2MsgProofData> queryL2MsgProof(
            @RequestBody QueryL2MsgProofRequest queryL2MsgProofRequest,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        try {
            log.info("query l2 msg proof by nonce {} now", queryL2MsgProofRequest.getNonce());
            L2MsgProofData data = msgRepository.getL2MsgProof(queryL2MsgProofRequest.getNonce());
            if (ObjectUtil.isNull(data)) {
                log.info("none proof for nonce {} found", queryL2MsgProofRequest.getNonce());
                return QueryResponse.failed(ErrorCode.L2_MSG_PROOF_NOT_FOUND);
            }
            return QueryResponse.success(data);
        } catch (Throwable t) {
            log.error("query l2 msg proof error for nonce {}", queryL2MsgProofRequest.getNonce(), t);
            return QueryResponse.failed(ErrorCode.INTERNAL_ERROR);
        }
    }

    @RequestMapping(value = "/query/batchmetarange", method = RequestMethod.POST)
    @ResponseBody
    public QueryResponse<List<BatchMeta>> queryBatchMetaRange(
            @RequestBody QueryBatchMetaRequest queryBatchMetaRequest,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        try {
            if (ObjectUtil.isNull(queryBatchMetaRequest.getBatchIndexList())) {
                return QueryResponse.success(new ArrayList<>());
            }
            log.info("query batch metas by list {} now",
                    queryBatchMetaRequest.getBatchIndexList().stream().map(BigInteger::toString).collect(Collectors.joining(",")));
            List<BigInteger> missedBatchIndexes = Collections.synchronizedList(new ArrayList<>());
            List<CompletableFuture<BatchMeta>> futures = queryBatchMetaRequest.getBatchIndexList().stream()
                    .map(x -> CompletableFuture.supplyAsync(
                            () -> {
                                var res = batchDataRepository.getBatchMeta(x);
                                if (ObjectUtil.isNull(res)) {
                                    missedBatchIndexes.add(x);
                                    return null;
                                }
                                return res;
                            }
                    )).toList();
            var resList = futures.stream().map(CompletableFuture::join).filter(ObjectUtil::isNotNull).toList();
            if (resList.size() != queryBatchMetaRequest.getBatchIndexList().size()) {
                var hint = missedBatchIndexes.stream().sorted().map(BigInteger::toString).collect(Collectors.joining(",", "[", "]"));
                log.warn("none batch found for indexes {} found", hint);
                return QueryResponse.failed(ErrorCode.BATCH_NOT_FOUND, hint);
            }
            return QueryResponse.success(resList);
        } catch (Throwable t) {
            log.error("query batch metas failed: ", t);
            return QueryResponse.failed(ErrorCode.INTERNAL_ERROR);
        }
    }
}
