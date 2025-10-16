package com.alipay.antchain.l2.relayer.signservice.config;

import com.alipay.antchain.l2.relayer.signservice.core.TxSignServiceType;
import jakarta.annotation.Resource;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(SpringExtension.class)
@RunWith(SpringRunner.class)
@SpringBootTest(properties = {
        "jovay.sign-service.test.type=aliyun_kms",
        "jovay.sign-service.test.kms.endpoint=localhost",
        "jovay.sign-service.test1.type=web3j_native",
        "jovay.sign-service.test1.web3j-native.private-key=0x1234"
})
public class TxSignServicesPropertiesTest {

    @Resource
    private TxSignServicesProperties txSignServicesProperties;

    @Test
    public void test() {
        assertEquals(2, txSignServicesProperties.getSignService().size());

        assertNotNull(txSignServicesProperties.getSignService().get("test"));
        assertEquals(TxSignServiceType.ALIYUN_KMS, txSignServicesProperties.getSignService().get("test").getType());
        assertEquals("localhost", txSignServicesProperties.getSignService().get("test").getKms().getEndpoint());

        assertNotNull(txSignServicesProperties.getSignService().get("test1"));
        assertEquals(TxSignServiceType.WEB3J_NATIVE, txSignServicesProperties.getSignService().get("test1").getType());
        assertEquals("0x1234", txSignServicesProperties.getSignService().get("test1").getWeb3jNative().getPrivateKey());
    }
}
