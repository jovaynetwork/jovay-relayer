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

package com.alipay.antchain.l2.relayer.commons.l2basic;

import java.math.BigInteger;
import java.util.List;

import cn.hutool.core.util.ObjectUtil;
import com.alipay.antchain.l2.relayer.commons.exceptions.InvalidBatchException;
import com.alipay.antchain.l2.relayer.commons.utils.RollupUtils;

public record ChunksPayload(BatchVersionEnum batchVersion, List<Chunk> chunks) implements IBatchPayload {

    @Override
    public byte[] serialize() {
        return RollupUtils.serializeChunks(batchVersion, chunks);
    }

    @Override
    public BigInteger getStartBlockNumber() {
        if (ObjectUtil.isEmpty(chunks)) {
            throw new RuntimeException("batch has no chunk");
        }
        return this.chunks.stream().map(Chunk::getStartBlockNumber).min(BigInteger::compareTo).get();
    }

    @Override
    public BigInteger getEndBlockNumber() {
        if (ObjectUtil.isEmpty(chunks)) {
            throw new RuntimeException("batch has no chunk");
        }
        return this.chunks.stream().map(Chunk::getEndBlockNumber).max(BigInteger::compareTo).get();
    }

    @Override
    public void validate() throws InvalidBatchException {
        BigInteger currEnd = null;
        for (int i = 0; i < chunks.size(); i++) {
            Chunk chunk = chunks.get(i);
            if (ObjectUtil.isNotNull(currEnd)) {
                if (!currEnd.add(BigInteger.ONE).equals(chunk.getStartBlockNumber())) {
                    throw new InvalidBatchException("discontinuous chunks block numbers: (chunk: {})", i);
                }
            }
            currEnd = chunk.getEndBlockNumber();
        }
    }

    @Override
    public long getRawTxTotalSize() {
        if (ObjectUtil.isEmpty(chunks)) {
            throw new RuntimeException("batch has no chunk");
        }
        long batchTxsLength = 0;
        for(Chunk chunk: chunks) {
            batchTxsLength += chunk.getL2RawTransactionTotalLength();
        }
        return batchTxsLength;
    }

    @Override
    public int getL2TxCount() {
        if (ObjectUtil.isEmpty(chunks)) {
            return 0;
        }
        return chunks.stream().mapToInt(c -> c.getL2TransactionList().size()).sum();
    }
}
