package com.alipay.antchain.l2.relayer.commons.specs.forks;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.annotation.JSONField;
import com.alipay.antchain.l2.relayer.commons.exceptions.InvalidRollupSpecsException;
import com.alipay.antchain.l2.relayer.commons.l2basic.BatchVersionEnum;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ForkInfo {

    @JSONField(name = "batch_version")
    private BatchVersionEnum batchVersion;

    public ForkInfo validate() throws InvalidRollupSpecsException {
        if (ObjectUtil.isNull(batchVersion)) {
            throw new InvalidRollupSpecsException("batch version is null");
        }
        return this;
    }
}
