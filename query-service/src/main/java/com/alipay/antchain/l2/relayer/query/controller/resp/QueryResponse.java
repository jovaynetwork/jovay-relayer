package com.alipay.antchain.l2.relayer.query.controller.resp;

import java.io.Serializable;

import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.l2.relayer.query.commons.ErrorCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@ToString(callSuper = true)
@Slf4j
@NoArgsConstructor
public class QueryResponse<T> implements Serializable {

    private static final long serialVersionUID = -3169584167980038283L;
    /**
     * 请求是否成功
     */
    private boolean success;

    /**
     * 异常码
     */
    private String errorCode;

    /**
     * 异常信息
     */
    private String errorMsg;

    /**
     * 请求结果数据
     */
    private T data;

    public QueryResponse(boolean success, String errorCode, String errorMsg) {
        this.success = success;
        this.errorCode = errorCode;
        this.errorMsg = errorMsg;
    }

    public QueryResponse(boolean success, T data) {
        this.success = success;
        this.data = data;
    }

    public static <T> QueryResponse<T> success(T data) {
        return new QueryResponse<>(true, data);
    }

    public static QueryResponse success() {
        return new QueryResponse<>(true, null);
    }

    public static QueryResponse failed() {
        return new QueryResponse(false, null, null);
    }

    public static QueryResponse failed(ErrorCode code) {
        return new QueryResponse(false, code.getCode(), code.getMsg());
    }

    public static QueryResponse failed(ErrorCode code, String msg) {
        return new QueryResponse(false, code.getCode(), StrUtil.format(code.getMsg(), msg));
    }
}
