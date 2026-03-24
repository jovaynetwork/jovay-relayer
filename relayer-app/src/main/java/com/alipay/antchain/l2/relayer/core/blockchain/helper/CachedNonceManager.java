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

package com.alipay.antchain.l2.relayer.core.blockchain.helper;

import java.math.BigInteger;
import java.util.regex.Pattern;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.l2.relayer.commons.enums.ChainTypeEnum;
import com.alipay.antchain.l2.relayer.dal.repository.IRollupRepository;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthSendTransaction;

@Slf4j
public class CachedNonceManager implements INonceManager {

    private final static Pattern BLOB_POOL_NONCE_TOO_HIGHT_PATTERN = Pattern.compile("nonce too high: tx nonce (\\d+), gapped nonce (\\d+)");

    private final RedissonClient redisson;

    private final RLock updateNonceLock;

    private final String account;

    private final long chainId;

    private final Web3j web3j;

    private final ChainTypeEnum chainType;

    private final IRollupRepository rollupRepository;

    public CachedNonceManager(RedissonClient redisson, Web3j web3j, long chainId, String account, ChainTypeEnum chainType, IRollupRepository rollupRepository) {
        this.redisson = redisson;
        this.updateNonceLock = redisson.getLock(getEthNonceLockKey(chainId, account));
        this.account = account;
        this.chainId = chainId;
        this.web3j = web3j;
        this.rollupRepository = rollupRepository;
        this.chainType = chainType;
    }

    @Override
    public BigInteger getNextNonce() {
        updateNonceLock.lock();
        try {
            var nonce = redisson.getAtomicLong(getEthNonceValKey(chainId, account));
            if (!nonce.isExists()) {
                var nonceFromChain = getNonceFromChain();
                log.info("read nonce {} from chain for {} and set it to redis", nonceFromChain, account);
                nonce.set(nonceFromChain.longValue());
                return nonceFromChain;
            }
            // get current nonce and the increments should be done after sending tx
            return BigInteger.valueOf(nonce.get());
        } finally {
            updateNonceLock.unlock();
        }
    }

    @Override
    public void incrementNonce() {
        updateNonceLock.lock();
        try {
            var nonce = redisson.getAtomicLong(getEthNonceValKey(chainId, account));
            if (!nonce.isExists()) {
                throw new RuntimeException(StrUtil.format("nonce {} not exist on redis!", getEthNonceValKey(chainId, account)));
            }
            nonce.incrementAndGet();
        } finally {
            updateNonceLock.unlock();
        }
    }

    @Override
    public boolean ifResetNonce(EthSendTransaction result) {
        if (result.getError().getCode() == -32000 && StrUtil.containsAny(result.getError().getMessage(), "nonce too low")) {
            return true;
        }
        var msgMatcher = BLOB_POOL_NONCE_TOO_HIGHT_PATTERN.matcher(result.getError().getMessage());
        if (msgMatcher.find()) {
            log.warn("rpc call to send blob tx returns that nonce too high: {}", result.getError().getMessage());
            var nonces = parseNonceFromError(result.getError().getMessage());
            if (ObjectUtil.isEmpty(nonces)) {
                return false;
            }
            log.info("parsed nonces from error message: tx nonce {}, gapped nonce {}", nonces[0], nonces[1]);
            var latestNonce = rollupRepository.queryLatestNonce(chainType, account);
            log.info("latest nonce from local storage: {}", latestNonce);
            return latestNonce.compareTo(nonces[1]) < 0;
        }
        return false;
    }

    @Override
    public void resetNonce() {
        updateNonceLock.lock();
        try {
            var nonceKey = getEthNonceValKey(chainId, account);
            log.info("try to reset nonce {} to redis", nonceKey);
            var nonce = redisson.getAtomicLong(nonceKey);
            if (!nonce.isExists()) {
                log.error("nonce {} not exist on redis!", nonceKey);
                return;
            }
            nonce.delete();
        } finally {
            updateNonceLock.unlock();
        }
    }

    public void setNonceIntoCache(BigInteger nonceToSet) {
        updateNonceLock.lock();
        try {
            log.info("set nonce {} to redis", nonceToSet);
            redisson.getAtomicLong(getEthNonceValKey(chainId, account)).set(nonceToSet.longValue());
        } finally {
            updateNonceLock.unlock();
        }
    }

    @SneakyThrows
    private BigInteger getNonceFromChain() {
        var ethGetTransactionCount =
                web3j.ethGetTransactionCount(this.account, DefaultBlockParameterName.PENDING).send();
        if (ethGetTransactionCount.hasError()) {
            throw new RuntimeException("failed to query tx count: " + ethGetTransactionCount.getError().getMessage());
        }
        return ethGetTransactionCount.getTransactionCount();
    }

    private BigInteger[] parseNonceFromError(String errorMessage) {
        if (errorMessage == null) {
            return null;
        }
        var matcher = BLOB_POOL_NONCE_TOO_HIGHT_PATTERN.matcher(errorMessage);
        if (matcher.find()) {
            var txNonce = new BigInteger(matcher.group(1));
            var gappedNonce = new BigInteger(matcher.group(2));
            return new BigInteger[]{txNonce, gappedNonce};
        }
        return null;
    }
}