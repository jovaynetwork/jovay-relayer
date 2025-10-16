package com.alipay.antchain.l2.relayer.commons.specs;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.Map;

import cn.hutool.core.map.MapUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import com.alipay.antchain.l2.relayer.commons.specs.forks.ForkInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class RollupSpecs implements IRollupSpecs {

    public static RollupSpecs fromJson(String json) {
        return JSON.parseObject(json, RollupSpecs.class);
    }

    @JSONField(name = "network")
    private RollupSpecsNetwork network;

    @JSONField(name = "layer2_chain_id")
    private BigInteger layer2ChainId;

    @JSONField(name = "layer1_chain_id")
    private BigInteger layer1ChainId;

    @JSONField(name = "forks")
    private Map<BigInteger, ForkInfo> forks;

    @Override
    public ForkInfo getFork(BigInteger currBatchIndex) {
        for (var entry : MapUtil.sort(forks, Comparator.reverseOrder()).entrySet()) {
            if (entry.getKey().compareTo(currBatchIndex) <= 0) {
                return entry.getValue().validate();
            }
        }
        throw new RuntimeException("No fork found for batch index " + currBatchIndex);
    }
}
