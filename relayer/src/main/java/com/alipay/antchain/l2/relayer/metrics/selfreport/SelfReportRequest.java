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
public class SelfReportRequest {

    private String from;

    private String to;

    private long costTime;

    public String toJson() {
        return JSON.toJSONString(this);
    }
}
