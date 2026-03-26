/*
 * Copyright 2026 Ant Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alipay.antchain.l2.relayer.signservice.autocofigure;

import com.alipay.antchain.l2.relayer.signservice.config.TxSignServicesProperties;
import com.alipay.antchain.l2.relayer.signservice.core.TxSignServiceFactory;
import com.alipay.antchain.l2.relayer.signservice.inject.TxSignServiceBeanPostProcessor;
import org.junit.Test;
import org.springframework.core.env.Environment;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class TxSignServiceAutoConfigurationTest {

    @Test
    public void testTxSignServiceFactory() {
        TxSignServiceFactory factory = TxSignServiceAutoConfiguration.txSignServiceFactory();
        assertNotNull("TxSignServiceFactory should not be null", factory);
    }

    @Test
    public void testTxSignServiceBeanPostProcessor() {
        TxSignServicesProperties properties = mock(TxSignServicesProperties.class);
        TxSignServiceFactory factory = mock(TxSignServiceFactory.class);
        Environment environment = mock(Environment.class);

        TxSignServiceBeanPostProcessor processor = TxSignServiceAutoConfiguration.txSignServiceBeanPostProcessor(
                properties, factory, environment
        );

        assertNotNull("TxSignServiceBeanPostProcessor should not be null", processor);
    }

    @Test
    public void testTxSignServiceBeanPostProcessorType() {
        TxSignServicesProperties properties = mock(TxSignServicesProperties.class);
        TxSignServiceFactory factory = mock(TxSignServiceFactory.class);
        Environment environment = mock(Environment.class);

        Object processor = TxSignServiceAutoConfiguration.txSignServiceBeanPostProcessor(
                properties, factory, environment
        );

        assertTrue("Processor should be instance of TxSignServiceBeanPostProcessor",
                processor instanceof TxSignServiceBeanPostProcessor);
    }
}
