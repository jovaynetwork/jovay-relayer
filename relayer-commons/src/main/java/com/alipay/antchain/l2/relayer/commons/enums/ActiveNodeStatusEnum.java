package com.alipay.antchain.l2.relayer.commons.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum ActiveNodeStatusEnum {

    ONLINE("online"),

    OFFLINE("offline");

    ActiveNodeStatusEnum(String code) {
        this.code = code;
    }

    @EnumValue
    private final String code;
}
