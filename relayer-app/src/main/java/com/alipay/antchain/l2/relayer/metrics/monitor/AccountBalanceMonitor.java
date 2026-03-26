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

package com.alipay.antchain.l2.relayer.metrics.monitor;

import java.math.BigDecimal;
import jakarta.annotation.Resource;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alipay.antchain.l2.relayer.core.blockchain.L1Client;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.utils.Convert;

@Component
@ConditionalOnProperty(name = "l2-relayer.metrics.blockchain.acc-balance-monitor.switch", havingValue = "true")
@Slf4j
public class AccountBalanceMonitor {

    @Resource
    private L1Client l1Client;

    @Value("${l2-relayer.metrics.blockchain.acc-balance-monitor.l1-balance-threshold:3.0}")
    private BigDecimal l1BalanceThreshold;

    @Scheduled(fixedRateString = "${l2-relayer.metrics.blockchain.acc-balance-monitor.l1-interval:10000}")
    public void monitorL1Acc() {
        try {
            checkAccount(l1Client.getLegacyPoolTxManager().getAddress());
            if (ObjectUtil.isNotNull(l1Client.getBlobPoolTxManager())) {
                checkAccount(l1Client.getBlobPoolTxManager().getAddress());
            }
        } catch (Throwable t) {
            log.error("failed to monitor l1 account balance", t);
        }
    }

    private void checkAccount(String address) {
        log.debug("try to monitor l1 account balance for {}", address);
        var bal = l1Client.queryAccountBalance(address, DefaultBlockParameterName.LATEST);
        if (ObjectUtil.isNull(bal)) {
            log.warn("query l1 account balance for {} and get null", address);
        }
        log.debug("l1 account balance for {} is {}", address, bal);

        if (new BigDecimal(bal).compareTo(Convert.toWei(l1BalanceThreshold, Convert.Unit.ETHER)) <= 0) {
            log.error("🚨 l1 account balance {} for {} is less than threshold {}",
                    Convert.fromWei(bal.toString(), Convert.Unit.ETHER), address, l1BalanceThreshold);
        } else {
            log.info("♻️ healthy l1 account balance {} for {}",
                    Convert.fromWei(bal.toString(), Convert.Unit.ETHER), address);
        }
    }
}
