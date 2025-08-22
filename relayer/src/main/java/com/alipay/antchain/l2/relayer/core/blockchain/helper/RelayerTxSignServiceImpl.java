package com.alipay.antchain.l2.relayer.core.blockchain.helper;

import com.alipay.antchain.l2.relayer.commons.l2basic.L1MsgRawTransactionWrapper;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.Sign;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.service.TxSignService;

public class RelayerTxSignServiceImpl implements TxSignService {

    private final Credentials credentials;

    public RelayerTxSignServiceImpl(Credentials credentials) {
        this.credentials = credentials;
    }

    @Override
    public byte[] sign(RawTransaction rawTransaction, long chainId) {
        final byte[] signedMessage;

        if (rawTransaction instanceof L1MsgRawTransactionWrapper) {
            byte[] encodedTransaction = ((L1MsgRawTransactionWrapper) rawTransaction).encodeWithoutSig();
            Sign.SignatureData signatureData =
                    Sign.signMessage(encodedTransaction, credentials.getEcKeyPair());

            Sign.SignatureData eip155SignatureData = TransactionEncoder.createEip155SignatureData(signatureData, chainId);
            return ((L1MsgRawTransactionWrapper) rawTransaction).encodeWithSig(eip155SignatureData);
        } else {
            if (chainId > -1 && rawTransaction.getType().isLegacy()) {
                signedMessage = TransactionEncoder.signMessage(rawTransaction, chainId, credentials);
            } else {
                signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
            }
        }

        return signedMessage;
    }

    @Override
    public String getAddress() {
        return credentials.getAddress();
    }
}
