package com.alipay.antchain.l2.relayer.engine.dynamicconf;

import java.util.List;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.serializer.ValueFilter;

public record ValueDesensitizeFilter(List<String> sensitiveKeys) implements ValueFilter {

    @Override
    public Object process(Object object, String name, Object value) {
        if (ObjectUtil.isNotEmpty(sensitiveKeys) && sensitiveKeys.contains(name) && ObjectUtil.isNotNull(value)) {
            return desensitize(value);
        }
        return value;
    }

    public Object desensitize(Object value) {
        // only string type we desensitize it.
        if (!(value instanceof String)) {
            return value;
        }
        return "******";
    }
}
