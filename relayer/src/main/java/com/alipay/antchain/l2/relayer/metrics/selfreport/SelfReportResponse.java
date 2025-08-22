package com.alipay.antchain.l2.relayer.metrics.selfreport;

import com.alibaba.fastjson.JSON;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SelfReportResponse {

    public static SelfReportResponse fromJson(String json) {
        return JSON.parseObject(json, SelfReportResponse.class);
    }

    private boolean isSuccess;

    private String errorMessage;
}
