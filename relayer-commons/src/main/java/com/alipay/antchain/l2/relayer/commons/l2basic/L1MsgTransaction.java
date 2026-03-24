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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjectUtil;
import org.web3j.abi.datatypes.Address;
import org.web3j.crypto.Sign;
import org.web3j.crypto.transaction.type.ITransaction;
import org.web3j.crypto.transaction.type.TransactionType;
import org.web3j.rlp.RlpDecoder;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpString;
import org.web3j.rlp.RlpType;
import org.web3j.utils.Bytes;
import org.web3j.utils.Numeric;

public class L1MsgTransaction implements ITransaction {

    public static final Address L1_MAILBOX_AS_SENDER = new Address("0x5100000000000000000000000000000000000000");

    public static final Address L2_MAILBOX_AS_RECEIVER = new Address("0x6100000000000000000000000000000000000000");

    public static L1MsgTransaction decode(byte[] raw) {
        Assert.equals(L1MsgRawTransactionWrapper.MAGIC_NUM, raw[0], "first byte supposed to be `0x7f` but not");
        RlpList rlpList = RlpDecoder.decode(Arrays.copyOfRange(raw, 1, raw.length));

        final RlpList values = (RlpList) rlpList.getValues().get(0);

        final BigInteger nonce = ((RlpString) values.getValues().get(0)).asPositiveBigInteger();
        final BigInteger gasLimit = ((RlpString) values.getValues().get(1)).asPositiveBigInteger();
        final String data = ((RlpString) values.getValues().get(2)).asString();

        return new L1MsgTransaction(nonce, gasLimit, data);
    }

    private final BigInteger nonce;

    private final BigInteger gasLimit;

    private final String data;

    public L1MsgTransaction(BigInteger nonce, BigInteger gasLimit, String data) {
        this.nonce = nonce;
        this.gasLimit = gasLimit;
        this.data = data;
    }

    @Override
    public List<RlpType> asRlpValues(Sign.SignatureData signatureData) {
        List<RlpType> result = new ArrayList<>();
        result.add(RlpString.create(getNonce()));
        result.add(RlpString.create(getGasLimit()));
        // value field will already be hex encoded, so we need to convert into binary first
        byte[] data = Numeric.hexStringToByteArray(getData());
        result.add(RlpString.create(data));

        if (signatureData != null && ObjectUtil.isNotEmpty(signatureData.getR())) {
            result.add(RlpString.create(Bytes.trimLeadingZeroes(signatureData.getV())));
            result.add(RlpString.create(Bytes.trimLeadingZeroes(signatureData.getR())));
            result.add(RlpString.create(Bytes.trimLeadingZeroes(signatureData.getS())));
        }

        return result;
    }

    @Override
    public BigInteger getNonce() {
        return nonce;
    }

    @Override
    public BigInteger getGasPrice() {
        return BigInteger.ZERO;
    }

    @Override
    public BigInteger getGasLimit() {
        return gasLimit;
    }

    @Override
    public String getTo() {
        return L2_MAILBOX_AS_RECEIVER.toString();
    }

    @Override
    public BigInteger getValue() {
        return BigInteger.ZERO;
    }

    @Override
    public String getData() {
        return data;
    }

    @Override
    public TransactionType getType() {
        // we're going to do with this type
        return TransactionType.LEGACY;
    }
}
