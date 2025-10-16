package com.alipay.antchain.l2.relayer.signservice.inject;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface JovayTxSignService {

    String value() default "";
}
