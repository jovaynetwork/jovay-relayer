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

import cn.hutool.core.lang.Assert;
import com.alipay.antchain.l2.relayer.commons.l2basic.L1MsgTransaction;
import com.alipay.antchain.l2.relayer.core.blockchain.bpo.EthBlobForkConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.IGasPrice;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.SendTxResult;
import lombok.Getter;
import org.redisson.api.RedissonClient;
import org.web3j.abi.datatypes.Address;
import org.web3j.crypto.Blob;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.service.TxSignService;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

public class AcbRawTransactionManager extends BaseRawTransactionManager implements ITransactionManager {

    @Getter
    private final RemoteNonceManager nonceManager;

    public AcbRawTransactionManager(Web3j web3j, TxSignService txSignService, long chainId, RedissonClient redisson, EthBlobForkConfig ethBlobForkConfig) {
        super(web3j, txSignService, chainId, redisson, ethBlobForkConfig);
        this.nonceManager = new RemoteNonceManager(txSignService.getAddress(), web3j);
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
                return sendTx(gasPrice, gasLimit, to, data, nonceManager.getNextNonce(), value, constructor);
            } finally {
                getSendTxLock().unlock();
            }
        }
    }

    @Override
    public EthSendTransaction sendTransaction(BigInteger gasPrice, BigInteger gasLimit, String to, String data, BigInteger value, boolean constructor) throws IOException {
        getSendTxLock().lock();
        try {
            return super.sendTransaction(gasPrice, gasLimit, to, data, value, constructor);
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
            return sendTx(blobs, gasPrice, gasLimit, to, nonceManager.getNextNonce(), value, data);
        } finally {
            getSendTxLock().unlock();
        }
    }
}
