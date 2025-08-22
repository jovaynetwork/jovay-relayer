package com.alipay.antchain.l2.relayer.core.blockchain;

import java.math.BigInteger;

import com.alipay.antchain.l2.relayer.commons.l2basic.L1MsgTransaction;
import com.alipay.antchain.l2.relayer.commons.models.TransactionInfo;

public interface L2ClientInterface {

    TransactionInfo sendL1MsgTx(L1MsgTransaction l1MsgTransaction);

    BigInteger queryL2MailboxPendingNonce();

    BigInteger queryL2MailboxLatestNonce();
}
