package com.alipay.antchain.l2.relayer.signservice.core;

import java.math.BigInteger;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.ReflectUtil;
import com.alipay.antchain.l2.relayer.signservice.config.KmsConfig;
import com.aliyun.kms20160120.Client;
import com.aliyun.kms20160120.models.*;
import lombok.SneakyThrows;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.web3j.crypto.RawTransaction;

import static org.mockito.Mockito.*;

public class KmsTxSignServiceTest {

    private KmsTxSignService kmsTxSignService;

    @Test
    @SneakyThrows
    public void test() {
        var kmsConf = new KmsConfig();
        kmsConf.setEndpoint("localhost");
        kmsConf.setAccessKeyId("accessKeyId");
        kmsConf.setAccessKeySecret("accessKeySecret");
        kmsConf.setPrivateKeyId("privateKeyId");
        kmsConf.setCa("ca");
        kmsConf.setPublicKey("publicKey");

        kmsTxSignService = new KmsTxSignService(kmsConf);

        var mockKmsClient = mock(Client.class);
        var kmsClientField = ReflectUtil.getField(KmsTxSignService.class, "kmsClient");
        kmsClientField.setAccessible(true);
        kmsClientField.set(kmsTxSignService, mockKmsClient);

        var listKeyVersionsResponse = new ListKeyVersionsResponse();
        var listKeyVersionsResponseBody = new ListKeyVersionsResponseBody();
        var versions = new ListKeyVersionsResponseBody.ListKeyVersionsResponseBodyKeyVersions();
        var version = new ListKeyVersionsResponseBody.ListKeyVersionsResponseBodyKeyVersionsKeyVersion();
        version.setKeyVersionId("keyVersionId");
        versions.setKeyVersion(ListUtil.toList(version));
        listKeyVersionsResponseBody.setKeyVersions(versions);
        listKeyVersionsResponse.setBody(listKeyVersionsResponseBody);

        var getPublicKeyResponseBody = new GetPublicKeyResponseBody();
        getPublicKeyResponseBody.setPublicKey("""
                -----BEGIN PUBLIC KEY-----
                MFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAEQJ74liqhPPUkFeNkfokfRLGgnU7wdZht
                1CnVXhBhddI2NCw1NVXCAKwt2RgTVoqPeeSWiT/AXgdTLDx5oVzcnQ==
                -----END PUBLIC KEY-----""");
        var getPublicKeyResponse = new GetPublicKeyResponse();
        getPublicKeyResponse.setBody(getPublicKeyResponseBody);

        when(mockKmsClient.listKeyVersions(notNull())).thenReturn(listKeyVersionsResponse);
        when(mockKmsClient.getPublicKey(notNull())).thenReturn(getPublicKeyResponse);

        var retrievePublicKeyMethod = ReflectUtil.getMethod(KmsTxSignService.class, "retrievePublicKey");
        retrievePublicKeyMethod.setAccessible(true);
        retrievePublicKeyMethod.invoke(kmsTxSignService);

        Assertions.assertEquals("0x409ef8962aa13cf52415e3647e891f44b1a09d4ef075986dd429d55e106175d236342c353555c200ac2dd91813568a8f79e496893fc05e07532c3c79a15cdc9d", kmsConf.getPublicKey());
        Assertions.assertEquals(version.getKeyVersionId(), kmsConf.getPrivateKeyVersionId());

        var rawTx = RawTransaction.createTransaction(
                BigInteger.ONE,
                BigInteger.ONE,
                BigInteger.ONE,
                "to",
                "data"
        );

        var asymmetricSignResponse = new AsymmetricSignResponse();
        var asymmetricSignResponseBody = new AsymmetricSignResponseBody();
        asymmetricSignResponseBody.setValue("MEYCIQCg8fzrWFk5LTxgW0zbxF+26+CH57m01gly0LnTc97cQwIhAPUxg9vo2oOovEVoYJ7gPzywMOdwXvUtdwZDJjkDmlri");
        asymmetricSignResponse.setBody(asymmetricSignResponseBody);
        when(mockKmsClient.asymmetricSign(notNull())).thenReturn(asymmetricSignResponse);

        Assertions.assertEquals(
                "f84c01010181ef8082dafa26a0a0f1fceb5859392d3c605b4cdbc45fb6ebe087e7b9b4d60972d0b9d373dedc43a00ace7c2417257c5743ba979f611fc0c20a7df576505372c4b98f3853cc9be65f",
                HexUtil.encodeHexStr(kmsTxSignService.sign(rawTx, 1))
        );
    }
}
