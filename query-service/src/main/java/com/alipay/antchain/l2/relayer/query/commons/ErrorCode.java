package com.alipay.antchain.l2.relayer.query.commons;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    SUCCESS("0", "success"),

    L2_MSG_PROOF_NOT_FOUND("1001", "l2 msg proof not found"),

    BATCH_NOT_FOUND("1002", "batch not found: {}"),

    INTERNAL_ERROR("-1", "unexpected error");

    private final String code;

    private final String msg;
}
