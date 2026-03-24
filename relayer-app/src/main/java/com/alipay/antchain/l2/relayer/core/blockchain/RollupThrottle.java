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

package com.alipay.antchain.l2.relayer.core.blockchain;

import java.math.BigInteger;
import jakarta.annotation.Resource;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.web3j.protocol.core.DefaultBlockParameterName;

@Component
@Slf4j
public class RollupThrottle {

    @Value("${l2-relayer.l1-client.blob-pool-tx-traffic-limit:16}")
    private int l1BlobPoolTxTrafficLimit;

    @Value("${l2-relayer.l1-client.legacy-pool-tx-traffic-limit:-1}")
    private int l1LegacyPoolTxTrafficLimit;

    @Resource(name = "l1Client")
    private L1Client l1Client;

    public boolean checkL1BlobPoolTraffic() {
        if (l1BlobPoolTxTrafficLimit < 0) {
            return true;
        }
        var pendingTxNum = l1Client.queryTxCount(l1Client.getBlobPoolTxManager().getAddress(), DefaultBlockParameterName.PENDING);
        var packagedTxNum = l1Client.queryTxCount(l1Client.getBlobPoolTxManager().getAddress(), DefaultBlockParameterName.LATEST);
        log.info("L1 blob pool traffic for account {} : pending tx num: {}, packaged tx num: {}",
                l1Client.getBlobPoolTxManager().getAddress(), pendingTxNum, packagedTxNum);
        return packagedTxNum.add(BigInteger.valueOf(l1BlobPoolTxTrafficLimit)).compareTo(pendingTxNum) > 0;
    }

    public boolean checkL1LegacyPoolTraffic() {
        if (l1LegacyPoolTxTrafficLimit < 0) {
            return true;
        }
        var pendingTxNum = l1Client.queryTxCount(l1Client.getLegacyPoolTxManager().getAddress(), DefaultBlockParameterName.PENDING);
        var packagedTxNum = l1Client.queryTxCount(l1Client.getLegacyPoolTxManager().getAddress(), DefaultBlockParameterName.LATEST);
        log.info("L1 legacy pool traffic for account {} : pending tx num: {}, packaged tx num: {}",
                l1Client.getLegacyPoolTxManager().getAddress(), pendingTxNum, packagedTxNum);
        return packagedTxNum.add(BigInteger.valueOf(l1LegacyPoolTxTrafficLimit)).compareTo(pendingTxNum) > 0;
    }
}
