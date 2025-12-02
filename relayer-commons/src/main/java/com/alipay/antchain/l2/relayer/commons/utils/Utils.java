package com.alipay.antchain.l2.relayer.commons.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.Security;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECPublicKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.KeyUtil;
import cn.hutool.crypto.PemUtil;
import cn.hutool.jwt.signers.AlgorithmUtil;
import com.alipay.antchain.l2.relayer.commons.l2basic.BlockContext;
import com.alipay.antchain.l2.trace.*;
import ethereum.ckzg4844.CKZG4844JNI;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes48;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jcajce.provider.asymmetric.util.EC5Util;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.math.ec.ECCurve;
import org.web3j.crypto.*;
import org.web3j.crypto.Hash;
import org.web3j.crypto.transaction.type.Transaction1559;
import org.web3j.crypto.transaction.type.Transaction2930;
import org.web3j.crypto.transaction.type.Transaction4844;
import org.web3j.rlp.RlpEncoder;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpString;
import org.web3j.rlp.RlpType;
import org.web3j.utils.Numeric;

public class Utils {

    public static BlockContext convertFromBlockTrace(BasicBlockTrace blockTrace) {
        return BlockContext.builder()
                .specVersion(blockTrace.getSpecVersion())
                .blockNumber(BigInteger.valueOf(blockTrace.getHeader().getNumber()))
                .baseFee(BigInteger.valueOf(blockTrace.getHeader().getBaseFeePerGas()))
                .gasLimit(BigInteger.valueOf(blockTrace.getHeader().getGasLimit()))
                .timestamp(blockTrace.getHeader().getTimestamp())
                .numTransactions(blockTrace.getTransactionsList().size())
                .numL1Messages((int) blockTrace.getTransactionsList().stream().filter(tx -> tx.getType() == TransactionType.TRANSACTION_TYPE_L1_MESSAGE).count())
                .build();
    }

    public static SignedRawTransaction convertFromLegacyProtoTx(LegacyTransaction protoTx) {
        return new SignedRawTransaction(
                BigInteger.valueOf(protoTx.getNonce()),
                BigInteger.valueOf(protoTx.getGasPrice()),
                BigInteger.valueOf(protoTx.getGas()),
                Numeric.toHexString(protoTx.getTo().toByteArray()),
                new BigInteger(1, protoTx.getValue().getValue().toByteArray()),
                HexUtil.encodeHexStr(protoTx.getData().toByteArray()),
                getSignatureData(protoTx.getV(), protoTx.getR(), protoTx.getS())
        );
    }

    public static SignedRawTransaction convertFromEip1559ProtoTx(EIP1559Transaction protoTx) {
        return new SignedRawTransaction(
                Transaction1559.createTransaction(
                        protoTx.getChainId(),
                        BigInteger.valueOf(protoTx.getNonce()),
                        BigInteger.valueOf(protoTx.getGas()),
                        Numeric.toHexString(protoTx.getTo().toByteArray()),
                        new BigInteger(1, protoTx.getValue().getValue().toByteArray()),
                        HexUtil.encodeHexStr(protoTx.getData().toByteArray()),
                        BigInteger.valueOf(protoTx.getMaxPriorityFeePerGas()),
                        BigInteger.valueOf(protoTx.getMaxFeePerGas()),
                        protoTx.getAccessList().getAccessListItemsList().stream()
                                .map(accessListItem -> new AccessListObject(
                                        Numeric.toHexString(accessListItem.getAddress().toByteArray()),
                                        accessListItem.getStorageKeysList().stream().map(x -> Numeric.toHexString(x.getValue().toByteArray())).collect(Collectors.toList())
                                )).collect(Collectors.toList())
                ),
                getSignatureData(protoTx.getV() + 27, protoTx.getR(), protoTx.getS())
        );
    }

    public static SignedRawTransaction convertFromEip2930ProtoTx(EIP2930Transaction protoTx) {
        return new SignedRawTransaction(
                Transaction2930.createTransaction(
                        protoTx.getChainId(),
                        BigInteger.valueOf(protoTx.getNonce()),
                        BigInteger.valueOf(protoTx.getGasPrice()),
                        BigInteger.valueOf(protoTx.getGas()),
                        Numeric.toHexString(protoTx.getTo().toByteArray()),
                        new BigInteger(1, protoTx.getValue().getValue().toByteArray()),
                        HexUtil.encodeHexStr(protoTx.getData().toByteArray()),
                        protoTx.getAccessList().getAccessListItemsList().stream()
                                .map(accessListItem -> new AccessListObject(
                                        Numeric.toHexString(accessListItem.getAddress().toByteArray()),
                                        accessListItem.getStorageKeysList().stream().map(x -> Numeric.toHexString(x.getValue().toByteArray())).collect(Collectors.toList())
                                )).collect(Collectors.toList())
                ),
                getSignatureData(protoTx.getV() + 27, protoTx.getR(), protoTx.getS())
        );
    }

    public static int getTxDataSize(Transaction tx) {
        switch (tx.getType()) {
            case TRANSACTION_TYPE_LEGACY:
                return tx.getLegacyTx().getData().size();
            case TRANSACTION_TYPE_EIP_2930:
                return tx.getEip2930Tx().getData().size();
            case TRANSACTION_TYPE_EIP_1559:
                return tx.getEip1559Tx().getData().size();
            case TRANSACTION_TYPE_L1_MESSAGE:
                return tx.getL1MsgTx().getData().size();
            default:
                throw new RuntimeException("not support tx type " + tx.getType());
        }
    }

    public static String calcEip4844TxHash(Transaction4844 transaction4844, Sign.SignatureData signatureData) {
        List<RlpType> resultTx = new ArrayList<>();

        resultTx.add(RlpString.create(transaction4844.getChainId()));
        resultTx.add(RlpString.create(transaction4844.getNonce()));
        resultTx.add(RlpString.create(transaction4844.getMaxPriorityFeePerGas()));
        resultTx.add(RlpString.create(transaction4844.getMaxFeePerGas()));
        resultTx.add(RlpString.create(transaction4844.getGasLimit()));

        String to = transaction4844.getTo();
        if (to != null && !to.isEmpty()) {
            resultTx.add(RlpString.create(Numeric.hexStringToByteArray(to)));
        } else {
            resultTx.add(RlpString.create(""));
        }

        resultTx.add(RlpString.create(transaction4844.getValue()));
        byte[] data = Numeric.hexStringToByteArray(transaction4844.getData());
        resultTx.add(RlpString.create(data));

        // access list
        resultTx.add(new RlpList());

        // Blob Transaction: max_fee_per_blob_gas and versioned_hashes
        resultTx.add(RlpString.create(transaction4844.getMaxFeePerBlobGas()));
        resultTx.add(new RlpList(transaction4844.getRlpVersionedHashes()));
        if (signatureData != null) {
            resultTx.add(RlpString.create(Sign.getRecId(signatureData, transaction4844.getChainId())));
            resultTx.add(
                    RlpString.create(
                            org.web3j.utils.Bytes.trimLeadingZeroes(signatureData.getR())));
            resultTx.add(
                    RlpString.create(
                            org.web3j.utils.Bytes.trimLeadingZeroes(signatureData.getS())));
        }

        var encoded = RlpEncoder.encode(new RlpList(resultTx));
        return Numeric.toHexString(Hash.sha3(
                ByteBuffer.allocate(encoded.length + 1)
                        .put(transaction4844.getType().getRlpType())
                        .put(encoded)
                        .array()
        ));
    }

    /**
     * EOA钱包地址私钥转KeyPair
     *
     *
     * @param privateKey
     * @return
     * @throws Exception
     */
    @SneakyThrows
    public static KeyPair convertKeyPair(String privateKey) {
        Security.addProvider(new BouncyCastleProvider());
        // 获取 secp256k1 曲线参数
        ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("secp256k1");

        ECCurve ecCurve = ecSpec.getCurve();

        var d = Numeric.toBigInt(privateKey);

        var ecPrivateKeySpec = new java.security.spec.ECPrivateKeySpec(d, EC5Util.convertSpec(EC5Util.convertCurve(ecCurve, null), ecSpec));

        KeyFactory keyFactory = KeyFactory.getInstance("ECDSA", "BC");

        var priKey = keyFactory.generatePrivate(ecPrivateKeySpec);

        var publicKeyPoint = Sign.publicPointFromPrivate(((BCECPrivateKey) priKey).getD());
        java.security.spec.ECPublicKeySpec publicKeySpec = new ECPublicKeySpec(EC5Util.convertPoint(publicKeyPoint), ecPrivateKeySpec.getParams());
        var pubKey = keyFactory.generatePublic(publicKeySpec);

        return new KeyPair(pubKey, priKey);
    }

    @SneakyThrows
    public static String convertPublicKeyToEthAddress(String pemStr) {
        return Numeric.prependHexPrefix(Keys.getAddress(getECPubkeyPoint(pemStr)));
    }

    public static BigInteger getECPubkeyPoint(@NonNull String pubkeyPem) throws IOException {
        var subjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(PemUtil.readPem(new ByteArrayInputStream(pubkeyPem.getBytes())));
        var pubKey = KeyUtil.generatePublicKey(
                AlgorithmUtil.getAlgorithm(subjectPublicKeyInfo.getAlgorithm().getAlgorithm().getId()),
                subjectPublicKeyInfo.getEncoded()
        );
        var x = ((ECPublicKey) pubKey).getW().getAffineX();
        var y = ((ECPublicKey) pubKey).getW().getAffineY();
        return x.shiftLeft(256).add(y);
    }

    private static Sign.SignatureData getSignatureData(long v, U256 r, U256 s) {
        Sign.SignatureData signatureData;
        if (v == 0) {
            if (!new BigInteger(1, s.getValue().toByteArray()).equals(BigInteger.ZERO)
                || !new BigInteger(1, r.getValue().toByteArray()).equals(BigInteger.ZERO)) {
                throw new RuntimeException(StrUtil.format("Unexpected sig value from tx : (v: {}, r: {}, s:{})",
                        v, HexUtil.encodeHexStr(r.getValue().toByteArray()), HexUtil.encodeHexStr(s.getValue().toByteArray())));
            }
            signatureData = new Sign.SignatureData(new byte[]{}, new byte[]{}, new byte[]{});
        } else {
            signatureData = new Sign.SignatureData(BigInteger.valueOf(v).toByteArray(), r.getValue().toByteArray(), s.getValue().toByteArray());
        }
        return signatureData;
    }

    public static List<Bytes> getCellProofs(Blob blobData) {
        var cellProofs = CKZG4844JNI.computeCellsAndKzgProofs(blobData.getData().toArray()).getProofs();
        Assert.isTrue(cellProofs.length == 128 * 48);
        var result = new ArrayList<Bytes>();
        for (int i = 0; i < cellProofs.length; i += 48) {
            result.add(Bytes48.wrap(Arrays.copyOfRange(cellProofs, i, i + 48)));
        }
        return result;
    }
}
