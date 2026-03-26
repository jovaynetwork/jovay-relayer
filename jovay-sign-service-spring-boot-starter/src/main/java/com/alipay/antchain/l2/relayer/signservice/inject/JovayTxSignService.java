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

import java.lang.annotation.*;

/**
 * Annotation for injecting Jovay transaction signing services.
 * <p>
 * This annotation can be applied to fields, methods, or parameters to automatically inject
 * a configured {@link org.web3j.service.TxSignService} instance. The injection can be
 * conditional based on Spring Environment properties.
 * </p>
 * <p>
 * The annotation supports conditional injection similar to Spring's {@code @ConditionalOnProperty},
 * allowing services to be injected only when specific configuration properties match expected values.
 * </p>
 *
 * <h3>Basic Usage</h3>
 * <pre>{@code
 * @JovayTxSignService("mySignService")
 * private TxSignService txSignService;
 * }</pre>
 *
 * <h3>Conditional Injection</h3>
 * <pre>{@code
 * @JovayTxSignService(
 *     value = "blobPoolSignService",
 *     conditionalProperty = "l2-relayer.rollup.da-type",
 *     conditionalPropertyHavingValue = "BLOBS"
 * )
 * private TxSignService blobPoolTxSignService;
 * }</pre>
 *
 * @see TxSignServiceBeanPostProcessor
 * @since 0.11.0
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface JovayTxSignService {

    /**
     * The name of the transaction signing service to inject.
     * <p>
     * This value corresponds to a key in the {@code jovay.sign-service} configuration properties.
     * For example, if the configuration contains {@code jovay.sign-service.myService.type=aliyun_kms},
     * then {@code value="myService"} will inject that configured service.
     * </p>
     *
     * @return the service name, must not be empty
     */
    String value() default "";

    /**
     * The names of the properties that must be present and match for the service to be injected.
     * <p>
     * If multiple properties are specified, all of them must match (AND logic).
     * If this array is empty, the service will always be injected (unless other conditions fail).
     * </p>
     * <p>
     * Example:
     * <pre>{@code
     * conditionalProperty = {"feature.enabled", "environment.type"}
     * }</pre>
     * </p>
     *
     * @return array of property names to check
     */
    String[] conditionalProperty() default {};

    /**
     * The expected value of the conditional property for the service to be injected.
     * <p>
     * If specified, the property value must exactly match this value for injection to occur.
     * If empty, the presence of the property is sufficient (any non-null value will match).
     * </p>
     * <p>
     * Example:
     * <pre>{@code
     * conditionalProperty = "l2-relayer.rollup.da-type",
     * conditionalPropertyHavingValue = "BLOBS"
     * }</pre>
     * This will only inject the service when {@code l2-relayer.rollup.da-type=BLOBS}.
     * </p>
     *
     * @return the expected property value
     */
    String conditionalPropertyHavingValue() default "";

    /**
     * Whether to inject the service if the conditional property is missing.
     * <p>
     * If {@code true}, the service will be injected even if the specified property is not present
     * in the Spring Environment. If {@code false}, the property must be present for injection.
     * </p>
     * <p>
     * This is useful for providing default behavior when a configuration property is optional.
     * </p>
     * <p>
     * Example:
     * <pre>{@code
     * @JovayTxSignService(
     *     value = "defaultService",
     *     conditionalProperty = "optional.feature",
     *     conditionalPropertyMatchIfMissing = true
     * )
     * }</pre>
     * This will inject the service even if {@code optional.feature} is not configured.
     * </p>
     *
     * @return {@code true} to inject when property is missing, {@code false} otherwise
     */
    boolean conditionalPropertyMatchIfMissing() default false;
}
