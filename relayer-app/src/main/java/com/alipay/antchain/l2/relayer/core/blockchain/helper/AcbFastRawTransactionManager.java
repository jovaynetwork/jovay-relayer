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

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjectUtil;
import com.alipay.antchain.l2.relayer.commons.enums.ChainTypeEnum;
import com.alipay.antchain.l2.relayer.commons.l2basic.L1MsgTransaction;
import com.alipay.antchain.l2.relayer.core.blockchain.bpo.EthBlobForkConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.IGasPrice;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.SendTxResult;
import com.alipay.antchain.l2.relayer.dal.repository.IRollupRepository;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.web3j.abi.datatypes.Address;
import org.web3j.crypto.Blob;
import org.web3j.crypto.RawTransaction;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.service.TxSignService;

@Slf4j
@Getter
public class AcbFastRawTransactionManager extends BaseRawTransactionManager implements ITransactionManager {

    private final String account;

    private final CachedNonceManager nonceManager;

    public AcbFastRawTransactionManager(Web3j web3j, TxSignService txSignService, long chainId, RedissonClient redisson,
                                        EthBlobForkConfig ethBlobForkConfig, ChainTypeEnum chainType, IRollupRepository rollupRepository) {
        super(web3j, txSignService, chainId, redisson, ethBlobForkConfig);
        this.account = txSignService.getAddress();
        this.nonceManager = new CachedNonceManager(redisson, web3j, chainId, account, chainType, rollupRepository);
    }

    @Override
    public SendTxResult sendTx(IGasPrice gasPrice, BigInteger gasLimit, String to, String data, BigInteger value, boolean constructor) throws IOException {

        if (L1MsgTransaction.L2_MAILBOX_AS_RECEIVER.equals(new Address(to))) {
            getSendL1MsgLock().lock();
            try {
                EthGetTransactionCount ethGetTransactionCount =
                        super.getWeb3j().ethGetTransactionCount(
                                        L1MsgTransaction.L1_MAILBOX_AS_SENDER.toString(), DefaultBlockParameterName.PENDING)
                                .send();
                return sendTx(gasPrice, gasLimit, to, data, ethGetTransactionCount.getTransactionCount(), value, constructor);
            } finally {
                getSendL1MsgLock().unlock();
            }
        } else {
            // do not use this method to send L1Msg
            Assert.notEquals(L1MsgTransaction.L2_MAILBOX_AS_RECEIVER, new Address(to));
            getSendTxLock().lock();
            try {
                var nonce = nonceManager.getNextNonce();
                SendTxResult result;
                try {
                    result = sendTx(gasPrice, gasLimit, to, data, nonce, value, constructor);
                } catch (Throwable t) {
                    log.error("unexpected exception when send tx, gonna to return the nonce {} of {}", nonce, this.account);
                    throw t;
                }
                if (ObjectUtil.isNull(result.getEthSendTransaction()) || result.getEthSendTransaction().hasError()) {
                    if (nonceManager.ifResetNonce(result.getEthSendTransaction())) {
                        nonceManager.resetNonce();
                        return result;
                    }
                } else {
                    nonceManager.incrementNonce();
                }
                return result;
            } finally {
                getSendTxLock().unlock();
            }
        }
    }

    @Override
    public EthSendTransaction sendTransaction(BigInteger gasPrice, BigInteger gasLimit, String to, String data, BigInteger value, boolean constructor) throws IOException {
        getSendTxLock().lock();
        try {
            var nonce = nonceManager.getNextNonce();
            RawTransaction rawTransaction =
                    RawTransaction.createTransaction(nonce, gasPrice, gasLimit, to, value, data);
            EthSendTransaction result = signAndSend(rawTransaction);
            if (ObjectUtil.isNull(result) || result.hasError()) {
                if (nonceManager.ifResetNonce(result)) {
                    nonceManager.resetNonce();
                    return result;
                }
            } else {
                nonceManager.incrementNonce();
            }
            return result;
        } finally {
            getSendTxLock().unlock();
        }
    }

    @Override
    public SendTxResult sendTx(
            List<Blob> blobs,
            IGasPrice gasPrice,
            BigInteger gasLimit,
            String to,
            BigInteger value,
            String data
    ) throws IOException {
        // do not use this method to send L1Msg
        Assert.notEquals(L1MsgTransaction.L2_MAILBOX_AS_RECEIVER, new Address(to));
        getSendTxLock().lock();
        try {
            var nonce = nonceManager.getNextNonce();
            SendTxResult result;
            try {
                result = sendTx(blobs, gasPrice, gasLimit, to, nonce, value, data);
            } catch (Throwable t) {
                log.error("unexpected exception when send 4844 tx, gonna to return the nonce {} of {}", nonce, this.account);
                throw t;
            }
            if (ObjectUtil.isNull(result.getEthSendTransaction()) || result.getEthSendTransaction().hasError()) {
                if (nonceManager.ifResetNonce(result.getEthSendTransaction())) {
                    nonceManager.resetNonce();
                    return result;
                }
            } else {
                nonceManager.incrementNonce();
            }
            return result;
        } finally {
            getSendTxLock().unlock();
        }
    }
}
