package com.alipay.antchain.l2.relayer.commons.l2basic;

import org.web3j.crypto.Hash;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.Sign;
import org.web3j.crypto.transaction.type.ITransaction;
import org.web3j.rlp.RlpEncoder;
import org.web3j.rlp.RlpList;

public class L1MsgRawTransactionWrapper extends RawTransaction {

    public static final byte MAGIC_NUM = 0x7f;

    public L1MsgRawTransactionWrapper(ITransaction transaction) {
        super(transaction);
    }

    public byte[] encodeWithoutSig() {
        return encodeWithSig(null);
    }

    public byte[] encodeWithSig(Sign.SignatureData signatureData) {
        RlpList rlpList = new RlpList(getTransaction().asRlpValues(signatureData));
        byte[] encoded = RlpEncoder.encode(rlpList);
        byte[] res = new byte[encoded.length + 1];
        res[0] = MAGIC_NUM;
        System.arraycopy(encoded, 0, res, 1, encoded.length);
        return res;
    }

    public byte[] calcHash() {
        return Hash.sha3(encodeWithoutSig());
    }
}
