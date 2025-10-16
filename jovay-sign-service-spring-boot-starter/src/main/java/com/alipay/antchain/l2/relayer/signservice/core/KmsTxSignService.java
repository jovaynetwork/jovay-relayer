package com.alipay.antchain.l2.relayer.signservice.core;

import java.math.BigInteger;
import java.util.Base64;
import java.util.Objects;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.l2.relayer.commons.l2basic.L1MsgRawTransactionWrapper;
import com.alipay.antchain.l2.relayer.commons.utils.BytesUtils;
import com.alipay.antchain.l2.relayer.commons.utils.Utils;
import com.alipay.antchain.l2.relayer.signservice.config.KmsConfig;
import com.aliyun.kms20160120.Client;
import com.aliyun.kms20160120.models.AsymmetricSignRequest;
import com.aliyun.kms20160120.models.GetPublicKeyRequest;
import com.aliyun.kms20160120.models.ListKeyVersionsRequest;
import com.aliyun.teaopenapi.models.Config;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.web3j.crypto.*;
import org.web3j.service.TxSignService;
import org.web3j.utils.Numeric;

import static org.web3j.crypto.TransactionEncoder.*;

@Slf4j
public class KmsTxSignService implements TxSignService {

    private final Client kmsClient;

    private final KmsConfig kmsConfig;

    private String address;

    @SneakyThrows
    public KmsTxSignService(KmsConfig config) {
        var conf = new Config().setAccessKeyId(config.getAccessKeyId())
                .setAccessKeySecret(config.getAccessKeySecret())
                .setEndpoint(config.getEndpoint());
        if (ObjectUtil.isNotEmpty(StringUtils.trimLeadingCharacter(config.getCa(), '\n'))) {
            conf.setCa(StrUtil.replace(config.getCa(), "\\n", "\n"));
        }
        this.kmsClient = new Client(conf);
        this.kmsConfig = config;
        if (StrUtil.isEmpty(this.kmsConfig.getPublicKey())) {
            retrievePublicKey();
        }
    }

    @Override
    @SneakyThrows
    public byte[] sign(RawTransaction rawTransaction, long chainId) {
        // 获取key version id
        if (StrUtil.isEmpty(this.kmsConfig.getPrivateKeyVersionId())) {
            var createKeyRequest = new ListKeyVersionsRequest().setKeyId(this.kmsConfig.getPrivateKeyId());
            var versionResp = kmsClient.listKeyVersions(createKeyRequest);
            this.kmsConfig.setPrivateKeyVersionId(versionResp.getBody().getKeyVersions().getKeyVersion().get(0).getKeyVersionId());
        }

        byte[] encodedTransaction;
        if (rawTransaction instanceof L1MsgRawTransactionWrapper) {
            encodedTransaction = ((L1MsgRawTransactionWrapper) rawTransaction).encodeWithoutSig();
        } else {
            if (chainId > -1 && rawTransaction.getType().isLegacy()) {
                encodedTransaction = encode(rawTransaction, chainId);
            } else {
                if (rawTransaction.getTransaction().getType().isEip4844()) {
                    encodedTransaction = encode4844(rawTransaction);
                } else {
                    encodedTransaction = encode(rawTransaction);
                }
            }
        }

        var digest = Hash.sha3(encodedTransaction);
        // 签名
        var signature = signFromKms(digest);
        var publicKey = new BigInteger(Numeric.cleanHexPrefix(this.kmsConfig.getPublicKey()), 16);
        var signatureData = Sign.createSignatureData(signature, publicKey, digest);

        if (rawTransaction instanceof L1MsgRawTransactionWrapper) {
            return ((L1MsgRawTransactionWrapper) rawTransaction).encodeWithSig(createEip155SignatureData(signatureData, chainId));
        }
        return encode(rawTransaction, rawTransaction.getType().isLegacy() ? createEip155SignatureData(signatureData, chainId) : signatureData);
    }

    @Override
    public String getAddress() {
        if (StrUtil.isNotEmpty(this.address)) {
            return this.address;
        }
        var pubkey = new BigInteger(1, Numeric.hexStringToByteArray(this.kmsConfig.getPublicKey()));
        this.address = Numeric.prependHexPrefix(Keys.getAddress(pubkey));
        return this.address;
    }

    private ECDSASignature signFromKms(byte[] digest) throws Exception {
        var signRequest = new AsymmetricSignRequest()
                .setKeyId(this.kmsConfig.getPrivateKeyId())
                .setKeyVersionId(this.kmsConfig.getPrivateKeyVersionId())
                .setAlgorithm("ECDSA_SHA_256")
                .setDigest(Base64.getEncoder().encodeToString(digest));
        var response = kmsClient.asymmetricSign(signRequest);
        var signatureString = response.getBody().getValue();
        if (StrUtil.isEmpty(signatureString)) {
            throw new RuntimeException("signature is null");
        }

        var signBytes = Base64.getDecoder().decode(signatureString);
        return CryptoUtils.fromDerFormat(signBytes).toCanonicalised();
    }

    private void retrievePublicKey() {
        retrievePublicKeyByReq();
    }

    @SneakyThrows
    private void retrievePublicKeyByReq() {
        // get key version id
        if (StrUtil.isEmpty(this.kmsConfig.getPrivateKeyVersionId())) {
            var listKeyVersionsRequest = new ListKeyVersionsRequest().setKeyId(this.kmsConfig.getPrivateKeyId());
            var versionResp = kmsClient.listKeyVersions(listKeyVersionsRequest);
            this.kmsConfig.setPrivateKeyVersionId(versionResp.getBody().getKeyVersions().getKeyVersion().get(0).getKeyVersionId());
        }

        var request = new GetPublicKeyRequest();
        request.setKeyId(this.kmsConfig.getPrivateKeyId());
        request.setKeyVersionId(this.kmsConfig.getPrivateKeyVersionId());
        var pubKeyPem = kmsClient.getPublicKey(request).getBody().getPublicKey();
        Objects.requireNonNull(pubKeyPem, "publicKey is null");

        // recover PublicKey from public key pem
        this.kmsConfig.setPublicKey(Numeric.toHexString(BytesUtils.toUnsignedByteArray(64, Utils.getECPubkeyPoint(pubKeyPem))));
    }
}
