package com.alipay.antchain.l2.relayer.commons.specs;

import com.alibaba.fastjson.annotation.JSONCreator;
import com.alibaba.fastjson.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum RollupSpecsNetwork {

    /**
     * For the Jovay mainnet
     */
    MAINNET("mainnet"),

    /**
     * For the Jovay testnet
     */
    TESTNET("testnet"),

    /**
     * For the Jovay private net
     */
    PRIVATE_NET("private_net");

    @JSONField
    private final String name;

    @JSONCreator
    public RollupSpecsNetwork from(String name) {
        return switch (name) {
            case "mainnet" -> MAINNET;
            case "testnet" -> TESTNET;
            case "private_net" -> PRIVATE_NET;
            default -> throw new IllegalStateException("Unexpected value: " + name);
        };
    }
}
