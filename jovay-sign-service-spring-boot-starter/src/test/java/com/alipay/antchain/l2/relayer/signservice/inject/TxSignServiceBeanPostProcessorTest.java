package com.alipay.antchain.l2.relayer.signservice.inject;

import com.alipay.antchain.l2.relayer.signservice.config.TxSignServiceProperties;
import com.alipay.antchain.l2.relayer.signservice.config.TxSignServicesProperties;
import com.alipay.antchain.l2.relayer.signservice.core.TxSignServiceFactory;
import jakarta.annotation.Resource;
import lombok.Getter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.web3j.crypto.RawTransaction;
import org.web3j.service.TxSignService;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(
        properties = {
                "jovay.sign-service.test.type=aliyun_kms",
                "jovay.sign-service.test.kms.endpoint=localhost",
                "jovay.sign-service.test1.type=web3j_native",
                "jovay.sign-service.test1.web3j-native.private-key=0x1234",
                "jovay.sign-service.test2.type=web3j_native",
                "jovay.sign-service.test2.web3j-native.private-key=0x1234",
                "jovay.sign-service.test3.type=web3j_native",
                "jovay.sign-service.test3.web3j-native.private-key=0x1234",
                "jovay.sign-service.test4.type=aliyun_kms",
                "jovay.sign-service.test4.kms.endpoint=localhost",
                "jovay.sign-service.test5.type=web3j_native",
                "jovay.sign-service.test5.web3j-native.private-key=0x1234",
                "test.feature.enabled=true",
                "test.da.type=BLOBS"
        },
        classes = {TxSignServiceBeanPostProcessorTest.TestConfig.class, TxSignServiceBeanPostProcessorTest.TestConfig2.class}
)
@DirtiesContext
@RunWith(SpringRunner.class)
public class TxSignServiceBeanPostProcessorTest {

    @Component
    @Getter
    static class TestConfig {

        private TxSignService test2;

        @JovayTxSignService("test")
        private TxSignService kmsTxSignService;

        @JovayTxSignService("test1")
        private TxSignService web3jTxSignService;

        @JovayTxSignService("test2")
        public void setTest2(TxSignService txSignService) {
            test2 = txSignService;
        }

        // Test conditional injection with property matching
        @JovayTxSignService(
                value = "test3",
                conditionalProperty = "test.feature.enabled",
                conditionalPropertyHavingValue = "true"
        )
        private TxSignService conditionalEnabledService;

        // Test conditional injection with property not matching
        @JovayTxSignService(
                value = "test4",
                conditionalProperty = "test.feature.enabled",
                conditionalPropertyHavingValue = "false"
        )
        private TxSignService conditionalDisabledService;

        // Test conditional injection with property matching BLOBS
        @JovayTxSignService(
                value = "test5",
                conditionalProperty = "test.da.type",
                conditionalPropertyHavingValue = "BLOBS"
        )
        private TxSignService conditionalBlobsService;

        // Test conditional injection with missing property and matchIfMissing=true
        @JovayTxSignService(
                value = "test1",
                conditionalProperty = "test.missing.property",
                conditionalPropertyMatchIfMissing = true
        )
        private TxSignService conditionalMissingMatchService;

        // Test conditional injection with missing property and matchIfMissing=false
        @JovayTxSignService(
                value = "test4",
                conditionalProperty = "test.missing.property",
                conditionalPropertyMatchIfMissing = false
        )
        private TxSignService conditionalMissingNoMatchService;
    }

    @Configuration
    @EnableConfigurationProperties({TxSignServicesProperties.class})
    static class TestConfig2 {

        @Bean
        @Primary
        public static TxSignServiceBeanPostProcessor txSignServiceBeanPostProcessor(
                TxSignServicesProperties txSignServicesProperties,
                Environment environment
        ) {
            return new TxSignServiceBeanPostProcessor(txSignServicesProperties, txSignServiceFactory, environment);
        }
    }

    static class KmsMockService implements TxSignService {
        @Override
        public byte[] sign(RawTransaction rawTransaction, long chainId) {
            return new byte[0];
        }

        @Override
        public String getAddress() {
            return "";
        }
    }

    static class NativeMockService implements TxSignService {
        @Override
        public byte[] sign(RawTransaction rawTransaction, long chainId) {
            return new byte[0];
        }

        @Override
        public String getAddress() {
            return "";
        }
    }

    private static TxSignServiceFactory txSignServiceFactory = mock(TxSignServiceFactory.class);

    static {
        when(txSignServiceFactory.createTxSignService(anyString(), notNull())).then(
                invocationOnMock -> {
                    var prop = (TxSignServiceProperties) invocationOnMock.getArguments()[1];
                    return switch (prop.getType()) {
                        case ALIYUN_KMS -> new KmsMockService();
                        case WEB3J_NATIVE -> new NativeMockService();
                    };
                }
        );
    }

    @Resource
    private TestConfig testConfig;

    @Test
    public void testBasicInjection() {
        assertInstanceOf(KmsMockService.class, testConfig.getKmsTxSignService());
        assertInstanceOf(NativeMockService.class, testConfig.getWeb3jTxSignService());
        assertInstanceOf(NativeMockService.class, testConfig.getTest2());
    }

    @Test
    public void testConditionalInjectionWithPropertyMatching() {
        // Should be injected because test.feature.enabled=true
        assertInstanceOf(NativeMockService.class, testConfig.getConditionalEnabledService());
    }

    @Test
    public void testConditionalInjectionWithPropertyNotMatching() {
        // Should NOT be injected because test.feature.enabled=true but expecting false
        assert testConfig.getConditionalDisabledService() == null;
    }

    @Test
    public void testConditionalInjectionWithBlobsProperty() {
        // Should be injected because test.da.type=BLOBS
        assertInstanceOf(NativeMockService.class, testConfig.getConditionalBlobsService());
    }

    @Test
    public void testConditionalInjectionWithMissingPropertyMatchIfMissing() {
        // Should be injected because matchIfMissing=true
        assertInstanceOf(NativeMockService.class, testConfig.getConditionalMissingMatchService());
    }

    @Test
    public void testConditionalInjectionWithMissingPropertyNoMatch() {
        // Should NOT be injected because property is missing and matchIfMissing=false
        assert testConfig.getConditionalMissingNoMatchService() == null;
    }
}
