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

package com.alipay.antchain.l2.relayer.signservice.inject;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.alipay.antchain.l2.relayer.signservice.config.TxSignServicesProperties;
import com.alipay.antchain.l2.relayer.signservice.core.TxSignServiceFactory;
import lombok.NonNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;

public class TxSignServiceBeanPostProcessor implements BeanPostProcessor {

    private final TxSignServicesProperties txSignServicesProperties;

    private final TxSignServiceFactory txSignServiceFactory;

    public TxSignServiceBeanPostProcessor(
            @NonNull TxSignServicesProperties txSignServicesProperties,
            @NonNull TxSignServiceFactory txSignServiceFactory
    ) {
        this.txSignServicesProperties = txSignServicesProperties;
        this.txSignServiceFactory = txSignServiceFactory;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName)
            throws BeansException {
        Class<?> clazz = bean.getClass();
        do {
            processFields(clazz, bean);
            processMethods(clazz, bean);

            clazz = clazz.getSuperclass();
        } while (clazz != null);
        return bean;
    }

    /**
     * Processes the bean's fields in the given class.
     *
     * @param clazz The class to process.
     * @param bean The bean to process.
     */
    private void processFields(final Class<?> clazz, final Object bean) {
        for (final Field field : clazz.getDeclaredFields()) {
            final JovayTxSignService annotation = AnnotationUtils.findAnnotation(field, JovayTxSignService.class);
            if (annotation != null) {
                var name = annotation.value();
                var properties = txSignServicesProperties.getSignService().get(name);
                if (null == properties) {
                    throw new RuntimeException("tx sign service properties not found: " + name);
                }
                ReflectionUtils.makeAccessible(field);
                ReflectionUtils.setField(field, bean, txSignServiceFactory.createTxSignService(name, properties));
            }
        }
    }


    /**
     * Processes the bean's methods in the given class.
     *
     * @param clazz The class to process.
     * @param bean The bean to process.
     */
    private void processMethods(final Class<?> clazz, final Object bean) {
        for (final Method method : clazz.getDeclaredMethods()) {
            final JovayTxSignService annotation = AnnotationUtils.findAnnotation(method, JovayTxSignService.class);
            if (annotation != null) {
                final Class<?>[] paramTypes = method.getParameterTypes();
                if (paramTypes.length != 1) {
                    throw new BeanDefinitionStoreException(
                            "Method " + method + " doesn't have exactly one parameter.");
                }
                var name = annotation.value();
                var properties = txSignServicesProperties.getSignService().get(name);
                if (null == properties) {
                    throw new RuntimeException("tx sign service properties not found: " + name);
                }
                ReflectionUtils.makeAccessible(method);
                ReflectionUtils.invokeMethod(method, bean, txSignServiceFactory.createTxSignService(name, properties));
            }
        }
    }
}
