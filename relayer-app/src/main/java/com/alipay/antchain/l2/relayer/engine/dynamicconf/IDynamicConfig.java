package com.alipay.antchain.l2.relayer.engine.dynamicconf;

public interface IDynamicConfig {

    String get(String key);

    void set(String key, String value);
}
