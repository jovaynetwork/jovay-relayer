package com.alipay.antchain.l2.relayer.core.blockchain;

import java.math.BigInteger;

import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

public interface BasicBlockchainClient {

    BigInteger queryLatestBlockNumber(DefaultBlockParameterName blockParameterName);

    TransactionReceipt queryTxReceipt(String txhash);

    Transaction queryTx(String txhash);

    EthSendTransaction sendRawTx(byte[] rawSignedTx);

    BigInteger queryTxCount(String address, DefaultBlockParameterName name);

    BigInteger queryAccountBalance(String address, DefaultBlockParameter blockParameter);
}
