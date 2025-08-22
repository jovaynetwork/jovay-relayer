package com.alipay.antchain.l2.relayer.dal.repository;

import java.util.Map;

public interface ISystemConfigRepository {

    String getSystemConfig(String key);

    boolean hasSystemConfig(String key);

    void setSystemConfig(Map<String, String> configs);

    void setSystemConfig(String key, String value);

    boolean isAnchorBatchSet();

    void markAnchorBatchHasBeenSet();
}
