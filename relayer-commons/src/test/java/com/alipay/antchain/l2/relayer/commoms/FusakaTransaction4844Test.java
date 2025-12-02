package com.alipay.antchain.l2.relayer.commoms;

import java.nio.charset.Charset;

import cn.hutool.core.io.FileUtil;
import com.alipay.antchain.l2.relayer.commons.utils.EthTxDecoder;
import org.junit.Assert;
import org.junit.Test;
import org.web3j.crypto.SignedRawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.utils.Numeric;

public class FusakaTransaction4844Test {

    private static final String HEX_RAW_TX = FileUtil.readString("raw_eip4844_fusaka_tx", Charset.defaultCharset());

    @Test
    public void test() {
        var rawTx = (SignedRawTransaction) EthTxDecoder.decode(HEX_RAW_TX);
        Assert.assertArrayEquals(
                Numeric.hexStringToByteArray(HEX_RAW_TX),
                TransactionEncoder.encode(rawTx, rawTx.getSignatureData())
        );
    }
}
